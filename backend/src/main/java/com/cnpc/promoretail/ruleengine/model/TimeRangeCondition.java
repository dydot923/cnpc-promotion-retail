package com.cnpc.promoretail.ruleengine.model;

import java.time.LocalTime;

public record TimeRangeCondition(
        LocalTime from,
        LocalTime to
) {
}
