package com.cnpc.promoretail.promotion.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cnpc.promoretail.promotion.coupon.persistence.CouponEntity;
import com.cnpc.promoretail.promotion.coupon.persistence.CouponMapper;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisCouponRepository implements CouponRepository {

    private final CouponMapper mapper;

    public MybatisCouponRepository(CouponMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Coupon save(Coupon coupon) {
        Optional<CouponEntity> existing = findEntity(coupon.couponId());
        CouponEntity entity = CouponEntity.from(coupon);
        if (existing.isPresent()) {
            entity.setId(existing.get().getId());
            mapper.updateById(entity);
        } else {
            mapper.insert(entity);
        }
        return coupon;
    }

    @Override
    public Optional<Coupon> findByCouponId(String couponId) {
        return findEntity(couponId).map(CouponEntity::toCoupon);
    }

    @Override
    public List<Coupon> findByHolderMemberId(String holderMemberId) {
        if (holderMemberId == null || holderMemberId.isBlank()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CouponEntity>()
                        .eq(CouponEntity::getHolderMemberId, holderMemberId))
                .stream()
                .map(CouponEntity::toCoupon)
                .toList();
    }

    @Override
    public List<Coupon> findAvailableByHolderMemberId(String holderMemberId, LocalDate businessDate) {
        if (holderMemberId == null || holderMemberId.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<CouponEntity> wrapper = new LambdaQueryWrapper<CouponEntity>()
                .eq(CouponEntity::getHolderMemberId, holderMemberId)
                .eq(CouponEntity::getStatus, CouponStatus.AVAILABLE.name())
                .orderByAsc(CouponEntity::getCouponId);
        addValidDateFilter(wrapper, businessDate);
        return mapper.selectList(wrapper).stream()
                .map(CouponEntity::toCoupon)
                .toList();
    }

    @Override
    public Optional<Coupon> redeemIfAvailable(
            String couponId,
            String holderMemberId,
            LocalDate businessDate,
            LocalDateTime usedAt,
            String operatorId
    ) {
        LambdaUpdateWrapper<CouponEntity> wrapper = new LambdaUpdateWrapper<CouponEntity>()
                .eq(CouponEntity::getCouponId, couponId)
                .eq(CouponEntity::getStatus, CouponStatus.AVAILABLE.name())
                .set(CouponEntity::getStatus, CouponStatus.USED.name())
                .set(CouponEntity::getUsedAt, usedAt)
                .set(CouponEntity::getOperatorId, operatorId);
        if (holderMemberId != null && !holderMemberId.isBlank()) {
            wrapper.eq(CouponEntity::getHolderMemberId, holderMemberId);
        }
        addValidDateFilter(wrapper, businessDate);
        return mapper.update(null, wrapper) == 0
                ? Optional.empty()
                : findByCouponId(couponId);
    }

    @Override
    public List<Coupon> findAll() {
        return mapper.selectList(null).stream()
                .map(CouponEntity::toCoupon)
                .toList();
    }

    private Optional<CouponEntity> findEntity(String couponId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CouponEntity>()
                .eq(CouponEntity::getCouponId, couponId)));
    }

    private void addValidDateFilter(LambdaQueryWrapper<CouponEntity> wrapper, LocalDate businessDate) {
        if (businessDate == null) {
            return;
        }
        wrapper.and(query -> query.isNull(CouponEntity::getValidFrom)
                        .or()
                        .le(CouponEntity::getValidFrom, businessDate))
                .and(query -> query.isNull(CouponEntity::getValidUntil)
                        .or()
                        .ge(CouponEntity::getValidUntil, businessDate));
    }

    private void addValidDateFilter(LambdaUpdateWrapper<CouponEntity> wrapper, LocalDate businessDate) {
        if (businessDate == null) {
            return;
        }
        wrapper.and(query -> query.isNull(CouponEntity::getValidFrom)
                        .or()
                        .le(CouponEntity::getValidFrom, businessDate))
                .and(query -> query.isNull(CouponEntity::getValidUntil)
                        .or()
                        .ge(CouponEntity::getValidUntil, businessDate));
    }
}
