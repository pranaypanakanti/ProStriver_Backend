package com.springai.kafka;

import com.springai.study_planner.job.JobStatus;
import com.springai.study_planner.job.StudyPlanJob;
import com.springai.study_planner.job.StudyPlanJobRepository;
import com.springai.study_planner.job.Tier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the half of the failure guarantee that moved out of {@link StudyPlanConsumer}.
 *
 * <p>The consumer rethrows and never marks a job FAILED. That makes the recoverer inside
 * {@link KafkaErrorHandlerConfig} solely responsible for putting a job into a terminal state. If it
 * is misconfigured, a job that exhausts its retries sits in PROCESSING forever with nothing logged
 * as a failure, which is invisible rather than loud. These tests pin that down.
 *
 * <p>Plain unit tests. No Spring context and no broker: the {@code @Bean} method is called directly
 * on a bare config instance, and the returned handler is driven through its public API.
 */
class KafkaErrorHandlerConfigTest {

    private static final String TOPIC = "study-plan-jobs";
    private static final String DLT_TOPIC = "study-plan-jobs.DLT";
    private static final String JOB_ID = "job-123";
    private static final String USER_ID = "user-abc";

    private KafkaTemplate<String, StudyPlanJobMessage> kafkaTemplate;
    private StudyPlanJobRepository jobRepository;
    private Consumer<?, ?> consumer;
    private MessageListenerContainer container;
    private DefaultErrorHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        jobRepository = mock(StudyPlanJobRepository.class);
        consumer = mock(Consumer.class);
        container = mock(MessageListenerContainer.class);

        handler = new KafkaErrorHandlerConfig().errorHandler(kafkaTemplate, jobRepository);
    }

    /**
     * Replaces the configured exponential backoff with one that gives up immediately, so a test can
     * reach the recovery path without sleeping through real retry intervals. This only reconfigures
     * the object the bean method returned, it changes nothing in production.
     */
    private void exhaustRetriesImmediately() {
        handler.setBackOffFunction((record, ex) -> new FixedBackOff(0L, 0L));
    }

    @Test
    void recoverer_retriesExhausted_marksTheJobFailedAndSavesIt() {
        exhaustRetriesImmediately();
        StudyPlanJob job = jobInProgress();
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));

        boolean recovered = handler.handleOne(
                new RuntimeException("Gemini unavailable"), jobRecord(), consumer, container);

        assertThat(recovered).as("the record is recovered, not retried again").isTrue();
        verify(jobRepository).save(job);
        assertThat(job.getStatus())
                .as("nothing else marks a job terminal once the consumer has rethrown")
                .isEqualTo(JobStatus.FAILED);
        assertThat(job.getUpdatedAt()).isNotNull();
    }

    @Test
    void recoverer_retriesExhausted_publishesTheRecordToTheDeadLetterTopic() {
        exhaustRetriesImmediately();
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(jobInProgress()));

        handler.handleOne(new RuntimeException("Gemini unavailable"), jobRecord(), consumer, container);

        ArgumentCaptor<ProducerRecord<String, StudyPlanJobMessage>> published =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(published.capture());

        assertThat(published.getValue().topic()).isEqualTo(DLT_TOPIC);
        assertThat(published.getValue().partition()).isZero();
    }

    @Test
    void recoverer_nonRetryableException_recoversOnTheFirstFailureWithoutRetrying() {
        // No backoff override here, so this also proves addNotRetryableExceptions is wired up: an
        // IllegalArgumentException must skip the retries entirely rather than waiting them out.
        StudyPlanJob job = jobInProgress();
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));

        boolean recovered = handler.handleOne(
                new IllegalArgumentException("unparseable input"), jobRecord(), consumer, container);

        assertThat(recovered).isTrue();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        verify(jobRepository).save(job);
    }

    /**
     * Publishing to the DLT and marking the job FAILED are independent obligations. A broker problem
     * must not cost us the status update, or the job sits in PROCESSING forever with the failure
     * recorded nowhere.
     */
    @Test
    void recoverer_deadLetterPublishingFails_stillMarksTheJobFailedAndSaves() {
        exhaustRetriesImmediately();
        doThrow(new KafkaException("broker unreachable"))
                .when(kafkaTemplate).send(any(ProducerRecord.class));

        StudyPlanJob job = jobInProgress();
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));

        boolean recovered = handler.handleOne(
                new RuntimeException("Gemini unavailable"), jobRecord(), consumer, container);

        assertThat(recovered).isTrue();
        verify(jobRepository).save(job);
        assertThat(job.getStatus())
                .as("the status update survives a failed DLT publish")
                .isEqualTo(JobStatus.FAILED);
        assertThat(job.getUpdatedAt()).isNotNull();
    }

    /**
     * Premise for the test above. If a rejected send were swallowed inside
     * {@link DeadLetterPublishingRecoverer}, that test would pass without ever reaching the guard it
     * exists to cover. This pins the assumption that the failure really does escape {@code accept}.
     */
    @Test
    void deadLetterPublishing_whenTheBrokerRejectsTheRecord_throwsOutOfAccept() {
        doThrow(new KafkaException("broker unreachable"))
                .when(kafkaTemplate).send(any(ProducerRecord.class));

        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, ex) -> new TopicPartition(DLT_TOPIC, 0));

        assertThatThrownBy(() -> dltRecoverer.accept(jobRecord(), new RuntimeException("boom")))
                .as("a failed publish propagates, which is why the production guard is needed")
                .isInstanceOf(Exception.class);
    }

    @Test
    void recoverer_jobMissingFromMongo_savesNothingAndStillRecovers() {
        exhaustRetriesImmediately();
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.empty());

        boolean recovered = handler.handleOne(
                new RuntimeException("Gemini unavailable"), jobRecord(), consumer, container);

        assertThat(recovered).isTrue();
        verify(jobRepository, never()).save(any());
    }

    @Test
    void recoverer_mongoUnavailable_swallowsTheFailureAndStillRecovers() {
        exhaustRetriesImmediately();
        when(jobRepository.findByJobId(anyString()))
                .thenThrow(new DataAccessResourceFailureException("mongo down"));

        assertThatCode(() -> handler.handleOne(
                new RuntimeException("Gemini unavailable"), jobRecord(), consumer, container))
                .as("a Mongo outage must not stop the record being recovered")
                .doesNotThrowAnyException();

        verify(jobRepository, never()).save(any());
    }

    @Test
    void recoverer_recordValueIsNotAJobMessage_leavesMongoAlone() {
        exhaustRetriesImmediately();

        handler.handleOne(new RuntimeException("bad payload"),
                new ConsumerRecord<>(TOPIC, 0, 42L, "key", "not a job message"),
                consumer, container);

        verifyNoInteractions(jobRepository);
    }

    private ConsumerRecord<String, Object> jobRecord() {
        return new ConsumerRecord<>(TOPIC, 0, 42L, "key", new StudyPlanJobMessage(JOB_ID, USER_ID));
    }

    private StudyPlanJob jobInProgress() {
        StudyPlanJob job = new StudyPlanJob();
        job.setJobId(JOB_ID);
        job.setUserId(USER_ID);
        job.setStatus(JobStatus.PROCESSING);
        job.setTier(Tier.FREE);
        return job;
    }
}
