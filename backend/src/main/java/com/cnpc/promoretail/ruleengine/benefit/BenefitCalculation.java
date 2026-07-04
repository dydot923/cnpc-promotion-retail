package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.List;

public record BenefitCalculation(
        PromotionCandidate candidate,
        List<String> blockedReasons
) {

    public BenefitCalculation {
        blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
    }

    public boolean available() {
        return candidate != null && blockedReasons.isEmpty();
    }

    public static BenefitCalculation available(PromotionCandidate candidate) {
        return new BenefitCalculation(candidate, List.of());
    }

    public static BenefitCalculation blocked(List<String> reasons) {
        return new BenefitCalculation(null, reasons);
    }
}

