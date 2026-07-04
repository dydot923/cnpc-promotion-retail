package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;

public interface BenefitCalculator {

    boolean supports(PromotionRuleType type);

    BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals);
}

