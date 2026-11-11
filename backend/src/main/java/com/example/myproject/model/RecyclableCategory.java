package com.example.myproject.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "Recyclable_Category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_name",
                        columnNames = "category_name"
                )
        }
)
public class RecyclableCategory {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "categoryid")
        private Integer categoryId;

        @Column(
                name = "category_name",
                nullable = false,
                unique = true
        )
        private String categoryName;

        @Column(
                name = "description"
        )
        private String description;

        @Column(
                name = "rate_per_kg",
                nullable = false,
                precision = 10,
                scale = 2
        )
        private BigDecimal ratePerKg;

        @Column(name = "status")
        private String status;


        public Integer getCategoryId() {
                return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
                this.categoryId = categoryId;
        }


        public String getCategoryName() {
                return categoryName;
        }

        public void setCategoryName(String categoryName) {
                this.categoryName = categoryName;
        }


        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }


        public BigDecimal getRatePerKg() {
                return ratePerKg;
        }

        public void setRatePerKg(BigDecimal ratePerKg) {
                this.ratePerKg = ratePerKg;
        }


        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }
}