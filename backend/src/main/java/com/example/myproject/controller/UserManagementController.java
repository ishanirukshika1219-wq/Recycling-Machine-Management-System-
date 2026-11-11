package com.example.myproject.controller;

import com.example.myproject.model.Admin;
import com.example.myproject.model.Customer;
import com.example.myproject.model.RewardAccount;
import com.example.myproject.model.Staff;

import com.example.myproject.repository.AdminRepository;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.RewardAccountRepository;
import com.example.myproject.repository.StaffRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AdminRepository adminRepository;
    private final RewardAccountRepository rewardAccountRepository;

    public UserManagementController(
            CustomerRepository customerRepository,
            StaffRepository staffRepository,
            AdminRepository adminRepository,
            RewardAccountRepository rewardAccountRepository) {

        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.adminRepository = adminRepository;
        this.rewardAccountRepository = rewardAccountRepository;
    }


    // =========================================================
    // KPI STATS
    // =========================================================

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {

        List<Customer> customers = customerRepository.findAll();
        List<Staff> staffList = staffRepository.findAll();
        long totalAdmins = adminRepository.count();

        long deactivatedCustomers = customers.stream()
                .filter(c -> "Deactivated".equalsIgnoreCase(c.getStatus()))
                .count();

        long deactivatedStaff = staffList.stream()
                .filter(s -> "Deactivated".equalsIgnoreCase(s.getStatus()))
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalCustomers", customers.size());
        stats.put("collectionStaff", staffList.size());
        stats.put("systemAdmins", totalAdmins);
        stats.put("deactivatedAccounts", deactivatedCustomers + deactivatedStaff);

        return ResponseEntity.ok(stats);
    }


    // =========================================================
    // FULL USER DIRECTORY (customers + staff + admins combined)
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getAllUsers() {

        List<Map<String, Object>> result = new ArrayList<>();


        /* ---- CUSTOMERS ---- */

        for (Customer c : customerRepository.findAll()) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("userId", "CUST-" + String.format("%06d", c.getCustomerId()));
            row.put("rawId", c.getCustomerId());
            row.put("role", "customer");
            row.put("roleLabel", "Customer");
            row.put("name", c.getName());
            row.put("email", c.getEmail());
            row.put("phone", c.getPhone());
            row.put("registeredDate", c.getRegisteredDate());
            row.put("profilePhoto", c.getProfilePhoto());

            Integer points = rewardAccountRepository
                    .findByCustomerId(c.getCustomerId())
                    .map(RewardAccount::getPointsBalance)
                    .orElse(0);

            row.put("activityText", points + " Points");

            row.put("status", c.getStatus() == null ? "Active" : c.getStatus());

            result.add(row);
        }


        /* ---- COLLECTION STAFF ---- */

        for (Staff s : staffRepository.findAll()) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("userId", "STF-" + String.format("%03d", s.getStaffId()));
            row.put("rawId", s.getStaffId());
            row.put("role", "staff");
            row.put("roleLabel", "Staff");
            row.put("name", s.getName());
            row.put("email", s.getUsername());
            row.put("phone", null);
            row.put("registeredDate", null);
            row.put("profilePhoto", null);

            row.put("activityText", "-");

            row.put("status", s.getStatus() == null ? "Active" : s.getStatus());

            result.add(row);
        }


        /* ---- SYSTEM ADMINS ---- */

        for (Admin a : adminRepository.findAll()) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("userId", "ADM-" + String.format("%03d", a.getAdminId()));
            row.put("rawId", a.getAdminId());
            row.put("role", "admin");
            row.put("roleLabel", "Admin");
            row.put("name", a.getName());
            row.put("email", a.getEmail());
            row.put("phone", null);
            row.put("registeredDate", null);
            row.put("profilePhoto", null);

            row.put("activityText", "System Owner");

            /* Admins have no status column - always treated as Active */

            row.put("status", "Active");

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }


    // =========================================================
    // UPDATE CUSTOMER STATUS (Deactivate / Activate)
    // =========================================================

    @PostMapping("/customers/{id}/status")
    public ResponseEntity<?> updateCustomerStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {

        String status = (String) request.get("status");

        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Status is required.");
        }

        Customer customer = customerRepository.findById(id).orElse(null);

        if (customer == null) {
            return ResponseEntity.badRequest().body("Customer not found.");
        }

        customer.setStatus(status);
        customerRepository.save(customer);

        return ResponseEntity.ok(customer);
    }


    // =========================================================
    // UPDATE STAFF STATUS (Deactivate / Activate)
    // =========================================================

    @PostMapping("/staff/{id}/status")
    public ResponseEntity<?> updateStaffStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {

        String status = (String) request.get("status");

        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Status is required.");
        }

        Staff staff = staffRepository.findById(id).orElse(null);

        if (staff == null) {
            return ResponseEntity.badRequest().body("Staff not found.");
        }

        staff.setStatus(status);
        staffRepository.save(staff);

        return ResponseEntity.ok(staff);
    }
}
