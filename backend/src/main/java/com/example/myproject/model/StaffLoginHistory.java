package com.example.myproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_login_history")
public class StaffLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Long loginHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StaffID", nullable = false)
    private Staff staff;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    public StaffLoginHistory() {
    }

    public Long getLoginHistoryId() {
        return loginHistoryId;
    }

    public void setLoginHistoryId(Long loginHistoryId) {
        this.loginHistoryId = loginHistoryId;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public LocalDateTime getLogoutAt() {
        return logoutAt;
    }

    public void setLogoutAt(LocalDateTime logoutAt) {
        this.logoutAt = logoutAt;
    }
}