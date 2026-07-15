package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.ruleengine.context.FuelType;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutExchangeOfferResponse(
        String ruleId,
        String activityName,
        String ruleVersion,
        String productCode,
        String productName,
        String barcode,
        String category,
        BigDecimal unitPrice,
        BigDecimal exchangePrice,
        int exchangeQuantity,
        BigDecimal minFuelAmount,
        List<FuelType> fuelTypes,
        BigDecimal estimatedDiscount,
        BigDecimal inventoryQuantity,
        boolean eligible,
        List<String> blockedReasons
) {
}
