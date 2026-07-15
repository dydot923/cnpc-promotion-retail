package com.cnpc.promoretail.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

public record CheckoutTransaction(
        String txnNo,
        String confirmationId,
        String calculationId,
        String selectedCandidateId,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        String paymentMethod,
        String operatorId,
        String operatorName,
        String memberCode,
        String stationCode,
        String status,
        Instant createdAt,
        List<CheckoutTransactionItem> items
) {

    public CheckoutTransaction {
        if (txnNo == null || txnNo.isBlank()) {
            throw new IllegalArgumentException("txnNo is required");
        }
        if (confirmationId == null || confirmationId.isBlank()) {
            throw new IllegalArgumentException("confirmationId is required");
        }
        if (calculationId == null || calculationId.isBlank()) {
            throw new IllegalArgumentException("calculationId is required");
        }
        if (selectedCandidateId == null || selectedCandidateId.isBlank()) {
            throw new IllegalArgumentException("selectedCandidateId is required");
        }
        totalAmount = money(totalAmount);
        discountAmount = money(discountAmount);
        payableAmount = money(payableAmount);
        paymentMethod = paymentMethod == null ? "" : paymentMethod;
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
        memberCode = memberCode == null ? "" : memberCode;
        stationCode = stationCode == null ? "" : stationCode;
        status = status == null || status.isBlank() ? "CONFIRMED" : status;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        items = items == null ? List.of() : List.copyOf(items);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
