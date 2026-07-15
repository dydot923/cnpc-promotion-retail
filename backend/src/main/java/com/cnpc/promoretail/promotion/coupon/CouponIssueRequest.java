package com.cnpc.promoretail.promotion.coupon;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CouponIssueRequest(
        @NotBlank String couponTemplateId,
        @NotBlank String holderMemberId,
        @Min(1) @Max(1000) Integer quantity,
        LocalDate validFrom,
        LocalDate validUntil,
        String operatorId,
        String operatorName,
        String reason
) {
}
