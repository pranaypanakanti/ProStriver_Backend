package com.spring_ai.ratelimit;

public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {}