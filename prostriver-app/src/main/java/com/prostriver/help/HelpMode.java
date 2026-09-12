package com.prostriver.help;

/**
 * The outcome of a help question. Chosen by retrieval score before any model call, so two of these
 * three never reach OpenAI.
 */
public enum HelpMode {

    /** Retrieval was confident, and the model answered from the matched document. */
    ANSWERED,

    /** The question is about ProStriver, but nothing confidently answers it. No model call. */
    INSUFFICIENT,

    /** The question is not about anything the documentation covers. No model call. */
    OUT_OF_SCOPE
}
