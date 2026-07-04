package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.GiftItem;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class GiftItemBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.GIFT_ITEM;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        if (rule.benefit().giftItemCode() == null || rule.benefit().giftItemQuantity() <= 0) {
            return BenefitCalculation.blocked(List.of("赠品信息不完整。"));
        }

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                totals.originalAmount(),
                BigDecimal.ZERO,
                List.of(new GiftItem(rule.benefit().giftItemCode(), rule.benefit().giftItemName(),
                        rule.benefit().giftItemQuantity())),
                List.of(),
                "满足买赠条件，应赠送指定赠品。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority()
        );
        return BenefitCalculation.available(candidate);
    }
}
