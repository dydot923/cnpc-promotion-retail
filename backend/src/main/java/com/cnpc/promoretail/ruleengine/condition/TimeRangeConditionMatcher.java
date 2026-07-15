package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.TimeRangeCondition;
import java.time.LocalTime;
import java.util.List;

public class TimeRangeConditionMatcher {

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        TimeRangeCondition timeRange = condition.timeRangeCondition();
        if (timeRange == null || timeRange.from() == null || timeRange.to() == null) {
            return List.of();
        }
        LocalTime transactionTime = context.transactionTime();
        if (transactionTime == null) {
            return List.of("缺少交易时间，无法判断活动时段");
        }
        return matches(transactionTime, timeRange) ? List.of() : List.of("交易时间不在活动时段内");
    }

    public boolean matches(LocalTime transactionTime, TimeRangeCondition condition) {
        if (transactionTime == null || condition == null || condition.from() == null || condition.to() == null) {
            return true;
        }
        LocalTime from = condition.from();
        LocalTime to = condition.to();
        if (from.equals(to)) {
            return true;
        }
        if (from.isBefore(to)) {
            return !transactionTime.isBefore(from) && !transactionTime.isAfter(to);
        }
        return !transactionTime.isBefore(from) || !transactionTime.isAfter(to);
    }
}
