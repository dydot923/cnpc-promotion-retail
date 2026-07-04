package com.cnpc.promoretail.ruleengine;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.util.List;

public interface PromotionEngine {

    CalculationResult calculate(OrderContext context, List<PromotionRule> rules);
}

