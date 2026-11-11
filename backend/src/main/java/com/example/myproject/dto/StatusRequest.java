package com.example.myproject.dto;

/**
 * Body sent by the admin / staff pages.
 * Admin: { "status": "Confirmed" }
 * Staff: { "status": "Completed", "staffId": 3, "staffName": "Nimal" }  (staff fields optional)
 */
public record StatusRequest(String status, Long staffId, String staffName) {}