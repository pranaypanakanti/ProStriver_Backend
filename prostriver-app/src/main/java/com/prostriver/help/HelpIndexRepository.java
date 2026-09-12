package com.prostriver.help;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Native SQL access to {@code help_question_index}.
 *
 * <p>This table is deliberately NOT a JPA entity and is NOT behind a Spring Data repository.
 * {@code spring.jpa.hibernate.ddl-auto} is {@code validate} and Hibernate has no idea what a
 * pgvector {@code vector} column is, so mapping it would stop the whole application from booting.
 * Everything here goes through {@link NamedParameterJdbcTemplate} and hand written SQL.
 *
 * <p>The query vector is bound as {@link Types#OTHER}, which makes the PostgreSQL driver send it
 * with an unspecified type OID and lets the server apply pgvector's own input function. Binding it
 * as a normal string would make the cast fail, because there is no varchar to vector cast.
 */
@Repository
@Profile("api")
public class HelpIndexRepository {

    /**
     * Fixed by the live column definition, {@code embedding vector(1536)}, and by the configured
     * embedding model, {@code text-embedding-3-small}. Checked before every statement, because
     * {@code ddl-auto: validate} cannot check this column for us.
     */
    static final int EXPECTED_DIMENSIONS = 1536;

    /**
     * {@code <=>} is cosine distance, so similarity is {@code 1 - distance}. Ordering by the raw
     * distance operator is what lets the HNSW index serve the query.
     *
     * <p>KNOWN RISK, UNTESTED IN PRODUCTION. Binding the vector as {@link Types#OTHER} sends it with
     * an unspecified type OID, so the server has to infer the type at Parse time and apply pgvector's
     * input function. Our connection reaches Postgres through Supavisor in transaction mode on port
     * 6543 with {@code prepareThreshold=0}, and that combination of pooled transaction mode plus
     * unspecified-type inference has never been exercised here. If this query fails with a type
     * resolution or "could not determine data type" error, work through the fallbacks in order:
     *
     * <ol>
     *   <li>Point the connection at the direct Postgres port 5432 instead of the pooler and retry.
     *       This only tells you whether Supavisor is the cause, it is not the fix.</li>
     *   <li>Bind a {@code PGobject} with type {@code vector} instead of {@link Types#OTHER}. This is
     *       the proper fix. It needs {@code org.postgresql:postgresql} moved from {@code runtime} to
     *       compile scope in {@code prostriver-app/pom.xml}.</li>
     *   <li>Last resort only: format the vector numerically straight into the SQL string. It removes
     *       the type inference entirely, at the cost of losing parameterisation on this statement.
     *       Only acceptable because the values are floats we generated, never user input.</li>
     * </ol>
     */
    private static final String NEAREST_SQL = """
            SELECT doc_slug,
                   question_text,
                   1 - (embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM help_question_index
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT 1
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO help_question_index (doc_slug, question_text, embedding, created_at)
            VALUES (:slug, :question, CAST(:embedding AS vector), now())
            ON CONFLICT (doc_slug, question_text)
            DO UPDATE SET embedding = EXCLUDED.embedding
            """;

    private static final String COUNT_SQL = "SELECT count(*) FROM help_question_index";

    private static final String DELETE_UNKNOWN_SLUGS_SQL = """
            DELETE FROM help_question_index
            WHERE doc_slug NOT IN (:slugs)
            """;

    private static final String DELETE_DROPPED_PHRASINGS_SQL = """
            DELETE FROM help_question_index
            WHERE doc_slug = :slug
              AND question_text NOT IN (:questions)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public HelpIndexRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The single closest indexed phrasing, or empty when the index holds no rows at all.
     */
    public Optional<RetrievalHit> findNearest(float[] queryEmbedding) {
        requireDimensions(queryEmbedding);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("embedding", toVectorLiteral(queryEmbedding), Types.OTHER);

        List<RetrievalHit> hits = jdbc.query(NEAREST_SQL, parameters, (rs, rowNum) -> new RetrievalHit(
                rs.getString("doc_slug"),
                rs.getString("question_text"),
                rs.getDouble("similarity")));

        return hits.stream().findFirst();
    }

    /**
     * Writes one phrasing's embedding, replacing whatever was there for the same
     * {@code (doc_slug, question_text)}. {@code created_at} is left alone on update, since it
     * records when the row first appeared.
     */
    public int upsert(String docSlug, String questionText, float[] embedding) {
        requireDimensions(embedding);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("slug", docSlug)
                .addValue("question", questionText)
                .addValue("embedding", toVectorLiteral(embedding), Types.OTHER);

        return jdbc.update(UPSERT_SQL, parameters);
    }

    /**
     * Deletes every row whose {@code doc_slug} is not one of {@code slugs}.
     *
     * <p>REINDEX PATH ONLY. Never call this while serving a request. It is package private and
     * deliberately blunt: it assumes the caller has already proven that every document loaded and
     * embedded cleanly, because running it after a partial load wipes the surviving rows of any
     * document that failed.
     *
     * @throws IllegalArgumentException if {@code slugs} is empty, which would delete the whole index
     */
    int deleteRowsForUnknownSlugs(Collection<String> slugs) {
        if (slugs.isEmpty()) {
            throw new IllegalArgumentException("Refusing to prune against an empty slug set, that would empty the index");
        }
        return jdbc.update(DELETE_UNKNOWN_SLUGS_SQL, new MapSqlParameterSource("slugs", slugs));
    }

    /**
     * Deletes the rows of one document whose {@code question_text} is no longer among
     * {@code questions}, which is how an edited or removed phrasing stops being retrievable.
     *
     * <p>REINDEX PATH ONLY, with the same caveat as {@link #deleteRowsForUnknownSlugs(Collection)}.
     *
     * @throws IllegalArgumentException if {@code questions} is empty, which would delete the whole
     *                                  document from the index
     */
    int deleteDroppedPhrasings(String docSlug, Collection<String> questions) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Refusing to prune '" + docSlug + "' against an empty phrasing set, that would remove the document");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("slug", docSlug)
                .addValue("questions", questions);

        return jdbc.update(DELETE_DROPPED_PHRASINGS_SQL, parameters);
    }

    /**
     * Total rows in the index, used by the loader to report on itself.
     */
    public long countRows() {
        Long count = jdbc.getJdbcTemplate().queryForObject(COUNT_SQL, Long.class);
        return count == null ? 0L : count;
    }

    private void requireDimensions(float[] embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("Embedding must not be null");
        }
        if (embedding.length != EXPECTED_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Embedding has " + embedding.length + " dimensions but help_question_index.embedding is vector("
                            + EXPECTED_DIMENSIONS + "). Check spring.ai.openai.embedding.options.model.");
        }
    }

    /**
     * pgvector's text form, {@code [0.1,0.2,...]}.
     */
    static String toVectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder(embedding.length * 12 + 2);
        literal.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(embedding[i]);
        }
        return literal.append(']').toString();
    }
}
