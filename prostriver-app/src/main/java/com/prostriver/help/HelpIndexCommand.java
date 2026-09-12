package com.prostriver.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-off loader that embeds every question phrasing and upserts it into {@code help_question_index}.
 *
 * <p>Off unless {@code prostriver.help.reindex=true} is passed explicitly. The property is absent
 * from every yaml file on purpose, and {@link ConditionalOnProperty} does not match a missing
 * property, so this bean does not exist on a normal application start. Run it by hand:
 *
 * <pre>
 * java -jar prostriver.jar --spring.profiles.active=api --prostriver.help.reindex=true
 * </pre>
 *
 * <p>It does not stop the application afterwards. It logs what it did and hands control back.
 */
@Component
@Profile("api")
@ConditionalOnProperty(name = "prostriver.help.reindex", havingValue = "true")
public class HelpIndexCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HelpIndexCommand.class);

    private final DocLoader docLoader;
    private final OpenAiEmbeddingModel embeddingModel;
    private final HelpIndexRepository repository;

    public HelpIndexCommand(DocLoader docLoader,
                            OpenAiEmbeddingModel embeddingModel,
                            HelpIndexRepository repository) {
        this.docLoader = docLoader;
        this.embeddingModel = embeddingModel;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<RagDocument> documents = docLoader.all();

        if (documents.isEmpty()) {
            log.error("Reindex asked for, but no help documents loaded. Nothing to index.");
            return;
        }

        int expectedRows = documents.stream().mapToInt(document -> document.questions().size()).sum();
        log.info("Reindexing {} help documents, {} phrasings. Index currently holds {} rows.",
                documents.size(), expectedRows, repository.countRows());

        int upserted = 0;
        int failedDocuments = 0;

        for (RagDocument document : documents) {
            try {
                upserted += index(document);
            } catch (RuntimeException e) {
                failedDocuments++;
                log.error("Failed to index '{}': {}", document.slug(), e.getMessage(), e);
            }
        }

        report(documents, upserted, failedDocuments, expectedRows);
    }

    private int index(RagDocument document) {
        List<String> questions = document.questions();
        List<float[]> embeddings = embeddingModel.embed(questions);

        if (embeddings.size() != questions.size()) {
            throw new IllegalStateException("Asked for " + questions.size() + " embeddings but got "
                    + embeddings.size());
        }

        for (int i = 0; i < questions.size(); i++) {
            repository.upsert(document.slug(), questions.get(i), embeddings.get(i));
        }

        log.info("Indexed {} phrasings for '{}'", questions.size(), document.slug());
        return questions.size();
    }

    /**
     * Removes rows the documentation no longer contains: whole documents that are gone, and
     * individual phrasings that were edited or dropped. Without this, editing a phrasing leaves the
     * old one in the index where it still matches queries and still returns its slug, so removing a
     * bad phrasing would have no effect on retrieval.
     *
     * <p>Only ever called after a fully clean run. See {@link #run(ApplicationArguments)}.
     */
    private int prune(List<RagDocument> documents) {
        List<String> slugs = documents.stream().map(RagDocument::slug).toList();

        int pruned = repository.deleteRowsForUnknownSlugs(slugs);
        for (RagDocument document : documents) {
            pruned += repository.deleteDroppedPhrasings(document.slug(), document.questions());
        }
        return pruned;
    }

    private void report(List<RagDocument> documents, int upserted, int failedDocuments, int expectedRows) {
        if (failedDocuments > 0) {
            log.error("Reindex finished with {} failed document(s). {} phrasings were written.",
                    failedDocuments, upserted);
            log.warn("Skipping stale-row pruning because {} document(s) failed. Pruning against a partial "
                            + "load would delete every surviving row for those documents, and the help endpoint "
                            + "would quietly stop answering for them with no error anywhere. "
                            + "Fix the failures and run the reindex again.",
                    failedDocuments);
        } else {
            log.info("Reindex finished. {} phrasings written.", upserted);
            log.info("Pruned {} stale row(s).", prune(documents));
        }

        long rows = repository.countRows();
        if (rows > expectedRows) {
            log.warn("The index holds {} rows but the documentation only has {} phrasings. "
                            + "The extra {} row(s) are stale and point at phrasings or slugs that no longer exist. "
                            + "They can still be retrieved, and will be answered as insufficient.",
                    rows, expectedRows, rows - expectedRows);
        }
    }
}
