package com.kronos.analytics;

import com.kronos.analytics.dto.AnalyticsOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> overview(Authentication auth) {
        return ResponseEntity.ok(analyticsService.overview(auth.getName()));
    }
}