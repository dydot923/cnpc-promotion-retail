package com.cnpc.promoretail.ruleengine.context;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OrderContext(
        StationContext station,
        CustomerContext customer,
        FuelContext fuel,
        List<CartItem> cartItems,
        LocalDate businessDate,
        LocalTime businessTime
) {

    public OrderContext {
        station = station == null ? StationContext.defaultStation() : station;
        customer = customer == null ? CustomerContext.anonymous() : customer;
        fuel = fuel == null ? FuelContext.empty() : fuel;
        cartItems = cartItems == null ? List.of() : List.copyOf(cartItems);
    }
}

