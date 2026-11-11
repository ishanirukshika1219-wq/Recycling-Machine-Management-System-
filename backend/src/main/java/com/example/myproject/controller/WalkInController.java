package com.example.myproject.controller;

import com.example.myproject.service.WalkInService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/walkin")
@CrossOrigin(origins = "*")
public class WalkInController {

    private final WalkInService walkInService;

    public WalkInController(WalkInService walkInService) {
        this.walkInService = walkInService;
    }

    /** Verify Account button: ?query=CUST-000012  or  0771234567  or  12 */
    @GetMapping("/customer")
    public Map<String, Object> lookupCustomer(@RequestParam String query) {
        return walkInService.lookupCustomer(query);
    }

    /** Calculation Breakdown card (live) */
    @GetMapping("/preview")
    public Map<String, Object> preview(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String weightKg,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(defaultValue = "false") boolean guest) {

        BigDecimal weight = null;

        try {
            if (weightKg != null && !weightKg.trim().isEmpty()) {
                weight = new BigDecimal(weightKg.trim());
            }
        } catch (NumberFormatException e) {
            weight = null;
        }

        return walkInService.preview(categoryId, weight, customerId, guest);
    }

    /** Process Deposit button */
    @PostMapping("/deposits")
    public ResponseEntity<?> process(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walkInService.processDeposit(request));
    }

    /** Today's Counter Entries */
    @GetMapping("/deposits/today")
    public List<Map<String, Object>> today() {
        return walkInService.today();
    }

    // =========================================================
// ALL MANUAL WALK-IN DEPOSIT HISTORY
// =========================================================

    @GetMapping("/deposits/history")
    public List<Map<String, Object>> history() {
        return walkInService.history();
    }

    // Staff-specific processed deposit history
    @GetMapping("/deposits/history/staff/{staffId}")
    public List<Map<String, Object>> historyByStaff(
            @PathVariable Integer staffId) {

        return walkInService.historyByStaff(staffId);
    }

    /** Every error is sent as { "message": "..." } */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {

        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "Request failed." : e.getReason()));
    }


}