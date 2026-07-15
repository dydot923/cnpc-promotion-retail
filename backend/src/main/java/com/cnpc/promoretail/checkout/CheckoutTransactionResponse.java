package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import com.cnpc.promoretail.checkout.model.CheckoutTransactionItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutTransactionResponse(
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

    public static CheckoutTransactionResponse from(CheckoutTransaction transaction) {
        return new CheckoutTransactionResponse(
                transaction.txnNo(),
                transaction.confirmationId(),
                transaction.calculationId(),
                transaction.selectedCandidateId(),
                transaction.totalAmount(),
                transaction.discountAmount(),
                transaction.payableAmount(),
                transaction.paymentMethod(),
                transaction.operatorId(),
                transaction.operatorName(),
                transaction.memberCode(),
                transaction.stationCode(),
                transaction.status(),
                transaction.createdAt(),
                transaction.items()
        );
    }
}
