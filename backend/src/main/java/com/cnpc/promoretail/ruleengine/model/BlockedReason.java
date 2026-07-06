package com.cnpc.promoretail.ruleengine.model;

public record BlockedReason(
        String code,
        String message
) {

    public BlockedReason {
        code = code == null || code.isBlank() ? "PROMOTION_BLOCKED" : code;
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("blocked reason message is required");
        }
    }

    public static BlockedReason of(String message) {
        return new BlockedReason("PROMOTION_BLOCKED", message);
    }
}

