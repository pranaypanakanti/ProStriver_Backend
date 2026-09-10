package com.prostriver.help;

import com.prostriver.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fixed window accounting for the help endpoint's own limiter. No Redis, no Spring.
 */
class HelpRateLimiterTest {

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    private static final int LIMIT = 30;

    /** 10:15:00 UTC exactly, so 2700 seconds remain in the hour. */
    private static final Instant QUARTER_PAST = Instant.parse("2026-09-05T10:15:00Z");

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private HelpRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        HelpProperties properties = new HelpProperties();
        properties.setRateLimitPerHour(LIMIT);

        limiter = new HelpRateLimiter(redis, Clock.fixed(QUARTER_PAST, ZoneOffset.UTC), properties);
    }

    @Test
    void check_firstRequestOfTheWindow_isAllowedAndSetsTheExpiryToTheWindowEnd() {
        when(values.increment(anyString())).thenReturn(1L);

        HelpRateLimiter.Decision decision = limiter.check(USER);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(LIMIT - 1);
        assertThat(decision.retryAfterSeconds()).isZero();
        verify(redis).expire(anyString(), eqDuration(2700));
    }

    @Test
    void check_laterRequestInTheSameWindow_doesNotResetTheExpiry() {
        when(values.increment(anyString())).thenReturn(7L);

        HelpRateLimiter.Decision decision = limiter.check(USER);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(LIMIT - 7);
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void check_requestExactlyAtTheLimit_isStillAllowed() {
        when(values.increment(anyString())).thenReturn((long) LIMIT);

        HelpRateLimiter.Decision decision = limiter.check(USER);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void check_requestPastTheLimit_isRefusedWithTheSecondsLeftInTheWindow() {
        when(values.increment(anyString())).thenReturn(LIMIT + 1L);

        HelpRateLimiter.Decision decision = limiter.check(USER);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds())
                .as("2700 seconds from 10:15 to the top of the next hour")
                .isEqualTo(2700L);
    }

    @Test
    void check_redisUnreachable_refusesWithServiceUnavailableRatherThanServingUnmetered() {
        when(values.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThatThrownBy(() -> limiter.check(USER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("temporarily unavailable")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void check_redisReturnsNoCount_refusesRatherThanGuessing() {
        when(values.increment(anyString())).thenReturn(null);

        assertThatThrownBy(() -> limiter.check(USER))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void check_keyIsNamespacedPerUserAndWindow() {
        when(values.increment(anyString())).thenReturn(1L);

        limiter.check(USER);

        long expectedWindow = QUARTER_PAST.getEpochSecond() / 3600L;
        verify(values).increment("help:ratelimit:" + USER + ":" + expectedWindow);
    }

    private static Duration eqDuration(long seconds) {
        return org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(seconds));
    }
}
