package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class FuelVolumeDiscountBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.FUEL_VOLUME_DISCOUNT;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        BigDecimal minVolume = money(rule.condition().minFuelVolume());
        if (minVolume.compareTo(BigDecimal.ZERO) > 0 && context.fuel().volume().compareTo(minVolume) < 0) {
            return BenefitCalculation.blocked(List.of("油品升数未满" + minVolume.stripTrailingZeros().toPlainString() + "升"));
        }
        BigDecimal discountPerUnit = money(rule.benefit().discountPerUnit());
        if (discountPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("每升立减金额必须大于0"));
        }
        if (context.fuel().volume().compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("油品升数必须大于0"));
        }

        BigDecimal discount = money(discountPerUnit.multiply(context.fuel().volume()));
        discount = discount.min(totals.originalAmount());
        BigDecimal payable = money(totals.originalAmount().subtract(discount).max(BigDecimal.ZERO));
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "油品按升立减，" + discountPerUnit + "元/升，合计优惠" + discount + "元"));
    }

    @Override
    protected List<CartItem> eligibleItems(OrderContext context, PromotionRule rule) {
        if (rule.condition().productCodes().isEmpty()) {
            return context.cartItems();
        }
        return super.eligibleItems(context, rule);
    }
}
