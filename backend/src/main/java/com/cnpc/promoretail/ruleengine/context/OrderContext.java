package com.cnpc.promoretail.ruleengine.context;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OrderContext(
        StationContext station,
        CustomerContext customer,
        FuelContext fuel,
        List<CartItem> cartItems,
        LocalDate businessDate,
        LocalTime businessTime,
        List<Coupon> availableCoupons,
        BigDecimal rechargeAmount
) {

    public OrderContext {
        station = station == null ? StationContext.defaultStation() : station;
        customer = customer == null ? CustomerContext.anonymous() : customer;
        fuel = fuel == null ? FuelContext.empty() : fuel;
        cartItems = cartItems == null ? List.of() : List.copyOf(cartItems);
        availableCoupons = availableCoupons == null ? List.of() : List.copyOf(availableCoupons);
        rechargeAmount = rechargeAmount == null ? BigDecimal.ZERO : rechargeAmount.max(BigDecimal.ZERO);
    }

    public OrderContext(
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            List<CartItem> cartItems,
            LocalDate businessDate,
            LocalTime businessTime,
            List<Coupon> availableCoupons
    ) {
        this(station, customer, fuel, cartItems, businessDate, businessTime, availableCoupons, BigDecimal.ZERO);
    }

    public OrderContext(
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            List<CartItem> cartItems,
            LocalDate businessDate,
            LocalTime businessTime
    ) {
        this(station, customer, fuel, cartItems, businessDate, businessTime, List.of(), BigDecimal.ZERO);
    }

    public LocalDate transactionDate() {
        return businessDate;
    }

    public LocalTime transactionTime() {
        return businessTime;
    }

    public String stationCode() {
        return station.stationId();
    }
}
