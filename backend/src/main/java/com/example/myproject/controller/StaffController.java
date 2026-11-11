package com.example.myproject.controller;

import com.example.myproject.model.Staff;
import com.example.myproject.model.StaffPasswordResetToken;
import com.example.myproject.model.StaffLoginHistory;

import com.example.myproject.repository.StaffRepository;
import com.example.myproject.repository.StaffPasswordResetTokenRepository;
import com.example.myproject.repository.StaffLoginHistoryRepository;
import com.example.myproject.repository.AppointmentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "*")
public class StaffController {

    private final StaffRepository staffRepository;
    private final StaffPasswordResetTokenRepository resetTokenRepository;
    private final StaffLoginHistoryRepository loginHistoryRepository;
    private final AppointmentRepository appointmentRepository;
    private final JavaMailSender mailSender;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private static final String ALLOWED_EMAIL =
            "staff7247@gmail.com";

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.frontend.base-url:http://localhost:5500}")
    private String frontendBaseUrl;

    public StaffController(
            StaffRepository staffRepository,
            StaffPasswordResetTokenRepository resetTokenRepository,
            StaffLoginHistoryRepository loginHistoryRepository,
            AppointmentRepository appointmentRepository,
            JavaMailSender mailSender) {

        this.staffRepository = staffRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.appointmentRepository = appointmentRepository;
        this.mailSender = mailSender;
    }

    // ==========================================
    // STAFF LOGIN
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {

            response.put("success", false);
            response.put("message", "Email and password are required.");

            return ResponseEntity.badRequest().body(response);
        }

        email = email.trim().toLowerCase();

        if (!ALLOWED_EMAIL.equalsIgnoreCase(email)) {

            response.put("success", false);
            response.put("message", "Invalid staff email or password.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        Optional<Staff> optionalStaff =
                staffRepository.findByEmail(email);

        if (optionalStaff.isEmpty()) {

            response.put("success", false);
            response.put("message", "Invalid staff email or password.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        Staff staff = optionalStaff.get();

        if (staff.getStatus() == null
                || !"Active".equalsIgnoreCase(staff.getStatus().trim())) {

            response.put("success", false);
            response.put("message",
                    "Your staff account is inactive. Contact the administrator.");

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(response);
        }

        String storedPassword = staff.getPassword();

        boolean passwordMatches;

        if (storedPassword != null && storedPassword.startsWith("$2")) {

            passwordMatches =
                    passwordEncoder.matches(password, storedPassword);

        } else {

            passwordMatches =
                    storedPassword != null && storedPassword.equals(password);

            if (passwordMatches) {
                staff.setPassword(passwordEncoder.encode(password));
                staffRepository.save(staff);
            }
        }

        if (!passwordMatches) {

            response.put("success", false);
            response.put("message", "Invalid staff email or password.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        // Create a new login history record
        StaffLoginHistory history = new StaffLoginHistory();
        history.setStaff(staff);
        history.setLoginAt(LocalDateTime.now());
        history.setLogoutAt(null);

        loginHistoryRepository.save(history);

        // Return safe staff details
        Map<String, Object> staffDetails = new HashMap<>();

        staffDetails.put("staffId", staff.getStaffId());
        staffDetails.put("name", staff.getName());
        staffDetails.put("email", staff.getEmail());
        staffDetails.put("loginHistoryId", history.getLoginHistoryId());

        response.put("success", true);
        response.put("message", "Login successful.");
        response.put("staff", staffDetails);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // STAFF LOGOUT
    // ==========================================

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        Object historyIdObject = request.get("loginHistoryId");
        Object staffIdObject = request.get("staffId");

        if (historyIdObject == null || staffIdObject == null) {

            response.put("success", false);
            response.put("message", "Login session details are missing.");

            return ResponseEntity.badRequest().body(response);
        }

        try {

            Long historyId = Long.valueOf(historyIdObject.toString());
            Integer staffId = Integer.valueOf(staffIdObject.toString());

            Optional<StaffLoginHistory> optionalHistory =
                    loginHistoryRepository
                            .findByLoginHistoryIdAndStaff_StaffId(
                                    historyId, staffId
                            );

            if (optionalHistory.isEmpty()) {

                response.put("success", false);
                response.put("message", "Login session was not found.");

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

            StaffLoginHistory history = optionalHistory.get();

            if (history.getLogoutAt() == null) {
                history.setLogoutAt(LocalDateTime.now());
                loginHistoryRepository.save(history);
            }

            response.put("success", true);
            response.put("message", "Logout recorded successfully.");

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {

            response.put("success", false);
            response.put("message", "Invalid login session details.");

            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==========================================
    // ADMIN: STAFF MANAGEMENT DATA
    // ==========================================

    @GetMapping("/management")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getStaffManagement() {

        Map<String, Object> response = new HashMap<>();

        List<Staff> staffList = staffRepository.findAll();

        List<StaffLoginHistory> histories =
                loginHistoryRepository.findAllByOrderByLoginAtDesc();

        List<Map<String, Object>> staffDetailsList =
                staffList.stream().map(staff -> {

                    Map<String, Object> item = new HashMap<>();

                    item.put("staffId", staff.getStaffId());
                    item.put("name", staff.getName());
                    item.put("username", staff.getUsername());
                    item.put("email", staff.getEmail());
                    item.put("status", staff.getStatus());

                    long completedAppointments =
                            appointmentRepository
                                    .countByStaffIdAndStatusIgnoreCase(
                                            staff.getStaffId().longValue(),
                                            "Completed"
                                    );

                    item.put("completedAppointments",
                            completedAppointments);

                    Optional<StaffLoginHistory> latestHistory =
                            histories.stream()
                                    .filter(h -> h.getStaff().getStaffId()
                                            .equals(staff.getStaffId()))
                                    .findFirst();

                    if (latestHistory.isPresent()) {

                        StaffLoginHistory history = latestHistory.get();

                        item.put("lastLogin", history.getLoginAt());
                        item.put("lastLogout", history.getLogoutAt());

                        LocalDateTime endTime =
                                history.getLogoutAt() != null
                                        ? history.getLogoutAt()
                                        : LocalDateTime.now();

                        item.put("lastSessionDuration",
                                formatDuration(
                                        Duration.between(
                                                history.getLoginAt(),
                                                endTime
                                        )
                                ));

                        item.put("currentlyLoggedIn",
                                history.getLogoutAt() == null);

                    } else {

                        item.put("lastLogin", null);
                        item.put("lastLogout", null);
                        item.put("lastSessionDuration", "-");
                        item.put("currentlyLoggedIn", false);
                    }

                    return item;

                }).toList();

        List<Map<String, Object>> historyList =
                histories.stream().map(history -> {

                    Map<String, Object> item = new HashMap<>();

                    item.put("loginHistoryId",
                            history.getLoginHistoryId());

                    item.put("staffId",
                            history.getStaff().getStaffId());

                    item.put("staffName",
                            history.getStaff().getName());

                    item.put("loginAt", history.getLoginAt());
                    item.put("logoutAt", history.getLogoutAt());

                    LocalDateTime endTime =
                            history.getLogoutAt() != null
                                    ? history.getLogoutAt()
                                    : LocalDateTime.now();

                    item.put("duration",
                            formatDuration(
                                    Duration.between(
                                            history.getLoginAt(),
                                            endTime
                                    )
                            ));

                    item.put("sessionStatus",
                            history.getLogoutAt() == null
                                    ? "Logged In"
                                    : "Logged Out");

                    return item;

                }).toList();

        response.put("staff", staffDetailsList);
        response.put("history", historyList);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // ADMIN: ACTIVATE / DEACTIVATE STAFF
    // ==========================================

    @PutMapping("/{staffId}/status")
    public ResponseEntity<Map<String, Object>> updateStaffStatus(
            @PathVariable Integer staffId,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        String status = request.get("status");

        if (status == null
                || (!status.equalsIgnoreCase("Active")
                && !status.equalsIgnoreCase("Inactive"))) {

            response.put("success", false);
            response.put("message", "Status must be Active or Inactive.");

            return ResponseEntity.badRequest().body(response);
        }

        Optional<Staff> optionalStaff =
                staffRepository.findById(staffId);

        if (optionalStaff.isEmpty()) {

            response.put("success", false);
            response.put("message", "Staff member not found.");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        Staff staff = optionalStaff.get();

        staff.setStatus(
                status.equalsIgnoreCase("Active") ? "Active" : "Inactive"
        );

        staffRepository.save(staff);

        response.put("success", true);
        response.put("message", "Staff status updated successfully.");
        response.put("status", staff.getStatus());

        return ResponseEntity.ok(response);
    }

    private String formatDuration(Duration duration) {

        long seconds = Math.max(0, duration.getSeconds());

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format(
                "%02d:%02d:%02d",
                hours, minutes, remainingSeconds
        );
    }

    // ==========================================
    // FORGOT PASSWORD
    // ==========================================

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {

            response.put("success", false);
            response.put("message",
                    "Please enter your staff email address.");

            return ResponseEntity.badRequest().body(response);
        }

        email = email.trim().toLowerCase();

        response.put("success", true);
        response.put("message",
                "If this is the registered staff email, a password reset link has been sent.");

        if (!ALLOWED_EMAIL.equalsIgnoreCase(email)) {
            return ResponseEntity.ok(response);
        }

        Optional<Staff> optionalStaff =
                staffRepository.findByEmail(email);

        if (optionalStaff.isEmpty()) {
            return ResponseEntity.ok(response);
        }

        Staff staff = optionalStaff.get();

        if (staff.getStatus() == null
                || !"Active".equalsIgnoreCase(staff.getStatus().trim())) {
            return ResponseEntity.ok(response);
        }

        resetTokenRepository.deleteByStaff_StaffId(staff.getStaffId());

        String token =
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", "");

        StaffPasswordResetToken resetToken =
                new StaffPasswordResetToken();

        resetToken.setStaff(staff);
        resetToken.setToken(token);
        resetToken.setExpiryDate(
                LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);

        resetTokenRepository.save(resetToken);

        String resetLink =
                frontendBaseUrl.replaceAll("/$", "")
                        + "/staff-reset-password.html?token=" + token;

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(mailFrom);
            message.setTo(staff.getEmail());
            message.setSubject("SRMMS Staff Password Reset");

            message.setText(
                    "Hello " + staff.getName() + ",\n\n"
                            + "We received a request to reset your SRMMS staff password.\n"
                            + "Open the link below within 15 minutes to create a new password:\n\n"
                            + resetLink + "\n\n"
                            + "If you did not request this, you can ignore this email.\n\n"
                            + "SRMMS Team"
            );

            mailSender.send(message);

        } catch (Exception ex) {

            resetTokenRepository.delete(resetToken);

            response.put("success", false);
            response.put("message",
                    "The reset email could not be sent. Please check the email configuration and try again.");

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // RESET PASSWORD
    // ==========================================

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        String token = request.get("token");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (token == null || token.trim().isEmpty()
                || newPassword == null
                || confirmPassword == null) {

            response.put("success", false);
            response.put("message", "All fields are required.");

            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword.length() < 8) {

            response.put("success", false);
            response.put("message",
                    "Password must contain at least 8 characters.");

            return ResponseEntity.badRequest().body(response);
        }

        if (!newPassword.equals(confirmPassword)) {

            response.put("success", false);
            response.put("message", "Passwords do not match.");

            return ResponseEntity.badRequest().body(response);
        }

        Optional<StaffPasswordResetToken> optionalToken =
                resetTokenRepository.findByTokenAndUsedFalse(
                        token.trim());

        if (optionalToken.isEmpty()) {

            response.put("success", false);
            response.put("message",
                    "This reset link is invalid or has already been used.");

            return ResponseEntity.badRequest().body(response);
        }

        StaffPasswordResetToken resetToken = optionalToken.get();

        if (resetToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            resetTokenRepository.delete(resetToken);

            response.put("success", false);
            response.put("message",
                    "This reset link has expired. Please request a new one.");

            return ResponseEntity.badRequest().body(response);
        }

        Staff staff = resetToken.getStaff();

        staff.setPassword(passwordEncoder.encode(newPassword));

        staffRepository.save(staff);

        resetTokenRepository.deleteByStaff_StaffId(
                staff.getStaffId());

        response.put("success", true);
        response.put("message",
                "Your password has been reset successfully. You can now log in.");

        return ResponseEntity.ok(response);
    }
}