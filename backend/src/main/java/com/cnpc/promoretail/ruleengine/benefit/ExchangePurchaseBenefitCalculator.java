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
        int configuredExchangeQuantity = rule.benefit().exchangeQuantity();
        int remainingExchangeQuantity = rule.benefit().applicableExchangeQuantity(
                items.stream().mapToInt(CartItem::quantity).sum());
        BigDecimal discount = BigDecimal.ZERO;
        boolean packagePriceApplied = false;
        for (CartItem item : items) {
            if (remainingExchangeQuantity <= 0) {
                break;
            }
            int appliedQuantity = Math.min(item.quantity(), remainingExchangeQuantity);
            boolean packagePrice = configuredExchangeQuantity > 1
                    && exchangePrice.compareTo(item.unitPrice()) >= 0;
            if (packagePrice && appliedQuantity < configuredExchangeQuantity) {
                return BenefitCalculation.blocked(List.of("换购商品数量未达到组合换购要求，需要 "
                        + configuredExchangeQuantity + " 件。"));
            }
            if (item.inventoryQuantity() != null
                    && item.inventoryQuantity().compareTo(BigDecimal.valueOf(appliedQuantity)) < 0) {
                return BenefitCalculation.blocked(List.of("换购商品库存不足，商品 " + item.productCode()
                        + " 库存 " + item.inventoryQuantity().stripTrailingZeros().toPlainString()
                        + " 件，需要 " + appliedQuantity + " 件。"));
            }
            BigDecimal lineDiscount = packagePrice
                    ? item.unitPrice().multiply(BigDecimal.valueOf(appliedQuantity)).subtract(exchangePrice)
                            .max(BigDecimal.ZERO)
                    : item.unitPrice().subtract(exchangePrice).max(BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(appliedQuantity));
            discount = discount.add(lineDiscount);
            remainingExchangeQuantity -= appliedQuantity;
            packagePriceApplied = packagePriceApplied || packagePrice;
        }

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("换购价未低于当前执行价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        String priceText = packagePriceApplied
                ? "满足油品门槛，适用商品按组合换购价 " + exchangePrice + " 元计算。"
                : "满足油品门槛，适用商品按换购价 " + exchangePrice + " 元计算。";
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                priceText));
    }
}
