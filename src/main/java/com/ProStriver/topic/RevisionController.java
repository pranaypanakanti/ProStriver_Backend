package com.ProStriver.topic;

import com.ProStriver.topic.dto.TodayRevisionItemResponse;
import com.ProStriver.topic.dto.UpcomingRevisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Profile("api")
@RestController
@RequestMapping("/api/topics/revisions")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping("/today")
    public ResponseEntity<List<TodayRevisionItemResponse>> today(Authentication auth) {
        return ResponseEntity.ok(revisionService.today(auth.getName()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingRevisionResponse>> upcoming(Authentication auth) {
        return ResponseEntity.ok(revisionService.upcoming(auth.getName()));
    }

    @PatchMapping("/{revisionId}/complete")
    public ResponseEntity<Void> complete(Authentication auth, @PathVariable UUID revisionId) {
        revisionService.complete(auth.getName(), revisionId);
        return ResponseEntity.ok().build();
    }
}