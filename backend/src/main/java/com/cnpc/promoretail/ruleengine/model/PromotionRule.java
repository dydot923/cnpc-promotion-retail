package com.cnpc.promoretail.ruleengine.model;

public record PromotionRule(
        String ruleId,
        String activityName,
        PromotionRuleType ruleType,
        int priority,
        String exclusiveGroup,
        boolean stackable,
        PromotionRuleStatus status,
        RuleCondition condition,
        RuleBenefit benefit,
        String version
) {

    public PromotionRule {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("activityName is required");
        }
        if (ruleType == null) {
            throw new IllegalArgumentException("ruleType is required");
        }
        status = status == null ? PromotionRuleStatus.PENDING_CONFIRMATION : status;
        condition = condition == null ? RuleCondition.empty() : condition;
        if (benefit == null) {
            throw new IllegalArgumentException("benefit is required");
        }
        version = version == null || version.isBlank() ? "unversioned" : version;
    }

    public boolean active() {
        return status == PromotionRuleStatus.ACTIVE;
    }
}

