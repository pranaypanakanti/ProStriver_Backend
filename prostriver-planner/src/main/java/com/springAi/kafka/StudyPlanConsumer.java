package com.springAi.kafka;

import com.springAi.studyPlanner.StudyPlanService;
import com.springAi.studyPlanner.entities.MainTopic;
import com.springAi.studyPlanner.entities.StudyPlanResponse;
import com.springAi.studyPlanner.entities.SubTopic;
import com.springAi.studyPlanner.job.JobStatus;
import com.springAi.studyPlanner.job.StudyPlanJob;
import com.springAi.studyPlanner.job.StudyPlanJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StudyPlanConsumer {

    private static final Logger log = LoggerFactory.getLogger(StudyPlanConsumer.class);

    private final StudyPlanJobRepository jobRepository;

    private final StudyPlanService studyPlanService;

    public StudyPlanConsumer(StudyPlanJobRepository jobRepository, StudyPlanService studyPlanService) {
        this.jobRepository = jobRepository;
        this.studyPlanService = studyPlanService;
    }

    @KafkaListener(
            topics = "study-plan-jobs",
            groupId = "study-plan-group",
            concurrency = "2"
    )
    public void consume(StudyPlanJobMessage message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        String jobId = message.getJobId();
        log.info("Received job {} on partition {}", jobId, partition);

        StudyPlanJob job = jobRepository.findByJobId(message.getJobId()).orElse(null);
        if (job == null) {
            log.error("No job found in MongoDB for jobId {}", message.getJobId());
            return;
        }

        if (job.getStatus() == JobStatus.DONE) {
            log.warn("Job {} already DONE — skipping (idempotency guard)", jobId);
            return;
        }

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            StudyPlanRequest input = job.getInput();
            StudyPlanResponse plan = studyPlanService.studyPlanner(
                    input.getTopic(),
                    input.getTimeAvailable(),
                    input.getPurpose(),
                    input.getLevel(),
                    input.getNotes());

            int totalSubtopics = assignIds(plan);

            job.setPlan(plan);
            job.setTotalSubtopics(totalSubtopics);
            job.setCompletedSubtopics(0);
            job.setStatus(JobStatus.DONE);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            log.info("Job {} DONE → {} subtopics", jobId, totalSubtopics);

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            job.setStatus(JobStatus.FAILED);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            throw e;
        }
    }

    private int assignIds(StudyPlanResponse plan) {
        int subtopicCount = 0;

        List<MainTopic> mainTopics = plan.getMainTopics();
        if (mainTopics == null) {
            return 0;
        }

        for (MainTopic topic : mainTopics) {
            topic.setTopicId(UUID.randomUUID().toString());

            List<SubTopic> subTopics = topic.getSubTopics();
            if (subTopics == null) {
                continue;
            }
            for (SubTopic sub : subTopics) {
                sub.setSubtopicId(UUID.randomUUID().toString());
                sub.setDone(false);
                subtopicCount++;
            }
        }
        return subtopicCount;

    }

}