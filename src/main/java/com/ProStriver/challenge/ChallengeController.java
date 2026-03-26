package com.ProStriver.challenge;

import com.ProStriver.challenge.dto.ChallengeMeResponse;
import com.ProStriver.challenge.dto.ChallengePlanResponse;
import com.ProStriver.challenge.dto.SelectChallengeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping("/plans")
    public ResponseEntity<List<ChallengePlanResponse>> plans() {
        return ResponseEntity.ok(challengeService.plans());
    }

    @PostMapping("/select")
    public ResponseEntity<ChallengeMeResponse> select(Authentication auth, @Valid @RequestBody SelectChallengeRequest req) {
        return ResponseEntity.ok(challengeService.select(auth.getName(), req.getChallengeType()));
    }

    @GetMapping("/me")
    public ResponseEntity<ChallengeMeResponse> me(Authentication auth) {
        return ResponseEntity.ok(challengeService.me(auth.getName()));
    }

    @PostMapping("/me/quit")
    public ResponseEntity<Void> quit(Authentication auth) {
        challengeService.quit(auth.getName());
        return ResponseEntity.ok().build();
    }
}