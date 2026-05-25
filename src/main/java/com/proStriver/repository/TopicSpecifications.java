package com.proStriver.repository;

import com.proStriver.entity.Topic;
import com.proStriver.entity.enums.TopicStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TopicSpecifications {

    private TopicSpecifications() {}

    public static Specification<Topic> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Topic> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Topic> hasStatus(TopicStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Topic> createdOnOrAfter(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Topic> createdBefore(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    public static Specification<Topic> searchText(String q) {
        if (q == null || q.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern)
            );
        };
    }
}