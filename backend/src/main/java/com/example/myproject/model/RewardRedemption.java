package com.example.myproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_redemption")
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redemption_id")
    private Integer redemptionId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @Column(name = "redemption_date")
    private LocalDateTime redemptionDate = LocalDateTime.now();

    @Column(name = "status")
    private String status = "Approved";

    public Integer getRedemptionId() { return redemptionId; }
    public void setRedemptionId(Integer redemptionId) { this.redemptionId = redemptionId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }

    public LocalDateTime getRedemptionDate() { return redemptionDate; }
    public void setRedemptionDate(LocalDateTime redemptionDate) { this.redemptionDate = redemptionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
