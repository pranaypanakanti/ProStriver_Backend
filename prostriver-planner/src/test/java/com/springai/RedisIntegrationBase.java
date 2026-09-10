package com.springai;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Abstract base class for all integration tests that require a real Redis.
 *
 * <p>{@link com.springai.ratelimit.RateLimitConfig} builds its
 * {@code ProxyManager<String>} bean by hand from a raw {@code REDIS_URI}
 * property using a Lettuce client, bypassing Spring Data Redis
 * autoconfiguration entirely. That means Spring Boot's
 * {@code @ServiceConnection} support for Redis — which targets
 * {@code RedisConnectionDetails} — cannot wire this bean automatically.
 * Instead, this base class starts a plain {@link GenericContainer} running
 * {@code redis:7-alpine} and publishes the {@code REDIS_URI} property via
 * {@link DynamicPropertySource}, since the container's mapped port is only
 * known at runtime.
 *
 * <p>The container is declared {@code static} so it starts once for the
 * entire test suite rather than per-class. Spring Boot's context cache keeps
 * the {@code ApplicationContext} alive across tests that share the same
 * context configuration, meaning only one container and one context are
 * created per Maven module run.
 *
 * <p>The {@code @Testcontainers} extension is inherited by subclasses via
 * JUnit Jupiter's {@code @ExtendWith} inheritance, so subclasses do not need
 * to repeat it.
 */
@Testcontainers
public abstract class RedisIntegrationBase {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("REDIS_URI", () ->
                "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }
}
