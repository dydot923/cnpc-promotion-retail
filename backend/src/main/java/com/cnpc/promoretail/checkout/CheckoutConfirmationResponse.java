package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.checkout.model.CheckoutConfirmation;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.time.Instant;
import java.util.List;

public record CheckoutConfirmationResponse(
        String confirmationId,
        String calculationId,
        String selectedCandidateId,
        PromotionCandidate selectedCandidateSnapshot,
        List<CartItem> cartItems,
        String operatorId,
        String operatorName,
        boolean skipped,
        Instant confirmedAt
) {

    public static CheckoutConfirmationResponse from(CheckoutConfirmation confirmation) {
        return from(confirmation, List.of());
    }

    public static CheckoutConfirmationResponse from(CheckoutConfirmation confirmation, List<CartItem> cartItems) {
        return new CheckoutConfirmationResponse(
                confirmation.confirmationId(),
                confirmation.calculationId(),
                confirmation.selectedCandidateId(),
                confirmation.selectedCandidateSnapshot(),
                cartItems == null ? List.of() : List.copyOf(cartItems),
                confirmation.operatorId(),
                confirmation.operatorName(),
                confirmation.skipped(),
                confirmation.confirmedAt()
        );
    }
}
