package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import java.util.List;
import java.util.Optional;

public interface BenefitPackageRepository {

    List<BenefitPackage> findActive();

    Optional<BenefitPackage> findActiveByPackageCode(String packageCode);
}
