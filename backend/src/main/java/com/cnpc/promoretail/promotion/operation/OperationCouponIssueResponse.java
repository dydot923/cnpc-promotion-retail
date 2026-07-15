package com.cnpc.promoretail.promotion.operation;

import com.cnpc.promoretail.promotion.coupon.CouponResponse;
import java.util.List;

public record OperationCouponIssueResponse(
        String activityCode,
        String memberCode,
        String eventKey,
        List<CouponResponse> coupons
) {
}
