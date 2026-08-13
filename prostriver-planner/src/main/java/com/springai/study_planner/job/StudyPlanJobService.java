package com.springai.study_planner.job;

import com.mongodb.client.result.DeleteResult;
import com.springai.kafka.StudyPlanProducer;
import com.springai.kafka.StudyPlanRequest;
import com.springai.study_planner.entities.MainTopic;
import com.springai.study_planner.entities.StudyPlanResponse;
import com.springai.study_planner.entities.SubTopic;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudyPlanJobService {

    private final StudyPlanJobRepository jobRepository;
    private final StudyPlanProducer producer;
    private final MongoTemplate mongoTemplate;
    private static final String USER_ID = "userId";
    private static final String JOB_ID = "jobId";

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
        Query q = Query.query(Criteria.where(JOB_ID).is(jobId).and(USER_ID).is(userId));
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
                Criteria.where(JOB_ID).is(jobId)
                        .and(USER_ID).is(userId)
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

    public List<StudyPlanSummaryResponse> listUserPlans(String userId) {
        Query q = Query.query(Criteria.where(USER_ID).is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        q.fields().exclude("plan");

        return mongoTemplate.find(q, StudyPlanJob.class)
                .stream()
                .map(StudyPlanSummaryResponse::from)
                .toList();
    }

    public boolean deletePlan(String jobId, String userId) {
        Query q = Query.query(Criteria.where(JOB_ID).is(jobId).and(USER_ID).is(userId));
        DeleteResult result = mongoTemplate.remove(q, StudyPlanJob.class);
        return result.getDeletedCount() > 0;
    }

}