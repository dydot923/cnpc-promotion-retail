package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.List;

public record BenefitCalculation(
        List<PromotionCandidate> candidates,
        List<String> blockedReasons
) {

    public BenefitCalculation {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
    }

    public boolean available() {
        return !candidates.isEmpty() && blockedReasons.isEmpty();
    }

    public boolean hasCandidates() {
        return !candidates.isEmpty();
    }

    public PromotionCandidate candidate() {
        return candidates.stream().findFirst().orElse(null);
    }

    public static BenefitCalculation available(PromotionCandidate candidate) {
        return new BenefitCalculation(List.of(candidate), List.of());
    }

    public static BenefitCalculation available(List<PromotionCandidate> candidates) {
        return new BenefitCalculation(candidates, List.of());
    }

    public static BenefitCalculation mixed(List<PromotionCandidate> candidates, List<String> reasons) {
        return new BenefitCalculation(candidates, reasons);
    }

    public static BenefitCalculation blocked(List<String> reasons) {
        return new BenefitCalculation(List.of(), reasons);
    }
}
