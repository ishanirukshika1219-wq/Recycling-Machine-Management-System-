package com.example.myproject.controller;

import com.example.myproject.dto.AppointmentRequest;
import com.example.myproject.dto.StatusRequest;
import com.example.myproject.entity.Appointment;
import com.example.myproject.service.AppointmentService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(
            AppointmentService appointmentService
    ) {
        this.appointmentService = appointmentService;
    }

    // =========================================================
    // GET ALL APPOINTMENTS
    // =========================================================

    /**
     * Used by Admin and Staff dashboards.
     *
     * Returns all appointments from database.
     */
    @GetMapping
    public List<Appointment> getAll() {

        return appointmentService.getAll();
    }

    // =========================================================
    // CUSTOMER APPOINTMENTS
    // =========================================================

    @GetMapping("/customer/{customerId}")
    public List<Appointment> getByCustomer(
            @PathVariable Long customerId
    ) {

        return appointmentService.getByCustomer(
                customerId
        );
    }

    // =========================================================
    // AVAILABLE SLOTS
    // =========================================================

    @GetMapping("/available-slots")
    public List<String> getAvailableSlots(

            @RequestParam String category,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date
    ) {

        return appointmentService.getAvailableSlots(
                category,
                date
        );
    }

    // =========================================================
    // CUSTOMER BOOKING
    // =========================================================

    @PostMapping
    public ResponseEntity<Appointment> book(
            @RequestBody AppointmentRequest request
    ) {

        Appointment appointment =
                appointmentService.book(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointment);
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @PutMapping("/{id}/status")
    public Appointment updateStatus(

            @PathVariable Long id,

            @RequestBody StatusRequest request
    ) {

        return appointmentService.updateStatus(
                id,
                request.status(),
                request.staffId(),
                request.staffName()
        );
    }

    // =========================================================
    // CUSTOMER CANCEL
    // =========================================================

    @PutMapping("/{id}/cancel")
    public Appointment cancel(

            @PathVariable Long id,

            @RequestParam Long customerId
    ) {

        return appointmentService.cancelByCustomer(
                id,
                customerId
        );
    }

    // =========================================================
    // ERROR HANDLING
    // =========================================================

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException e
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            IllegalStateException.class
    )
    public ResponseEntity<Map<String, String>> conflict(
            IllegalStateException e
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }
}