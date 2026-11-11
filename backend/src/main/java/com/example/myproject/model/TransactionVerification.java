package com.example.myproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_verification")
public class TransactionVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Integer verificationId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Integer transactionId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "action_date")
    private LocalDateTime actionDate = LocalDateTime.now();

    public Integer getVerificationId() {
        return verificationId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }
}