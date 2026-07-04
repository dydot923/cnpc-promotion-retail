package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;

public interface ConditionMatcher {

    ConditionMatchResult match(OrderContext context, PromotionRule rule);
}

