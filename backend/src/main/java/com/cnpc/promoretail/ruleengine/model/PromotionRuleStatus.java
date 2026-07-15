package com.cnpc.promoretail.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PromotionRuleStatus {
    DRAFT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    REJECTED,
    NEEDS_MANUAL_REVIEW,
    DISABLED,
    ARCHIVED;

    @JsonCreator
    public static PromotionRuleStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING_CONFIRMATION;
        }
        try {
            return PromotionRuleStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING_CONFIRMATION;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
