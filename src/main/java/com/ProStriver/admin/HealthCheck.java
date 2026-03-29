package com.ProStriver.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("api")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class HealthCheck {

    @GetMapping("/health-check")
    public String healthCheck(){
        return "Positive";
    }
}
