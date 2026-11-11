package com.example.myproject.repository;

import com.example.myproject.model.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Integer> {

    List<RewardRedemption> findByCustomerIdOrderByRedemptionDateDesc(Integer customerId);

    List<RewardRedemption> findByStatusOrderByRedemptionDateDesc(String status);
}