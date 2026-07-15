package com.cnpc.promoretail.promotion.benefitpackage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackagePurchaseEntity;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackagePurchaseMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisBenefitPackagePurchaseRepository implements BenefitPackagePurchaseRepository {

    private final BenefitPackagePurchaseMapper mapper;

    public MybatisBenefitPackagePurchaseRepository(BenefitPackagePurchaseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BenefitPackagePurchase save(BenefitPackagePurchase purchase) {
        mapper.insert(BenefitPackagePurchaseEntity.from(purchase));
        return purchase;
    }

    @Override
    public List<BenefitPackagePurchase> findByMemberCode(String memberCode) {
        if (memberCode == null || memberCode.isBlank()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<BenefitPackagePurchaseEntity>()
                        .eq(BenefitPackagePurchaseEntity::getMemberCode, memberCode.trim())
                        .orderByDesc(BenefitPackagePurchaseEntity::getPurchasedAt))
                .stream()
                .map(BenefitPackagePurchaseEntity::toPurchase)
                .toList();
    }
}
