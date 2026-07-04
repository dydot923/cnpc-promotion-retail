package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class PercentageDiscountBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.PERCENTAGE_DISCOUNT;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("没有可执行折扣的商品。"));
        }

        BigDecimal rate = rule.benefit().discountRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            return BenefitCalculation.blocked(List.of("折扣率必须大于 0 且小于 1。"));
        }

        BigDecimal subtotal = eligibleSubtotal(items);
        BigDecimal discount = subtotal.multiply(BigDecimal.ONE.subtract(rate));
        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中折扣促销，适用商品按 " + rate + " 折扣率计算。"));
    }
}
