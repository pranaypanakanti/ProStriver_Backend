package com.springAi.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final int HOURLY_LIMIT = 2;
    private static final int DAILY_LIMIT = 5;

    private final ProxyManager<String> proxyManager;

    public RateLimitService(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public RateLimitResult tryConsume(String userId) {
        String key = "rate:studyplan:" + userId;

        Bucket bucket = proxyManager.builder().build(key, this::configuration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, probe.getRemainingTokens(), 0);
        }
        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        return new RateLimitResult(false, 0, retryAfterSeconds);
    }

    private BucketConfiguration configuration() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(HOURLY_LIMIT).refillGreedy(HOURLY_LIMIT, Duration.ofHours(1)))
                .addLimit(limit -> limit.capacity(DAILY_LIMIT).refillGreedy(DAILY_LIMIT, Duration.ofDays(1)))
                .build();
    }
}