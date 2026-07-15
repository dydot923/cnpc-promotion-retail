package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryCouponRepository implements CouponRepository {

    private final ConcurrentMap<String, Coupon> coupons = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        coupons.put(coupon.couponId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findByCouponId(String couponId) {
        return Optional.ofNullable(coupons.get(couponId));
    }

    @Override
    public List<Coupon> findByHolderMemberId(String holderMemberId) {
        if (holderMemberId == null || holderMemberId.isBlank()) {
            return List.of();
        }
        return coupons.values().stream()
                .filter(coupon -> holderMemberId.equals(coupon.holderMemberId()))
                .sorted(Comparator.comparing(Coupon::couponId))
                .toList();
    }

    @Override
    public List<Coupon> findAvailableByHolderMemberId(String holderMemberId, LocalDate businessDate) {
        if (holderMemberId == null || holderMemberId.isBlank()) {
            return List.of();
        }
        return coupons.values().stream()
                .filter(coupon -> holderMemberId.equals(coupon.holderMemberId()))
                .filter(coupon -> availableOn(coupon, businessDate))
                .sorted(Comparator.comparing(Coupon::couponId))
                .toList();
    }

    @Override
    public synchronized Optional<Coupon> redeemIfAvailable(
            String couponId,
            String holderMemberId,
            LocalDate businessDate,
            LocalDateTime usedAt,
            String operatorId
    ) {
        Coupon coupon = coupons.get(couponId);
        if (coupon == null || !holderMatches(coupon, holderMemberId) || !availableOn(coupon, businessDate)) {
            return Optional.empty();
        }
        Coupon redeemed = coupon.markUsed(usedAt, operatorId);
        coupons.put(couponId, redeemed);
        return Optional.of(redeemed);
    }

    @Override
    public List<Coupon> findAll() {
        return coupons.values().stream()
                .sorted(Comparator.comparing(Coupon::couponId))
                .toList();
    }

    private boolean availableOn(Coupon coupon, LocalDate businessDate) {
        if (coupon.status() != CouponStatus.AVAILABLE) {
            return false;
        }
        if (businessDate == null) {
            return true;
        }
        return (coupon.validFrom() == null || !businessDate.isBefore(coupon.validFrom()))
                && (coupon.validUntil() == null || !businessDate.isAfter(coupon.validUntil()));
    }

    private boolean holderMatches(Coupon coupon, String holderMemberId) {
        String expectedHolder = holderMemberId == null ? "" : holderMemberId;
        return expectedHolder.isBlank() || expectedHolder.equals(coupon.holderMemberId());
    }
}
