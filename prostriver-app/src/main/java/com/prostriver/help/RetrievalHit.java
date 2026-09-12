package com.prostriver.help;

/**
 * The nearest indexed question phrasing to a user's question.
 *
 * <p>{@code similarity} is cosine similarity in {@code [-1, 1]}, already converted from the cosine
 * distance pgvector's {@code <=>} operator returns. Higher is closer. The gate reads this and
 * nothing else when it decides whether the model is allowed to be called.
 */
public record RetrievalHit(String slug, String questionText, double similarity) {
}
