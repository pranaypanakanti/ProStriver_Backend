package com.prostriver.help;

/**
 * What the model is required to return, as structured output rather than prose.
 *
 * <p>{@code answerFound} exists so the residual case, where retrieval picked the right document but
 * that document does not contain the specific detail asked for, is a branch in our code rather than
 * something the model may or may not comply with. When it is false the {@code answer} text is
 * discarded and the canned insufficient response is returned instead.
 */
public record GroundedAnswer(boolean answerFound, String answer) {
}
