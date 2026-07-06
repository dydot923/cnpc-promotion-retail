package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class AmountOffBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.AMOUNT_OFF;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("没有可执行满减的商品。"));
        }

        BigDecimal eligibleSubtotal = eligibleSubtotal(items);
        if (rule.condition().minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && eligibleSubtotal.compareTo(rule.condition().minCartAmount()) < 0) {
            return BenefitCalculation.blocked(List.of("适用商品金额未达到满减门槛。"));
        }

        BigDecimal amountOff = money(rule.benefit().amountOff());
        if (amountOff.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("满减金额必须大于 0。"));
        }

        BigDecimal discount = amountOff.min(eligibleSubtotal);
        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中满减促销，直接优惠 " + discount + " 元。"));
    }
}
