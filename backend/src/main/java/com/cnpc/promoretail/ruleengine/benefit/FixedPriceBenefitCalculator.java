package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class FixedPriceBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.FIXED_PRICE;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("没有可执行固定促销价的商品。"));
        }

        BigDecimal fixedPrice = money(rule.benefit().fixedPrice());
        BigDecimal discount = items.stream()
                .map(item -> item.unitPrice().subtract(fixedPrice)
                        .max(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("固定促销价未低于当前执行价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中固定促销价，适用商品按 " + fixedPrice + " 元结算。"));
    }
}
