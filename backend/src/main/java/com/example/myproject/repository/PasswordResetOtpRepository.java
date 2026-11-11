package com.example.myproject.repository;

import com.example.myproject.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Integer> {

    Optional<PasswordResetOtp> findByEmailIgnoreCase(String email);
}