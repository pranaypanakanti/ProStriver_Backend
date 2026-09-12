package com.prostriver.help;

import java.util.List;

/**
 * One help documentation page, parsed from a markdown file under {@code help-docs/}.
 *
 * <p>{@code questions} are the user-phrased strings that get embedded, one index row each,
 * all pointing back at {@code slug}. The {@code body} is never embedded. It is handed to the
 * model verbatim, and only when retrieval has already decided the question is answerable.
 */
public record RagDocument(
        String slug,
        String title,
        String summary,
        List<String> questions,
        String body
) {

    public RagDocument {
        questions = List.copyOf(questions);
    }

    /**
     * The lightweight identity of this document, for the API response.
     */
    public DocSource source() {
        return new DocSource(slug, title);
    }
}
