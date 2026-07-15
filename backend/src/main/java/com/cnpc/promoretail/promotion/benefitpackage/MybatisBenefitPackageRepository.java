package com.cnpc.promoretail.promotion.benefitpackage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackageEntity;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackageItemEntity;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackageItemMapper;
import com.cnpc.promoretail.promotion.benefitpackage.persistence.BenefitPackageMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisBenefitPackageRepository implements BenefitPackageRepository {

    private final BenefitPackageMapper packageMapper;
    private final BenefitPackageItemMapper itemMapper;

    public MybatisBenefitPackageRepository(
            BenefitPackageMapper packageMapper,
            BenefitPackageItemMapper itemMapper
    ) {
        this.packageMapper = packageMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public List<BenefitPackage> findActive() {
        return packageMapper.selectList(new LambdaQueryWrapper<BenefitPackageEntity>()
                        .eq(BenefitPackageEntity::getStatus, "ACTIVE")
                        .orderByAsc(BenefitPackageEntity::getPackageCode))
                .stream()
                .map(this::toPackage)
                .toList();
    }

    @Override
    public Optional<BenefitPackage> findActiveByPackageCode(String packageCode) {
        if (packageCode == null || packageCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(packageMapper.selectOne(new LambdaQueryWrapper<BenefitPackageEntity>()
                        .eq(BenefitPackageEntity::getPackageCode, packageCode.trim())
                        .eq(BenefitPackageEntity::getStatus, "ACTIVE")
                        .last("limit 1")))
                .map(this::toPackage);
    }

    private BenefitPackage toPackage(BenefitPackageEntity entity) {
        List<BenefitPackageItem> items = itemMapper.selectList(new LambdaQueryWrapper<BenefitPackageItemEntity>()
                        .eq(BenefitPackageItemEntity::getPackageCode, entity.getPackageCode())
                        .orderByAsc(BenefitPackageItemEntity::getSourceRowNumber, BenefitPackageItemEntity::getId))
                .stream()
                .map(BenefitPackageItemEntity::toItem)
                .toList();
        return entity.toPackage(items);
    }
}
