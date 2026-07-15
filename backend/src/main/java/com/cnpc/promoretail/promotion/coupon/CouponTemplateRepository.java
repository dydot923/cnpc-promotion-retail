package com.cnpc.promoretail.promotion.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponTemplateRepository {

    CouponTemplate save(CouponTemplate couponTemplate);

    Optional<CouponTemplate> findByTemplateId(String couponTemplateId);

    List<CouponTemplate> findAll();
}
