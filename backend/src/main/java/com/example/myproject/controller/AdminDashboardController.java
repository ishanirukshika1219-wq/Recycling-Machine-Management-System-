package com.example.myproject.controller;

import com.example.myproject.model.RecyclableCategory;
import com.example.myproject.model.RecyclingTransaction;
import com.example.myproject.model.TransactionItem;
import com.example.myproject.repository.CategoryRepository;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.TransactionItemRepository;
import com.example.myproject.repository.TransactionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final CategoryRepository categoryRepository;

    public AdminDashboardController(
            TransactionRepository transactionRepository,
            CustomerRepository customerRepository,
            TransactionItemRepository transactionItemRepository,
            CategoryRepository categoryRepository) {

        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.transactionItemRepository = transactionItemRepository;
        this.categoryRepository = categoryRepository;
    }


    // =========================================================
    // ADMIN DASHBOARD STATISTICS
    // =========================================================

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {

        List<RecyclingTransaction> transactions =
                transactionRepository.findAll();

        long totalTransactions = transactions.size();

        BigDecimal collectedWeight = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        for (RecyclingTransaction transaction : transactions) {

            if (transaction.getTotalWeight() != null) {
                collectedWeight = collectedWeight.add(
                        transaction.getTotalWeight()
                );
            }

            if (transaction.getTotalValue() != null) {
                totalValue = totalValue.add(
                        transaction.getTotalValue()
                );
            }
        }

        long activeUsers = customerRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalTransactions", totalTransactions);
        stats.put("collectedWeight", collectedWeight);
        stats.put("totalValue", totalValue);
        stats.put("activeUsers", activeUsers);

        return ResponseEntity.ok(stats);
    }


    // =========================================================
    // RECENT TRANSACTIONS
    // =========================================================

    @GetMapping("/recent-transactions")
    public ResponseEntity<?> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {

        List<RecyclingTransaction> transactions =
                transactionRepository.findAll();

        // Newest transactions first
        transactions.sort(
                Comparator.comparing(
                        RecyclingTransaction::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        // Limit
        if (limit < 1) {
            limit = 5;
        }

        if (transactions.size() > limit) {
            transactions =
                    transactions.subList(0, limit);
        }

        List<Map<String, Object>> result =
                new ArrayList<>();


        // =====================================================
        // CREATE RESPONSE DATA
        // =====================================================

        for (RecyclingTransaction transaction : transactions) {

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put(
                    "transactionId",
                    transaction.getTransactionId()
            );

            row.put(
                    "customerId",
                    transaction.getCustomerId()
            );

            row.put(
                    "transactionDate",
                    transaction.getTransactionDate()
            );

            row.put(
                    "totalWeight",
                    transaction.getTotalWeight()
            );

            row.put(
                    "totalValue",
                    transaction.getTotalValue()
            );

            row.put(
                    "status",
                    transaction.getStatus()
            );


            // =================================================
            // FIND CATEGORY NAME
            // =================================================

            String categoryName = "-";

            List<TransactionItem> items =
                    transactionItemRepository
                            .findByTransactionId(
                                    transaction.getTransactionId()
                            );

            if (!items.isEmpty()) {

                Integer categoryId =
                        items.get(0).getCategoryId();

                if (categoryId != null) {

                    categoryName =
                            categoryRepository
                                    .findById(categoryId)
                                    .map(RecyclableCategory::getCategoryName)
                                    .orElse("-");
                }
            }

            row.put(
                    "categoryName",
                    categoryName
            );


            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    // =========================================================
// ALL TRANSACTIONS FOR ADMIN
// =========================================================

    @GetMapping("/all-transactions")
    public ResponseEntity<?> getAllTransactions() {

        List<RecyclingTransaction> transactions =
                transactionRepository.findAll();

        // Newest first
        transactions.sort(
                Comparator.comparing(
                        RecyclingTransaction::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (RecyclingTransaction transaction : transactions) {

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put(
                    "transactionId",
                    transaction.getTransactionId()
            );

            row.put(
                    "customerId",
                    transaction.getCustomerId()
            );

            row.put(
                    "transactionDate",
                    transaction.getTransactionDate()
            );

            row.put(
                    "totalWeight",
                    transaction.getTotalWeight()
            );

            row.put(
                    "totalValue",
                    transaction.getTotalValue()
            );

            row.put(
                    "rewardPoints",
                    transaction.getRewardPoints()
            );

            row.put(
                    "status",
                    transaction.getStatus()
            );

            // -------------------------------------------------
            // CATEGORY + RATE
            // -------------------------------------------------

            String categoryName = "-";
            BigDecimal ratePerKg = null;

            List<TransactionItem> items =
                    transactionItemRepository.findByTransactionId(
                            transaction.getTransactionId()
                    );

            if (!items.isEmpty()) {

                TransactionItem item = items.get(0);

                Integer categoryId =
                        item.getCategoryId();

                if (categoryId != null) {

                    RecyclableCategory category =
                            categoryRepository
                                    .findById(categoryId)
                                    .orElse(null);

                    if (category != null) {

                        categoryName =
                                category.getCategoryName();
                    }
                }

                ratePerKg =
                        item.getRatePerKg();
            }

            row.put(
                    "categoryName",
                    categoryName
            );

            row.put(
                    "ratePerKg",
                    ratePerKg
            );

            // -------------------------------------------------
            // STAFF ID
            // -------------------------------------------------

            // Current RecyclingTransaction does not appear
            // to have a staffId field, so return null.
            row.put(
                    "staffId",
                    null
            );

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }
}
