package com.cnpc.promoretail.ruleengine.context;

import java.math.BigDecimal;

public record FuelContext(
        FuelType fuelType,
        String fuelGrade,
        BigDecimal amount,
        BigDecimal volume
) {

    public FuelContext {
        fuelType = fuelType == null ? FuelType.NONE : fuelType;
        amount = amount == null ? BigDecimal.ZERO : amount;
        volume = volume == null ? BigDecimal.ZERO : volume;
    }

    public static FuelContext empty() {
        return new FuelContext(FuelType.NONE, null, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

