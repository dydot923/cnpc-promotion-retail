package com.cnpc.promoretail.ruleengine.model;

public record BundleItem(
        String productCode,
        int quantity
) {

    public BundleItem {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("bundle productCode is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("bundle item quantity must be positive");
        }
    }
}
