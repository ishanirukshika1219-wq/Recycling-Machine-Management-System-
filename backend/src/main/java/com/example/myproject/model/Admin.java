package com.example.myproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Admin")
public class Admin {

    // =========================================================
    // ADMIN ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AdminID")
    private Integer adminId;


    // =========================================================
    // ADMIN BASIC INFORMATION
    // =========================================================

    @Column(name = "Name", nullable = false)
    private String name;


    @Column(name = "Email", nullable = false, unique = true)
    private String email;


    @Column(name = "Password", nullable = false)
    private String password;


    // =========================================================
    // SYSTEM SETTINGS
    // =========================================================

    @Column(name = "SystemName")
    private String systemName;


    @Column(name = "AlertEmail")
    private String alertEmail;


    // NEW: phone number shown in System Information
    @Column(name = "phone", length = 20)
    private String phone;


    // NEW: location shown in System Information
    @Column(name = "location", length = 255)
    private String location;


    @Column(name = "Currency")
    private String currency;


    // =========================================================
    // OPERATIONAL SETTINGS
    // =========================================================

    @Column(name = "AutoVerification")
    private Boolean autoVerification = false;


    @Column(name = "MaintenanceMode")
    private Boolean maintenanceMode = false;


    @Column(name = "SmsAlerts")
    private Boolean smsAlerts = false;


    // =========================================================
    // DEFAULT CONSTRUCTOR
    // =========================================================

    public Admin() {
    }


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }


    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }


    public Boolean getAutoVerification() {
        return autoVerification;
    }

    public void setAutoVerification(Boolean autoVerification) {
        this.autoVerification = autoVerification;
    }


    public Boolean getMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(Boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }


    public Boolean getSmsAlerts() {
        return smsAlerts;
    }

    public void setSmsAlerts(Boolean smsAlerts) {
        this.smsAlerts = smsAlerts;
    }
}