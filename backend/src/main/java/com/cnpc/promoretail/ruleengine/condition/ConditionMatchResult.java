package com.cnpc.promoretail.ruleengine.condition;

import java.util.List;

public record ConditionMatchResult(
        boolean matched,
        List<String> blockedReasons
) {

    public ConditionMatchResult {
        blockedReasons = blockedReasons == null ? List.of() : List.copyOf(blockedReasons);
    }

    public static ConditionMatchResult success() {
        return new ConditionMatchResult(true, List.of());
    }

    public static ConditionMatchResult blocked(List<String> reasons) {
        return new ConditionMatchResult(false, reasons);
    }
}
