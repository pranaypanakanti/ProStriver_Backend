package com.ProStriver.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Component
public class BrevoHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(BrevoHealthIndicator.class);

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @PostConstruct
    public void checkBrevoConnectivity() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/account",
                    HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Brevo API connectivity OK");
            } else {
                log.warn("Brevo API returned unexpected status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Brevo API connectivity check FAILED — emails will not work!", e);
        }
    }
}