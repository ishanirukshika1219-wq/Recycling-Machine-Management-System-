package com.example.myproject.service;

import com.example.myproject.dto.AppointmentRequest;
import com.example.myproject.entity.Appointment;
import com.example.myproject.repository.AppointmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentService {

    public static final String PENDING = "Pending";
    public static final String CONFIRMED = "Confirmed";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";

    /*
     * These statuses keep a time slot reserved.
     * Cancelled appointments make the slot available again.
     */
    private static final Set<String> BLOCKING = Set.of(
            PENDING,
            CONFIRMED,
            COMPLETED
    );

    /*
     * Available appointment time slots.
     * Change only if your recycling center has different operating hours.
     */
    private static final List<LocalTime> SLOTS = List.of(
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(13, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            LocalTime.of(17, 0)
    );

    private static final DateTimeFormatter HHMM =
            DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    // =========================================================
    // READ - GET ALL APPOINTMENTS
    // =========================================================

    /**
     * Returns ALL appointments from the database.
     *
     * Important:
     * No date filtering is done here.
     * Therefore old, today's and future appointments remain available
     * to Admin and Staff dashboards.
     */
    public List<Appointment> getAll() {
        return appointmentRepository
                .findAllByOrderByAppointmentDateDescAppointmentTimeDesc();
    }

    /**
     * Returns appointments belonging to one customer.
     */
    public List<Appointment> getByCustomer(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }

        return appointmentRepository
                .findByCustomerIdOrderByAppointmentDateDescAppointmentTimeDesc(
                        customerId
                );
    }

    // =========================================================
    // AVAILABLE TIME SLOTS
    // =========================================================

    /**
     * Returns available appointment slots for a category and date.
     *
     * Booked Pending / Confirmed / Completed slots are blocked.
     * Cancelled slots become available again.
     *
     * For today's date, already-passed times are removed.
     */
    public List<String> getAvailableSlots(
            String category,
            LocalDate date
    ) {

        if (isBlank(category)) {
            return List.of();
        }

        if (date == null) {
            return List.of();
        }

        // Do not allow booking dates in the past.
        if (date.isBefore(LocalDate.now())) {
            return List.of();
        }

        List<LocalTime> booked =
                appointmentRepository.findBookedTimes(
                        category.trim(),
                        date,
                        BLOCKING
                );

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        boolean isToday = date.equals(today);

        return SLOTS.stream()

                // Remove already booked slots
                .filter(slot -> !booked.contains(slot))

                // If today, remove already-passed slots
                .filter(slot -> !isToday || slot.isAfter(now))

                // Convert LocalTime -> HH:mm
                .map(slot -> slot.format(HHMM))

                .toList();
    }

    // =========================================================
    // CUSTOMER BOOKING
    // =========================================================

    /**
     * Creates a new customer appointment.
     *
     * New appointments are always created as Pending.
     */
    @Transactional
    public synchronized Appointment book(AppointmentRequest req) {

        if (req == null) {
            throw new IllegalArgumentException(
                    "Appointment details are missing."
            );
        }

        // -----------------------------------------------------
        // CUSTOMER
        // -----------------------------------------------------

        if (req.customerId() == null) {
            throw new IllegalArgumentException(
                    "Customer is missing. Please log in again."
            );
        }

        // -----------------------------------------------------
        // CATEGORY
        // -----------------------------------------------------

        if (isBlank(req.categoryName())) {
            throw new IllegalArgumentException(
                    "Please select a waste category."
            );
        }

        // -----------------------------------------------------
        // DATE + TIME
        // -----------------------------------------------------

        LocalDate date;
        LocalTime time;

        try {

            date = LocalDate.parse(
                    req.appointmentDate()
            );

            time = LocalTime.parse(
                    req.appointmentTime()
            );

        } catch (DateTimeParseException | NullPointerException e) {

            throw new IllegalArgumentException(
                    "Please select a valid appointment date and time."
            );
        }

        // -----------------------------------------------------
        // DATE VALIDATION
        // -----------------------------------------------------

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "You cannot book an appointment for a past date."
            );
        }

        // -----------------------------------------------------
        // TIME NORMALIZATION
        // -----------------------------------------------------

        time = time.withSecond(0).withNano(0);

        // -----------------------------------------------------
        // CATEGORY NORMALIZATION
        // -----------------------------------------------------

        String category = req.categoryName().trim();

        // -----------------------------------------------------
        // CHECK SLOT
        // -----------------------------------------------------

        String requestedTime = time.format(HHMM);

        List<String> availableSlots =
                getAvailableSlots(category, date);

        if (!availableSlots.contains(requestedTime)) {

            throw new IllegalStateException(
                    "That appointment time is no longer available. "
                            + "Please select another time."
            );
        }

        // -----------------------------------------------------
        // CREATE APPOINTMENT
        // -----------------------------------------------------

        Appointment appointment = new Appointment();

        appointment.setCustomerId(
                req.customerId()
        );

        appointment.setCustomerName(
                isBlank(req.customerName())
                        ? "Customer"
                        : req.customerName().trim()
        );

        appointment.setCategoryName(
                category
        );

        appointment.setAppointmentDate(
                date
        );

        appointment.setAppointmentTime(
                time
        );

        appointment.setNotes(
                isBlank(req.notes())
                        ? null
                        : req.notes().trim()
        );

        /*
         * Customer bookings always start as Pending.
         *
         * Admin later changes:
         * Pending -> Confirmed
         *
         * Staff later changes:
         * Confirmed -> Completed
         */
        appointment.setStatus(PENDING);

        appointment.setCreatedAt(
                LocalDateTime.now()
        );

        return appointmentRepository.save(
                appointment
        );
    }

    // =========================================================
    // STATUS UPDATE
    // =========================================================

    /**
     * Changes appointment status.
     *
     * Allowed workflow:
     *
     * Pending   -> Confirmed
     * Pending   -> Cancelled
     *
     * Confirmed -> Completed
     * Confirmed -> Cancelled
     */
    @Transactional
    public Appointment updateStatus(
            Long id,
            String newStatus,
            Long staffId,
            String staffName
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Appointment ID is required."
            );
        }

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found."
                                )
                        );

        String target =
                normalizeStatus(newStatus);

        String current =
                normalizeStatus(appointment.getStatus());

        // -----------------------------------------------------
        // VALID STATUS FLOW
        // -----------------------------------------------------

        boolean allowed = false;

        // Pending -> Confirmed / Cancelled
        if (PENDING.equals(current)) {

            allowed =
                    CONFIRMED.equals(target)
                            || CANCELLED.equals(target);
        }

        // Confirmed -> Completed / Cancelled
        else if (CONFIRMED.equals(current)) {

            allowed =
                    COMPLETED.equals(target)
                            || CANCELLED.equals(target);
        }

        if (!allowed) {

            throw new IllegalStateException(
                    "Cannot change appointment status from "
                            + current
                            + " to "
                            + target
                            + "."
            );
        }

        // -----------------------------------------------------
        // UPDATE STATUS
        // -----------------------------------------------------

        appointment.setStatus(target);

        // -----------------------------------------------------
        // STAFF DETAILS
        // -----------------------------------------------------

        /*
         * When staff handles the appointment,
         * save staff ID and staff name.
         */
        if (staffId != null) {

            appointment.setStaffId(
                    staffId
            );

            appointment.setStaffName(
                    isBlank(staffName)
                            ? null
                            : staffName.trim()
            );
        }

        return appointmentRepository.save(
                appointment
        );
    }

    // =========================================================
    // CUSTOMER CANCEL
    // =========================================================

    /**
     * Customer can cancel only their own
     * Pending or Confirmed appointment.
     */
    @Transactional
    public Appointment cancelByCustomer(
            Long id,
            Long customerId
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Appointment ID is required."
            );
        }

        if (customerId == null) {
            throw new IllegalArgumentException(
                    "Customer ID is required."
            );
        }

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found."
                                )
                        );

        // -----------------------------------------------------
        // OWNER CHECK
        // -----------------------------------------------------

        if (!customerId.equals(
                appointment.getCustomerId()
        )) {

            throw new IllegalArgumentException(
                    "You can only cancel your own appointments."
            );
        }

        // -----------------------------------------------------
        // STATUS CHECK
        // -----------------------------------------------------

        String current =
                normalizeStatus(
                        appointment.getStatus()
                );

        if (!PENDING.equals(current)
                && !CONFIRMED.equals(current)) {

            throw new IllegalStateException(
                    "This appointment can no longer be cancelled."
            );
        }

        // -----------------------------------------------------
        // CANCEL
        // -----------------------------------------------------

        appointment.setStatus(
                CANCELLED
        );

        return appointmentRepository.save(
                appointment
        );
    }

    // =========================================================
    // STATUS NORMALIZATION
    // =========================================================

    /**
     * Converts different status formats into
     * the standard database status values.
     *
     * Example:
     * "pending"   -> "Pending"
     * "CONFIRMED"  -> "Confirmed"
     * "completed"  -> "Completed"
     * "canceled"   -> "Cancelled"
     */
    private String normalizeStatus(String status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Status is required."
            );
        }

        return switch (
                status.trim().toLowerCase()
                ) {

            case "pending" ->
                    PENDING;

            case "confirmed" ->
                    CONFIRMED;

            case "completed" ->
                    COMPLETED;

            case "cancelled",
                 "canceled" ->
                    CANCELLED;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown appointment status: "
                                    + status
                    );
        };
    }

    // =========================================================
    // STRING HELPER
    // =========================================================

    private boolean isBlank(String value) {

        return value == null
                || value.trim().isEmpty();
    }
}