package com.cnpc.promoretail.ruleengine.model;

import com.cnpc.promoretail.ruleengine.context.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
        BigDecimal minInventoryQuantity,
        DateCondition dateCondition,
        TimeRangeCondition timeRangeCondition,
        Set<String> stationProvinces,
        Set<String> memberLevels,
        boolean birthdayMonthRequired,
        Set<String> memberTags,
        BigDecimal minFuelVolume,
        Set<String> includedCategories,
        int minProductQuantity,
        BigDecimal minRechargeAmount
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
        stationProvinces = stationProvinces == null ? Set.of() : Set.copyOf(stationProvinces);
        memberLevels = memberLevels == null ? Set.of() : Set.copyOf(memberLevels);
        memberTags = memberTags == null ? Set.of() : Set.copyOf(memberTags);
        minFuelVolume = minFuelVolume == null ? BigDecimal.ZERO : minFuelVolume;
        includedCategories = includedCategories == null ? Set.of() : Set.copyOf(includedCategories);
        minProductQuantity = Math.max(minProductQuantity, 0);
        minRechargeAmount = minRechargeAmount == null ? BigDecimal.ZERO : minRechargeAmount;
    }

    public PromotionCondition(
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
            BigDecimal minInventoryQuantity,
            DateCondition dateCondition,
            TimeRangeCondition timeRangeCondition,
            Set<String> stationProvinces,
            Set<String> memberLevels,
            boolean birthdayMonthRequired,
            Set<String> memberTags,
            BigDecimal minFuelVolume,
            Set<String> includedCategories,
            int minProductQuantity
    ) {
        this(productCodes, excludedCategories, fuelTypes, stationTypes, daysOfMonth, startDate, endDate,
                minCartAmount, minFuelAmount, memberRequired, minInventoryQuantity,
                dateCondition, timeRangeCondition, stationProvinces, memberLevels, birthdayMonthRequired, memberTags,
                minFuelVolume, includedCategories, minProductQuantity, BigDecimal.ZERO);
    }

    public PromotionCondition(
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
            BigDecimal minInventoryQuantity,
            DateCondition dateCondition,
            TimeRangeCondition timeRangeCondition,
            Set<String> stationProvinces,
            Set<String> memberLevels,
            boolean birthdayMonthRequired,
            BigDecimal minFuelVolume,
            Set<String> includedCategories,
            int minProductQuantity
    ) {
        this(productCodes, excludedCategories, fuelTypes, stationTypes, daysOfMonth, startDate, endDate,
                minCartAmount, minFuelAmount, memberRequired, minInventoryQuantity,
                dateCondition, timeRangeCondition, stationProvinces, memberLevels, birthdayMonthRequired,
                Set.of(), minFuelVolume, includedCategories, minProductQuantity, BigDecimal.ZERO);
    }

    public PromotionCondition(
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
            BigDecimal minInventoryQuantity,
            DateCondition dateCondition,
            TimeRangeCondition timeRangeCondition,
            Set<String> stationProvinces,
            Set<String> memberLevels,
            boolean birthdayMonthRequired,
            BigDecimal minFuelVolume
    ) {
        this(productCodes, excludedCategories, fuelTypes, stationTypes, daysOfMonth, startDate, endDate,
                minCartAmount, minFuelAmount, memberRequired, minInventoryQuantity,
                dateCondition, timeRangeCondition, stationProvinces, memberLevels, birthdayMonthRequired,
                Set.of(), minFuelVolume, Set.of(), 0, BigDecimal.ZERO);
    }

    public PromotionCondition(
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
        this(productCodes, excludedCategories, fuelTypes, stationTypes, daysOfMonth, startDate, endDate,
                minCartAmount, minFuelAmount, memberRequired, minInventoryQuantity,
                null, null, Set.of(), Set.of(), false, BigDecimal.ZERO);
    }

    public boolean matchesAnyCategory(List<String> categories) {
        return categories == null || categories.isEmpty()
                || categories.stream().anyMatch(category -> category != null && !category.isBlank());
    }

    public static PromotionCondition empty() {
        return new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO);
    }

    public static PromotionCondition withIncludedCategories(Set<String> includedCategories) {
        return new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                null, null, Set.of(), Set.of(), false, BigDecimal.ZERO, includedCategories, 0);
    }

    public PromotionCondition withAdditionalExcludedCategories(Set<String> additionalExcludedCategories) {
        if (additionalExcludedCategories == null || additionalExcludedCategories.isEmpty()) {
            return this;
        }
        Set<String> mergedExcludedCategories = new HashSet<>(excludedCategories);
        mergedExcludedCategories.addAll(additionalExcludedCategories);
        return new PromotionCondition(productCodes, mergedExcludedCategories, fuelTypes, stationTypes, daysOfMonth,
                startDate, endDate, minCartAmount, minFuelAmount, memberRequired, minInventoryQuantity,
                dateCondition, timeRangeCondition, stationProvinces, memberLevels, birthdayMonthRequired, memberTags,
                minFuelVolume, includedCategories, minProductQuantity, minRechargeAmount);
    }
}
