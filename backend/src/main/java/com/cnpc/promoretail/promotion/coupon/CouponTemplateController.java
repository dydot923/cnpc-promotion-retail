package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon-templates")
public class CouponTemplateController {

    private final CouponManagementService couponManagementService;

    public CouponTemplateController(CouponManagementService couponManagementService) {
        this.couponManagementService = couponManagementService;
    }

    @GetMapping
    public ApiResponse<List<CouponTemplateResponse>> list() {
        return ApiResponse.ok(couponManagementService.templates());
    }

    @GetMapping("/{couponTemplateId}")
    public ApiResponse<CouponTemplateResponse> get(@PathVariable String couponTemplateId) {
        return ApiResponse.ok(couponManagementService.template(couponTemplateId));
    }

    @PostMapping
    public ApiResponse<CouponTemplateResponse> create(@Valid @RequestBody CouponTemplateRequest request) {
        return ApiResponse.ok(couponManagementService.saveTemplate(null, request));
    }

    @PutMapping("/{couponTemplateId}")
    public ApiResponse<CouponTemplateResponse> update(
            @PathVariable String couponTemplateId,
            @Valid @RequestBody CouponTemplateRequest request
    ) {
        return ApiResponse.ok(couponManagementService.saveTemplate(couponTemplateId, request));
    }
}
