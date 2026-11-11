
package com.example.myproject.repository;

import com.example.myproject.model.StaffPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffPasswordResetTokenRepository
        extends JpaRepository<StaffPasswordResetToken, Long> {

    Optional<StaffPasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByStaff_StaffId(Integer staffId);
}
