package com.society.jaswanti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@jaswanti.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "utf-8");

            // Modern HTML email template
            String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>"
                    + "<div style='background-color: #d32f2f; padding: 20px; text-align: center; color: white;'>"
                    + "  <h1 style='margin: 0; font-size: 24px; font-weight: bold;'>Jaswanti Residency</h1>"
                    + "</div>"
                    + "<div style='padding: 30px; background-color: #ffffff; color: #333333;'>"
                    + "  <h2 style='color: #d32f2f; margin-top: 0;'>Action Required</h2>"
                    + "  <p style='font-size: 16px; line-height: 1.5;'>" + body.replace("\n", "<br>") + "</p>"
                    + "</div>"
                    + "<div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 12px; color: #888888;'>"
                    + "  &copy; " + java.time.Year.now().getValue() + " Jaswanti Residency. All rights reserved."
                    + "</div>"
                    + "</div>";

            helper.setText(htmlMsg, true); // true indicates HTML
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("Failed to send HTML email: {}", e.getMessage());
        }
    }
}
