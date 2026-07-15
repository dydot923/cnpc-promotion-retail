package com.cnpc.promoretail.promotion.coupon;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryCouponTemplateRepository implements CouponTemplateRepository {

    private final ConcurrentMap<String, CouponTemplate> templates = new ConcurrentHashMap<>();

    @Override
    public CouponTemplate save(CouponTemplate couponTemplate) {
        templates.put(couponTemplate.couponTemplateId(), couponTemplate);
        return couponTemplate;
    }

    @Override
    public Optional<CouponTemplate> findByTemplateId(String couponTemplateId) {
        return Optional.ofNullable(templates.get(couponTemplateId));
    }

    @Override
    public List<CouponTemplate> findAll() {
        return templates.values().stream()
                .sorted(Comparator.comparing(CouponTemplate::couponTemplateId))
                .toList();
    }
}
