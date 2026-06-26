package com.springAi.studyPlanner.job;

import com.springAi.kafka.StudyPlanProducer;
import com.springAi.kafka.StudyPlanRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudyPlanJobService {

    private final StudyPlanJobRepository jobRepository;
    private final StudyPlanProducer producer;

    public StudyPlanJobService(StudyPlanJobRepository jobRepository, StudyPlanProducer producer) {
        this.jobRepository = jobRepository;
        this.producer = producer;
    }

    public String submit(String userId, StudyPlanRequest input) {

        String jobId = UUID.randomUUID().toString();

        StudyPlanJob job = new StudyPlanJob();
        job.setJobId(jobId);
        job.setUserId(userId);
        job.setTier(Tier.FREE);
        job.setStatus(JobStatus.QUEUED);
        job.setInput(input);
        job.setStartPreparation(false);
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        jobRepository.save(job);
        producer.publish(userId, jobId);

        return jobId;
    }

    public Optional<StudyPlanJob> findJob(String jobId) {
        return jobRepository.findByJobId(jobId);
    }
}