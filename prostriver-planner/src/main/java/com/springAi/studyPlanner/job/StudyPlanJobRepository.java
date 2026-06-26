package com.springAi.studyPlanner.job;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudyPlanJobRepository extends MongoRepository<StudyPlanJob, String> {

    Optional<StudyPlanJob> findByJobId(String jobId);
}