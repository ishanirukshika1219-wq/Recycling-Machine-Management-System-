package com.example.myproject.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_item")
public class TransactionItem {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "transaction_itemid")
        private Integer transactionItemId;

        @Column(name = "transactionid", nullable = false)
        private Integer transactionId;

        @Column(name = "categoryid", nullable = false)
        private Integer categoryId;

        @Column(name = "weight_kg", precision = 10, scale = 2)
        private BigDecimal weightKg;

        @Column(name = "rate_per_kg", precision = 10, scale = 2)
        private BigDecimal ratePerKg;

        @Column(name = "item_value", precision = 10, scale = 2)
        private BigDecimal itemValue;

        public TransactionItem() {}

        public Integer getTransactionItemId() { return transactionItemId; }
        public void setTransactionItemId(Integer transactionItemId) { this.transactionItemId = transactionItemId; }

        public Integer getTransactionId() { return transactionId; }
        public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

        public BigDecimal getRatePerKg() { return ratePerKg; }
        public void setRatePerKg(BigDecimal ratePerKg) { this.ratePerKg = ratePerKg; }

        public BigDecimal getItemValue() { return itemValue; }
        public void setItemValue(BigDecimal itemValue) { this.itemValue = itemValue; }
}