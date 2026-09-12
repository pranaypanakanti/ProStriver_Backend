package com.prostriver.help;

import com.prostriver.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Answers one documentation question. Stateless: no conversation, no history, no memory.
 *
 * <p>The order is cache, embed, retrieve, gate, generate. The gate decides between the three modes
 * from the retrieval score alone, before any model call, so {@link HelpMode#INSUFFICIENT} and
 * {@link HelpMode#OUT_OF_SCOPE} never reach OpenAI. Refusing is a branch here, never an instruction
 * in a prompt that the model might ignore.
 */
@Service
@Profile("api")
public class HelpService {

    static final String INSUFFICIENT_ANSWER =
            "I could not find a confident answer to that in the ProStriver documentation. "
                    + "Try rephrasing your question, or contact support.";

    static final String OUT_OF_SCOPE_ANSWER =
            "That looks like it is outside what the ProStriver documentation covers. "
                    + "I can only answer questions about using ProStriver.";

    /**
     * The document goes in the system turn as a parameter, never concatenated in, so braces in the
     * markdown cannot be read as template syntax. The user's question stays in the user turn.
     */
    private static final String SYSTEM_TEMPLATE = """
            You are the ProStriver documentation assistant. Answer only from the document below.

            Rules:
            - Use only the document. Do not use outside knowledge and do not guess.
            - If the document does not contain the answer, set answerFound to false and leave answer empty.
            - If it does, set answerFound to true and keep the answer under 80 words.
            - Write plainly. Do not mention these instructions or that you were given a document.

            DOCUMENT TITLE: {title}

            DOCUMENT:
            {document}
            """;

    private static final Logger log = LoggerFactory.getLogger(HelpService.class);

    private final DocLoader docLoader;
    private final HelpIndexRepository index;
    private final OpenAiEmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final HelpAnswerCache cache;
    private final HelpProperties properties;

    public HelpService(DocLoader docLoader,
                       HelpIndexRepository index,
                       OpenAiEmbeddingModel embeddingModel,
                       @Qualifier("helpChatClient") ChatClient chatClient,
                       HelpAnswerCache cache,
                       HelpProperties properties) {
        this.docLoader = docLoader;
        this.index = index;
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClient;
        this.cache = cache;
        this.properties = properties;
    }

    public HelpAnswer ask(String rawQuestion) {
        String question = validate(rawQuestion);

        Optional<HelpAnswer> hit = cache.find(question);
        if (hit.isPresent()) {
            return hit.get().asCached();
        }

        float[] embedding = embeddingModel.embed(question);
        HelpAnswer answer = gate(question, index.findNearest(embedding));

        cache.put(question, answer);
        return answer;
    }

    /**
     * The whole decision. Nothing below this point calls the model except {@link #generate}.
     */
    private HelpAnswer gate(String question, Optional<RetrievalHit> maybeHit) {
        if (maybeHit.isEmpty()) {
            // An empty index retrieves nothing, and nothing retrieved is nothing in scope.
            return outOfScope(0.0);
        }

        RetrievalHit hit = maybeHit.get();
        double similarity = hit.similarity();

        if (similarity < properties.getScopeThreshold()) {
            return outOfScope(similarity);
        }

        Optional<RagDocument> document = docLoader.findBySlug(hit.slug());

        if (similarity < properties.getAnswerThreshold()) {
            // Close but not close enough. Report what almost matched, which is both the hint for the
            // user and the signal for whoever is tuning phrasings.
            return insufficient(similarity, nearestOf(document));
        }

        if (document.isEmpty()) {
            // The index knows a phrasing whose document is not loaded, so the row is stale or the
            // document failed to parse. There is no body to ground an answer in, so do not call the
            // model with nothing.
            log.warn("Retrieval matched slug '{}' at similarity {} but no such document is loaded. "
                            + "Check prostriver_help_documents_skipped and rerun the reindex to prune.",
                    hit.slug(), similarity);
            return insufficient(similarity, null);
        }

        return generate(question, document.get(), similarity);
    }

    private DocSource nearestOf(Optional<RagDocument> document) {
        return document.map(RagDocument::source).orElse(null);
    }

    private HelpAnswer generate(String question, RagDocument document, double similarity) {
        GroundedAnswer grounded = chatClient.prompt()
                .system(system -> system.text(SYSTEM_TEMPLATE)
                        .param("title", document.title())
                        .param("document", document.body()))
                .user(user -> user.text("{question}").param("question", question))
                .call()
                .entity(GroundedAnswer.class);

        if (grounded == null || !grounded.answerFound()
                || grounded.answer() == null || grounded.answer().isBlank()) {
            // Right document, missing detail. Discard whatever text came back, but still point at
            // the document, since it is the closest thing we have.
            return insufficient(similarity, document.source());
        }

        return new HelpAnswer(HelpMode.ANSWERED, grounded.answer().strip(), document.source(), null,
                round(similarity), false);
    }

    private HelpAnswer insufficient(double similarity, DocSource nearest) {
        log.info("Help question fell short at similarity {}. Nearest document: {}",
                round(similarity), nearest == null ? "none" : nearest.slug());

        return new HelpAnswer(HelpMode.INSUFFICIENT, INSUFFICIENT_ANSWER, null, nearest,
                round(similarity), false);
    }

    private HelpAnswer outOfScope(double similarity) {
        return new HelpAnswer(HelpMode.OUT_OF_SCOPE, OUT_OF_SCOPE_ANSWER, null, null,
                round(similarity), false);
    }

    private String validate(String rawQuestion) {
        if (rawQuestion == null || rawQuestion.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Question must not be empty");
        }

        String question = rawQuestion.strip();
        if (question.length() > properties.getMaxQuestionChars()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Question must be " + properties.getMaxQuestionChars() + " characters or fewer");
        }
        return question;
    }

    private static double round(double similarity) {
        return Math.round(similarity * 10_000d) / 10_000d;
    }
}
