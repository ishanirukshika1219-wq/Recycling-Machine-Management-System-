package com.example.myproject.controller;

import com.example.myproject.service.PasswordResetService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/password-reset")
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /** Step 1: email the OTP. Body: { "email": "..." } */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {

        passwordResetService.sendOtp(request.get("email"));

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to " + request.get("email").trim() + "."));
    }

    /** Step 2: check the OTP. Body: { "email": "...", "otp": "123456" } */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {

        String token = passwordResetService.verifyOtp(
                request.get("email"), request.get("otp"));

        return ResponseEntity.ok(Map.of(
                "message", "OTP verified.",
                "resetToken", token));
    }

    /** Step 3: new password. Body: { email, resetToken, newPassword, confirmPassword } */
    @PutMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> request) {

        passwordResetService.resetPassword(
                request.get("email"),
                request.get("resetToken"),
                request.get("newPassword"),
                request.get("confirmPassword"));

        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    /** Every error is sent as { "message": "..." } so the page can show it. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {

        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "Request failed." : e.getReason()));
    }
}