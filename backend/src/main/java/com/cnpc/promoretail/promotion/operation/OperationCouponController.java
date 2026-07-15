package com.cnpc.promoretail.promotion.operation;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation-campaigns")
public class OperationCouponController {

    private final OperationCouponService operationCouponService;

    public OperationCouponController(OperationCouponService operationCouponService) {
        this.operationCouponService = operationCouponService;
    }

    @GetMapping
    public ApiResponse<List<OperationCampaignDefinition>> campaigns() {
        return ApiResponse.ok(List.of(
                new OperationCampaignDefinition(
                        "rfm-recovery",
                        "RFM 客户挽回",
                        "/operation-campaigns/rfm-recovery/coupons",
                        "按汽油/柴油客群发放油品券和便利店券",
                        List.of("memberCode"),
                        List.of("customerType", "businessDate", "operatorId", "operatorName")
                ),
                new OperationCampaignDefinition(
                        "birthday",
                        "生日礼包",
                        "/operation-campaigns/birthday/coupons",
                        "会员生日月发放汽油券、商品券和洗车券",
                        List.of("memberCode"),
                        List.of("businessDate", "operatorId", "operatorName")
                ),
                new OperationCampaignDefinition(
                        "sign-in",
                        "签到活动",
                        "/operation-campaigns/sign-in/coupons",
                        "3/7/10 天签到阶梯发券",
                        List.of("memberCode", "signInDays"),
                        List.of("businessDate", "operatorId", "operatorName")
                ),
                new OperationCampaignDefinition(
                        "group-buy",
                        "拼团活动",
                        "/operation-campaigns/group-buy/coupons",
                        "按成团人数和新老会员身份发券",
                        List.of("memberCode", "groupId", "groupSize"),
                        List.of("memberRole", "businessDate", "operatorId", "operatorName")
                ),
                new OperationCampaignDefinition(
                        "industry-certification",
                        "行业认证",
                        "/operation-campaigns/industry-certification/coupons",
                        "认证人群专属油品券和商品券",
                        List.of("memberCode"),
                        List.of("qualificationType", "businessDate", "operatorId", "operatorName")
                ),
                new OperationCampaignDefinition(
                        "ecommerce",
                        "电商平台",
                        "/operation-campaigns/ecommerce/coupons",
                        "按平台订单奖励编码发券",
                        List.of("memberCode"),
                        List.of("rewardCode", "quantity", "eventKey", "businessDate", "operatorId", "operatorName")
                )
        ));
    }

    @PostMapping("/rfm-recovery/coupons")
    public ApiResponse<OperationCouponIssueResponse> rfmRecovery(
            @Valid @RequestBody RfmRecoveryRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueRfmRecovery(request));
    }

    @PostMapping("/birthday/coupons")
    public ApiResponse<OperationCouponIssueResponse> birthday(
            @Valid @RequestBody OperationRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueBirthday(request));
    }

    @PostMapping("/sign-in/coupons")
    public ApiResponse<OperationCouponIssueResponse> signIn(
            @Valid @RequestBody SignInRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueSignIn(request));
    }

    @PostMapping("/group-buy/coupons")
    public ApiResponse<OperationCouponIssueResponse> groupBuy(
            @Valid @RequestBody GroupBuyRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueGroupBuy(request));
    }

    @PostMapping("/industry-certification/coupons")
    public ApiResponse<OperationCouponIssueResponse> industryCertification(
            @Valid @RequestBody QualificationRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueIndustryCertification(request));
    }

    @PostMapping("/ecommerce/coupons")
    public ApiResponse<OperationCouponIssueResponse> ecommerce(
            @Valid @RequestBody EcommerceRewardRequest request
    ) {
        return ApiResponse.ok(operationCouponService.issueEcommerce(request));
    }
}
