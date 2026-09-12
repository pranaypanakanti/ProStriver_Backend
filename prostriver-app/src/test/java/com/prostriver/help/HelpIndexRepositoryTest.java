package com.prostriver.help;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The parts of the repository that can be proven without a database: the pgvector text form and
 * the dimension guard that stands in for the schema check {@code ddl-auto: validate} cannot do.
 */
class HelpIndexRepositoryTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final HelpIndexRepository repository = new HelpIndexRepository(jdbc);

    @Test
    void toVectorLiteral_embedding_rendersThePgvectorTextForm() {
        assertThat(HelpIndexRepository.toVectorLiteral(new float[]{0.5f, -0.25f, 0.0f}))
                .isEqualTo("[0.5,-0.25,0.0]");
    }

    @Test
    void toVectorLiteral_singleValue_hasNoTrailingSeparator() {
        assertThat(HelpIndexRepository.toVectorLiteral(new float[]{1.0f})).isEqualTo("[1.0]");
    }

    @Test
    void findNearest_wrongDimensionCount_isRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> repository.findNearest(new float[768]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("768")
                .hasMessageContaining("vector(1536)");

        verifyNoInteractions(jdbc);
    }

    @Test
    void upsert_wrongDimensionCount_isRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> repository.upsert("faq", "is prostriver free", new float[3]))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(jdbc);
    }

    @Test
    void findNearest_nullEmbedding_isRejected() {
        assertThatThrownBy(() -> repository.findNearest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");

        verifyNoInteractions(jdbc);
    }

    @Test
    void deleteRowsForUnknownSlugs_emptySlugSet_refusesRatherThanEmptyingTheIndex() {
        assertThatThrownBy(() -> repository.deleteRowsForUnknownSlugs(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty the index");

        verifyNoInteractions(jdbc);
    }

    @Test
    void deleteDroppedPhrasings_emptyPhrasingSet_refusesRatherThanRemovingTheDocument() {
        assertThatThrownBy(() -> repository.deleteDroppedPhrasings("faq", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("faq");

        verifyNoInteractions(jdbc);
    }

    @Test
    void findNearest_correctDimensionCount_passesTheGuardAndQueries() {
        assertThat(repository.findNearest(new float[HelpIndexRepository.EXPECTED_DIMENSIONS]))
                .as("the guard accepts 1536 dimensions, and an index with no matching rows is empty, not an error")
                .isEmpty();
    }
}
