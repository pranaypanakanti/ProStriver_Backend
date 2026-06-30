package com.springAi.studyPlanner.job;

import com.springAi.kafka.StudyPlanProducer;
import com.springAi.kafka.StudyPlanRequest;
import com.springAi.studyPlanner.entities.MainTopic;
import com.springAi.studyPlanner.entities.StudyPlanResponse;
import com.springAi.studyPlanner.entities.SubTopic;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudyPlanJobService {

    private final StudyPlanJobRepository jobRepository;
    private final StudyPlanProducer producer;
    private final MongoTemplate mongoTemplate;

    public StudyPlanJobService(StudyPlanJobRepository jobRepository,
                               StudyPlanProducer producer,
                               MongoTemplate mongoTemplate) {
        this.jobRepository = jobRepository;
        this.producer = producer;
        this.mongoTemplate = mongoTemplate;
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

    public Optional<StudyPlanProgressResponse> startPreparation(String jobId, String userId) {
        Query q = Query.query(Criteria.where("jobId").is(jobId).and("userId").is(userId));
        Update u = new Update()
                .set("startPreparation", true)
                .set("updatedAt", Instant.now());

        StudyPlanJob updated = mongoTemplate.findAndModify(
                q, u, FindAndModifyOptions.options().returnNew(true), StudyPlanJob.class);

        return Optional.ofNullable(updated).map(StudyPlanProgressResponse::from);
    }

    public SubtopicUpdateResult setSubtopicDone(String jobId, String userId,
                                                String subtopicId, boolean targetDone) {

        Query gate = Query.query(
                Criteria.where("jobId").is(jobId)
                        .and("userId").is(userId)
                        .and("plan.mainTopics").elemMatch(
                                Criteria.where("subTopics").elemMatch(
                                        Criteria.where("subtopicId").is(subtopicId)
                                                .and("done").is(!targetDone))));

        Update u = new Update()
                .set("plan.mainTopics.$[].subTopics.$[s].done", targetDone)
                .inc("completedSubtopics", targetDone ? 1 : -1)
                .set("updatedAt", Instant.now())
                .filterArray(Criteria.where("s.subtopicId").is(subtopicId));

        StudyPlanJob updated = mongoTemplate.findAndModify(
                gate, u, FindAndModifyOptions.options().returnNew(true), StudyPlanJob.class);

        if (updated != null) {
            return SubtopicUpdateResult.updated(StudyPlanProgressResponse.from(updated));
        }

        StudyPlanJob job = jobRepository.findByJobId(jobId)
                .filter(j -> userId.equals(j.getUserId()))
                .orElse(null);

        if (job == null) {
            return SubtopicUpdateResult.notFound();
        }
        if (job.getStatus() != JobStatus.DONE || job.getPlan() == null) {
            return SubtopicUpdateResult.notReady();
        }
        if (!subtopicExists(job, subtopicId)) {
            return SubtopicUpdateResult.notFound();
        }
        return SubtopicUpdateResult.updated(StudyPlanProgressResponse.from(job));
    }

    private boolean subtopicExists(StudyPlanJob job, String subtopicId) {
        StudyPlanResponse plan = job.getPlan();
        if (plan == null || plan.getMainTopics() == null) return false;
        for (MainTopic mt : plan.getMainTopics()) {
            if (mt.getSubTopics() == null) continue;
            for (SubTopic st : mt.getSubTopics()) {
                if (subtopicId.equals(st.getSubtopicId())) return true;
            }
        }
        return false;
    }
}