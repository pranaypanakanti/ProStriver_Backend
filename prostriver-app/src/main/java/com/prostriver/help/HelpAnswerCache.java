package com.prostriver.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prostriver.common.crypto.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Caches whole help answers in Redis, keyed on the normalised question.
 *
 * <p>Its own key namespace, {@code help:answer:}. It shares nothing with the study plan rate limiter
 * or the Gemini cooldown keys.
 *
 * <p>A cache failure is never allowed to fail a request. A read that blows up is treated as a miss
 * and a write that blows up is logged and swallowed, so Redis being unavailable makes help slower
 * and more expensive, not broken.
 */
@Component
@Profile("api")
public class HelpAnswerCache {

    private static final String KEY_PREFIX = "help:answer:";

    private static final Logger log = LoggerFactory.getLogger(HelpAnswerCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public HelpAnswerCache(StringRedisTemplate redis, ObjectMapper objectMapper, HelpProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofDays(properties.getCacheTtlDays());
    }

    public Optional<HelpAnswer> find(String question) {
        try {
            String json = redis.opsForValue().get(key(question));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, HelpAnswer.class));
        } catch (Exception e) {
            log.warn("Help answer cache read failed, treating it as a miss: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String question, HelpAnswer answer) {
        try {
            redis.opsForValue().set(key(question), objectMapper.writeValueAsString(answer), ttl);
        } catch (Exception e) {
            log.warn("Help answer cache write failed, continuing without caching: {}", e.getMessage());
        }
    }

    /**
     * Hashed so the key length is bounded regardless of how long the question was.
     */
    static String key(String question) {
        return KEY_PREFIX + Sha256.hex(normalise(question));
    }

    /**
     * Lowercase, punctuation stripped, whitespace collapsed, so trivially different phrasings of the
     * same question share one cache entry.
     *
     * <p>Apostrophes are removed rather than replaced, so "can't" and "cant" collapse together.
     * Everything else non alphanumeric becomes a separator, so "sign-up" and "sign up" do too.
     */
    static String normalise(String question) {
        if (question == null) {
            return "";
        }
        return question
                .toLowerCase(Locale.ROOT)
                .replaceAll("['‘’]", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }
}
