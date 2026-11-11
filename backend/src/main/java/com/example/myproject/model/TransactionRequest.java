
package com.example.myproject.model;

import java.math.BigDecimal;

public class TransactionRequest {

        private Integer customerId;
        private Integer categoryId;
        private BigDecimal weightKg;
        private String paymentMethod;

        public Integer getCustomerId() {
                return customerId;
        }

        public void setCustomerId(Integer customerId) {
                this.customerId = customerId;
        }

        public Integer getCategoryId() {
                return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
                this.categoryId = categoryId;
        }

        public BigDecimal getWeightKg() {
                return weightKg;
        }

        public void setWeightKg(BigDecimal weightKg) {
                this.weightKg = weightKg;
        }

        public String getPaymentMethod() {
                return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
                this.paymentMethod = paymentMethod;
        }
}
