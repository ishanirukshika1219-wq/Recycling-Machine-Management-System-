package com.example.myproject.repository;

import com.example.myproject.entity.Appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    /*
     * Returns ALL appointments.
     *
     * Newest appointment date first.
     * For same date, latest time first.
     */
    List<Appointment>
    findAllByOrderByAppointmentDateDescAppointmentTimeDesc();


    /*
     * Returns appointments belonging to a customer.
     */
    List<Appointment>
    findByCustomerIdOrderByAppointmentDateDescAppointmentTimeDesc(
            Long customerId
    );




    /*
     * Finds already reserved appointment times
     * for a particular category and date.
     */
    @Query("""
            SELECT a.appointmentTime
            FROM Appointment a
            WHERE a.categoryName = :category
              AND a.appointmentDate = :date
              AND a.status IN :statuses
            """)
    List<LocalTime> findBookedTimes(
            @Param("category") String category,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<String> statuses
    );
    long countByStaffIdAndStatusIgnoreCase(
            Long staffId,
            String status
    );
}