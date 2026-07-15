package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.CouponRedeemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CouponRedeemBenefitCalculatorTest {

    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            calculators(),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void availableCouponCreatesRedeemCandidate() {
        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "50.00")), coupon("coupon-5", "5元券", "5.00", "40.00")),
                List.of(couponRule())
        );

        PromotionCandidate candidate = candidate(result, "cand-coupon-redeem-coupon-5");
        assertThat(candidate.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(candidate.payableAmount()).isEqualByComparingTo("45.00");
        assertThat(candidate.consumedCouponIds()).containsExactly("coupon-5");
        assertThat(candidate.explanation()).contains("5元券").contains("抵扣5.00元");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-coupon-redeem-coupon-5");
    }

    @Test
    void expiredCouponIsBlockedWithReason() {
        Coupon expired = coupon("expired", "过期券", "5.00", "40.00",
                List.of("饮料"), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), false, false, CouponStatus.AVAILABLE);

        CalculationResult result = engine.calculate(order(List.of(item("drink-1", "饮料", "50.00")), expired),
                List.of(couponRule()));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("券已过期"));
    }

    @Test
    void couponBelowMinSpendIsBlocked() {
        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "30.00")), coupon("coupon-5", "5元券", "5.00", "40.00")),
                List.of(couponRule())
        );

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("未满40.00元"));
    }

    @Test
    void couponCategoryMismatchIsBlocked() {
        CalculationResult result = engine.calculate(
                order(List.of(item("snack-1", "零食", "50.00")), coupon("coupon-5", "饮料券", "5.00", "40.00")),
                List.of(couponRule())
        );

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("适用范围不匹配"));
    }

    @Test
    void couponExcludedCategoryHitIsBlocked() {
        Coupon coupon = coupon("coupon-5", "非烟券", "5.00", "40.00",
                List.of(), List.of("香烟"), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), false, false, CouponStatus.AVAILABLE);

        CalculationResult result = engine.calculate(order(List.of(item("cigarette-1", "香烟", "50.00")), coupon),
                List.of(couponRule()));

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("排除品类命中"));
    }

    @Test
    void couponDiscountDoesNotExceedApplicableSubtotal() {
        Coupon coupon = coupon("coupon-10", "大额券", "10.00", "1.00");

        CalculationResult result = engine.calculate(order(List.of(item("drink-1", "饮料", "3.00")), coupon),
                List.of(couponRule()));

        PromotionCandidate candidate = candidate(result, "cand-coupon-redeem-coupon-10");
        assertThat(candidate.discountAmount()).isEqualByComparingTo("3.00");
        assertThat(candidate.payableAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void multipleCouponsCreateIndependentCandidates() {
        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "50.00")),
                        coupon("coupon-5", "5元券", "5.00", "40.00"),
                        coupon("coupon-8", "8元券", "8.00", "40.00")),
                List.of(couponRule())
        );

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-coupon-redeem-coupon-5", "cand-coupon-redeem-coupon-8");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-coupon-redeem-coupon-8");
    }

    @Test
    void nonStackableCouponCompetesWithDirectDiscount() {
        PromotionRule discount = rule("discount-10", PromotionRuleType.PERCENTAGE_DISCOUNT,
                PromotionCondition.empty(), PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount", false);

        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "100.00")), coupon("coupon-5", "5元券", "5.00", "40.00")),
                List.of(couponRule(), discount)
        );

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-discount-10");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-discount-10");
    }

    @Test
    void stackableCouponCanCoexistWithGiftCouponCandidate() {
        Coupon stackableCoupon = coupon("coupon-5", "可叠加券", "5.00", "40.00",
                List.of("饮料"), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), false, true, CouponStatus.AVAILABLE);
        PromotionRule giftCoupon = rule("gift-coupon", PromotionRuleType.GIFT_COUPON,
                PromotionCondition.empty(), PromotionBenefit.giftCoupon("赠券", new BigDecimal("3.00")),
                "coupon_gift", true);

        CalculationResult result = engine.calculate(order(List.of(item("drink-1", "饮料", "50.00")), stackableCoupon),
                List.of(couponRule(), giftCoupon));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-coupon-redeem-coupon-5", "cand-gift-coupon");
    }

    @Test
    void sequenceCouponCanBeRedeemedAfterPreviousCouponWasUsed() {
        Coupon firstUsed = sequenceCoupon("shake-1", 1, CouponStatus.USED);
        Coupon secondAvailable = sequenceCoupon("shake-2", 2, CouponStatus.AVAILABLE);

        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "50.00")), firstUsed, secondAvailable),
                List.of(couponRule())
        );

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-coupon-redeem-shake-2");
        assertThat(candidate(result, "cand-coupon-redeem-shake-2").explanation()).isNotBlank();
    }

    @Test
    void sequenceCouponBlocksWhenPreviousCouponWasNotUsed() {
        Coupon secondAvailable = sequenceCoupon("shake-2", 2, CouponStatus.AVAILABLE);

        CalculationResult result = engine.calculate(
                order(List.of(item("drink-1", "饮料", "50.00")), secondAvailable),
                List.of(couponRule())
        );

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("需先核销第1张券"));
    }

    @Test
    void sequenceCouponFromThirdOrderRequiresEEnjoyCardPayment() {
        Coupon firstUsed = sequenceCoupon("shake-1", 1, CouponStatus.USED);
        Coupon secondUsed = sequenceCoupon("shake-2", 2, CouponStatus.USED);
        Coupon thirdAvailable = sequenceCoupon("shake-3", 3, CouponStatus.AVAILABLE);

        CalculationResult blocked = engine.calculate(
                orderWithCustomer(List.of(item("drink-1", "饮料", "50.00")),
                        new CustomerContext(true, "gold", List.of(), 7, "CASH"),
                        firstUsed, secondUsed, thirdAvailable),
                List.of(couponRule())
        );
        assertThat(blocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("仅限e享卡支付"));

        CalculationResult allowed = engine.calculate(
                orderWithCustomer(List.of(item("drink-1", "饮料", "50.00")),
                        new CustomerContext(true, "gold", List.of(), 7, "E_ENJOY_CARD"),
                        firstUsed, secondUsed, thirdAvailable),
                List.of(couponRule())
        );
        assertThat(allowed.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-coupon-redeem-shake-3");
    }

    private static List<BenefitCalculator> calculators() {
        return List.of(new CouponRedeemBenefitCalculator(), new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(), new GiftCouponBenefitCalculator());
    }

    private static PromotionRule couponRule() {
        return rule("coupon-redeem", PromotionRuleType.COUPON_REDEEM,
                PromotionCondition.empty(), PromotionBenefit.couponRedeem(), "direct_discount", false);
    }

    private static PromotionRule rule(String id, PromotionRuleType type, PromotionCondition condition,
                                      PromotionBenefit benefit, String exclusiveGroup, boolean stackable) {
        return new PromotionRule(id, id, type, 50, exclusiveGroup, stackable,
                PromotionRuleStatus.CONFIRMED, condition, benefit, "test-v1");
    }

    private static OrderContext order(List<CartItem> items, Coupon... coupons) {
        return orderWithCustomer(items, new CustomerContext(true, "gold", List.of(), 7), coupons);
    }

    private static OrderContext orderWithCustomer(List<CartItem> items, CustomerContext customer, Coupon... coupons) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                customer,
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                LocalTime.of(20, 30),
                List.of(coupons)
        );
    }

    private static CartItem item(String productCode, String category, String unitPrice) {
        return new CartItem("line-" + productCode, productCode, "barcode-" + productCode, productCode,
                1, new BigDecimal(unitPrice), category, new BigDecimal("20"));
    }

    private static Coupon coupon(String id, String name, String faceValue, String minSpend) {
        return coupon(id, name, faceValue, minSpend, List.of("饮料"), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), false, false, CouponStatus.AVAILABLE);
    }

    private static Coupon coupon(String id, String name, String faceValue, String minSpend,
                                 List<String> applicableCategories, List<String> excludedCategories,
                                 List<String> applicableProductCodes, List<String> excludedProductCodes,
                                 LocalDate validFrom, LocalDate validUntil, boolean memberOnly,
                                 boolean stackable, CouponStatus status) {
        return new Coupon(id, "template-" + id, name, new BigDecimal(faceValue), new BigDecimal(minSpend),
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, memberOnly, stackable, status,
                LocalDateTime.of(2026, 7, 1, 8, 0), null, "operator");
    }

    private static Coupon sequenceCoupon(String id, int sequenceOrder, CouponStatus status) {
        return new Coupon(id, "template-shake", "微信摇一摇序列券" + sequenceOrder,
                new BigDecimal("5.00"), new BigDecimal("40.00"),
                List.of("饮料"), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                true, false, status,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                status == CouponStatus.USED ? LocalDateTime.of(2026, 7, sequenceOrder, 9, 0) : null,
                "operator", BigDecimal.ZERO, "wechat-shake-2026", sequenceOrder);
    }

    private static PromotionCandidate candidate(CalculationResult result, String candidateId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.candidateId().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }
}
