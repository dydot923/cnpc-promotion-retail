package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class ExchangePurchaseBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.EXCHANGE_PURCHASE;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("购物车中没有可换购商品。"));
        }

        BigDecimal exchangePrice = money(rule.benefit().exchangePrice());
        BigDecimal discount = items.stream()
                .map(item -> item.unitPrice().subtract(exchangePrice)
                        .max(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("换购价未低于当前执行价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "满足油品门槛，适用商品按换购价 " + exchangePrice + " 元计算。"));
    }
}
