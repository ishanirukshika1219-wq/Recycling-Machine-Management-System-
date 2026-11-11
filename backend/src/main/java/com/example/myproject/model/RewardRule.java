package com.example.myproject.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reward_rules")
public class RewardRule {

    @Id
    @Column(name = "rule_id")
    private Integer ruleId = 1;

    @Column(name = "base_points_per_kg", nullable = false)
    private Integer basePointsPerKg = 10;

    @Column(name = "min_tx_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal minTxWeight = new BigDecimal("0.50");

    @Column(name = "redemption_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal redemptionRate = new BigDecimal("0.10");

    @Column(name = "bulk_bonus_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal bulkBonusWeight = new BigDecimal("5.00");

    @Column(name = "bulk_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal bulkMultiplier = new BigDecimal("1.50");

    @Column(name = "first_tx_bonus", nullable = false)
    private Integer firstTxBonus = 50;

    public Integer getRuleId() { return ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Integer getBasePointsPerKg() { return basePointsPerKg; }
    public void setBasePointsPerKg(Integer basePointsPerKg) { this.basePointsPerKg = basePointsPerKg; }

    public BigDecimal getMinTxWeight() { return minTxWeight; }
    public void setMinTxWeight(BigDecimal minTxWeight) { this.minTxWeight = minTxWeight; }

    public BigDecimal getRedemptionRate() { return redemptionRate; }
    public void setRedemptionRate(BigDecimal redemptionRate) { this.redemptionRate = redemptionRate; }

    public BigDecimal getBulkBonusWeight() { return bulkBonusWeight; }
    public void setBulkBonusWeight(BigDecimal bulkBonusWeight) { this.bulkBonusWeight = bulkBonusWeight; }

    public BigDecimal getBulkMultiplier() { return bulkMultiplier; }
    public void setBulkMultiplier(BigDecimal bulkMultiplier) { this.bulkMultiplier = bulkMultiplier; }

    public Integer getFirstTxBonus() { return firstTxBonus; }
    public void setFirstTxBonus(Integer firstTxBonus) { this.firstTxBonus = firstTxBonus; }
}
