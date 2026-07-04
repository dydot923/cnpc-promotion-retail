package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

abstract class AbstractBenefitCalculator implements BenefitCalculator {

    protected List<CartItem> eligibleItems(OrderContext context, PromotionRule rule) {
        return context.cartItems().stream()
                .filter(item -> item.matchesProductScope(rule.condition().productCodes()))
                .filter(item -> !item.excludedByCategory(rule.condition().excludedCategories()))
                .toList();
    }

    protected BigDecimal eligibleSubtotal(List<CartItem> items) {
        return money(items.stream()
                .map(CartItem::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    protected PromotionCandidate candidate(
            PromotionRule rule,
            BigDecimal originalAmount,
            BigDecimal payableAmount,
            BigDecimal discountAmount,
            String explanation
    ) {
        return new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                money(originalAmount),
                money(payableAmount),
                money(discountAmount),
                List.of(),
                List.of(),
                explanation,
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority()
        );
    }

    protected BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

