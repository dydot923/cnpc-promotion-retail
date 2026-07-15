package com.cnpc.promoretail.promotion.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.coupon.persistence.CouponTemplateEntity;
import com.cnpc.promoretail.promotion.coupon.persistence.CouponTemplateMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisCouponTemplateRepository implements CouponTemplateRepository {

    private final CouponTemplateMapper mapper;

    public MybatisCouponTemplateRepository(CouponTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CouponTemplate save(CouponTemplate couponTemplate) {
        Optional<CouponTemplateEntity> existing = findEntity(couponTemplate.couponTemplateId());
        CouponTemplateEntity entity = CouponTemplateEntity.from(couponTemplate);
        if (existing.isPresent()) {
            entity.setId(existing.get().getId());
            mapper.updateById(entity);
        } else {
            mapper.insert(entity);
        }
        return couponTemplate;
    }

    @Override
    public Optional<CouponTemplate> findByTemplateId(String couponTemplateId) {
        return findEntity(couponTemplateId).map(CouponTemplateEntity::toCouponTemplate);
    }

    @Override
    public List<CouponTemplate> findAll() {
        return mapper.selectList(null).stream()
                .map(CouponTemplateEntity::toCouponTemplate)
                .toList();
    }

    private Optional<CouponTemplateEntity> findEntity(String couponTemplateId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CouponTemplateEntity>()
                .eq(CouponTemplateEntity::getCouponTemplateId, couponTemplateId)));
    }
}
