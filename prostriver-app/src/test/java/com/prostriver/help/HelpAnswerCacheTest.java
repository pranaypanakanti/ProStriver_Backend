package com.prostriver.help;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Question normalisation for the cache key. Pure string logic, no Redis.
 */
class HelpAnswerCacheTest {

    @Test
    void normalise_mixedCase_isLowercased() {
        assertThat(HelpAnswerCache.normalise("HOW Do I Sign Up")).isEqualTo("how do i sign up");
    }

    @Test
    void normalise_trailingPunctuation_isStripped() {
        assertThat(HelpAnswerCache.normalise("How do I sign up?")).isEqualTo("how do i sign up");
        assertThat(HelpAnswerCache.normalise("How do I sign up!!!")).isEqualTo("how do i sign up");
    }

    @Test
    void normalise_repeatedWhitespace_isCollapsed() {
        assertThat(HelpAnswerCache.normalise("how   do \t i \n sign up")).isEqualTo("how do i sign up");
    }

    @Test
    void normalise_surroundingWhitespace_isRemoved() {
        assertThat(HelpAnswerCache.normalise("   how do i sign up   ")).isEqualTo("how do i sign up");
    }

    @Test
    void normalise_hyphenatedWords_becomeSeparateWords() {
        assertThat(HelpAnswerCache.normalise("how do i sign-up"))
                .as("so 'sign-up' and 'sign up' share one cache entry")
                .isEqualTo("how do i sign up");
    }

    @Test
    void normalise_apostrophes_areRemovedRatherThanSplit() {
        assertThat(HelpAnswerCache.normalise("i can't log in")).isEqualTo("i cant log in");
        assertThat(HelpAnswerCache.normalise("i can’t log in"))
                .as("a curly apostrophe collapses the same way as a straight one")
                .isEqualTo("i cant log in");
        assertThat(HelpAnswerCache.normalise("i cant log in")).isEqualTo("i cant log in");
    }

    @Test
    void normalise_differentPhrasingsOfTheSameQuestion_produceTheSameKey() {
        assertThat(HelpAnswerCache.key("How do I sign-up?"))
                .isEqualTo(HelpAnswerCache.key("  how   do i sign up  "));
    }

    @Test
    void normalise_differentQuestions_produceDifferentKeys() {
        assertThat(HelpAnswerCache.key("how do i sign up"))
                .isNotEqualTo(HelpAnswerCache.key("how do i log in"));
    }

    @Test
    void normalise_nullOrBlank_isEmpty() {
        assertThat(HelpAnswerCache.normalise(null)).isEmpty();
        assertThat(HelpAnswerCache.normalise("   ")).isEmpty();
        assertThat(HelpAnswerCache.normalise("???")).isEmpty();
    }

    @Test
    void key_anyQuestion_isNamespacedAndFixedLength() {
        String key = HelpAnswerCache.key("x".repeat(300));

        assertThat(key).startsWith("help:answer:");
        assertThat(key).hasSize("help:answer:".length() + 64);
    }
}
