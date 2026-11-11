package com.example.myproject.controller;

import com.example.myproject.model.RewardAccount;
import com.example.myproject.model.RewardRedemption;
import com.example.myproject.repository.RewardAccountRepository;
import com.example.myproject.repository.RewardRedemptionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = "*")
public class RewardController {

    private final RewardAccountRepository rewardAccountRepository;
    private final RewardRedemptionRepository redemptionRepository;

    public RewardController(
            RewardAccountRepository rewardAccountRepository,
            RewardRedemptionRepository redemptionRepository) {

        this.rewardAccountRepository = rewardAccountRepository;
        this.redemptionRepository = redemptionRepository;
    }


    // =========================================================
    // GET REWARD ACCOUNT (Total / Available points)
    // =========================================================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getRewardAccount(
            @PathVariable Integer customerId) {

        RewardAccount account =
                rewardAccountRepository
                        .findByCustomerId(customerId)
                        .orElseGet(() -> {

                            RewardAccount newAccount = new RewardAccount();
                            newAccount.setCustomerId(customerId);
                            newAccount.setPointsBalance(0);
                            newAccount.setTotalPointsEarned(0);
                            newAccount.setTotalPointsRedeemed(0);

                            return newAccount;
                        });

        return ResponseEntity.ok(account);
    }


    // =========================================================
    // REDEEM POINTS
    // (creates a redemption with status "Pending" and holds
    //  the points until an admin approves or rejects it)
    // =========================================================

    @PostMapping("/redeem")
    @Transactional
    public ResponseEntity<?> redeemPoints(
            @RequestBody Map<String, Object> request) {

        try {

            Integer customerId =
                    (Integer) request.get("customerId");

            String itemName =
                    (String) request.get("itemName");

            Integer pointsRequired =
                    (Integer) request.get("pointsRequired");

            if (customerId == null || itemName == null || pointsRequired == null) {

                return ResponseEntity
                        .badRequest()
                        .body("Missing required redemption details.");
            }


            RewardAccount account =
                    rewardAccountRepository
                            .findByCustomerId(customerId)
                            .orElse(null);

            if (account == null ||
                    account.getPointsBalance() < pointsRequired) {

                return ResponseEntity
                        .badRequest()
                        .body("Not enough points to redeem this reward.");
            }


            /* Deduct points immediately (held pending admin decision) */

            account.setPointsBalance(
                    account.getPointsBalance() - pointsRequired
            );

            account.setTotalPointsRedeemed(
                    account.getTotalPointsRedeemed() + pointsRequired
            );

            rewardAccountRepository.save(account);


            /* Save redemption record as Pending */

            RewardRedemption redemption = new RewardRedemption();

            redemption.setCustomerId(customerId);
            redemption.setItemName(itemName);
            redemption.setPointsUsed(pointsRequired);
            redemption.setStatus("Pending");

            RewardRedemption saved =
                    redemptionRepository.save(redemption);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to redeem points.");
        }
    }


    // =========================================================
    // GET REDEMPTION HISTORY (for a specific customer)
    // =========================================================

    @GetMapping("/redemptions/customer/{customerId}")
    public List<RewardRedemption> getRedemptionHistory(
            @PathVariable Integer customerId) {

        return redemptionRepository
                .findByCustomerIdOrderByRedemptionDateDesc(customerId);
    }


    // =========================================================
    // GET PENDING REDEMPTIONS (for admin dashboard)
    // =========================================================

    @GetMapping("/redemptions/pending")
    public List<RewardRedemption> getPendingRedemptions() {

        return redemptionRepository
                .findByStatusOrderByRedemptionDateDesc("Pending");
    }


    // =========================================================
    // APPROVE REDEMPTION
    // =========================================================

    @PostMapping("/redemptions/{id}/approve")
    public ResponseEntity<?> approveRedemption(
            @PathVariable Integer id) {

        RewardRedemption redemption =
                redemptionRepository.findById(id).orElse(null);

        if (redemption == null) {
            return ResponseEntity.badRequest().body("Redemption not found.");
        }

        redemption.setStatus("Approved");
        redemptionRepository.save(redemption);

        return ResponseEntity.ok(redemption);
    }


    // =========================================================
    // REJECT REDEMPTION
    // (refunds the held points back to the customer's account)
    // =========================================================

    @PostMapping("/redemptions/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectRedemption(
            @PathVariable Integer id) {

        RewardRedemption redemption =
                redemptionRepository.findById(id).orElse(null);

        if (redemption == null) {
            return ResponseEntity.badRequest().body("Redemption not found.");
        }

        RewardAccount account =
                rewardAccountRepository
                        .findByCustomerId(redemption.getCustomerId())
                        .orElse(null);

        if (account != null) {

            account.setPointsBalance(
                    account.getPointsBalance() + redemption.getPointsUsed()
            );

            account.setTotalPointsRedeemed(
                    account.getTotalPointsRedeemed() - redemption.getPointsUsed()
            );

            rewardAccountRepository.save(account);
        }

        redemption.setStatus("Rejected");
        redemptionRepository.save(redemption);

        return ResponseEntity.ok(redemption);
    }
}