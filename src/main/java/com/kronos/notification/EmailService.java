package com.kronos.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${kronos.mail.from:no-reply@prostriver.me}")
    private String from;

    @Value("${kronos.mail.from-name:ProStriver}")
    private String fromName;

    public void sendReminder(String toEmail, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(new InternetAddress(from, fromName));

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }

    public void sendOtp(String toEmail, String subject, String otp) {
        String body = """
                Hello,

                Your Kronos verification code is: %s

                This code will expire soon. For your security, do not share this code with anyone.

                If you did not request this code, you can safely ignore this email.

                Regards,
                Kronos Team
                """.formatted(otp);

        sendReminder(toEmail, subject, body);
    }
}