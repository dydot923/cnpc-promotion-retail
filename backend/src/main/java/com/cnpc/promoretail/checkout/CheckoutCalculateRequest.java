package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CheckoutCalculateRequest(
        @Valid @NotNull OrderContext orderContext,
        LocalDate transactionDate,
        LocalTime transactionTime,
        String stationType,
        String stationProvince,
        String stationCity,
        String stationCode,
        Boolean isMember,
        String memberLevel,
        Integer memberBirthMonth,
        String paymentMethod,
        FuelType fuelType,
        BigDecimal fuelAmount,
        BigDecimal fuelVolume,
        BigDecimal rechargeAmount,
        List<Coupon> availableCoupons,
        List<String> selectedCouponIds,
        String memberCode
) {

    public CheckoutCalculateRequest(@Valid @NotNull OrderContext orderContext) {
        this(orderContext, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, List.of(), List.of(), null);
    }

    public CheckoutCalculateRequest(
            @Valid @NotNull OrderContext orderContext,
            LocalDate transactionDate,
            LocalTime transactionTime,
            String stationType,
            String stationProvince,
            Boolean isMember,
            String memberLevel,
            Integer memberBirthMonth,
            String paymentMethod,
            FuelType fuelType,
            BigDecimal fuelAmount,
            BigDecimal fuelVolume,
            List<Coupon> availableCoupons,
            List<String> selectedCouponIds
    ) {
        this(orderContext, transactionDate, transactionTime, stationType, stationProvince, null, null, isMember,
                memberLevel, memberBirthMonth, paymentMethod, fuelType, fuelAmount, fuelVolume, null,
                availableCoupons, selectedCouponIds, null);
    }

    public CheckoutCalculateRequest(
            @Valid @NotNull OrderContext orderContext,
            LocalDate transactionDate,
            LocalTime transactionTime,
            String stationType,
            String stationProvince,
            Boolean isMember,
            String memberLevel,
            Integer memberBirthMonth,
            String paymentMethod,
            FuelType fuelType,
            BigDecimal fuelAmount,
            BigDecimal fuelVolume,
            List<Coupon> availableCoupons,
            List<String> selectedCouponIds,
            String memberCode
    ) {
        this(orderContext, transactionDate, transactionTime, stationType, stationProvince, null, null, isMember,
                memberLevel, memberBirthMonth, paymentMethod, fuelType, fuelAmount, fuelVolume, null,
                availableCoupons, selectedCouponIds, memberCode);
    }

    public CheckoutCalculateRequest(
            @Valid @NotNull OrderContext orderContext,
            LocalDate transactionDate,
            LocalTime transactionTime,
            String stationType,
            String stationProvince,
            String stationCity,
            String stationCode,
            Boolean isMember,
            String memberLevel,
            Integer memberBirthMonth,
            String paymentMethod,
            FuelType fuelType,
            BigDecimal fuelAmount,
            BigDecimal fuelVolume,
            List<Coupon> availableCoupons,
            List<String> selectedCouponIds,
            String memberCode
    ) {
        this(orderContext, transactionDate, transactionTime, stationType, stationProvince, stationCity, stationCode,
                isMember, memberLevel, memberBirthMonth, paymentMethod, fuelType, fuelAmount, fuelVolume, null,
                availableCoupons, selectedCouponIds, memberCode);
    }

    public CheckoutCalculateRequest {
        rechargeAmount = rechargeAmount == null ? BigDecimal.ZERO : rechargeAmount.max(BigDecimal.ZERO);
        availableCoupons = availableCoupons == null ? List.of() : List.copyOf(availableCoupons);
        selectedCouponIds = selectedCouponIds == null ? List.of() : List.copyOf(selectedCouponIds);
        memberCode = memberCode == null ? "" : memberCode;
    }
}
