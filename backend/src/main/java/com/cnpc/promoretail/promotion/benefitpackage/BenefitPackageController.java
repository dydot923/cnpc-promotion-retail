package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benefit-packages")
public class BenefitPackageController {

    private final BenefitPackageService benefitPackageService;

    public BenefitPackageController(BenefitPackageService benefitPackageService) {
        this.benefitPackageService = benefitPackageService;
    }

    @GetMapping
    public ApiResponse<List<BenefitPackageResponse>> packages() {
        return ApiResponse.ok(benefitPackageService.packages());
    }

    @GetMapping("/{packageCode}")
    public ApiResponse<BenefitPackageResponse> benefitPackage(@PathVariable String packageCode) {
        return ApiResponse.ok(benefitPackageService.getPackage(packageCode));
    }

    @PostMapping("/{packageCode}/purchase")
    public ApiResponse<BenefitPackagePurchaseResponse> purchase(
            @PathVariable String packageCode,
            @Valid @RequestBody BenefitPackagePurchaseRequest request
    ) {
        return ApiResponse.ok(benefitPackageService.purchase(packageCode, request));
    }
}
