package com.example.myproject.controller;

import com.example.myproject.model.Admin;
import com.example.myproject.repository.AdminRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminRepository adminRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // digits, + - ( ) and spaces, 7 to 20 characters
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9+\\-\\s()]{7,20}$");

    public AdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private Map<String, Object> message(String text) {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("message", text);

        return m;
    }

    /* The password hash is never sent to the browser */

    private Map<String, Object> settingsMap(Admin admin) {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("adminId", admin.getAdminId());
        m.put("name", admin.getName());
        m.put("email", admin.getEmail());
        m.put("systemName", admin.getSystemName());
        m.put("alertEmail", admin.getAlertEmail());
        m.put("phone", admin.getPhone());
        m.put("location", admin.getLocation());
        m.put("currency", admin.getCurrency());
        m.put("autoVerification", admin.getAutoVerification());
        m.put("maintenanceMode", admin.getMaintenanceMode());
        m.put("smsAlerts", admin.getSmsAlerts());

        return m;
    }

    private boolean passwordMatches(String raw, String stored) {

        if (stored == null) {
            return false;
        }

        if (stored.startsWith("$2")) {
            return passwordEncoder.matches(raw, stored);
        }

        /* Older plain-text passwords */
        return stored.equals(raw);
    }


    // =========================================================
    // GET ADMIN SETTINGS
    // =========================================================

    @GetMapping("/settings/{id}")
    public ResponseEntity<?> getSettings(@PathVariable Integer id) {

        Admin admin = adminRepository.findById(id).orElse(null);

        if (admin == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(message("Admin not found."));
        }

        return ResponseEntity.ok(settingsMap(admin));
    }


    // =========================================================
    // SAVE ADMIN SETTINGS
    // =========================================================

    @PutMapping("/settings/{id}")
    public ResponseEntity<?> saveSettings(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {

        Admin admin = adminRepository.findById(id).orElse(null);

        if (admin == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(message("Admin not found."));
        }

        String systemName =
                body.get("systemName") == null
                        ? "" : body.get("systemName").toString().trim();

        String alertEmail =
                body.get("alertEmail") == null
                        ? "" : body.get("alertEmail").toString().trim();

        String currency =
                body.get("currency") == null
                        ? "LKR" : body.get("currency").toString().trim();

        if (systemName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(message("System name is required."));
        }

        if (alertEmail.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(message("Administrator alert email is required."));
        }

        // ---------------------------------------------------------
        // NEW: PHONE NUMBER + LOCATION (both optional)
        // ---------------------------------------------------------

        if (body.containsKey("phone")) {

            String phone =
                    body.get("phone") == null
                            ? "" : body.get("phone").toString().trim();

            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                return ResponseEntity.badRequest()
                        .body(message("Enter a valid phone number (digits, +, -, spaces only)."));
            }

            admin.setPhone(phone);
        }

        if (body.containsKey("location")) {

            String location =
                    body.get("location") == null
                            ? "" : body.get("location").toString().trim();

            if (location.length() > 255) {
                return ResponseEntity.badRequest()
                        .body(message("Location is too long (maximum 255 characters)."));
            }

            admin.setLocation(location);
        }

        admin.setSystemName(systemName);
        admin.setAlertEmail(alertEmail);
        admin.setCurrency(currency);

        admin.setAutoVerification(
                Boolean.TRUE.equals(body.get("autoVerification")));

        admin.setMaintenanceMode(
                Boolean.TRUE.equals(body.get("maintenanceMode")));

        admin.setSmsAlerts(
                Boolean.TRUE.equals(body.get("smsAlerts")));

        adminRepository.save(admin);

        return ResponseEntity.ok(settingsMap(admin));
    }

    // =========================================================
// ADMIN LOGIN
// =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(
            @RequestBody LoginRequest request) {

        Optional<Admin> adminOpt =
                adminRepository.findByEmailIgnoreCase(request.getEmail());

        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();

            if (passwordMatches(
                    request.getPassword(),
                    admin.getPassword())) {

                return ResponseEntity.ok(settingsMap(admin));
            }
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(message("Invalid email or password."));
    }


    // =========================================================
    // CHANGE ADMIN PASSWORD
    // =========================================================

    @PutMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        Admin admin = adminRepository.findById(id).orElse(null);

        if (admin == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(message("Admin not found."));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (currentPassword == null || newPassword == null
                || confirmPassword == null) {

            return ResponseEntity.badRequest()
                    .body(message("Please fill all password fields."));
        }

        if (!passwordMatches(currentPassword, admin.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(message("Current password is incorrect."));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(message("New password must contain at least 6 characters."));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(message("New password and confirm password do not match."));
        }

        admin.setPassword(passwordEncoder.encode(newPassword));

        adminRepository.save(admin);

        return ResponseEntity.ok(message("Password updated successfully."));
    }

}