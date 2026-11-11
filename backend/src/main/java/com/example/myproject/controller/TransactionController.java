package com.example.myproject.controller;

import com.example.myproject.model.RecyclableCategory;
import com.example.myproject.model.RecyclingTransaction;
import com.example.myproject.model.RewardAccount;
import com.example.myproject.model.RewardRule;
import com.example.myproject.model.TransactionItem;
import com.example.myproject.model.TransactionRequest;
import com.example.myproject.model.Customer;

import com.example.myproject.repository.CategoryRepository;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.RewardAccountRepository;
import com.example.myproject.repository.RewardRuleRepository;
import com.example.myproject.repository.TransactionItemRepository;
import com.example.myproject.repository.TransactionRepository;

import com.example.myproject.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final RewardAccountRepository rewardAccountRepository;
    private final RewardRuleRepository ruleRepository;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;

    public TransactionController(
            TransactionRepository transactionRepository,
            TransactionItemRepository itemRepository,
            CategoryRepository categoryRepository,
            RewardAccountRepository rewardAccountRepository,
            RewardRuleRepository ruleRepository,
            NotificationService notificationService,
            CustomerRepository customerRepository) {

        this.transactionRepository = transactionRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.rewardAccountRepository = rewardAccountRepository;
        this.ruleRepository = ruleRepository;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
    }

    // =========================================================
    // CREATE RECYCLING TRANSACTION
    // =========================================================

    @PostMapping
    @Transactional
    public ResponseEntity<?> createTransaction(
            @RequestBody TransactionRequest request) {

        try {

            if (request.getWeightKg() == null ||
                    request.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {

                return ResponseEntity.badRequest()
                        .body("Weight must be greater than zero.");
            }

            RewardRule rule = ruleRepository.findById(1)
                    .orElseGet(() -> ruleRepository.save(new RewardRule()));

            if (request.getWeightKg().compareTo(rule.getMinTxWeight()) < 0) {

                String minWeight = rule.getMinTxWeight()
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString();

                return ResponseEntity.badRequest()
                        .body("Minimum weight required for a recycling transaction is "
                                + minWeight + " kg. Please enter " + minWeight
                                + " kg or more.");
            }

            RecyclableCategory category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Category not found with ID: "
                                            + request.getCategoryId()
                            )
                    );

            BigDecimal rate = category.getRatePerKg();

            BigDecimal itemValue =
                    rate.multiply(request.getWeightKg());

            // Calculate reward points
            BigDecimal rawPoints =
                    request.getWeightKg().multiply(
                            BigDecimal.valueOf(rule.getBasePointsPerKg())
                    );

            // Bulk multiplier
            if (rule.getBulkBonusWeight().compareTo(BigDecimal.ZERO) > 0 &&
                    request.getWeightKg().compareTo(rule.getBulkBonusWeight()) >= 0) {

                rawPoints = rawPoints.multiply(rule.getBulkMultiplier());
            }

            int pointsEarned =
                    rawPoints.setScale(0, RoundingMode.DOWN).intValue();

            // First deposit bonus
            boolean isFirstTransaction =
                    transactionRepository
                            .findByCustomerId(request.getCustomerId())
                            .stream()
                            .noneMatch(t ->
                                    "Completed".equalsIgnoreCase(t.getStatus())
                            );

            if (isFirstTransaction) {
                pointsEarned += rule.getFirstTxBonus();
            }

            // Create transaction
            RecyclingTransaction transaction =
                    new RecyclingTransaction();

            transaction.setCustomerId(request.getCustomerId());
            transaction.setTotalWeight(request.getWeightKg());
            transaction.setTotalValue(itemValue);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setStatus("Pending");
            transaction.setSource("ONLINE");
            transaction.setRewardPoints(pointsEarned);

            RecyclingTransaction savedTransaction =
                    transactionRepository.save(transaction);

            // Create transaction item
            TransactionItem item = new TransactionItem();

            item.setTransactionId(savedTransaction.getTransactionId());
            item.setCategoryId(category.getCategoryId());
            item.setWeightKg(request.getWeightKg());
            item.setRatePerKg(rate);
            item.setItemValue(itemValue);

            itemRepository.save(item);

            // Update reward account
            final int finalPointsEarned = pointsEarned;

            RewardAccount rewardAccount =
                    rewardAccountRepository
                            .findByCustomerId(request.getCustomerId())
                            .orElseGet(() -> {

                                RewardAccount newAccount =
                                        new RewardAccount();

                                newAccount.setCustomerId(
                                        request.getCustomerId()
                                );

                                newAccount.setPointsBalance(0);
                                newAccount.setTotalPointsEarned(0);
                                newAccount.setTotalPointsRedeemed(0);

                                return newAccount;
                            });

            rewardAccount.setPointsBalance(
                    rewardAccount.getPointsBalance() + finalPointsEarned
            );

            rewardAccount.setTotalPointsEarned(
                    rewardAccount.getTotalPointsEarned() + finalPointsEarned
            );

            rewardAccountRepository.save(rewardAccount);

            // Send Pending email and SMS after commit
            final Integer notifyCustomerId = request.getCustomerId();
            final String notifyCategory = category.getCategoryName();
            final double notifyWeight = request.getWeightKg().doubleValue();
            final double notifyAmount = itemValue.doubleValue();
            final String notifyStatus = savedTransaction.getStatus();

            Runnable sendNotifications = () ->
                    notificationService.sendTransactionNotifications(
                            notifyCustomerId,
                            notifyCategory,
                            notifyWeight,
                            notifyAmount,
                            finalPointsEarned,
                            notifyStatus
                    );

            if (TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                log.info(
                                        "Transaction created. Sending Pending notification. Transaction ID: {}",
                                        savedTransaction.getTransactionId()
                                );

                                sendNotifications.run();
                            }
                        }
                );

            } else {
                sendNotifications.run();
            }

            return ResponseEntity.ok(savedTransaction);

        } catch (RuntimeException e) {

            log.error("Transaction creation failed: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {

            log.error("Unexpected transaction creation error", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the transaction.");
        }
    }

    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    @GetMapping
    public List<RecyclingTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // =========================================================
    // GET CUSTOMER TRANSACTIONS
    // =========================================================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerTransactions(
            @PathVariable Integer customerId) {

        List<RecyclingTransaction> transactions =
                transactionRepository.findByCustomerId(customerId);

        return ResponseEntity.ok(transactions);
    }

    // =========================================================
    // GET CUSTOMER TRANSACTIONS WITH CATEGORY NAME
    // =========================================================

    @GetMapping("/customer/{customerId}/details")
    public ResponseEntity<?> getCustomerTransactionsWithCategory(
            @PathVariable Integer customerId) {

        List<RecyclingTransaction> transactions =
                transactionRepository.findByCustomerId(customerId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (RecyclingTransaction transaction : transactions) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("transactionId", transaction.getTransactionId());
            row.put("customerId", transaction.getCustomerId());
            row.put("transactionDate", transaction.getTransactionDate());
            row.put("status", transaction.getStatus());
            row.put("totalWeight", transaction.getTotalWeight());
            row.put("totalValue", transaction.getTotalValue());
            row.put("rewardPoints", transaction.getRewardPoints());

            String categoryName = "-";

            List<TransactionItem> items =
                    itemRepository.findByTransactionId(
                            transaction.getTransactionId()
                    );

            if (!items.isEmpty()) {

                Integer categoryId = items.get(0).getCategoryId();

                categoryName = categoryRepository.findById(categoryId)
                        .map(RecyclableCategory::getCategoryName)
                        .orElse("-");
            }

            row.put("categoryName", categoryName);
            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // GET TRANSACTION BY ID
    // =========================================================

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransactionById(
            @PathVariable Integer transactionId) {

        return transactionRepository.findById(transactionId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =========================================================
    // GET PENDING ONLINE DEPOSITS
    // =========================================================

    @GetMapping("/pending-online")
    public ResponseEntity<?> getPendingOnlineDeposits() {

        List<RecyclingTransaction> transactions =
                transactionRepository
                        .findByStatusIgnoreCaseAndSourceIgnoreCaseOrderByTransactionDateDesc(
                                "Pending",
                                "ONLINE"
                        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (RecyclingTransaction transaction : transactions) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("transactionId", transaction.getTransactionId());
            row.put("customerId", transaction.getCustomerId());
            row.put("transactionDate", transaction.getTransactionDate());
            row.put("status", transaction.getStatus());
            row.put("source", transaction.getSource());
            row.put("totalWeight", transaction.getTotalWeight());
            row.put("totalValue", transaction.getTotalValue());
            row.put("rewardPoints", transaction.getRewardPoints());

            // Get category name
            String categoryName = "-";

            List<TransactionItem> items =
                    itemRepository.findByTransactionId(
                            transaction.getTransactionId()
                    );

            if (!items.isEmpty()) {

                Integer categoryId = items.get(0).getCategoryId();

                categoryName = categoryRepository.findById(categoryId)
                        .map(RecyclableCategory::getCategoryName)
                        .orElse("-");
            }

            row.put("categoryName", categoryName);

            // Get customer name
            String customerName =
                    customerRepository.findById(transaction.getCustomerId())
                            .map(Customer::getName)
                            .orElse("Unknown Customer");

            row.put("customerName", customerName);

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // VERIFY ONLINE DEPOSIT
    // =========================================================

    @PatchMapping("/{transactionId}/verify")
    @Transactional
    public ResponseEntity<?> verifyDeposit(
            @PathVariable Integer transactionId) {

        RecyclingTransaction transaction =
                transactionRepository.findById(transactionId).orElse(null);

        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Transaction not found.");
        }

        if (!"ONLINE".equalsIgnoreCase(transaction.getSource())) {
            return ResponseEntity.badRequest()
                    .body("Only online deposits can be verified here.");
        }

        if (!"Pending".equalsIgnoreCase(transaction.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("This transaction is not pending.");
        }

        transaction.setStatus("Completed");

        RecyclingTransaction savedTransaction =
                transactionRepository.save(transaction);

        // Send Verified email after database commit
        sendDecisionEmailAfterCommit(
                savedTransaction,
                "Completed",
                null
        );

        log.info(
                "Online deposit verified. Transaction ID: {}",
                transactionId
        );

        return ResponseEntity.ok(
                "Deposit verified successfully. Customer email notification has been queued."
        );
    }

    // =========================================================
    // REJECT ONLINE DEPOSIT
    // =========================================================

    @PatchMapping("/{transactionId}/reject")
    @Transactional
    public ResponseEntity<?> rejectDeposit(
            @PathVariable Integer transactionId) {

        RecyclingTransaction transaction =
                transactionRepository.findById(transactionId).orElse(null);

        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Transaction not found.");
        }

        if (!"ONLINE".equalsIgnoreCase(transaction.getSource())) {
            return ResponseEntity.badRequest()
                    .body("Only online deposits can be rejected here.");
        }

        if (!"Pending".equalsIgnoreCase(transaction.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("This transaction is not pending.");
        }

        transaction.setStatus("Rejected");

        RecyclingTransaction savedTransaction =
                transactionRepository.save(transaction);

        // Send Rejected email after database commit
        sendDecisionEmailAfterCommit(
                savedTransaction,
                "Rejected",
                null
        );

        log.info(
                "Online deposit rejected. Transaction ID: {}",
                transactionId
        );

        return ResponseEntity.ok(
                "Deposit rejected successfully. Customer email notification has been queued."
        );
    }

    // =========================================================
    // SEND VERIFY / REJECT EMAIL AFTER DATABASE COMMIT
    // =========================================================

    private void sendDecisionEmailAfterCommit(
            RecyclingTransaction transaction,
            String status,
            String rejectionReason) {

        try {

            List<TransactionItem> items =
                    itemRepository.findByTransactionId(
                            transaction.getTransactionId()
                    );

            String categoryName = "Recyclables";

            if (!items.isEmpty()) {

                Integer categoryId = items.get(0).getCategoryId();

                categoryName = categoryRepository.findById(categoryId)
                        .map(RecyclableCategory::getCategoryName)
                        .orElse("Recyclables");
            }

            String transactionDate =
                    transaction.getTransactionDate() == null
                            ? "N/A"
                            : transaction.getTransactionDate().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    );

            final Integer customerId = transaction.getCustomerId();
            final Integer transactionId = transaction.getTransactionId();
            final String finalCategory = categoryName;
            final String finalDate = transactionDate;

            final double weight =
                    transaction.getTotalWeight() == null
                            ? 0.0
                            : transaction.getTotalWeight().doubleValue();

            final double amount =
                    transaction.getTotalValue() == null
                            ? 0.0
                            : transaction.getTotalValue().doubleValue();

            final int points = transaction.getRewardPoints();

            final String finalStatus = status;
            final String finalReason = rejectionReason;

            Runnable sendEmail = () -> {

                log.info(
                        "Preparing decision email. Transaction ID: {}, Customer ID: {}, Status: {}",
                        transactionId,
                        customerId,
                        finalStatus
                );

                notificationService.sendDepositDecisionEmail(
                        customerId,
                        transactionId,
                        finalDate,
                        finalCategory,
                        weight,
                        amount,
                        points,
                        finalStatus,
                        finalReason
                );
            };

            if (TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                log.info(
                                        "Database committed. Triggering decision email for transaction ID: {}",
                                        transactionId
                                );

                                sendEmail.run();
                            }
                        }
                );

            } else {

                log.warn(
                        "No active transaction synchronization. Sending decision email directly for transaction ID: {}",
                        transactionId
                );

                sendEmail.run();
            }

        } catch (Exception e) {

            log.error(
                    "Could not prepare decision email for transaction ID: {}",
                    transaction.getTransactionId(),
                    e
            );
        }
    }
}