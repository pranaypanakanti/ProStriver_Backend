package com.springai.kafka;

import com.springai.study_planner.StudyPlanService;
import com.springai.study_planner.entities.MainTopic;
import com.springai.study_planner.entities.StudyPlanResponse;
import com.springai.study_planner.entities.SubTopic;
import com.springai.study_planner.job.JobStatus;
import com.springai.study_planner.job.StudyPlanJob;
import com.springai.study_planner.job.StudyPlanJobRepository;
import com.springai.study_planner.job.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StudyPlanConsumer#consume}.
 *
 * <p>All collaborators ({@link StudyPlanJobRepository}, {@link StudyPlanService})
 * are mocked — no Spring context, no Kafka broker. The behaviour under test is
 * pure branching/orchestration logic, most importantly the Kafka
 * <b>idempotency guard</b>: a redelivered message for a job already {@code DONE}
 * must be a no-op.
 */
@ExtendWith(MockitoExtension.class)
class StudyPlanConsumerTest {

    @Mock
    private StudyPlanJobRepository jobRepository;

    @Mock
    private StudyPlanService studyPlanService;

    // ---- fixtures ---------------------------------------------------------

    private static final String JOB_ID = "job-123";
    private static final String USER_ID = "user-abc";

    private StudyPlanJob buildJob(JobStatus status) {
        StudyPlanJob job = new StudyPlanJob();
        job.setJobId(JOB_ID);
        job.setUserId(USER_ID);
        job.setStatus(status);
        job.setTier(Tier.FREE);
        job.setStartPreparation(false);
        job.setInput(new StudyPlanRequest("Java", "10 hours", "Interview prep", "Beginner", "no notes"));
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return job;
    }

    private StudyPlanResponse buildPlanWithSubtopics(int subtopicCount) {
        SubTopic subTopic1 = new SubTopic();
        subTopic1.setSubTopicName("Sub 1");

        MainTopic mainTopic = new MainTopic();
        mainTopic.setTopicName("Main topic");

        if (subtopicCount == 1) {
            mainTopic.setSubTopics(List.of(subTopic1));
        } else {
            SubTopic subTopic2 = new SubTopic();
            subTopic2.setSubTopicName("Sub 2");
            mainTopic.setSubTopics(List.of(subTopic1, subTopic2));
        }

        StudyPlanResponse plan = new StudyPlanResponse();
        plan.setMainTopics(List.of(mainTopic));
        return plan;
    }

    // ---- tests --------------------------------------------------------

    @Test
    void consume_jobAlreadyDone_skipsProcessingAndDoesNotSave() {
        StudyPlanJob job = buildJob(JobStatus.DONE);
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));
        StudyPlanConsumer consumer = new StudyPlanConsumer(jobRepository, studyPlanService);

        consumer.consume(new StudyPlanJobMessage(JOB_ID, USER_ID), 0);

        verifyNoInteractions(studyPlanService);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void consume_unknownJobId_returnsEarlyWithoutSideEffects() {
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.empty());
        StudyPlanConsumer consumer = new StudyPlanConsumer(jobRepository, studyPlanService);

        consumer.consume(new StudyPlanJobMessage(JOB_ID, USER_ID), 0);

        verifyNoInteractions(studyPlanService);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void consume_jobQueued_invokesPlannerWithJobInputAndSavesDoneJob() {
        StudyPlanJob job = buildJob(JobStatus.QUEUED);
        StudyPlanResponse plan = buildPlanWithSubtopics(2);
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(studyPlanService.studyPlanner("Java", "10 hours", "Interview prep", "Beginner", "no notes"))
                .thenReturn(plan);
        StudyPlanConsumer consumer = new StudyPlanConsumer(jobRepository, studyPlanService);

        consumer.consume(new StudyPlanJobMessage(JOB_ID, USER_ID), 0);

        verify(studyPlanService).studyPlanner("Java", "10 hours", "Interview prep", "Beginner", "no notes");

        ArgumentCaptor<StudyPlanJob> savedJobCaptor = ArgumentCaptor.forClass(StudyPlanJob.class);
        verify(jobRepository, times(2)).save(savedJobCaptor.capture());
        StudyPlanJob finalSavedJob = savedJobCaptor.getAllValues().get(savedJobCaptor.getAllValues().size() - 1);

        assertThat(finalSavedJob.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(finalSavedJob.getCompletedSubtopics()).isEqualTo(0);
        assertThat(finalSavedJob.getTotalSubtopics()).isEqualTo(2);
    }

    /**
     * The consumer deliberately does NOT mark the job FAILED. Since the DLT was introduced it saves
     * PROCESSING, rethrows, and lets the retry machinery decide when the job is really finished.
     * Marking FAILED here would end the job on the first attempt and defeat the retries.
     *
     * <p>The other half of that guarantee, that something does eventually mark the job FAILED, is
     * covered by {@code KafkaErrorHandlerConfigTest}.
     */
    @Test
    void consume_plannerThrows_savesProcessingAndRethrowsWithoutMarkingFailed() {
        StudyPlanJob job = buildJob(JobStatus.QUEUED);
        RuntimeException failure = new RuntimeException("Gemini unavailable");
        when(jobRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(studyPlanService.studyPlanner(any(), any(), any(), any(), any())).thenThrow(failure);
        StudyPlanConsumer consumer = new StudyPlanConsumer(jobRepository, studyPlanService);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> consumer.consume(new StudyPlanJobMessage(JOB_ID, USER_ID), 0));

        assertThat(thrown).isSameAs(failure);

        ArgumentCaptor<StudyPlanJob> savedJobCaptor = ArgumentCaptor.forClass(StudyPlanJob.class);
        verify(jobRepository, times(1)).save(savedJobCaptor.capture());

        assertThat(savedJobCaptor.getValue().getStatus())
                .as("the only save is the PROCESSING marker, the terminal state belongs to the DLT recoverer")
                .isEqualTo(JobStatus.PROCESSING);
        assertThat(job.getStatus()).isNotEqualTo(JobStatus.FAILED);
    }
}
