package com.example.myproject.repository;

import com.example.myproject.model.TransactionVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionVerificationRepository
        extends JpaRepository<TransactionVerification, Integer> {

    boolean existsByTransactionId(Integer transactionId);

    List<TransactionVerification> findByStaffIdOrderByActionDateDesc(
            Integer staffId
    );
}