package com.prostriver.help;

/**
 * The document a help answer came from, as returned to the client.
 * Null in the response when no document was used.
 */
public record DocSource(String slug, String title) {
}
