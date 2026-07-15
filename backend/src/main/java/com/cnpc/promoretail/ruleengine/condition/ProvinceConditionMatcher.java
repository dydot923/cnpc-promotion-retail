package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import java.util.List;

public class ProvinceConditionMatcher {

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        if (condition.stationProvinces().isEmpty()) {
            return List.of();
        }
        String province = context.station().region();
        return province != null && condition.stationProvinces().contains(province)
                ? List.of()
                : List.of("省区不在活动范围内");
    }
}
