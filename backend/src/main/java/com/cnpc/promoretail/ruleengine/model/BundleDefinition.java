package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record BundleDefinition(
        String bundleId,
        String name,
        BigDecimal bundlePrice,
        BigDecimal thresholdAmount,
        String activityId,
        List<BundleItem> items
) {

    public BundleDefinition {
        if (bundleId == null || bundleId.isBlank()) {
            throw new IllegalArgumentException("bundleId is required");
        }
        name = name == null || name.isBlank() ? bundleId : name;
        bundlePrice = money(bundlePrice);
        thresholdAmount = money(thresholdAmount);
        activityId = activityId == null ? "" : activityId;
        items = items == null ? List.of() : List.copyOf(items);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
