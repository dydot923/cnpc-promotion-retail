package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import java.util.List;

public interface BenefitPackagePurchaseRepository {

    BenefitPackagePurchase save(BenefitPackagePurchase purchase);

    List<BenefitPackagePurchase> findByMemberCode(String memberCode);
}
