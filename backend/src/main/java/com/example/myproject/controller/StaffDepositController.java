package com.example.myproject.controller;

import com.example.myproject.dto.TransactionActionRequest;
import com.example.myproject.model.RecyclingTransaction;
import com.example.myproject.model.RewardAccount;
import com.example.myproject.model.TransactionVerification;
import com.example.myproject.repository.TransactionRepository;
import com.example.myproject.repository.RewardAccountRepository;
import com.example.myproject.repository.TransactionVerificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/staff-deposits")
@CrossOrigin(origins = "*")
public class StaffDepositController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RewardAccountRepository rewardAccountRepository;

    @Autowired
    private TransactionVerificationRepository verificationRepository;


    // GET ALL PENDING DEPOSITS
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDeposits() {

        List<RecyclingTransaction> pending =
                transactionRepository
                        .findByStatusIgnoreCaseOrderByTransactionDateDesc(
                                "Pending"
                        );

        return ResponseEntity.ok(pending);
    }


    // VERIFY DEPOSIT
    @PutMapping("/{transactionId}/verify")
    @Transactional
    public ResponseEntity<?> verifyDeposit(
            @PathVariable Integer transactionId,
            @RequestBody TransactionActionRequest request) {

        try {

            if (request.getStaffId() == null) {
                return ResponseEntity.badRequest()
                        .body("Staff ID is required.");
            }

            Optional<RecyclingTransaction> optional =
                    transactionRepository.findById(transactionId);

            if (optional.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Transaction not found.");
            }

            RecyclingTransaction transaction = optional.get();

            if (!"Pending".equalsIgnoreCase(transaction.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("This deposit has already been processed.");
            }

            if (verificationRepository.existsByTransactionId(transactionId)) {
                return ResponseEntity.badRequest()
                        .body("This deposit has already been verified or rejected.");
            }

            // Update transaction status
            transaction.setStatus("Completed");
            transactionRepository.save(transaction);

            // Credit reward points only after verification
            int points = transaction.getRewardPoints() == null
                    ? 0 : transaction.getRewardPoints();

            if (points > 0) {

                List<RewardAccount> accounts =
                        rewardAccountRepository.findAll();

                RewardAccount account = accounts.stream()
                        .filter(a -> Objects.equals(
                                a.getCustomerId(),
                                transaction.getCustomerId()
                        ))
                        .findFirst()
                        .orElse(null);

                if (account == null) {

                    account = new RewardAccount();
                    account.setCustomerId(transaction.getCustomerId());
                    account.setPointsBalance(0);
                    account.setTotalPointsEarned(0);
                    account.setTotalPointsRedeemed(0);
                }

                account.setPointsBalance(
                        account.getPointsBalance() + points
                );

                account.setTotalPointsEarned(
                        account.getTotalPointsEarned() + points
                );

                rewardAccountRepository.save(account);
            }

            // Save staff verification details
            TransactionVerification verification =
                    new TransactionVerification();

            verification.setTransactionId(transactionId);
            verification.setStaffId(request.getStaffId());
            verification.setAction("VERIFIED");
            verification.setReason(request.getReason());
            verification.setActionDate(LocalDateTime.now());

            verificationRepository.save(verification);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit verified successfully.");
            response.put("transactionId", transactionId);
            response.put("status", "Completed");
            response.put("rewardPointsCredited", points);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("Verification failed: " + e.getMessage());
        }
    }


    // REJECT DEPOSIT
    @PutMapping("/{transactionId}/reject")
    @Transactional
    public ResponseEntity<?> rejectDeposit(
            @PathVariable Integer transactionId,
            @RequestBody TransactionActionRequest request) {

        try {

            if (request.getStaffId() == null) {
                return ResponseEntity.badRequest()
                        .body("Staff ID is required.");
            }

            if (request.getReason() == null ||
                    request.getReason().trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("Please provide a rejection reason.");
            }

            Optional<RecyclingTransaction> optional =
                    transactionRepository.findById(transactionId);

            if (optional.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Transaction not found.");
            }

            RecyclingTransaction transaction = optional.get();

            if (!"Pending".equalsIgnoreCase(transaction.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("This deposit has already been processed.");
            }

            if (verificationRepository.existsByTransactionId(transactionId)) {
                return ResponseEntity.badRequest()
                        .body("This deposit has already been processed.");
            }

            // Update transaction status
            transaction.setStatus("Rejected");
            transactionRepository.save(transaction);

            // Save rejection details
            TransactionVerification verification =
                    new TransactionVerification();

            verification.setTransactionId(transactionId);
            verification.setStaffId(request.getStaffId());
            verification.setAction("REJECTED");
            verification.setReason(request.getReason());
            verification.setActionDate(LocalDateTime.now());

            verificationRepository.save(verification);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit rejected successfully.");
            response.put("transactionId", transactionId);
            response.put("status", "Rejected");
            response.put("reason", request.getReason());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("Rejection failed: " + e.getMessage());
        }
    }


    // STAFF PROCESSED LOGS
    @GetMapping("/staff/{staffId}/logs")
    public ResponseEntity<?> getStaffLogs(
            @PathVariable Integer staffId) {

        List<TransactionVerification> logs =
                verificationRepository
                        .findByStaffIdOrderByActionDateDesc(staffId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (TransactionVerification log : logs) {

            Map<String, Object> row = new HashMap<>();

            row.put("verificationId", log.getVerificationId());
            row.put("transactionId", log.getTransactionId());
            row.put("staffId", log.getStaffId());
            row.put("action", log.getAction());
            row.put("reason", log.getReason());
            row.put("actionDate", log.getActionDate());

            transactionRepository.findById(log.getTransactionId())
                    .ifPresent(transaction -> {
                        row.put("customerId", transaction.getCustomerId());
                        row.put("totalWeight", transaction.getTotalWeight());
                        row.put("totalValue", transaction.getTotalValue());
                        row.put("source", transaction.getSource());
                        row.put("status", transaction.getStatus());
                    });

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }
}