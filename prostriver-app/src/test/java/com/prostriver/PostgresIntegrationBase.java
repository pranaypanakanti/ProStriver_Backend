package com.prostriver;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for all integration tests that require a real Postgres.
 *
 * <p>The container is declared {@code static} so it starts once for the entire
 * test suite rather than per-class. Spring Boot's context cache keeps the
 * {@code ApplicationContext} alive across tests that share the same context
 * configuration, meaning only one container and one context are created per
 * Maven module run.
 *
 * <p>{@code @ServiceConnection} registers a {@code JdbcConnectionDetails}
 * bean, which short-circuits Spring Boot's normal
 * {@code spring.datasource.*} property binding (that mechanism is gated by
 * {@code @ConditionalOnMissingBean(JdbcConnectionDetails.class)}), so the
 * unresolved {@code ${DB_URL}}/{@code ${DB_USER}}/{@code ${DB_PASSWORD}}
 * placeholders in {@code application.yaml} are never evaluated by tests that
 * extend this class.
 *
 * <p>The {@code @Testcontainers} extension is inherited by subclasses via
 * JUnit Jupiter's {@code @ExtendWith} inheritance, so subclasses do not need
 * to repeat it.
 */
@Testcontainers
public abstract class PostgresIntegrationBase {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
