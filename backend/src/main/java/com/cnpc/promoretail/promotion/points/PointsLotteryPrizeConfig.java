package com.cnpc.promoretail.promotion.points;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

public record PointsLotteryPrizeConfig(
        String prizeId,
        String activityCode,
        String prizeName,
        String prizeType,
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        int validDays,
        int weight,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static final String DEFAULT_ACTIVITY_CODE = "activity-board-v2-g2-points-lottery";
    public static final String PRIZE_TYPE_COUPON = "COUPON";
    public static final String PRIZE_TYPE_NO_PRIZE = "NO_PRIZE";

    public PointsLotteryPrizeConfig {
        if (prizeId == null || prizeId.isBlank()) {
            throw new IllegalArgumentException("prizeId is required");
        }
        prizeId = prizeId.trim();
        activityCode = activityCode == null || activityCode.isBlank()
                ? DEFAULT_ACTIVITY_CODE
                : activityCode.trim();
        prizeName = prizeName == null || prizeName.isBlank() ? prizeId.trim() : prizeName.trim();
        prizeType = normalizeType(prizeType);
        couponTemplateId = couponTemplateId == null ? "" : couponTemplateId.trim();
        couponName = couponName == null || couponName.isBlank() ? prizeName : couponName.trim();
        faceValue = money(faceValue);
        minSpendAmount = money(minSpendAmount);
        applicableCategories = applicableCategories == null ? List.of() : List.copyOf(applicableCategories);
        excludedCategories = excludedCategories == null ? List.of() : List.copyOf(excludedCategories);
        validDays = validDays <= 0 ? 30 : validDays;
        weight = Math.max(0, weight);
        status = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status) && weight > 0;
    }

    public boolean couponPrize() {
        return PRIZE_TYPE_COUPON.equalsIgnoreCase(prizeType);
    }

    public static PointsLotteryPrizeConfig defaultNoPrize() {
        return new PointsLotteryPrizeConfig(
                "g2-lottery-no-prize",
                DEFAULT_ACTIVITY_CODE,
                "No prize",
                PRIZE_TYPE_NO_PRIZE,
                "",
                "",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                30,
                50,
                "ACTIVE",
                null,
                null
        );
    }

    public static PointsLotteryPrizeConfig defaultStoreCoupon() {
        return new PointsLotteryPrizeConfig(
                "g2-lottery-store-10",
                DEFAULT_ACTIVITY_CODE,
                "10 yuan store coupon",
                PRIZE_TYPE_COUPON,
                "points-lottery-store-10",
                "Points lottery 10 yuan store coupon",
                new BigDecimal("10.00"),
                new BigDecimal("50.00"),
                List.of("store"),
                List.of("cigarette", "fertilizer", "\u9999\u70df", "\u5316\u80a5"),
                30,
                50,
                "ACTIVE",
                null,
                null
        );
    }

    private static String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return PRIZE_TYPE_NO_PRIZE;
        }
        String normalized = value.trim().toUpperCase();
        return PRIZE_TYPE_COUPON.equals(normalized) ? PRIZE_TYPE_COUPON : PRIZE_TYPE_NO_PRIZE;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
