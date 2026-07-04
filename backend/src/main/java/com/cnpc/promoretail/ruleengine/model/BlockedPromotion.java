package com.cnpc.promoretail.ruleengine.model;

import java.util.List;

public record BlockedPromotion(
        String ruleId,
        String title,
        PromotionRuleType ruleType,
        List<String> reasons,
        String ruleVersion
) {

    public BlockedPromotion {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}

