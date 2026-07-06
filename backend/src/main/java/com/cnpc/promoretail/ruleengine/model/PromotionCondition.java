package com.cnpc.promoretail.ruleengine.model;

import com.cnpc.promoretail.ruleengine.context.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PromotionCondition(
        Set<String> productCodes,
        Set<String> excludedCategories,
        Set<FuelType> fuelTypes,
        Set<String> stationTypes,
        Set<Integer> daysOfMonth,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minCartAmount,
        BigDecimal minFuelAmount,
        boolean memberRequired,
        BigDecimal minInventoryQuantity
) {

    public PromotionCondition {
        productCodes = productCodes == null ? Set.of() : Set.copyOf(productCodes);
        excludedCategories = excludedCategories == null ? Set.of() : Set.copyOf(excludedCategories);
        fuelTypes = fuelTypes == null ? Set.of() : Set.copyOf(fuelTypes);
        stationTypes = stationTypes == null ? Set.of() : Set.copyOf(stationTypes);
        daysOfMonth = daysOfMonth == null ? Set.of() : Set.copyOf(daysOfMonth);
        minCartAmount = minCartAmount == null ? BigDecimal.ZERO : minCartAmount;
        minFuelAmount = minFuelAmount == null ? BigDecimal.ZERO : minFuelAmount;
        minInventoryQuantity = minInventoryQuantity == null ? BigDecimal.ZERO : minInventoryQuantity;
    }

    public static PromotionCondition empty() {
        return new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO);
    }
}

