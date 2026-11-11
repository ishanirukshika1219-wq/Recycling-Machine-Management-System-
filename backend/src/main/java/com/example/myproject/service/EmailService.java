package com.example.myproject.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    /** The address emails are sent from (same as spring.mail.username for Gmail). */
    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Call THIS from your customer registration code, right after the customer is saved.
     * Runs in the background, so registration is never slowed down or broken if the mail fails.
     */
    @Async
    public void sendRegistrationEmailAsync(String toEmail, String customerName) {
        try {
            sendRegistrationEmail(toEmail, customerName);
        } catch (Exception e) {
            System.err.println("Could not send registration email to " + toEmail + ": " + e.getMessage());
        }
    }

    /**
     * Sends the "Registration successful" email.
     * You can also call this from your existing customer registration code
     * right after the customer is saved to the database.
     */
    public void sendRegistrationEmail(String toEmail, String customerName) throws MessagingException {

        String safeName = HtmlUtils.htmlEscape(
                customerName == null || customerName.isBlank() ? "Customer" : customerName.trim());

        String html =
                "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                        + "border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
                        + "<div style=\"background:#2e9e5b;color:#fff;padding:20px 24px;\">"
                        + "<h2 style=\"margin:0;\">&#9851; SRMMS</h2>"
                        + "<p style=\"margin:4px 0 0;\">Smart Recycling Machine Management System</p>"
                        + "</div>"
                        + "<div style=\"padding:24px;color:#374151;line-height:1.6;\">"
                        + "<h3 style=\"margin-top:0;\">Registration Successful!</h3>"
                        + "<p>Hi " + safeName + ",</p>"
                        + "<p>Your SRMMS account has been created successfully. "
                        + "You can now log in, book recycling appointments and earn reward points.</p>"
                        + "<p>Thank you for helping us keep the environment clean.</p>"
                        + "<p style=\"margin-bottom:0;\">Happy recycling,<br><strong>SRMMS Team</strong></p>"
                        + "</div>"
                        + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(toEmail.trim());
        helper.setSubject("Registration Successful - Welcome to SRMMS");
        helper.setText(html, true);   // true = HTML

        mailSender.send(message);
    }

    /** Generic HTML email (used for transaction notifications and promotions). */
    public void sendHtmlEmail(String toEmail, String subject, String html) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(toEmail.trim());
        helper.setSubject(subject);
        helper.setText(html, true);   // true = HTML

        mailSender.send(message);
    }
}