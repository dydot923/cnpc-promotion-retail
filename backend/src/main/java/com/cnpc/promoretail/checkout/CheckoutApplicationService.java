package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutApplicationService {

    private final PromotionEngine promotionEngine;

    public CheckoutApplicationService(PromotionEngine promotionEngine) {
        this.promotionEngine = promotionEngine;
    }

    public CalculationResult calculate(CheckoutCalculateRequest request) {
        // Active promotion rule loading will be wired to the promotion module after import persistence lands.
        return promotionEngine.calculate(request.orderContext(), List.of());
    }

    public String confirm(CheckoutConfirmRequest request) {
        // Confirmation persistence belongs to the audit/checkout storage milestone.
        return request.selectedCandidateId();
    }
}

