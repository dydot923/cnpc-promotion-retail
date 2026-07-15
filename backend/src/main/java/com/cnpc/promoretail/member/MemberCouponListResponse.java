package com.cnpc.promoretail.member;

import java.util.List;

public record MemberCouponListResponse(
        String memberCode,
        List<MemberCouponResponse> coupons
) {
}
