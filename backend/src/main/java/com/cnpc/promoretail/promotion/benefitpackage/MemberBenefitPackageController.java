package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberBenefitPackageController {

    private final BenefitPackageService benefitPackageService;

    public MemberBenefitPackageController(BenefitPackageService benefitPackageService) {
        this.benefitPackageService = benefitPackageService;
    }

    @GetMapping("/{memberCode}/benefit-packages")
    public ApiResponse<List<BenefitPackagePurchaseResponse>> memberPurchases(@PathVariable String memberCode) {
        return ApiResponse.ok(benefitPackageService.memberPurchases(memberCode));
    }
}
