package com.prostriver.help;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parses the eight real help documents off the classpath, then the deliberately broken fixtures.
 * No Spring context, no network, no database.
 */
class DocLoaderTest {

    private static final int EXPECTED_DOCUMENTS = 8;

    /**
     * Bump this whenever a document's {@code questions:} block changes, and remember that the index
     * does not follow until the reindex command is run.
     */
    private static final int EXPECTED_PHRASINGS = 86;

    private static MeterRegistry realDocsRegistry;
    private static DocLoader loader;

    @BeforeAll
    static void loadRealDocsOnce() {
        realDocsRegistry = new SimpleMeterRegistry();
        loader = new DocLoader(realDocsRegistry);
    }

    // --- the real shipped documentation ---

    @Test
    void all_realHelpDocs_loadsEveryDocumentInSlugOrder() {
        assertThat(loader.all())
                .extracting(RagDocument::slug)
                .containsExactly(
                        "about-vision",
                        "faq",
                        "features",
                        "getting-efficient-study-plans",
                        "getting-started",
                        "team",
                        "tech-stack",
                        "troubleshooting-errors");
    }

    @Test
    void all_realHelpDocs_holdsEightyThreePhrasings() {
        int phrasings = loader.all().stream()
                .mapToInt(document -> document.questions().size())
                .sum();

        assertThat(loader.all()).hasSize(EXPECTED_DOCUMENTS);
        assertThat(phrasings)
                .as("total question phrasings to embed, one index row each")
                .isEqualTo(EXPECTED_PHRASINGS);
    }

    @Test
    void all_realHelpDocs_eachDocumentCarriesEightToFifteenQuestions() {
        for (RagDocument document : loader.all()) {
            assertThat(document.questions())
                    .as("questions in %s", document.slug())
                    .hasSizeBetween(8, 15)
                    .doesNotHaveDuplicates()
                    .allSatisfy(question -> assertThat(question).isNotBlank());
        }
    }

    @Test
    void all_realHelpDocs_everyDocumentIsFullyPopulated() {
        for (RagDocument document : loader.all()) {
            assertThat(document.slug()).isNotBlank();
            assertThat(document.title()).as("title of %s", document.slug()).isNotBlank();
            assertThat(document.summary()).as("summary of %s", document.slug()).isNotBlank();
            assertThat(document.body()).as("body of %s", document.slug()).isNotBlank();
        }
    }

    @Test
    void all_realHelpDocs_bodyExcludesTheFrontMatter() {
        for (RagDocument document : loader.all()) {
            assertThat(document.body())
                    .as("body of %s starts at the markdown heading, not the front matter", document.slug())
                    .doesNotStartWith("---")
                    .startsWith("# ");
        }
    }

    @Test
    void findBySlug_knownSlug_returnsTheParsedDocument() {
        RagDocument faq = loader.findBySlug("faq").orElseThrow();

        assertThat(faq.title()).isEqualTo("FAQ");
        assertThat(faq.summary()).isEqualTo("Answers to common questions about ProStriver.");
        assertThat(faq.questions())
                .hasSize(10)
                .contains("is prostriver free", "what is spaced repetition");
        assertThat(faq.body()).startsWith("# Frequently Asked Questions");
    }

    @Test
    void findBySlug_titleContainingAnAmpersand_keepsTheWholeTitle() {
        assertThat(loader.findBySlug("about-vision").orElseThrow().title())
                .isEqualTo("About & Vision");
        assertThat(loader.findBySlug("troubleshooting-errors").orElseThrow().title())
                .isEqualTo("Troubleshooting & Errors");
    }

    @Test
    void findBySlug_unknownSlug_returnsEmpty() {
        assertThat(loader.findBySlug("no-such-document")).isEmpty();
    }

    @Test
    void findBySlug_nullSlug_returnsEmpty() {
        assertThat(loader.findBySlug(null)).isEmpty();
    }

    @Test
    void source_anyDocument_carriesSlugAndTitle() {
        RagDocument team = loader.findBySlug("team").orElseThrow();

        assertThat(team.source()).isEqualTo(new DocSource("team", "Team"));
    }

    @Test
    void questions_returnedList_isImmutable() {
        List<String> questions = loader.findBySlug("faq").orElseThrow().questions();

        assertThat(questions).isUnmodifiable();
    }

    @Test
    void gauges_realHelpDocs_reportTheLoadedCounts() {
        assertThat(gauge(realDocsRegistry, "prostriver.help.documents.loaded")).isEqualTo(EXPECTED_DOCUMENTS);
        assertThat(gauge(realDocsRegistry, "prostriver.help.documents.skipped")).isZero();
        assertThat(gauge(realDocsRegistry, "prostriver.help.phrasings.loaded")).isEqualTo(EXPECTED_PHRASINGS);
    }

    // --- degrading instead of failing ---

    @Test
    void load_directoryWithMalformedFiles_skipsThemIndividuallyAndKeepsTheRest() {
        MeterRegistry registry = new SimpleMeterRegistry();

        DocLoader broken = new DocLoader("classpath*:help-docs-broken/*.md", registry);

        assertThat(broken.all())
                .as("only the one parseable fixture survives")
                .extracting(RagDocument::slug)
                .containsExactly("fixture-good");
        assertThat(broken.findBySlug("fixture-good").orElseThrow().questions()).hasSize(2);
    }

    @Test
    void load_directoryWithMalformedFiles_countsEverySkipIntoTheGauge() {
        MeterRegistry registry = new SimpleMeterRegistry();

        DocLoader broken = new DocLoader("classpath*:help-docs-broken/*.md", registry);

        assertThat(broken.skippedFiles())
                .as("no front matter, missing slug, empty questions, unparseable YAML")
                .isEqualTo(4);
        assertThat(gauge(registry, "prostriver.help.documents.skipped")).isEqualTo(4);
        assertThat(gauge(registry, "prostriver.help.documents.loaded")).isEqualTo(1);
    }

    @Test
    void load_twoDocumentsWithTheSameSlug_keepsOneAndSkipsTheOther() {
        MeterRegistry registry = new SimpleMeterRegistry();

        DocLoader duplicates = new DocLoader("classpath*:help-docs-duplicate/*.md", registry);

        assertThat(duplicates.all()).hasSize(1);
        assertThat(duplicates.findBySlug("twin")).isPresent();
        assertThat(duplicates.skippedFiles()).isEqualTo(1);
    }

    @Test
    void load_locationMatchingNoDocuments_leavesAnEmptyIndexWithoutThrowing() {
        MeterRegistry registry = new SimpleMeterRegistry();

        DocLoader empty = new DocLoader("classpath*:help-docs-empty/*.md", registry);

        assertThat(empty.all()).isEmpty();
        assertThat(empty.findBySlug("anything")).isEmpty();
        assertThat(empty.skippedFiles()).isZero();
        assertThat(gauge(registry, "prostriver.help.documents.loaded")).isZero();
    }

    private static int gauge(MeterRegistry registry, String name) {
        return (int) registry.get(name).gauge().value();
    }
}
