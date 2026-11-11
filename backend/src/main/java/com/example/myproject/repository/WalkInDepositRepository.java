
package com.example.myproject.repository;

import com.example.myproject.model.WalkInDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WalkInDepositRepository
        extends JpaRepository<WalkInDeposit, Integer> {

    List<WalkInDeposit> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    // Get all walk-in deposit history
    List<WalkInDeposit> findAllByOrderByCreatedAtDesc();

    // Get walk-in deposits processed by a specific staff member
    List<WalkInDeposit> findByStaffIdOrderByCreatedAtDesc(Integer staffId);
}
