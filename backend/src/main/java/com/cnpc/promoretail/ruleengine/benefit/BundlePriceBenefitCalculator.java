package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class BundlePriceBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.BUNDLE_PRICE;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("购物车中没有组合包适用商品。"));
        }

        BigDecimal bundlePrice = money(rule.benefit().bundlePrice());
        BigDecimal subtotal = eligibleSubtotal(items);
        BigDecimal discount = subtotal.subtract(bundlePrice).max(BigDecimal.ZERO);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("组合包价未低于组合商品原价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中组合包价，组合商品按 " + bundlePrice + " 元结算。"));
    }
}
