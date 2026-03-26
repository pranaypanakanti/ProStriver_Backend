package com.ProStriver.repository;

import com.ProStriver.entity.RevisionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RevisionPlanRepository extends JpaRepository<RevisionPlan, UUID> {
    Optional<RevisionPlan> findByName(String name);
}