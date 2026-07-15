package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import java.util.List;

public class StationTypeConditionMatcher {

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        if (condition.stationTypes().isEmpty()) {
            return List.of();
        }
        String stationType = context.station().stationType();
        return condition.stationTypes().contains(stationType)
                ? List.of()
                : List.of("站点类型不匹配");
    }
}
