package com.example.myproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reward_account")
public class RewardAccount {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "reward_accountid")
        private Integer rewardAccountId;

        @Column(name = "customerid", nullable = false)
        private Integer customerId;

        @Column(name = "points_balance")
        private Integer pointsBalance = 0;

        @Column(name = "total_points_earned")
        private Integer totalPointsEarned = 0;

        @Column(name = "total_points_redeemed")
        private Integer totalPointsRedeemed = 0;

        public Integer getRewardAccountId() { return rewardAccountId; }
        public void setRewardAccountId(Integer rewardAccountId) { this.rewardAccountId = rewardAccountId; }

        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }

        public Integer getPointsBalance() { return pointsBalance; }
        public void setPointsBalance(Integer pointsBalance) { this.pointsBalance = pointsBalance; }

        public Integer getTotalPointsEarned() { return totalPointsEarned; }
        public void setTotalPointsEarned(Integer totalPointsEarned) { this.totalPointsEarned = totalPointsEarned; }

        public Integer getTotalPointsRedeemed() { return totalPointsRedeemed; }
        public void setTotalPointsRedeemed(Integer totalPointsRedeemed) { this.totalPointsRedeemed = totalPointsRedeemed; }
}