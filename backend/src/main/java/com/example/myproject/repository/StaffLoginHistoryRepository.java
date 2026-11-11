package com.example.myproject.repository;

import com.example.myproject.model.StaffLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffLoginHistoryRepository
        extends JpaRepository<StaffLoginHistory, Long> {

    List<StaffLoginHistory>
    findAllByOrderByLoginAtDesc();

    Optional<StaffLoginHistory>
    findByLoginHistoryIdAndStaff_StaffId(
            Long loginHistoryId,
            Integer staffId
    );

    Optional<StaffLoginHistory>
    findTopByStaff_StaffIdOrderByLoginAtDesc(
            Integer staffId
    );
}