package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.checkout.model.CheckoutCalculationRecord;
import com.cnpc.promoretail.checkout.repository.CheckoutCalculationRecordRepository;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CheckoutApplicationService {

    private final PromotionEngine promotionEngine;
    private final PromotionRuleRepository promotionRuleRepository;
    private final CheckoutCalculationRecordRepository checkoutCalculationRecordRepository;

    public CheckoutApplicationService(
            PromotionEngine promotionEngine,
            PromotionRuleRepository promotionRuleRepository,
            CheckoutCalculationRecordRepository checkoutCalculationRecordRepository
    ) {
        this.promotionEngine = promotionEngine;
        this.promotionRuleRepository = promotionRuleRepository;
        this.checkoutCalculationRecordRepository = checkoutCalculationRecordRepository;
    }

    public CalculationResult calculate(CheckoutCalculateRequest request) {
        CalculationResult result = promotionEngine.calculate(request.orderContext(), promotionRuleRepository.findConfirmedRules());
        checkoutCalculationRecordRepository.save(new CheckoutCalculationRecord(
                "calc-" + UUID.randomUUID(),
                request.orderContext(),
                result,
                result.ruleVersionIds(),
                Instant.now()
        ));
        return result;
    }

    public String confirm(CheckoutConfirmRequest request) {
        // Confirmation persistence belongs to the audit/checkout storage milestone.
        return request.selectedCandidateId();
    }
}
