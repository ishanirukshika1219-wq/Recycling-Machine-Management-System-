package com.example.myproject.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recycling_transaction")
public class RecyclingTransaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "transactionid")
        private Integer transactionId;

        @Column(name = "customerid", nullable = false)
        private Integer customerId;

        @Column(name = "transaction_date")
        private LocalDateTime transactionDate = LocalDateTime.now();

        @Column(name = "status", length = 30)
        private String status = "Completed";

        @Column(name = "total_weight", precision = 10, scale = 2)
        private BigDecimal totalWeight;

        @Column(name = "total_value", precision = 10, scale = 2)
        private BigDecimal totalValue;

        @Column(name = "reward_points")
        private Integer rewardPoints = 0;

        @Column(name = "source", length = 30)
        private String source = "ONLINE";


        public RecyclingTransaction() {
        }


        public Integer getTransactionId() {
                return transactionId;
        }

        public void setTransactionId(Integer transactionId) {
                this.transactionId = transactionId;
        }


        public Integer getCustomerId() {
                return customerId;
        }

        public void setCustomerId(Integer customerId) {
                this.customerId = customerId;
        }


        public LocalDateTime getTransactionDate() {
                return transactionDate;
        }

        public void setTransactionDate(LocalDateTime transactionDate) {
                this.transactionDate = transactionDate;
        }


        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }


        public BigDecimal getTotalWeight() {
                return totalWeight;
        }

        public void setTotalWeight(BigDecimal totalWeight) {
                this.totalWeight = totalWeight;
        }


        public BigDecimal getTotalValue() {
                return totalValue;
        }

        public void setTotalValue(BigDecimal totalValue) {
                this.totalValue = totalValue;
        }

        public String getSource() {
                return source;
        }

        public void setSource(String source) {
                this.source = source;
        }


        public Integer getRewardPoints() {
                return rewardPoints;
        }

        public void setRewardPoints(Integer rewardPoints) {
                this.rewardPoints = rewardPoints;
        }
}