package com.example.myproject.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Every deposit recorded at the staff counter (registered customer or guest). */
@Entity
@Table(name = "walkin_deposit")
public class WalkInDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** null for a guest walk-in */
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "rate_per_kg", precision = 10, scale = 2)
    private BigDecimal ratePerKg;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    /** cash | points | wallet */
    @Column(name = "payout_type", length = 20)
    private String payoutType;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_name", length = 150)
    private String staffName;

    /** The normal recycling_transaction row (only for registered customers) */
    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalkInDeposit() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getRatePerKg() { return ratePerKg; }
    public void setRatePerKg(BigDecimal ratePerKg) { this.ratePerKg = ratePerKg; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }

    public String getPayoutType() { return payoutType; }
    public void setPayoutType(String payoutType) { this.payoutType = payoutType; }

    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}