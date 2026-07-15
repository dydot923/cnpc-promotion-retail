package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import java.math.BigDecimal;
import java.util.List;

public class RechargeAmountConditionMatcher {

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        BigDecimal minRechargeAmount = condition.minRechargeAmount();
        if (minRechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        if (context.rechargeAmount().compareTo(minRechargeAmount) < 0) {
            return List.of("Recharge amount is below " + minRechargeAmount.stripTrailingZeros().toPlainString());
        }
        return List.of();
    }
}
