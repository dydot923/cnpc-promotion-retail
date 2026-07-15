package com.cnpc.promoretail.promotion.productgroup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.productgroup.persistence.ProductGroupEntity;
import com.cnpc.promoretail.promotion.productgroup.persistence.ProductGroupItemEntity;
import com.cnpc.promoretail.promotion.productgroup.persistence.ProductGroupItemMapper;
import com.cnpc.promoretail.promotion.productgroup.persistence.ProductGroupMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev-db", "postgres"})
public class MybatisProductGroupService implements ProductGroupService {

    private final ProductGroupMapper productGroupMapper;
    private final ProductGroupItemMapper productGroupItemMapper;

    public MybatisProductGroupService(
            ProductGroupMapper productGroupMapper,
            ProductGroupItemMapper productGroupItemMapper
    ) {
        this.productGroupMapper = productGroupMapper;
        this.productGroupItemMapper = productGroupItemMapper;
    }

    @Override
    public List<ProductGroupMapping> findAll() {
        return productGroupMapper.selectList(new LambdaQueryWrapper<ProductGroupEntity>()
                        .orderByAsc(ProductGroupEntity::getId))
                .stream()
                .map(this::toMapping)
                .toList();
    }

    @Override
    public Optional<ProductGroupMapping> findByGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(productGroupMapper.selectOne(new LambdaQueryWrapper<ProductGroupEntity>()
                        .eq(ProductGroupEntity::getId, groupId)
                        .last("limit 1")))
                .map(this::toMapping);
    }

    private ProductGroupMapping toMapping(ProductGroupEntity entity) {
        return new ProductGroupMapping(
                entity.getId(),
                entity.getName(),
                entity.getSource(),
                entity.getDescription(),
                productGroupItemMapper.selectList(new LambdaQueryWrapper<ProductGroupItemEntity>()
                                .eq(ProductGroupItemEntity::getGroupId, entity.getId())
                                .orderByAsc(ProductGroupItemEntity::getProductCode))
                        .stream()
                        .map(ProductGroupItemEntity::getProductCode)
                        .toList(),
                Boolean.TRUE.equals(entity.getDemoData())
        );
    }
}
