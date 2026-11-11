package com.example.myproject.service;

import com.example.myproject.model.Customer;
import com.example.myproject.model.PasswordResetOtp;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.PasswordResetOtpRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Forgot password with an OTP:
 *   1) sendOtp    -> a 6 digit code is emailed to the customer
 *   2) verifyOtp  -> the customer types the code; wrong code = error
 *   3) resetPassword -> only after the code was correct, the new password is saved
 *
 * (No @Transactional on purpose: a wrong-code error must still save the attempt counter.)
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int OTP_VALID_MINUTES = 5;
    private static final int MAX_WRONG_ATTEMPTS = 5;
    private static final int RESEND_WAIT_SECONDS = 30;
    private static final int TOKEN_VALID_MINUTES = 10;

    private final CustomerRepository customerRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(CustomerRepository customerRepository,
                                PasswordResetOtpRepository otpRepository,
                                EmailService emailService) {
        this.customerRepository = customerRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }


    // =========================================================
    // STEP 1: SEND OTP
    // =========================================================

    public void sendOtp(String email) {

        String cleanEmail = clean(email);

        if (cleanEmail.isEmpty()) {
            throw bad("Please enter your email.");
        }

        Customer customer = customerRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No customer account found with this email."));

        Optional<PasswordResetOtp> existing = otpRepository.findByEmailIgnoreCase(cleanEmail);

        // Stops spamming the "Send OTP" button
        if (existing.isPresent() && existing.get().getCreatedAt() != null) {

            long seconds = Duration.between(
                    existing.get().getCreatedAt(), LocalDateTime.now()).getSeconds();

            if (seconds < RESEND_WAIT_SECONDS) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + (RESEND_WAIT_SECONDS - seconds)
                                + " seconds before requesting a new OTP.");
            }
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));

        // The email is sent first: if it fails the customer is told, nothing is saved
        try {
            emailService.sendHtmlEmail(
                    customer.getEmail(),
                    "Your SRMMS password reset code",
                    buildOtpEmail(customer.getName(), otp));
        } catch (Exception e) {
            throw mailFailure(e, customer.getEmail());
        }

        LocalDateTime now = LocalDateTime.now();

        PasswordResetOtp record = existing.orElseGet(PasswordResetOtp::new);

        record.setEmail(customer.getEmail());
        record.setOtpHash(encoder.encode(otp));
        record.setCreatedAt(now);
        record.setExpiresAt(now.plusMinutes(OTP_VALID_MINUTES));
        record.setAttempts(0);
        record.setVerified(false);
        record.setResetToken(null);
        record.setTokenExpiresAt(null);

        otpRepository.save(record);
    }


    // =========================================================
    // STEP 2: CHECK THE OTP  (returns a token for step 3)
    // =========================================================

    public String verifyOtp(String email, String otp) {

        String cleanEmail = clean(email);
        String cleanOtp = clean(otp);

        if (cleanEmail.isEmpty() || cleanOtp.isEmpty()) {
            throw bad("Please enter the OTP.");
        }

        PasswordResetOtp record = otpRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> bad("Please request an OTP first."));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(record);
            throw bad("This OTP has expired. Please request a new one.");
        }

        int attempts = record.getAttempts() == null ? 0 : record.getAttempts();

        if (attempts >= MAX_WRONG_ATTEMPTS) {
            otpRepository.delete(record);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong attempts. Please request a new OTP.");
        }

        // Wrong OTP
        if (!encoder.matches(cleanOtp, record.getOtpHash())) {

            attempts++;
            record.setAttempts(attempts);
            otpRepository.save(record);

            int left = MAX_WRONG_ATTEMPTS - attempts;

            if (left <= 0) {
                otpRepository.delete(record);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many wrong attempts. Please request a new OTP.");
            }

            throw bad("Invalid OTP. Please check the code in your email and try again ("
                    + left + (left == 1 ? " attempt" : " attempts") + " left).");
        }

        // Correct OTP
        String token = UUID.randomUUID().toString();

        record.setVerified(true);
        record.setResetToken(token);
        record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES));

        otpRepository.save(record);

        return token;
    }


    // =========================================================
    // STEP 3: SET THE NEW PASSWORD  (only after the OTP was correct)
    // =========================================================

    public void resetPassword(String email, String token,
                              String newPassword, String confirmPassword) {

        String cleanEmail = clean(email);

        PasswordResetOtp record = otpRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> bad("Please verify your OTP first."));

        boolean tokenOk =
                Boolean.TRUE.equals(record.getVerified())
                        && record.getResetToken() != null
                        && token != null
                        && MessageDigest.isEqual(
                        record.getResetToken().getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))
                        && record.getTokenExpiresAt() != null
                        && record.getTokenExpiresAt().isAfter(LocalDateTime.now());

        if (!tokenOk) {
            otpRepository.delete(record);
            throw bad("Your OTP verification has expired. Please start again.");
        }

        if (newPassword == null || confirmPassword == null
                || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            throw bad("New password and confirm password are required.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw bad("New password and confirm password do not match.");
        }

        if (newPassword.length() < 6) {
            throw bad("Password must contain at least 6 characters.");
        }

        Customer customer = customerRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No customer account found with this email."));

        // Saved in the customer table (BCrypt), the same way the login checks it
        customer.setPassword(encoder.encode(newPassword));
        customerRepository.saveAndFlush(customer);

        // The code cannot be used again
        otpRepository.delete(record);
    }


    // =========================================================
    // MAIL FAILURE: say what really went wrong (page + console)
    // =========================================================

    private ResponseStatusException mailFailure(Exception e, String toEmail) {

        Throwable root = e;

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        // The real reason is printed in the IntelliJ console
        log.error("OTP EMAIL FAILED for {} -> {}: {}",
                toEmail, root.getClass().getSimpleName(), root.getMessage());
        log.error("Check spring.mail.username, spring.mail.password (Gmail App Password) "
                + "and app.mail.from in application.properties", e);

        String message;

        if (e instanceof MailAuthenticationException
                || root.getClass().getSimpleName().contains("AuthenticationFailed")) {

            message = "The server could not log in to its email account "
                    + "(wrong Gmail App Password or username). "
                    + "Please contact the administrator.";

        } else if (root instanceof UnknownHostException
                || root instanceof ConnectException
                || root instanceof SocketTimeoutException) {

            message = "The server could not connect to the email service. "
                    + "Please check the internet connection and try again.";

        } else {

            message = "Could not send the OTP email. Please try again later.";
        }

        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildOtpEmail(String name, String otp) {

        String safeName = HtmlUtils.htmlEscape(
                name == null || name.isBlank() ? "Customer" : name.trim());

        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
                + "<div style=\"background:#2e9e5b;color:#fff;padding:20px 24px;\">"
                + "<h2 style=\"margin:0;\">&#9851; SRMMS</h2></div>"
                + "<div style=\"padding:24px;color:#374151;line-height:1.6;\">"
                + "<h3 style=\"margin-top:0;\">Password Reset</h3>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Use this code to reset your password:</p>"
                + "<p style=\"font-size:32px;font-weight:bold;letter-spacing:8px;"
                + "text-align:center;margin:20px 0;color:#1d6b3e;\">" + otp + "</p>"
                + "<p>This code is valid for " + OTP_VALID_MINUTES + " minutes. "
                + "Do not share it with anyone.</p>"
                + "<p style=\"color:#6b7280;font-size:13px;\">If you did not ask for this, "
                + "you can ignore this email. Your password will not change.</p>"
                + "</div></div>";
    }
}