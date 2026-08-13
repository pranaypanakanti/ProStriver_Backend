package com.springai.ratelimit;

public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {}