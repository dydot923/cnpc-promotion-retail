package com.cnpc.promoretail.inventory.model;

import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record InventoryAlert(
        String alertId,
        String productCode,
        String barcode,
        String productName,
        String category,
        BigDecimal currentQuantity,
        BigDecimal threshold,
        BigDecimal suggestedReplenishmentQuantity,
        String relatedRuleId,
        PromotionRuleType relatedRuleType,
        InventoryAlertSeverity severity,
        String reason,
        String status,
        Instant handledAt,
        String handledBy,
        String handleNote,
        String replenishmentListId
) {

    public InventoryAlert {
        currentQuantity = quantity(currentQuantity);
        threshold = quantity(threshold);
        suggestedReplenishmentQuantity = quantity(suggestedReplenishmentQuantity);
        status = status == null || status.isBlank() ? "OPEN" : status;
        handledBy = handledBy == null ? "" : handledBy;
        handleNote = handleNote == null ? "" : handleNote;
        replenishmentListId = replenishmentListId == null ? "" : replenishmentListId;
    }

    public InventoryAlert(
            String alertId,
            String productCode,
            String barcode,
            String productName,
            String category,
            BigDecimal currentQuantity,
            BigDecimal threshold,
            BigDecimal suggestedReplenishmentQuantity,
            String relatedRuleId,
            PromotionRuleType relatedRuleType,
            InventoryAlertSeverity severity,
            String reason
    ) {
        this(alertId, productCode, barcode, productName, category, currentQuantity, threshold,
                suggestedReplenishmentQuantity, relatedRuleId, relatedRuleType, severity, reason,
                "OPEN", null, "", "", "");
    }

    public InventoryAlert withRecord(InventoryAlertRecord record) {
        if (record == null) {
            return this;
        }
        return new InventoryAlert(
                alertId,
                productCode,
                barcode,
                productName,
                category,
                currentQuantity,
                threshold,
                suggestedReplenishmentQuantity,
                relatedRuleId,
                relatedRuleType,
                severity,
                reason,
                record.status(),
                record.handledAt(),
                record.handledBy(),
                record.handleNote(),
                record.replenishmentListId()
        );
    }

    public boolean open() {
        return "OPEN".equalsIgnoreCase(status);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
