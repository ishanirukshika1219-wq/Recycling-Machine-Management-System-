package com.example.myproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
public class Customer {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "CustomerID")
        private Integer customerId;

        @Column(name = "Name", nullable = false, length = 100)
        private String name;

        @Column(name = "Email", unique = true, length = 100)
        private String email;

        @Column(name = "Phone", length = 15)
        private String phone;

        @Column(name = "Password", nullable = false, length = 100)
        private String password;

        @Column(name = "RegisteredDate")
        private LocalDateTime registeredDate;

        @Column(name = "Address", length = 255)
        private String address;

        @Column(name = "City", length = 100)
        private String city;

        @Column(name = "PostalCode", length = 20)
        private String postalCode;

        // =====================================================
        // PROFILE PHOTO
        // =====================================================

        @Lob
        @Column(name = "ProfilePhoto", columnDefinition = "LONGTEXT")
        private String profilePhoto;

        // =====================================================
        // NOTIFICATION PREFERENCES (Settings page)   <-- NEW
        // A value of null (older customers) is treated as the default:
        // email = on, sms = on, promotions = off
        // =====================================================

        @Column(name = "EmailNotifications")
        private Boolean emailNotifications = true;

        @Column(name = "SmsAlerts")
        private Boolean smsAlerts = true;

        @Column(name = "PromotionalOffers")
        private Boolean promotionalOffers = false;


        // =========================
        // GETTERS
        // =========================

        public Integer getCustomerId() {
                return customerId;
        }

        public String getName() {
                return name;
        }

        public String getEmail() {
                return email;
        }

        public String getPhone() {
                return phone;
        }

        public String getPassword() {
                return password;
        }

        public LocalDateTime getRegisteredDate() {
                return registeredDate;
        }

        public String getAddress() {
                return address;
        }

        public String getCity() {
                return city;
        }

        public String getPostalCode() {
                return postalCode;
        }

        public String getProfilePhoto() {
                return profilePhoto;
        }

        public Boolean getEmailNotifications() {
                return emailNotifications;
        }

        public Boolean getSmsAlerts() {
                return smsAlerts;
        }

        public Boolean getPromotionalOffers() {
                return promotionalOffers;
        }


        // =========================
        // SETTERS
        // =========================

        public void setCustomerId(Integer customerId) {
                this.customerId = customerId;
        }

        public void setName(String name) {
                this.name = name;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public void setPhone(String phone) {
                this.phone = phone;
        }

        public void setPassword(String password) {
                this.password = password;
        }

        public void setRegisteredDate(LocalDateTime registeredDate) {
                this.registeredDate = registeredDate;
        }

        public void setAddress(String address) {
                this.address = address;
        }

        public void setCity(String city) {
                this.city = city;
        }

        public void setPostalCode(String postalCode) {
                this.postalCode = postalCode;
        }

        public void setProfilePhoto(String profilePhoto) {
                this.profilePhoto = profilePhoto;
        }

        public void setEmailNotifications(Boolean emailNotifications) {
                this.emailNotifications = emailNotifications;
        }

        public void setSmsAlerts(Boolean smsAlerts) {
                this.smsAlerts = smsAlerts;
        }

        public void setPromotionalOffers(Boolean promotionalOffers) {
                this.promotionalOffers = promotionalOffers;
        }

        @Column(name = "status", length = 20)
        private String status = "Active";

        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }
}