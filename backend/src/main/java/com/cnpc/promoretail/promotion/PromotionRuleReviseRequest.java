package com.cnpc.promoretail.promotion;

import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PromotionRuleReviseRequest(
        @NotBlank String ruleId,
        @PositiveOrZero Integer priority,
        String exclusiveGroup,
        Boolean stackable,
        LocalDate startTime,
        LocalDate endTime,
        @Valid PromotionRuleParams ruleParams,
        @NotBlank @Size(max = 500) String reason,
        String operatorId,
        String operatorName
) {

    @AssertTrue(message = "startTime must be before or equal to endTime")
    public boolean isValidTimeRange() {
        return startTime == null || endTime == null || !startTime.isAfter(endTime);
    }

    public record PromotionRuleParams(
            PromotionCondition condition,
            PromotionBenefit benefit
    ) {
    }
}
