package com.cnpc.promoretail.promotion.points;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PointsActivity(
        String activityId,
        String ruleId,
        String activityName,
        BigDecimal pointsMultiplier,
        boolean memberRequired,
        LocalDate startDate,
        LocalDate endDate,
        Set<Integer> daysOfMonth,
        Set<String> stationTypes,
        Set<String> stationProvinces,
        Set<String> fuelTypes,
        Set<String> includedCategories,
        Set<String> excludedCategories,
        String status
) {

    public PointsActivity {
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("activityId is required");
        }
        activityName = activityName == null || activityName.isBlank() ? activityId : activityName;
        pointsMultiplier = pointsMultiplier == null || pointsMultiplier.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : pointsMultiplier;
        daysOfMonth = daysOfMonth == null ? Set.of() : Set.copyOf(daysOfMonth);
        stationTypes = stationTypes == null ? Set.of() : Set.copyOf(stationTypes);
        stationProvinces = stationProvinces == null ? Set.of() : Set.copyOf(stationProvinces);
        fuelTypes = fuelTypes == null ? Set.of() : Set.copyOf(fuelTypes);
        includedCategories = includedCategories == null ? Set.of() : Set.copyOf(includedCategories);
        excludedCategories = excludedCategories == null ? Set.of() : Set.copyOf(excludedCategories);
        status = status == null || status.isBlank() ? "ACTIVE" : status;
    }

    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
