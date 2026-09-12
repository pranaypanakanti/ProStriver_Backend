package com.prostriver.help;

/**
 * The response body for {@code POST /api/help/ask}. All three modes return HTTP 200.
 *
 * <p>{@code source} is the document an answer was actually drawn from, so it is populated only for
 * {@link HelpMode#ANSWERED}.
 *
 * <p>{@code nearest} is the document that came closest without clearing the bar, so it is populated
 * only for {@link HelpMode#INSUFFICIENT}. It is a pointer, not an attribution: nothing in the answer
 * came from it. It is null when the closest match was a slug with no loaded document behind it.
 *
 * <p>{@code confidence} is the retrieval similarity that decided the mode, which makes a surprising
 * result traceable back to the phrasing that matched.
 */
public record HelpAnswer(
        HelpMode mode,
        String answer,
        DocSource source,
        DocSource nearest,
        double confidence,
        boolean cached
) {

    /**
     * The same answer, flagged as served from cache.
     */
    public HelpAnswer asCached() {
        return new HelpAnswer(mode, answer, source, nearest, confidence, true);
    }
}
