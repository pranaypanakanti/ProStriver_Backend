package com.prostriver.help;

import com.prostriver.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The gate. Every mode is chosen from the retrieval score before any model call, so most of these
 * tests assert that OpenAI is never touched.
 *
 * <p>No Spring context, no network, no database.
 */
class HelpServiceTest {

    private static final double ANSWER_THRESHOLD = 0.78;
    private static final double SCOPE_THRESHOLD = 0.55;

    private static final float[] EMBEDDING = new float[HelpIndexRepository.EXPECTED_DIMENSIONS];

    private static final RagDocument FAQ = new RagDocument(
            "faq", "FAQ", "Answers to common questions.",
            List.of("is prostriver free"), "# Frequently Asked Questions\n\nYes, ProStriver is free.");

    private DocLoader docLoader;
    private HelpIndexRepository index;
    private OpenAiEmbeddingModel embeddingModel;
    private ChatClient chatClient;
    private HelpAnswerCache cache;
    private HelpService service;

    @BeforeEach
    void setUp() {
        docLoader = mock(DocLoader.class);
        index = mock(HelpIndexRepository.class);
        embeddingModel = mock(OpenAiEmbeddingModel.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        cache = mock(HelpAnswerCache.class);

        HelpProperties properties = new HelpProperties();
        properties.setAnswerThreshold(ANSWER_THRESHOLD);
        properties.setScopeThreshold(SCOPE_THRESHOLD);
        properties.setMaxQuestionChars(300);
        properties.setCacheTtlDays(7);
        properties.setRateLimitPerHour(30);

        when(cache.find(anyString())).thenReturn(Optional.empty());
        when(embeddingModel.embed(anyString())).thenReturn(EMBEDDING);

        service = new HelpService(docLoader, index, embeddingModel, chatClient, cache, properties);
    }

    // --- the three modes at their boundaries ---

    @Test
    void ask_similarityAtTheAnswerThreshold_answersFromTheDocument() {
        retrieves("faq", ANSWER_THRESHOLD);
        loads(FAQ);
        modelReturns(new GroundedAnswer(true, "Yes, ProStriver is free to use."));

        HelpAnswer answer = service.ask("is prostriver free");

        assertThat(answer.mode()).isEqualTo(HelpMode.ANSWERED);
        assertThat(answer.answer()).isEqualTo("Yes, ProStriver is free to use.");
        assertThat(answer.source()).isEqualTo(new DocSource("faq", "FAQ"));
        assertThat(answer.nearest()).as("nearest is for near misses only").isNull();
        assertThat(answer.confidence()).isEqualTo(ANSWER_THRESHOLD);
        assertThat(answer.cached()).isFalse();
    }

    @Test
    void ask_similarityJustBelowTheAnswerThreshold_isInsufficientAndNeverCallsTheModel() {
        retrieves("faq", 0.7799);

        HelpAnswer answer = service.ask("something almost covered");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.answer()).isEqualTo(HelpService.INSUFFICIENT_ANSWER);
        assertThat(answer.source()).isNull();
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_similarityAtTheScopeThreshold_isInsufficientAndNeverCallsTheModel() {
        retrieves("faq", SCOPE_THRESHOLD);

        HelpAnswer answer = service.ask("a distant question");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_similarityJustBelowTheScopeThreshold_isOutOfScopeAndNeverCallsTheModel() {
        retrieves("faq", 0.5499);

        HelpAnswer answer = service.ask("what is the capital of france");

        assertThat(answer.mode()).isEqualTo(HelpMode.OUT_OF_SCOPE);
        assertThat(answer.answer()).isEqualTo(HelpService.OUT_OF_SCOPE_ANSWER);
        assertThat(answer.source()).isNull();
        assertThat(answer.nearest()).as("out of scope offers no pointer").isNull();
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_indexReturnsNothing_isOutOfScopeAndNeverCallsTheModel() {
        when(index.findNearest(any())).thenReturn(Optional.empty());

        HelpAnswer answer = service.ask("anything at all");

        assertThat(answer.mode()).isEqualTo(HelpMode.OUT_OF_SCOPE);
        assertThat(answer.confidence()).isZero();
        verifyNoInteractions(chatClient);
    }

    // --- the residual cases ---

    @Test
    void ask_answeredBandButTheDocumentIsNotLoaded_isInsufficientAndNeverCallsTheModel() {
        retrieves("deleted-doc", 0.95);
        when(docLoader.findBySlug("deleted-doc")).thenReturn(Optional.empty());

        HelpAnswer answer = service.ask("a question whose document went away");

        assertThat(answer.mode())
                .as("a stale index row has no body to ground an answer in")
                .isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.answer()).isEqualTo(HelpService.INSUFFICIENT_ANSWER);
        assertThat(answer.source()).isNull();
        assertThat(answer.confidence()).isEqualTo(0.95);
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_modelSaysTheAnswerIsNotThere_isInsufficientAndDiscardsTheAnswerText() {
        retrieves("faq", 0.91);
        loads(FAQ);
        modelReturns(new GroundedAnswer(false, "Here is a guess the model should not have made."));

        HelpAnswer answer = service.ask("a detail the document does not carry");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.answer())
                .as("the model's text is discarded, not returned")
                .isEqualTo(HelpService.INSUFFICIENT_ANSWER)
                .doesNotContain("guess");
        assertThat(answer.source()).isNull();
    }

    @Test
    void ask_modelClaimsAnAnswerButReturnsBlankText_isInsufficient() {
        retrieves("faq", 0.91);
        loads(FAQ);
        modelReturns(new GroundedAnswer(true, "   "));

        assertThat(service.ask("is prostriver free").mode()).isEqualTo(HelpMode.INSUFFICIENT);
    }

    @Test
    void ask_modelReturnsNothingAtAll_isInsufficient() {
        retrieves("faq", 0.91);
        loads(FAQ);
        modelReturns(null);

        assertThat(service.ask("is prostriver free").mode()).isEqualTo(HelpMode.INSUFFICIENT);
    }

    // --- cache ---

    @Test
    void ask_cachedAnswer_isReturnedAsCachedWithoutEmbeddingOrRetrieving() {
        HelpAnswer stored = new HelpAnswer(HelpMode.ANSWERED, "Yes, it is free.",
                new DocSource("faq", "FAQ"), null, 0.9, false);
        when(cache.find("is prostriver free")).thenReturn(Optional.of(stored));

        HelpAnswer answer = service.ask("is prostriver free");

        assertThat(answer.cached()).isTrue();
        assertThat(answer.answer()).isEqualTo("Yes, it is free.");
        verifyNoInteractions(embeddingModel, index, chatClient);
    }

    @Test
    void ask_freshAnswer_isWrittenToTheCache() {
        retrieves("faq", 0.4);

        service.ask("what is the capital of france");

        verify(cache).put("what is the capital of france",
                new HelpAnswer(HelpMode.OUT_OF_SCOPE, HelpService.OUT_OF_SCOPE_ANSWER, null, null, 0.4, false));
    }

    // --- nearest, the near-miss pointer ---

    @Test
    void ask_borderlineInsufficient_reportsWhichDocumentAlmostMatched() {
        retrieves("faq", 0.7799);
        loads(FAQ);

        HelpAnswer answer = service.ask("something almost covered");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.source()).as("nothing was answered, so nothing is attributed").isNull();
        assertThat(answer.nearest()).isEqualTo(new DocSource("faq", "FAQ"));
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_modelSaysTheAnswerIsNotThere_stillReportsThatDocumentAsNearest() {
        retrieves("faq", 0.91);
        loads(FAQ);
        modelReturns(new GroundedAnswer(false, "a guess"));

        HelpAnswer answer = service.ask("a detail the document does not carry");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.source()).isNull();
        assertThat(answer.nearest()).isEqualTo(new DocSource("faq", "FAQ"));
    }

    @Test
    void ask_staleSlugWithNoLoadedDocument_hasNoNearestToReport() {
        retrieves("deleted-doc", 0.95);
        when(docLoader.findBySlug("deleted-doc")).thenReturn(Optional.empty());

        HelpAnswer answer = service.ask("a question whose document went away");

        assertThat(answer.mode()).isEqualTo(HelpMode.INSUFFICIENT);
        assertThat(answer.nearest())
                .as("there is no title to offer when the document is not loaded")
                .isNull();
    }

    // --- input validation ---

    @Test
    void ask_blankQuestion_isRejectedBeforeAnythingIsCalled() {
        assertThatThrownBy(() -> service.ask("   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be empty");

        verifyNoInteractions(embeddingModel, index, chatClient);
        verify(cache, never()).find(anyString());
    }

    @Test
    void ask_questionLongerThanTheLimit_isRejectedBeforeAnythingIsCalled() {
        assertThatThrownBy(() -> service.ask("x".repeat(301)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("300 characters or fewer");

        verifyNoInteractions(embeddingModel, index, chatClient);
    }

    @Test
    void ask_questionWithSurroundingWhitespace_isTrimmedBeforeUse() {
        retrieves("faq", 0.4);

        service.ask("  is prostriver free  ");

        verify(embeddingModel).embed("is prostriver free");
    }

    // --- helpers ---

    private void retrieves(String slug, double similarity) {
        when(index.findNearest(any())).thenReturn(Optional.of(new RetrievalHit(slug, "a phrasing", similarity)));
    }

    private void loads(RagDocument document) {
        when(docLoader.findBySlug(document.slug())).thenReturn(Optional.of(document));
    }

    @SuppressWarnings("unchecked")
    private void modelReturns(GroundedAnswer grounded) {
        when(chatClient.prompt()
                .system(any(Consumer.class))
                .user(any(Consumer.class))
                .call()
                .entity(GroundedAnswer.class))
                .thenReturn(grounded);
    }
}
