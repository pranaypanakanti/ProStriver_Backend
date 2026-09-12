package com.prostriver.help;

import com.prostriver.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Per-user rate limit for the help endpoint, in its own Redis key namespace.
 *
 * <p>Entirely separate from {@code com.springai.ratelimit.RateLimitService}. That one is Bucket4j
 * backed and governs study plan generation, and nothing here can change how it behaves. This uses a
 * plain fixed window: one counter per user per clock hour, under {@code help:ratelimit:}.
 *
 * <p>Redis failures are not caught. If the counter cannot be read, the request fails rather than
 * proceeding unmetered, which matches how {@code RateLimitService} already behaves and keeps a Redis
 * outage from turning into unbounded OpenAI spend.
 */
@Component
@Profile("api")
public class HelpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(HelpRateLimiter.class);

    private static final String KEY_PREFIX = "help:ratelimit:";
    private static final long SECONDS_PER_HOUR = 3600L;

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int limitPerHour;

    public HelpRateLimiter(StringRedisTemplate redis, Clock clock, HelpProperties properties) {
        this.redis = redis;
        this.clock = clock;
        this.limitPerHour = properties.getRateLimitPerHour();
    }

    /**
     * Counts this request against the caller's hourly allowance.
     *
     * @throws ApiException with {@link HttpStatus#SERVICE_UNAVAILABLE} when the counter cannot be
     *                      read, so the caller is refused rather than served unmetered
     */
    public Decision check(UUID userId) {
        long epochSeconds = clock.instant().getEpochSecond();
        long window = epochSeconds / SECONDS_PER_HOUR;
        long retryAfterSeconds = SECONDS_PER_HOUR - (epochSeconds % SECONDS_PER_HOUR);

        long used = increment(KEY_PREFIX + userId + ":" + window, retryAfterSeconds);

        if (used <= limitPerHour) {
            return new Decision(true, limitPerHour - used, 0L);
        }

        return new Decision(false, 0L, retryAfterSeconds);
    }

    private long increment(String key, long windowRemainingSeconds) {
        try {
            Long used = redis.opsForValue().increment(key);
            if (used == null) {
                throw new IllegalStateException("Redis INCR returned no value");
            }
            if (used == 1L) {
                // First hit of this window, so expire the counter exactly when the window rolls over.
                redis.expire(key, Duration.ofSeconds(windowRemainingSeconds));
            }
            return used;
        } catch (DataAccessException | IllegalStateException e) {
            // Failing open here would be the worst possible moment to do it. The answer cache lives
            // on this same Redis, so an outage makes every request a cache miss at the same instant
            // the limiter stops counting: every caller reaching OpenAI, unmetered.
            log.error("Help rate limiter could not reach Redis, refusing the request rather than serving "
                    + "it unmetered: {}", e.getMessage());

            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Help is temporarily unavailable. Try again shortly.");
        }
    }

    /**
     * @param allowed           whether the caller may proceed
     * @param remaining         requests left in the current hour
     * @param retryAfterSeconds seconds until the window resets, zero when allowed
     */
    public record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
    }
}
