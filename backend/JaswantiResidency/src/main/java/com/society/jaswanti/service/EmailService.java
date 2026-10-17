package com.society.jaswanti.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "utf-8");
            
            // Modern, impressive HTML email template
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
            helper.setFrom("simplymalhar@gmail.com");
            
            mailSender.send(mimeMessage);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Failed to send HTML email: " + e.getMessage());
        }
    }
}
