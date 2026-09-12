package com.springai.ratelimit;

import com.springai.RedisIntegrationBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RateLimitService#tryConsume}.
 *
 * <p>These tests run against a real Redis instance because
 * {@link RateLimitConfig} builds its {@code ProxyManager<String>} by hand
 * from a Lettuce connection, bypassing Spring Data Redis autoconfiguration.
 * A mocked {@code ProxyManager} would only prove that Bucket4j's own API
 * behaves as documented, not that this service is wired to a real,
 * network-backed bucket store.
 *
 * <p>Context setup:
 * <ul>
 *   <li>{@code @SpringBootTest(classes = ..., webEnvironment = NONE)} loads a
 *       minimal, non-web context containing only {@link RateLimitConfig}
 *       (which supplies the {@code ProxyManager<String>} bean) and
 *       {@link RateLimitService} itself — no web layer, no Mongo, no
 *       Kafka.</li>
 *   <li>{@link RedisIntegrationBase} starts a real {@code redis:7-alpine}
 *       container and publishes {@code REDIS_URI} so
 *       {@code RateLimitConfig} can connect to it.</li>
 * </ul>
 *
 * <p>Each test uses a fresh random {@code userId} so bucket state never
 * leaks between tests (there is no reset hook and buckets are keyed purely
 * by userId).
 */
@SpringBootTest(
        classes = {RateLimitConfig.class, RateLimitService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RateLimitServiceTest extends RedisIntegrationBase {

    @Autowired
    private RateLimitService rateLimitService;

    /**
     * A brand-new user's first call must be allowed, and the remaining
     * token count must reflect that exactly one token was consumed out of
     * the most restrictive (hourly, capacity 2) bandwidth.
     */
    @Test
    void tryConsume_freshUser_allowedAndRemainingTokensDecremented() {
        String userId = UUID.randomUUID().toString();

        RateLimitResult result = rateLimitService.tryConsume(userId);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(1);
        assertThat(result.retryAfterSeconds()).isEqualTo(0);
    }

    /**
     * The hourly limit is 2. The first two calls within the hour must be
     * allowed; the third must be rejected with a positive retry-after
     * duration.
     */
    @Test
    void tryConsume_thirdCallWithinHour_rejectedWithRetryAfter() {
        String userId = UUID.randomUUID().toString();

        RateLimitResult first = rateLimitService.tryConsume(userId);
        RateLimitResult second = rateLimitService.tryConsume(userId);
        RateLimitResult third = rateLimitService.tryConsume(userId);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isFalse();
        assertThat(third.remainingTokens()).isEqualTo(0);
        assertThat(third.retryAfterSeconds()).isGreaterThan(0);
    }

    /**
     * Buckets are keyed per userId, so one user exhausting their hourly cap
     * must have no effect on a different user's ability to consume.
     */
    @Test
    void tryConsume_differentUsers_haveIndependentBuckets() {
        String userA = UUID.randomUUID().toString();
        String userB = UUID.randomUUID().toString();

        // Exhaust userA's hourly cap of 2.
        rateLimitService.tryConsume(userA);
        rateLimitService.tryConsume(userA);
        RateLimitResult userAThirdCall = rateLimitService.tryConsume(userA);

        // userB has never called before, so their bucket must be fully fresh.
        RateLimitResult userBFirstCall = rateLimitService.tryConsume(userB);

        assertThat(userAThirdCall.allowed()).isFalse();
        assertThat(userBFirstCall.allowed()).isTrue();
        assertThat(userBFirstCall.remainingTokens()).isEqualTo(1);
    }

    // Not covered: independent verification of the 5/day limit's own
    // boundary (e.g., that a user can make calls on hour 2 and hour 3 of a
    // fresh day up to 5 total before the daily track — not the hourly
    // track — rejects them). Exercising that requires either waiting for
    // real hourly refills or manipulating wall-clock time, and
    // RateLimitService has no injectable clock to fake this with. Faking or
    // mocking time was explicitly out of scope, so this aspect is left
    // untested.
}
