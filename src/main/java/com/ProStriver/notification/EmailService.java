package com.ProStriver.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name}")
    private String senderName;

    public EmailService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendReminder(String toEmail, String subject, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", body
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_API_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} | subject: {}", toEmail, subject);
            } else {
                log.error("Brevo API returned {}: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Brevo API error: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Brevo API client error sending to {}: {} - {}",
                    toEmail, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to send email via Brevo API", e);
        } catch (Exception e) {
            log.error("Failed to send email to {} via Brevo API", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendOtp(String toEmail, String subject, String otp) {
        String body = """
                Hello,

                Your ProStriver verification code is: %s

                This code will expire soon. For your security, do not share this code with anyone.

                If you did not request this code, you can safely ignore this email.

                Regards,
                ProStriver Team
                """.formatted(otp);

        sendReminder(toEmail, subject, body);
    }
}