
package com.example.myproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Staff")
public class Staff {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "StaffID")
        private Integer staffId;

        @Column(name = "Name", nullable = false, length = 100)
        private String name;

        @Column(name = "Username", unique = true, nullable = false, length = 50)
        private String username;

        @Column(name = "Email", unique = true, nullable = false, length = 100)
        private String email;

        public String getEmail() {
                return email;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        @Column(name = "Password", nullable = false, length = 100)
        private String password;

        @Column(name = "Status", length = 20)
        private String status;

        public Staff() {
        }

        public Integer getStaffId() {
                return staffId;
        }

        public void setStaffId(Integer staffId) {
                this.staffId = staffId;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getUsername() {
                return username;
        }

        public void setUsername(String username) {
                this.username = username;
        }

        public String getPassword() {
                return password;
        }

        public void setPassword(String password) {
                this.password = password;
        }

        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }
}
