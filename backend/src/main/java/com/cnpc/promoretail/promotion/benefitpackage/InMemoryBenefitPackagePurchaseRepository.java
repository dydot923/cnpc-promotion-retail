package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryBenefitPackagePurchaseRepository implements BenefitPackagePurchaseRepository {

    private final List<BenefitPackagePurchase> purchases = new CopyOnWriteArrayList<>();

    @Override
    public BenefitPackagePurchase save(BenefitPackagePurchase purchase) {
        purchases.add(purchase);
        return purchase;
    }

    @Override
    public List<BenefitPackagePurchase> findByMemberCode(String memberCode) {
        if (memberCode == null || memberCode.isBlank()) {
            return List.of();
        }
        return purchases.stream()
                .filter(purchase -> memberCode.equalsIgnoreCase(purchase.memberCode()))
                .sorted(Comparator.comparing(BenefitPackagePurchase::purchasedAt).reversed())
                .toList();
    }
}
