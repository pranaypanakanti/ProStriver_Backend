package com.prostriver.help;

/**
 * The request body for {@code POST /api/help/ask}.
 *
 * <p>Deliberately carries no Bean Validation annotations. {@code HelpService} validates emptiness
 * and length itself and signals with {@code ApiException}, so every non-200 response from this
 * endpoint comes back in one shape. Adding {@code @NotBlank} here would route blank questions
 * through {@code MethodArgumentNotValidException} instead, which returns a field-error map and would
 * give the client a third body shape to parse.
 */
public record HelpAskRequest(String question) {
}
