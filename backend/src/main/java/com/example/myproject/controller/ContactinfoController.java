package com.example.myproject.controller;

import com.example.myproject.model.Admin;
import com.example.myproject.repository.AdminRepository;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public data used by the website pages (home, about, categories, contact ...).
 * Only the system name, phone, email and location are returned.
 * (The admin login email, password and other settings are never exposed here.)
 */
@RestController
@RequestMapping("/api/contact-info")
@CrossOrigin(origins = "*")
public class ContactinfoController {

    private final AdminRepository adminRepository;

    public ContactinfoController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @GetMapping
    public Map<String, Object> getContactInfo() {

        // The first admin (lowest AdminID) is the one whose details are shown
        Admin admin = adminRepository
                .findAll(Sort.by("adminId"))
                .stream()
                .findFirst()
                .orElse(null);

        Map<String, Object> info = new LinkedHashMap<>();

        info.put("systemName", admin == null || admin.getSystemName() == null ? "" : admin.getSystemName());
        info.put("phone", admin == null || admin.getPhone() == null ? "" : admin.getPhone());
        info.put("email", admin == null || admin.getAlertEmail() == null ? "" : admin.getAlertEmail());
        info.put("location", admin == null || admin.getLocation() == null ? "" : admin.getLocation());

        return info;
    }
}