package com.example.myproject.repository;

import com.example.myproject.model.RewardRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardRuleRepository extends JpaRepository<RewardRule, Integer> {
}