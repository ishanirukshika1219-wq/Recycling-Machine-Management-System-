package com.example.myproject.controller;

import com.example.myproject.model.RewardRedemption;
import com.example.myproject.model.RewardRule;
import com.example.myproject.repository.RewardRedemptionRepository;
import com.example.myproject.repository.RewardRuleRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reward-rules")
@CrossOrigin(origins = "*")
public class RewardRuleController {

    private final RewardRuleRepository ruleRepository;
    private final RewardRedemptionRepository redemptionRepository;

    public RewardRuleController(
            RewardRuleRepository ruleRepository,
            RewardRedemptionRepository redemptionRepository) {

        this.ruleRepository = ruleRepository;
        this.redemptionRepository = redemptionRepository;
    }


    // =========================================================
    // GET CURRENT RULES (creates the default row if missing)
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getRules() {

        RewardRule rule =
                ruleRepository.findById(1)
                        .orElseGet(() -> ruleRepository.save(new RewardRule()));

        return ResponseEntity.ok(rule);
    }


    // =========================================================
    // SAVE RULES
    // =========================================================

    @PostMapping
    public ResponseEntity<?> saveRules(@RequestBody RewardRule request) {

        if (request.getBasePointsPerKg() == null || request.getBasePointsPerKg() < 0
                || request.getFirstTxBonus() == null || request.getFirstTxBonus() < 0) {

            return ResponseEntity.badRequest()
                    .body("Points values must be zero or greater.");
        }

        if (isNegative(request.getMinTxWeight())
                || isNegative(request.getRedemptionRate())
                || isNegative(request.getBulkBonusWeight())
                || isNegative(request.getBulkMultiplier())) {

            return ResponseEntity.badRequest()
                    .body("Please enter valid values for all rules.");
        }

        request.setRuleId(1);

        RewardRule saved = ruleRepository.save(request);

        return ResponseEntity.ok(saved);
    }

    private boolean isNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0;
    }


    // =========================================================
    // ALL REDEMPTION REQUESTS (newest first)
    // =========================================================

    @GetMapping("/redemptions")
    public List<RewardRedemption> getAllRedemptions() {

        return redemptionRepository.findAll(
                Sort.by(Sort.Direction.DESC, "redemptionDate")
        );
    }
}
