package com.example.myproject.controller;

import com.example.myproject.model.Customer;
import com.example.myproject.model.RecyclableCategory;
import com.example.myproject.model.RecyclingTransaction;
import com.example.myproject.model.RewardRedemption;
import com.example.myproject.model.TransactionItem;

import com.example.myproject.repository.CategoryRepository;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.repository.RewardRedemptionRepository;
import com.example.myproject.repository.TransactionItemRepository;
import com.example.myproject.repository.TransactionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final RewardRedemptionRepository redemptionRepository;

    public ReportController(
            TransactionRepository transactionRepository,
            TransactionItemRepository itemRepository,
            CategoryRepository categoryRepository,
            CustomerRepository customerRepository,
            RewardRedemptionRepository redemptionRepository) {

        this.transactionRepository = transactionRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.customerRepository = customerRepository;
        this.redemptionRepository = redemptionRepository;
    }


    private static class CategoryAgg {
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal payout = BigDecimal.ZERO;
        double points = 0;
        int txnCount = 0;
    }

    private static class CustomerAgg {
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        int txnCount = 0;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }


    // =========================================================
    // GENERATE REPORT
    // =========================================================

    @GetMapping
    public ResponseEntity<?> generateReport(
            @RequestParam(defaultValue = "summary") String type,
            @RequestParam String from,
            @RequestParam String to) {

        if (!type.equals("summary") && !type.equals("materials")
                && !type.equals("rewards") && !type.equals("users")) {

            return ResponseEntity.badRequest().body("Unknown report type.");
        }

        LocalDate fromDate;
        LocalDate toDate;

        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("Please select valid From and To dates.");
        }

        if (fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest()
                    .body("From date cannot be later than To date.");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();


        // ---------- lookups ----------

        Map<Integer, String> categoryNames = new HashMap<>();
        for (RecyclableCategory c : categoryRepository.findAll()) {
            categoryNames.put(c.getCategoryId(), c.getCategoryName());
        }

        Map<Integer, Customer> customers = new HashMap<>();
        for (Customer c : customerRepository.findAll()) {
            customers.put(c.getCustomerId(), c);
        }

        Map<Integer, List<TransactionItem>> itemsByTx = new HashMap<>();
        for (TransactionItem item : itemRepository.findAll()) {
            itemsByTx
                    .computeIfAbsent(item.getTransactionId(), k -> new ArrayList<>())
                    .add(item);
        }


        // ---------- transactions inside the period (cancelled/rejected excluded) ----------

        List<RecyclingTransaction> periodTx = new ArrayList<>();

        for (RecyclingTransaction t : transactionRepository.findAll()) {

            LocalDateTime d = t.getTransactionDate();

            if (d == null || d.isBefore(start) || !d.isBefore(end)) {
                continue;
            }

            String status = t.getStatus() == null ? "" : t.getStatus();

            if (status.equalsIgnoreCase("Cancelled")
                    || status.equalsIgnoreCase("Rejected")) {
                continue;
            }

            periodTx.add(t);
        }


        // ---------- aggregate ----------

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;
        long totalPoints = 0;

        Map<Integer, CategoryAgg> categoryAggs = new LinkedHashMap<>();
        Map<Integer, CustomerAgg> customerAggs = new HashMap<>();

        for (RecyclingTransaction t : periodTx) {

            BigDecimal w = nz(t.getTotalWeight());
            BigDecimal v = nz(t.getTotalValue());
            int p = t.getRewardPoints() == null ? 0 : t.getRewardPoints();

            totalWeight = totalWeight.add(w);
            totalPayout = totalPayout.add(v);
            totalPoints += p;

            CustomerAgg ca = customerAggs.computeIfAbsent(
                    t.getCustomerId(), k -> new CustomerAgg());

            ca.weight = ca.weight.add(w);
            ca.value = ca.value.add(v);
            ca.txnCount++;

            List<TransactionItem> items =
                    itemsByTx.getOrDefault(
                            t.getTransactionId(),
                            Collections.emptyList());

            Set<Integer> countedCategories = new HashSet<>();

            for (TransactionItem item : items) {

                CategoryAgg agg = categoryAggs.computeIfAbsent(
                        item.getCategoryId(), k -> new CategoryAgg());

                BigDecimal itemWeight = nz(item.getWeightKg());

                agg.weight = agg.weight.add(itemWeight);
                agg.payout = agg.payout.add(nz(item.getItemValue()));

                if (w.compareTo(BigDecimal.ZERO) > 0) {
                    agg.points += p * itemWeight.doubleValue() / w.doubleValue();
                }

                if (countedCategories.add(item.getCategoryId())) {
                    agg.txnCount++;
                }
            }
        }


        // ---------- category breakdown (heaviest first) ----------

        List<Map.Entry<Integer, CategoryAgg>> categoryEntries =
                new ArrayList<>(categoryAggs.entrySet());

        categoryEntries.sort(
                (a, b) -> b.getValue().weight.compareTo(a.getValue().weight));

        BigDecimal categoryWeightTotal = BigDecimal.ZERO;

        for (CategoryAgg agg : categoryAggs.values()) {
            categoryWeightTotal = categoryWeightTotal.add(agg.weight);
        }

        List<Map<String, Object>> breakdown = new ArrayList<>();

        for (Map.Entry<Integer, CategoryAgg> entry : categoryEntries) {

            CategoryAgg agg = entry.getValue();

            BigDecimal percentage = BigDecimal.ZERO;

            if (categoryWeightTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = agg.weight
                        .multiply(BigDecimal.valueOf(100))
                        .divide(categoryWeightTotal, 1, RoundingMode.HALF_UP);
            }

            BigDecimal avgRate = BigDecimal.ZERO;

            if (agg.weight.compareTo(BigDecimal.ZERO) > 0) {
                avgRate = agg.payout.divide(agg.weight, 2, RoundingMode.HALF_UP);
            }

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("category",
                    categoryNames.getOrDefault(
                            entry.getKey(), "Category #" + entry.getKey()));
            row.put("weight", agg.weight.setScale(2, RoundingMode.HALF_UP));
            row.put("percentage", percentage);
            row.put("avgRate", avgRate);
            row.put("payout", agg.payout.setScale(2, RoundingMode.HALF_UP));
            row.put("points", Math.round(agg.points));
            row.put("txnCount", agg.txnCount);

            breakdown.add(row);
        }


        // ---------- top customer contributors (top 5 by weight) ----------

        List<Map.Entry<Integer, CustomerAgg>> customerEntries =
                new ArrayList<>(customerAggs.entrySet());

        customerEntries.sort(
                (a, b) -> b.getValue().weight.compareTo(a.getValue().weight));

        List<Map<String, Object>> topCustomers = new ArrayList<>();

        for (Map.Entry<Integer, CustomerAgg> entry : customerEntries) {

            if (topCustomers.size() >= 5) {
                break;
            }

            Customer c = customers.get(entry.getKey());

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("name",
                    c != null && c.getName() != null
                            ? c.getName()
                            : "Customer #" + entry.getKey());
            row.put("weight",
                    entry.getValue().weight.setScale(2, RoundingMode.HALF_UP));
            row.put("value",
                    entry.getValue().value.setScale(2, RoundingMode.HALF_UP));

            topCustomers.add(row);
        }


        // ---------- report preview log (depends on report type) ----------

        Map<String, Object> preview;

        if (type.equals("materials")) {
            preview = buildMaterialsPreview(breakdown);
        } else if (type.equals("rewards")) {
            preview = buildRewardsPreview(start, end);
        } else if (type.equals("users")) {
            preview = buildUsersPreview(customers, customerAggs, start, end);
        } else {
            preview = buildSummaryPreview(breakdown);
        }


        // ---------- response ----------

        Map<String, Object> kpis = new LinkedHashMap<>();

        kpis.put("transactions", periodTx.size());
        kpis.put("weight", totalWeight.setScale(2, RoundingMode.HALF_UP));
        kpis.put("payout", totalPayout.setScale(2, RoundingMode.HALF_UP));
        kpis.put("points", totalPoints);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("type", type);
        response.put("kpis", kpis);
        response.put("categoryBreakdown", breakdown);
        response.put("topCustomers", topCustomers);
        response.put("preview", preview);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // PREVIEW BUILDERS
    // =========================================================

    private Map<String, Object> col(
            String label, String type, boolean highlight, boolean bold) {

        Map<String, Object> c = new LinkedHashMap<>();

        c.put("label", label);
        c.put("type", type);
        c.put("highlight", highlight);
        c.put("bold", bold);

        return c;
    }

    private Map<String, Object> preview(
            List<Map<String, Object>> columns, List<List<Object>> rows) {

        Map<String, Object> p = new LinkedHashMap<>();

        p.put("columns", columns);
        p.put("rows", rows);

        return p;
    }


    private Map<String, Object> buildSummaryPreview(
            List<Map<String, Object>> breakdown) {

        List<Map<String, Object>> columns = new ArrayList<>();

        columns.add(col("Category", "text", false, true));
        columns.add(col("Total Weight", "kg", false, false));
        columns.add(col("Average Rate / kg", "money", false, false));
        columns.add(col("Total Payout (Rs.)", "money", true, false));
        columns.add(col("Points Issued", "int", false, false));
        columns.add(col("Txn Count", "int", false, false));

        List<List<Object>> rows = new ArrayList<>();

        for (Map<String, Object> b : breakdown) {
            rows.add(Arrays.<Object>asList(
                    b.get("category"),
                    b.get("weight"),
                    b.get("avgRate"),
                    b.get("payout"),
                    b.get("points"),
                    b.get("txnCount")));
        }

        return preview(columns, rows);
    }


    private Map<String, Object> buildMaterialsPreview(
            List<Map<String, Object>> breakdown) {

        List<Map<String, Object>> columns = new ArrayList<>();

        columns.add(col("Category", "text", false, true));
        columns.add(col("Total Weight", "kg", false, false));
        columns.add(col("Share of Total", "percent", false, false));
        columns.add(col("Total Payout (Rs.)", "money", true, false));
        columns.add(col("Txn Count", "int", false, false));

        List<List<Object>> rows = new ArrayList<>();

        for (Map<String, Object> b : breakdown) {
            rows.add(Arrays.<Object>asList(
                    b.get("category"),
                    b.get("weight"),
                    b.get("percentage"),
                    b.get("payout"),
                    b.get("txnCount")));
        }

        return preview(columns, rows);
    }


    private Map<String, Object> buildRewardsPreview(
            LocalDateTime start, LocalDateTime end) {

        List<Map<String, Object>> columns = new ArrayList<>();

        columns.add(col("Requested Date", "text", false, false));
        columns.add(col("Redemption ID", "text", false, false));
        columns.add(col("Customer ID", "text", false, false));
        columns.add(col("Reward Description", "text", false, true));
        columns.add(col("Points Used", "int", false, false));
        columns.add(col("Status", "text", false, false));

        List<RewardRedemption> list = new ArrayList<>();

        for (RewardRedemption r : redemptionRepository.findAll()) {

            LocalDateTime d = r.getRedemptionDate();

            if (d != null && !d.isBefore(start) && d.isBefore(end)) {
                list.add(r);
            }
        }

        list.sort((a, b) ->
                b.getRedemptionDate().compareTo(a.getRedemptionDate()));

        List<List<Object>> rows = new ArrayList<>();

        for (RewardRedemption r : list) {
            rows.add(Arrays.<Object>asList(
                    r.getRedemptionDate().toLocalDate().toString(),
                    "RDM-" + String.format("%04d", r.getRedemptionId()),
                    "CUST-" + String.format("%06d", r.getCustomerId()),
                    r.getItemName(),
                    r.getPointsUsed(),
                    r.getStatus()));
        }

        return preview(columns, rows);
    }


    private Map<String, Object> buildUsersPreview(
            Map<Integer, Customer> customers,
            Map<Integer, CustomerAgg> customerAggs,
            LocalDateTime start,
            LocalDateTime end) {

        List<Map<String, Object>> columns = new ArrayList<>();

        columns.add(col("Customer ID", "text", false, false));
        columns.add(col("Customer", "text", false, true));
        columns.add(col("Registered", "text", false, false));
        columns.add(col("Transactions", "int", false, false));
        columns.add(col("Weight Recycled", "kg", false, false));
        columns.add(col("Value Earned", "money", true, false));

        List<List<Object>> rows = new ArrayList<>();

        for (Customer c : customers.values()) {

            LocalDateTime reg = c.getRegisteredDate();

            boolean registeredInPeriod =
                    reg != null && !reg.isBefore(start) && reg.isBefore(end);

            CustomerAgg agg = customerAggs.get(c.getCustomerId());

            if (!registeredInPeriod && agg == null) {
                continue;
            }

            rows.add(Arrays.<Object>asList(
                    "CUST-" + String.format("%06d", c.getCustomerId()),
                    c.getName(),
                    reg == null ? "-" : reg.toLocalDate().toString(),
                    agg == null ? 0 : agg.txnCount,
                    (agg == null ? BigDecimal.ZERO : agg.weight)
                            .setScale(2, RoundingMode.HALF_UP),
                    (agg == null ? BigDecimal.ZERO : agg.value)
                            .setScale(2, RoundingMode.HALF_UP)));
        }

        rows.sort((a, b) ->
                ((BigDecimal) b.get(4)).compareTo((BigDecimal) a.get(4)));

        return preview(columns, rows);
    }
}
