package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCouponId(String couponId);

    List<Coupon> findByHolderMemberId(String holderMemberId);

    List<Coupon> findAvailableByHolderMemberId(String holderMemberId, LocalDate businessDate);

    Optional<Coupon> redeemIfAvailable(
            String couponId,
            String holderMemberId,
            LocalDate businessDate,
            LocalDateTime usedAt,
            String operatorId
    );

    List<Coupon> findAll();
}
