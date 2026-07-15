package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTrigger;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTriggerRepository;
import com.cnpc.promoretail.ruleengine.model.DateCondition;
import com.cnpc.promoretail.ruleengine.model.DateConditionType;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.time.LocalDate;
import java.util.List;

public class DateConditionMatcher {

    private final PromotionDateTriggerRepository dateTriggerRepository;

    public DateConditionMatcher() {
        this(PromotionDateTriggerRepository.empty());
    }

    public DateConditionMatcher(PromotionDateTriggerRepository dateTriggerRepository) {
        this.dateTriggerRepository = dateTriggerRepository == null
                ? PromotionDateTriggerRepository.empty()
                : dateTriggerRepository;
    }

    public List<String> mismatchReasons(OrderContext context, PromotionRule rule) {
        List<String> conditionReasons = mismatchReasons(context, rule.condition());
        if (!conditionReasons.isEmpty()) {
            return conditionReasons;
        }
        List<PromotionDateTrigger> triggers = dateTriggerRepository.findByRuleId(rule.ruleId());
        if (triggers.isEmpty()) {
            return List.of();
        }
        boolean matched = triggers.stream()
                .anyMatch(trigger -> trigger.isTriggered(context.transactionDate(), context.transactionTime()));
        return matched ? List.of() : List.of("日期触发器未命中规则：" + rule.ruleId());
    }

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        DateCondition dateCondition = condition.dateCondition();
        if (dateCondition == null) {
            return List.of();
        }
        LocalDate transactionDate = context.transactionDate();
        if (transactionDate == null) {
            return List.of("缺少交易日期，无法判断活动日期");
        }
        return matches(transactionDate, dateCondition) ? List.of() : List.of("非活动日期");
    }

    public boolean matches(LocalDate transactionDate, DateCondition condition) {
        if (transactionDate == null || condition == null || condition.type() == null) {
            return true;
        }
        if (condition.type() == DateConditionType.MONTHLY_DATES) {
            return condition.dates().contains(transactionDate.getDayOfMonth());
        }
        if (condition.type() == DateConditionType.EXCLUDE_MONTHLY_DATES) {
            return !condition.dates().contains(transactionDate.getDayOfMonth());
        }
        if (condition.type() == DateConditionType.MONTHLY_RANGE) {
            int from = condition.fromDay() == null ? 1 : condition.fromDay();
            int to = condition.toDay() == null ? 31 : condition.toDay();
            int current = transactionDate.getDayOfMonth();
            return current >= from && current <= to;
        }
        if (condition.type() == DateConditionType.DATE_RANGE) {
            boolean afterStart = condition.fromDate() == null || !transactionDate.isBefore(condition.fromDate());
            boolean beforeEnd = condition.toDate() == null || !transactionDate.isAfter(condition.toDate());
            return afterStart && beforeEnd;
        }
        return true;
    }
}
