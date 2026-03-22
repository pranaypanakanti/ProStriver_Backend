package com.kronos.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String toEmail, String subject, String otp) {
        String body = """
                Hello,

                Your ProStriver verification code is: %s

                This code will expire soon. For your security, do not share this code with anyone.

                If you did not request this code, you can safely ignore this email.

                Regards,
                ProStriver Team
                """.formatted(otp);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}