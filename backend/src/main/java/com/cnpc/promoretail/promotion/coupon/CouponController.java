package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponManagementService couponManagementService;

    public CouponController(CouponManagementService couponManagementService) {
        this.couponManagementService = couponManagementService;
    }

    @PostMapping("/issue")
    public ApiResponse<List<CouponResponse>> issue(@Valid @RequestBody CouponIssueRequest request) {
        return ApiResponse.ok(couponManagementService.issue(request));
    }

    @PostMapping("/redeem")
    public ApiResponse<CouponResponse> redeem(@Valid @RequestBody CouponRedeemRequest request) {
        return ApiResponse.ok(couponManagementService.redeem(request));
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> get(@PathVariable String couponId) {
        return ApiResponse.ok(couponManagementService.coupon(couponId));
    }

    @GetMapping("/stats")
    public ApiResponse<CouponStatsResponse> stats(
            @RequestParam(required = false) String couponTemplateId,
            @RequestParam(required = false) String holderMemberId
    ) {
        return ApiResponse.ok(couponManagementService.stats(couponTemplateId, holderMemberId));
    }
}
