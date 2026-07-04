package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class GiftCouponBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.GIFT_COUPON;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        if (rule.benefit().giftCouponName() == null) {
            return BenefitCalculation.blocked(List.of("赠券信息不完整。"));
        }

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                totals.originalAmount(),
                BigDecimal.ZERO,
                List.of(),
                List.of(new GiftCoupon(rule.benefit().giftCouponName(), rule.benefit().giftCouponAmount())),
                "满足赠券条件，应提示发放电子券。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority()
        );
        return BenefitCalculation.available(candidate);
    }
}
