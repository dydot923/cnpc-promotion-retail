package com.cnpc.promoretail.ruleengine.datetrigger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record PromotionDateTrigger(
        Long id,
        String activityCode,
        String ruleId,
        String triggerType,
        Set<Integer> daysOfMonth,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime timeFrom,
        LocalTime timeTo,
        String description,
        String sourceSheetName,
        Integer sourceRowNumber,
        boolean enabled
) {

    public PromotionDateTrigger {
        activityCode = blankToEmpty(activityCode);
        ruleId = blankToEmpty(ruleId);
        triggerType = blankToEmpty(triggerType).toUpperCase();
        daysOfMonth = daysOfMonth == null ? Set.of() : Set.copyOf(daysOfMonth);
        description = blankToEmpty(description);
        sourceSheetName = blankToEmpty(sourceSheetName);
    }

    public boolean isTriggered(LocalDate date, LocalTime time) {
        if (!enabled || date == null) {
            return false;
        }
        return switch (triggerType) {
            case "MONTHLY_DATES", "DAYS_OF_MONTH" -> daysOfMonth.contains(date.getDayOfMonth());
            case "EXCLUDE_MONTHLY_DATES" -> !daysOfMonth.contains(date.getDayOfMonth());
            case "DATE_RANGE" -> matchesDateRange(date);
            case "DATE_TIME_RANGE" -> matchesDateRange(date) && matchesTimeRange(time);
            default -> false;
        };
    }

    private boolean matchesDateRange(LocalDate date) {
        return (startDate == null || !date.isBefore(startDate))
                && (endDate == null || !date.isAfter(endDate));
    }

    private boolean matchesTimeRange(LocalTime time) {
        if (timeFrom == null && timeTo == null) {
            return true;
        }
        if (time == null) {
            return false;
        }
        if (timeFrom == null) {
            return !time.isAfter(timeTo);
        }
        if (timeTo == null) {
            return !time.isBefore(timeFrom);
        }
        if (!timeFrom.isAfter(timeTo)) {
            return !time.isBefore(timeFrom) && !time.isAfter(timeTo);
        }
        return !time.isBefore(timeFrom) || !time.isAfter(timeTo);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
