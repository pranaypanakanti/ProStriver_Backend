package com.springai;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot anchor required by {@code @DataMongoTest}.
 * The test-slice annotation disables all non-Mongo auto-configuration,
 * so this class exists only to satisfy the bootstrapper's need for a
 * {@code @SpringBootConfiguration} somewhere on the test classpath.
 *
 * It MUST NOT be instantiated or used at runtime — it lives solely under
 * {@code src/test/java}.
 */
@SpringBootApplication
public class TestPlannerApplication {
}
