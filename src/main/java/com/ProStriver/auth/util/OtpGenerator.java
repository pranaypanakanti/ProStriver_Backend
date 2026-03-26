package com.ProStriver.auth.util;

import java.security.SecureRandom;

public final class OtpGenerator {

    private static final SecureRandom RAND = new SecureRandom();

    private OtpGenerator() {}

    public static String generate6Digits() {
        int n = RAND.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}