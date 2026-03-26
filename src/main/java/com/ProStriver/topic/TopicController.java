package com.ProStriver.topic;

import com.ProStriver.entity.enums.TopicStatus;
import com.ProStriver.topic.dto.CreateTopicRequest;
import com.ProStriver.topic.dto.PatchTopicRequest;
import com.ProStriver.topic.dto.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    public ResponseEntity<TopicResponse> create(Authentication auth, @Valid @RequestBody CreateTopicRequest req) {
        return ResponseEntity.ok(topicService.create(auth.getName(), req));
    }

    @GetMapping
    public ResponseEntity<Page<TopicResponse>> list(
            Authentication auth,
            @RequestParam(required = false) TopicStatus status,
            @RequestParam(required = false) LocalDate createdDate,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(topicService.list(auth.getName(), status, createdDate, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> getOne(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(topicService.getOne(auth.getName(), id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TopicResponse> patch(Authentication auth, @PathVariable UUID id, @Valid @RequestBody PatchTopicRequest req) {
        return ResponseEntity.ok(topicService.patch(auth.getName(), id, req));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(Authentication auth, @PathVariable UUID id) {
        topicService.archive(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        topicService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}