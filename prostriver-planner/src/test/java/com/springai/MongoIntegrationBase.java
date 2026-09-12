package com.springai;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for all integration tests that require a real MongoDB.
 *
 * <p>The container is declared {@code static} so it starts once for the entire
 * test suite rather than per-class. Spring Boot's context cache keeps the
 * {@code ApplicationContext} alive across tests that share the same context
 * configuration, meaning only one container and one context are created per
 * Maven module run.
 *
 * <p>The {@code @Testcontainers} extension is inherited by subclasses via
 * JUnit Jupiter's {@code @ExtendWith} inheritance, so subclasses do not need
 * to repeat it.
 */
@Testcontainers
public abstract class MongoIntegrationBase {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");
}
