package com.cnpc.promoretail.member.model;

import java.math.BigDecimal;
import java.util.List;

public record MemberLevel(
        String levelCode,
        String levelName,
        BigDecimal discountRate,
        BigDecimal pointsMultiplier,
        BigDecimal minConsumption,
        List<String> benefits,
        int priority
) {

    public MemberLevel {
        benefits = benefits == null ? List.of() : List.copyOf(benefits);
    }
}
