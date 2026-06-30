package com.springAi.ratelimit;

public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {}