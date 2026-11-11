package com.example.myproject.repository;
import com.example.myproject.model.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


    @Repository
    public interface RewardAccountRepository extends JpaRepository<RewardAccount, Integer> {

        /**
         * Find customer reward account using their Customer ID.
         * Required for fetching or updating points during recycling transactions.
         */
        Optional<RewardAccount> findByCustomerId(Integer customerId);
    }

