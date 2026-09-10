package com.prostriver.challenge;

import com.prostriver.PostgresIntegrationBase;
import com.prostriver.config.TimeConfig;
import com.prostriver.entity.LockInChallenge;
import com.prostriver.entity.User;
import com.prostriver.entity.enums.ChallengeStatus;
import com.prostriver.entity.enums.ChallengeType;
import com.prostriver.entity.enums.NotificationPreference;
import com.prostriver.entity.enums.Role;
import com.prostriver.repository.LockInChallengeRepository;
import com.prostriver.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * Integration test proving the per-item isolation invariant documented in
 * {@code CLAUDE.md}: "Batch/scheduler per-item work runs in
 * {@code @Transactional(REQUIRES_NEW)} in a separate bean (proxy boundary).
 * Never fold it into a self-call."
 *
 * <p>{@link ChallengeStreakScheduler#evaluateYesterday()} loops over all
 * {@code ACTIVE} challenges and calls {@link ChallengeEvaluator#evaluateOne}
 * (a separate {@code @Component} bean, {@code REQUIRES_NEW}) for each one
 * inside a {@code try/catch}. This test proves that one challenge throwing
 * does not roll back — or otherwise affect — the other challenges in the
 * same batch.
 *
 * <p>Because every column {@code evaluateOne} touches is either
 * database-{@code NOT NULL} or a primitive {@code int}, the schema itself
 * prevents constructing a persistable-but-poisoned row to trigger a "real"
 * failure. Instead, a {@link MockitoSpyBean} wraps the real, already-
 * transactional-proxied {@link ChallengeEvaluator} bean: exactly one
 * challenge ID is stubbed to throw, and every other call is left unstubbed
 * so it delegates to the real method and genuinely commits via
 * {@code REQUIRES_NEW} against the Testcontainers Postgres instance.
 *
 * <p>Context setup:
 * <ul>
 *   <li>{@code @DataJpaTest} keeps JPA/Hibernate/DataSource auto-configuration
 *       active (unlike {@code @WebMvcTest}, which excludes it) so
 *       {@code entityManagerFactory} exists and the repository beans
 *       registered by {@code @EnableJpaRepositories} on
 *       {@code ProStriverApplication} resolve without issue.</li>
 *   <li>{@code @AutoConfigureTestDatabase(replace = NONE)} stops Spring Boot
 *       from swapping in an embedded database, so the real Testcontainers
 *       Postgres from {@link PostgresIntegrationBase} is used.</li>
 *   <li>{@code @TestPropertySource} overrides {@code ddl-auto} to
 *       {@code create-drop} for this test only — production config keeps
 *       {@code validate} (see {@code application.yaml}); there is no
 *       Flyway/Liquibase migration in this codebase to pre-populate a fresh
 *       Testcontainers instance with the schema, and CLAUDE.md explicitly
 *       calls out that "integration tests use Testcontainers, not the prod
 *       schema".</li>
 *   <li>{@code @Import} adds the scheduler, the evaluator, and
 *       {@link TimeConfig} (for the {@code Clock} bean) — none of these are
 *       picked up by {@code @DataJpaTest}'s component-scan filtering on
 *       their own.</li>
 *   <li>{@code @ActiveProfiles("worker")} is required because both
 *       {@link ChallengeStreakScheduler} and {@link ChallengeEvaluator} are
 *       {@code @Profile("worker")}.</li>
 *   <li>{@code @Transactional(propagation = NOT_SUPPORTED)} at the class
 *       level disables {@code @DataJpaTest}'s default per-test rollback
 *       wrapper, so assertions read genuinely committed rows rather than
 *       Hibernate first-level-cache illusions, and so the evaluator's
 *       {@code REQUIRES_NEW} commits are not nested inside (and rolled back
 *       by) an outer test transaction.</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("worker")
@Import({ChallengeStreakScheduler.class, ChallengeEvaluator.class, TimeConfig.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChallengeStreakSchedulerTest extends PostgresIntegrationBase {

    @Autowired
    private ChallengeStreakScheduler scheduler;

    @MockitoSpyBean
    private ChallengeEvaluator challengeEvaluator;

    @Autowired
    private LockInChallengeRepository lockInChallengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    // ---- cleanup ----------------------------------------------------------

    @AfterEach
    void cleanDatabase() {
        lockInChallengeRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---- test ---------------------------------------------------------------

    /**
     * Three ACTIVE challenges, owned by three different users, are evaluated
     * in one batch. The evaluator is stubbed to throw for exactly one of
     * them. Expected outcome:
     * <ul>
     *   <li>{@code evaluateYesterday()} itself does not throw — the
     *       scheduler's {@code try/catch} swallows the per-item
     *       exception.</li>
     *   <li>The two "good" challenges are genuinely persisted with
     *       {@code lastEvaluatedDate} now set to yesterday (re-fetched from
     *       the repository, not an in-memory reference).</li>
     *   <li>The "bad" challenge is completely unchanged — its failure
     *       neither half-committed nor rolled back the others.</li>
     * </ul>
     */
    @Test
    void evaluateYesterday_oneChallengeThrows_othersStillCommitAndBadOneUnchanged() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate startDate = yesterday.minusDays(60);
        LocalDate endDate = yesterday.plusYears(1);

        LockInChallenge good1 = persistActiveChallenge(startDate, endDate);
        LockInChallenge bad = persistActiveChallenge(startDate, endDate);
        LockInChallenge good2 = persistActiveChallenge(startDate, endDate);

        doThrow(new RuntimeException("boom"))
                .when(challengeEvaluator).evaluateOne(eq(bad.getId()), any(LocalDate.class));

        assertThatCode(() -> scheduler.evaluateYesterday()).doesNotThrowAnyException();

        LockInChallenge reloadedGood1 = lockInChallengeRepository.findById(good1.getId()).orElseThrow();
        LockInChallenge reloadedGood2 = lockInChallengeRepository.findById(good2.getId()).orElseThrow();
        LockInChallenge reloadedBad = lockInChallengeRepository.findById(bad.getId()).orElseThrow();

        assertThat(reloadedGood1.getLastEvaluatedDate()).isEqualTo(yesterday);
        assertThat(reloadedGood1.getFreezeUsed()).isEqualTo(1);

        assertThat(reloadedGood2.getLastEvaluatedDate()).isEqualTo(yesterday);
        assertThat(reloadedGood2.getFreezeUsed()).isEqualTo(1);

        assertThat(reloadedBad.getLastEvaluatedDate()).isNull();
        assertThat(reloadedBad.getFreezeUsed()).isZero();
        assertThat(reloadedBad.getCurrentStreak()).isZero();
        assertThat(reloadedBad.getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
    }

    // ---- fixtures -----------------------------------------------------------

    private LockInChallenge persistActiveChallenge(LocalDate startDate, LocalDate endDate) {
        User user = new User();
        user.setEmail("fixture-" + UUID.randomUUID() + "@prostriver.test");
        user.setFullName("Fixture User");
        user.setPassword("irrelevant-hash");
        user.setRole(Role.USER);
        user.setNotificationPreference(NotificationPreference.NONE);
        userRepository.save(user);

        LockInChallenge challenge = new LockInChallenge();
        challenge.setUser(user);
        challenge.setChallengeType(ChallengeType.THIRTY_DAY);
        challenge.setStartDate(startDate);
        challenge.setEndDate(endDate);
        challenge.setStatus(ChallengeStatus.ACTIVE);
        challenge.setCurrentStreak(0);
        challenge.setFreezeAllowed(ChallengeRules.freezeAllowed(ChallengeType.THIRTY_DAY));
        challenge.setFreezeUsed(0);
        challenge.setLastEvaluatedDate(null);

        return lockInChallengeRepository.save(challenge);
    }
}
