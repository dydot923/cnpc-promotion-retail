package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.Comparator;
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
        boolean packagePrice = rule.ruleId().startsWith("abv2-99-zone-")
                || (rule.exclusiveGroup() != null
                && rule.exclusiveGroup().startsWith("board_pack_price"));
        int packageQuantity = packagePrice
                ? Math.max(rule.condition().minProductQuantity(), 1)
                : 1;
        int totalQuantity = items.stream().mapToInt(CartItem::quantity).sum();
        int packageCount = totalQuantity / packageQuantity;
        int remainingQuantity = packageCount * packageQuantity;
        BigDecimal promotionalOriginalAmount = BigDecimal.ZERO;
        for (CartItem item : items.stream()
                .sorted(Comparator.comparing(CartItem::unitPrice).reversed())
                .toList()) {
            int appliedQuantity = Math.min(item.quantity(), remainingQuantity);
            promotionalOriginalAmount = promotionalOriginalAmount.add(
                    item.unitPrice().multiply(BigDecimal.valueOf(appliedQuantity)));
            remainingQuantity -= appliedQuantity;
            if (remainingQuantity == 0) {
                break;
            }
        }
        BigDecimal discount = promotionalOriginalAmount
                .subtract(fixedPrice.multiply(BigDecimal.valueOf(packageCount)));

        if (discount.compareTo(BigDecimal.ZERO) < 0
                || (discount.compareTo(BigDecimal.ZERO) == 0 && !packagePrice)) {
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
