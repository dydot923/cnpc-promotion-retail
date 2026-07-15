package com.cnpc.promoretail.ruleengine.model;

import java.time.LocalDate;
import java.util.Set;

public record DateCondition(
        DateConditionType type,
        Set<Integer> dates,
        Integer fromDay,
        Integer toDay,
        LocalDate fromDate,
        LocalDate toDate
) {

    public DateCondition {
        dates = dates == null ? Set.of() : Set.copyOf(dates);
    }

    public static DateCondition monthlyDates(Set<Integer> dates) {
        return new DateCondition(DateConditionType.MONTHLY_DATES, dates, null, null, null, null);
    }

    public static DateCondition excludeMonthlyDates(Set<Integer> dates) {
        return new DateCondition(DateConditionType.EXCLUDE_MONTHLY_DATES, dates, null, null, null, null);
    }

    public static DateCondition monthlyRange(int fromDay, int toDay) {
        return new DateCondition(DateConditionType.MONTHLY_RANGE, Set.of(), fromDay, toDay, null, null);
    }

    public static DateCondition dateRange(LocalDate fromDate, LocalDate toDate) {
        return new DateCondition(DateConditionType.DATE_RANGE, Set.of(), null, null, fromDate, toDate);
    }
}
