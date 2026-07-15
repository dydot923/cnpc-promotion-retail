package com.cnpc.promoretail.promotion.benefitpackage;

import java.util.List;

public interface BenefitPackageService {

    List<BenefitPackageResponse> packages();

    BenefitPackageResponse getPackage(String packageCode);

    BenefitPackagePurchaseResponse purchase(String packageCode, BenefitPackagePurchaseRequest request);

    List<BenefitPackagePurchaseResponse> memberPurchases(String memberCode);
}
