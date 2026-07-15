package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryBenefitPackageRepository implements BenefitPackageRepository {

    private final ConcurrentMap<String, BenefitPackage> packages = new ConcurrentHashMap<>();

    @Override
    public List<BenefitPackage> findActive() {
        return packages.values().stream()
                .filter(BenefitPackage::active)
                .sorted(Comparator.comparing(BenefitPackage::packageCode))
                .toList();
    }

    @Override
    public Optional<BenefitPackage> findActiveByPackageCode(String packageCode) {
        return Optional.ofNullable(packages.get(normalize(packageCode)))
                .filter(BenefitPackage::active);
    }

    public BenefitPackage save(BenefitPackage benefitPackage) {
        packages.put(normalize(benefitPackage.packageCode()), benefitPackage);
        return benefitPackage;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
