package com.example.myproject.service;

import com.example.myproject.model.Customer;
import com.example.myproject.model.RecyclableCategory;
import com.example.myproject.model.RecyclingTransaction;
import com.example.myproject.model.RewardAccount;
import com.example.myproject.model.RewardRule;
import com.example.myproject.model.TransactionItem;
import com.example.myproject.model.WalkInDeposit;

import com.example.myproject.repository.CategoryRepository;
import com.example.myproject.repository.CustomerLookupRepository;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.RewardAccountRepository;
import com.example.myproject.repository.RewardRuleRepository;
import com.example.myproject.repository.TransactionItemRepository;
import com.example.myproject.repository.TransactionRepository;
import com.example.myproject.repository.WalkInDepositRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Staff counter (walk-in) deposits.
 * - registered customer: a normal recycling transaction is saved (reward points, history,
 *   email / SMS settings) exactly like Start Recycling, plus a counter record
 * - guest: only the counter record is saved (no account, so no reward points)
 */
@Service
public class WalkInService {

    private final CustomerRepository customerRepository;
    private final CustomerLookupRepository customerLookupRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository itemRepository;
    private final RewardAccountRepository rewardAccountRepository;
    private final RewardRuleRepository ruleRepository;
    private final WalkInDepositRepository walkInDepositRepository;
    private final NotificationService notificationService;

    public WalkInService(CustomerRepository customerRepository,
                         CustomerLookupRepository customerLookupRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         TransactionItemRepository itemRepository,
                         RewardAccountRepository rewardAccountRepository,
                         RewardRuleRepository ruleRepository,
                         WalkInDepositRepository walkInDepositRepository,
                         NotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.customerLookupRepository = customerLookupRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.itemRepository = itemRepository;
        this.rewardAccountRepository = rewardAccountRepository;
        this.ruleRepository = ruleRepository;
        this.walkInDepositRepository = walkInDepositRepository;
        this.notificationService = notificationService;
    }


    // =========================================================
    // VERIFY ACCOUNT  (CUST-000012, 12, 0771234567 or an email)
    // =========================================================

    public Map<String, Object> lookupCustomer(String query) {

        String q = query == null ? "" : query.trim();

        if (q.isEmpty()) {
            throw bad("Enter a customer ID or phone number.");
        }

        String digits = q.replaceAll("[^0-9]", "");
        Customer customer = null;

        if (q.contains("@")) {

            customer = customerRepository.findByEmail(q).orElse(null);

        } else if (q.toUpperCase(Locale.ROOT).startsWith("CUST")) {

            customer = findById(digits);

        } else if (digits.length() >= 9) {

            customer = findByPhone(q, digits);

        } else if (!digits.isEmpty()) {

            customer = findById(digits);
        }

        if (customer == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No registered customer found. Check the ID / phone number, or use Guest Walk-in.");
        }

        if (!isActive(customer)) {
            throw bad("This customer account is not active.");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("customerId", customer.getCustomerId());
        result.put("customerCode", customerCode(customer.getCustomerId()));
        result.put("name", customer.getName());
        result.put("phone", customer.getPhone());
        result.put("email", customer.getEmail());

        return result;
    }

    private Customer findById(String digits) {

        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }

        return customerRepository.findById(Integer.parseInt(digits)).orElse(null);
    }

    private Customer findByPhone(String raw, String digits) {

        String last9 = digits.substring(digits.length() - 9);

        Set<String> variants = new LinkedHashSet<>();

        variants.add(raw);
        variants.add("0" + last9);
        variants.add(last9);
        variants.add("94" + last9);
        variants.add("+94" + last9);

        List<Customer> found = customerLookupRepository.findByPhoneIn(variants);

        return found.isEmpty() ? null : found.get(0);
    }


    // =========================================================
    // LIVE PREVIEW (the Calculation Breakdown card)
    // =========================================================

    public Map<String, Object> preview(Integer categoryId, BigDecimal weight,
                                       Integer customerId, boolean guest) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {

            Calc calc = calculate(categoryId, weight, guest ? null : customerId);

            result.put("valid", true);
            result.put("categoryName", calc.category().getCategoryName());
            result.put("ratePerKg", calc.category().getRatePerKg());
            result.put("weightKg", calc.weight());
            result.put("amount", calc.amount());
            result.put("rewardPoints", calc.points());

        } catch (ResponseStatusException e) {

            result.put("valid", false);
            result.put("message", e.getReason());
        }

        return result;
    }


    // =========================================================
    // PROCESS DEPOSIT
    // =========================================================

    @Transactional
    public Map<String, Object> processDeposit(Map<String, Object> body) {

        boolean guest = Boolean.TRUE.equals(body.get("guest"));

        Integer categoryId = toInteger(body.get("categoryId"));
        BigDecimal weight = toDecimal(body.get("weightKg"));

        String payoutType = text(body.get("payoutType")).toLowerCase(Locale.ROOT);

        if (!payoutType.equals("cash") && !payoutType.equals("points") && !payoutType.equals("wallet")) {
            payoutType = "cash";
        }

        Integer staffId = toInteger(body.get("staffId"));
        String staffName = text(body.get("staffName"));

        if (staffName.isEmpty()) {
            staffName = "Counter Staff";
        }

        // ---------- who is the customer ----------

        Customer customer = null;

        if (!guest) {

            Integer customerId = toInteger(body.get("customerId"));

            if (customerId == null) {
                throw bad("Verify the customer account first, or choose Guest Walk-in.");
            }

            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Customer not found."));

            if (!isActive(customer)) {
                throw bad("This customer account is not active.");
            }
        }

        // ---------- amount + points (same rules as Start Recycling) ----------

        Calc calc = calculate(categoryId, weight, customer == null ? null : customer.getCustomerId());

        RecyclableCategory category = calc.category();
        BigDecimal rate = category.getRatePerKg();

        Integer transactionId = null;

        // ---------- registered customer: the normal transaction ----------

        if (customer != null) {

            RecyclingTransaction transaction = new RecyclingTransaction();

            transaction.setCustomerId(customer.getCustomerId());
            transaction.setTotalWeight(calc.weight());
            transaction.setTotalValue(calc.amount());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setSource("WALK_IN");
            transaction.setRewardPoints(calc.points());

            RecyclingTransaction savedTransaction = transactionRepository.save(transaction);

            TransactionItem item = new TransactionItem();

            item.setTransactionId(savedTransaction.getTransactionId());
            item.setCategoryId(category.getCategoryId());
            item.setWeightKg(calc.weight());
            item.setRatePerKg(rate);
            item.setItemValue(calc.amount());

            itemRepository.save(item);

            final int points = calc.points();
            final Integer customerId = customer.getCustomerId();

            RewardAccount rewardAccount = rewardAccountRepository
                    .findByCustomerId(customerId)
                    .orElseGet(() -> {

                        RewardAccount newAccount = new RewardAccount();

                        newAccount.setCustomerId(customerId);
                        newAccount.setPointsBalance(0);
                        newAccount.setTotalPointsEarned(0);
                        newAccount.setTotalPointsRedeemed(0);

                        return newAccount;
                    });

            rewardAccount.setPointsBalance(rewardAccount.getPointsBalance() + points);
            rewardAccount.setTotalPointsEarned(rewardAccount.getTotalPointsEarned() + points);

            rewardAccountRepository.save(rewardAccount);

            transactionId = savedTransaction.getTransactionId();

            // email / SMS (only what the customer turned on in Settings), after the commit
            final String notifyCategory = category.getCategoryName();
            final double notifyWeight = calc.weight().doubleValue();
            final double notifyAmount = calc.amount().doubleValue();

            Runnable sendNotifications = () ->
                    notificationService.sendTransactionNotifications(
                            customerId, notifyCategory, notifyWeight, notifyAmount, points, "Completed");

            if (TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                sendNotifications.run();
                            }
                        });

            } else {

                sendNotifications.run();
            }
        }

        // ---------- counter record (registered + guest) ----------

        String guestName = text(body.get("guestName"));

        WalkInDeposit deposit = new WalkInDeposit();

        deposit.setCustomerId(customer == null ? null : customer.getCustomerId());
        deposit.setCustomerName(customer != null
                ? customer.getName()
                : (guestName.isEmpty() ? "Guest" : guestName));
        deposit.setCategoryId(category.getCategoryId());
        deposit.setCategoryName(category.getCategoryName());
        deposit.setRatePerKg(rate);
        deposit.setWeightKg(calc.weight());
        deposit.setAmount(calc.amount());
        deposit.setRewardPoints(calc.points());
        deposit.setPayoutType(payoutType);
        deposit.setStaffId(staffId);
        deposit.setStaffName(staffName);
        deposit.setTransactionId(transactionId);
        deposit.setCreatedAt(LocalDateTime.now());

        WalkInDeposit saved = walkInDepositRepository.save(deposit);

        return toMap(saved);
    }


    // =========================================================
    // TODAY'S COUNTER ENTRIES
    // =========================================================

    public List<Map<String, Object>> today() {

        LocalDate today = LocalDate.now();

        List<WalkInDeposit> list = walkInDepositRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        List<Map<String, Object>> result = new ArrayList<>();

        for (WalkInDeposit deposit : list) {
            result.add(toMap(deposit));
        }

        return result;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private record Calc(RecyclableCategory category, BigDecimal weight, BigDecimal amount, int points) {}

    /** Same rules as TransactionController (minimum weight, bulk multiplier, first deposit bonus) */
    private Calc calculate(Integer categoryId, BigDecimal weight, Integer registeredCustomerId) {

        if (categoryId == null) {
            throw bad("Select a material category.");
        }

        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw bad("Weight must be greater than zero.");
        }

        RecyclableCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> bad("Category not found."));

        if (category.getStatus() != null
                && !category.getStatus().trim().isEmpty()
                && !"Active".equalsIgnoreCase(category.getStatus().trim())) {
            throw bad("This category is not active.");
        }

        RewardRule rule = ruleRepository
                .findById(1)
                .orElseGet(() -> ruleRepository.save(new RewardRule()));

        if (weight.compareTo(rule.getMinTxWeight()) < 0) {

            String minWeight = rule.getMinTxWeight()
                    .setScale(2, RoundingMode.HALF_UP).toPlainString();

            throw bad("Minimum weight for a recycling transaction is " + minWeight + " kg.");
        }

        BigDecimal amount = category.getRatePerKg()
                .multiply(weight)
                .setScale(2, RoundingMode.HALF_UP);

        int points = 0;

        // Guests have no reward account, so they earn no points
        if (registeredCustomerId != null) {

            BigDecimal rawPoints = weight.multiply(BigDecimal.valueOf(rule.getBasePointsPerKg()));

            if (rule.getBulkBonusWeight().compareTo(BigDecimal.ZERO) > 0
                    && weight.compareTo(rule.getBulkBonusWeight()) >= 0) {

                rawPoints = rawPoints.multiply(rule.getBulkMultiplier());
            }

            points = rawPoints.setScale(0, RoundingMode.DOWN).intValue();

            boolean isFirstTransaction = transactionRepository
                    .findByCustomerId(registeredCustomerId)
                    .stream()
                    .noneMatch(t -> "Completed".equalsIgnoreCase(t.getStatus()));

            if (isFirstTransaction) {
                points += rule.getFirstTxBonus();
            }
        }

        return new Calc(category, weight, amount, points);
    }

    private Map<String, Object> toMap(WalkInDeposit deposit) {

        Map<String, Object> m = new LinkedHashMap<>();

        boolean guest = deposit.getCustomerId() == null;

        m.put("depositId", deposit.getId());
        m.put("receiptNo", String.format(Locale.ROOT, "RCP-%05d", deposit.getId()));
        m.put("dateTime", deposit.getCreatedAt() == null ? null : deposit.getCreatedAt().toString());
        m.put("guest", guest);
        m.put("customerId", deposit.getCustomerId());
        m.put("customerCode", guest ? null : customerCode(deposit.getCustomerId()));
        m.put("customerName", deposit.getCustomerName());
        m.put("categoryName", deposit.getCategoryName());
        m.put("ratePerKg", deposit.getRatePerKg());
        m.put("weightKg", deposit.getWeightKg());
        m.put("amount", deposit.getAmount());
        m.put("rewardPoints", deposit.getRewardPoints());
        m.put("payoutType", deposit.getPayoutType());
        m.put("payoutLabel", payoutLabel(deposit.getPayoutType()));
        m.put("staffId", deposit.getStaffId());
        m.put("staffName", deposit.getStaffName());
        m.put("transactionId", deposit.getTransactionId());


        return m;
    }

    private String payoutLabel(String type) {

        if ("points".equals(type)) {
            return "Convert to Reward Points";
        }

        if ("wallet".equals(type)) {
            return "SRMMS Digital Wallet";
        }

        return "Counter Cash Payout";
    }

    private String customerCode(Integer id) {
        return id == null ? null : String.format(Locale.ROOT, "CUST-%06d", id);
    }

    private boolean isActive(Customer customer) {

        String status = customer.getStatus();

        return status == null || status.trim().isEmpty() || "Active".equalsIgnoreCase(status.trim());
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer toInteger(Object value) {

        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw bad("Invalid number: " + value);
        }
    }

    private BigDecimal toDecimal(Object value) {

        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw bad("Enter a valid weight.");
        }
    }

    // =========================================================
// ALL MANUAL WALK-IN DEPOSIT HISTORY
// =========================================================

    public List<Map<String, Object>> history() {

        List<WalkInDeposit> list =
                walkInDepositRepository.findAllByOrderByCreatedAtDesc();

        List<Map<String, Object>> result = new ArrayList<>();

        for (WalkInDeposit deposit : list) {
            result.add(toMap(deposit));

        }

        return result;
    }

    // Get deposits processed by a specific staff member
    public List<Map<String, Object>> historyByStaff(Integer staffId) {

        if (staffId == null) {
            throw bad("Staff ID is required.");
        }

        List<WalkInDeposit> list =
                walkInDepositRepository.findByStaffIdOrderByCreatedAtDesc(staffId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (WalkInDeposit deposit : list) {
            result.add(toMap(deposit));
        }

        return result;
    }
}