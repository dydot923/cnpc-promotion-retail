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
        int packageQuantity = rule.ruleId().startsWith("abv2-99-zone-")
                ? Math.max(rule.condition().minProductQuantity(), 1)
                : 1;
        BigDecimal discount = items.stream()
                .map(item -> {
                    int packageCount = item.quantity() / packageQuantity;
                    if (packageCount <= 0) {
                        return BigDecimal.ZERO;
                    }
                    BigDecimal packageOriginalPrice = item.unitPrice()
                            .multiply(BigDecimal.valueOf(packageQuantity));
                    return packageOriginalPrice.subtract(fixedPrice)
                            .max(BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(packageCount));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of(packageQuantity > 1
                    ? "商品数量未达到促销包装系数，或固定促销包价未低于当前执行价。"
                    : "固定促销价未低于当前执行价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        String packageDescription = packageQuantity > 1
                ? "满" + packageQuantity + "件按" + fixedPrice + "元/组结算"
                : "按" + fixedPrice + "元/件结算";
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中固定促销价，" + packageDescription + "。"));
    }
}
