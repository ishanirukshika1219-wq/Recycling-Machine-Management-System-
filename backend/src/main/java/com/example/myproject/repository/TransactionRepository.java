package com.example.myproject.repository;

import com.example.myproject.model.RecyclingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<RecyclingTransaction, Integer> {

    List<RecyclingTransaction> findByCustomerId(Integer customerId);

    List<RecyclingTransaction> findByStatusIgnoreCaseOrderByTransactionDateDesc(
            String status
    );

    List<RecyclingTransaction> findByStatusIgnoreCaseAndSourceIgnoreCaseOrderByTransactionDateDesc(
            String status, String source
    );

    // Get all manual walk-in deposits, newest first
    List<RecyclingTransaction> findBySourceIgnoreCaseOrderByTransactionDateDesc(
            String source
    );


}