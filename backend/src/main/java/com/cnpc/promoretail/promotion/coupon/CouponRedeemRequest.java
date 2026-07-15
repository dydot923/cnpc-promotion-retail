package com.cnpc.promoretail.promotion.coupon;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CouponRedeemRequest(
        @NotBlank String couponId,
        String holderMemberId,
        LocalDate businessDate,
        String operatorId,
        String operatorName,
        String reason
) {
}
