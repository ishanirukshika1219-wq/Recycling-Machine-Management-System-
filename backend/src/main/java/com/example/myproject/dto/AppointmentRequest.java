package com.example.myproject.dto;

/**
 * Data received from the customer appointment
 * booking page.
 *
 * appointmentDate format:
 * yyyy-MM-dd
 *
 * appointmentTime format:
 * HH:mm
 */
public record AppointmentRequest(

        Long customerId,

        String customerName,

        String categoryName,

        String appointmentDate,

        String appointmentTime,

        String notes

) {
}