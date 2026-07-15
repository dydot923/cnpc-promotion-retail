package com.cnpc.promoretail.promotion.bundle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.bundle.persistence.BundleEntity;
import com.cnpc.promoretail.promotion.bundle.persistence.BundleItemEntity;
import com.cnpc.promoretail.promotion.bundle.persistence.BundleItemMapper;
import com.cnpc.promoretail.promotion.bundle.persistence.BundleMapper;
import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.model.BundleDefinition;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisBundleDefinitionProvider implements BundleDefinitionProvider {

    private final BundleMapper bundleMapper;
    private final BundleItemMapper bundleItemMapper;

    public MybatisBundleDefinitionProvider(BundleMapper bundleMapper, BundleItemMapper bundleItemMapper) {
        this.bundleMapper = bundleMapper;
        this.bundleItemMapper = bundleItemMapper;
    }

    @Override
    public Optional<BundleDefinition> findActiveBundle(String bundleId) {
        if (bundleId == null || bundleId.isBlank()) {
            return Optional.empty();
        }
        BundleEntity bundle = bundleMapper.selectOne(new LambdaQueryWrapper<BundleEntity>()
                .eq(BundleEntity::getId, bundleId)
                .eq(BundleEntity::getStatus, "ACTIVE")
                .last("limit 1"));
        if (bundle == null) {
            return Optional.empty();
        }
        return Optional.of(new BundleDefinition(
                bundle.getId(),
                bundle.getName(),
                bundle.getBundlePrice(),
                bundle.getThresholdAmount(),
                bundle.getActivityId(),
                bundleItemMapper.selectList(new LambdaQueryWrapper<BundleItemEntity>()
                                .eq(BundleItemEntity::getBundleId, bundleId)
                                .orderByAsc(BundleItemEntity::getId))
                        .stream()
                        .map(item -> new BundleItem(item.getProductCode(), item.getQuantity()))
                        .toList()
        ));
    }
}
