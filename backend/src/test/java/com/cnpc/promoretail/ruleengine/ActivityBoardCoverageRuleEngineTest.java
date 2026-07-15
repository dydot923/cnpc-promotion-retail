package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.CouponRedeemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FuelVolumeDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftItemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.DateCondition;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.GiftCouponTier;
import com.cnpc.promoretail.ruleengine.model.GiftItem;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.model.TimeRangeCondition;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActivityBoardCoverageRuleEngineTest {

    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            calculators(),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void a1TieredGasCouponGiftChoosesHighestQualifiedTier() {
        PromotionRule rule = rule("a1-day7-gas-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.CNG, FuelType.LNG), Set.of(),
                        Set.of(), null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO,
                        DateCondition.monthlyDates(Set.of(7, 17, 27)), null, Set.of("新疆"), Set.of(), false,
                        BigDecimal.ZERO),
                PromotionBenefit.tieredGiftCoupons(daySevenGasCouponTiers()), "coupon_gift", true);

        CalculationResult result = engine.calculate(order(List.of(), FuelType.LNG, "1000.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 7), LocalTime.of(10, 0), true,
                "gold", "gas_cng", List.of()), List.of(rule));

        PromotionCandidate candidate = candidate(result, "cand-a1-day7-gas-coupon");
        assertThat(candidate.coupons()).extracting(GiftCoupon::couponName)
                .containsExactly("30元LNG券", "12元便利店商品券");
        assertThat(candidate.coupons().getFirst().useThreshold()).isEqualByComparingTo("1000.00");
        assertThat(candidate.coupons().getFirst().validDays()).isEqualTo(15);
        assertThat(candidate.coupons().get(1).quantity()).isEqualTo(1);
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-a1-day7-gas-coupon");
    }

    @Test
    void a1TieredGasCouponGiftBlocksBelowFirstTierAndNonMember() {
        PromotionRule rule = rule("a1-day7-gas-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.CNG, FuelType.LNG), Set.of(),
                        Set.of(), null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO,
                        DateCondition.monthlyDates(Set.of(7, 17, 27)), null, Set.of("新疆"), Set.of(), false,
                        BigDecimal.ZERO),
                PromotionBenefit.tieredGiftCoupons(daySevenGasCouponTiers()), "coupon_gift", true);

        CalculationResult belowTier = engine.calculate(order(List.of(), FuelType.CNG, "499.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 7), LocalTime.of(10, 0), true,
                "gold", "gas_cng", List.of()), List.of(rule));
        CalculationResult nonMember = engine.calculate(order(List.of(), FuelType.CNG, "500.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 7), LocalTime.of(10, 0), false,
                "guest", "gas_cng", List.of()), List.of(rule));

        assertThat(belowTier.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("赠券档位"));
        assertThat(nonMember.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("会员"));
    }

    @Test
    void e1FuelGiftCouponSupportsGasolineAndDieselThresholdsWithoutCartItems() {
        PromotionRule gasoline = rule("e1-gasoline-gift-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.GASOLINE), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("230.00"), true, BigDecimal.ZERO),
                PromotionBenefit.giftCoupon("15元香烟券", new BigDecimal("15.00"), 1,
                        new BigDecimal("300.00"), 60), "coupon_gift", true);
        PromotionRule diesel = rule("e1-diesel-gift-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.DIESEL), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("280.00"), true, BigDecimal.ZERO),
                PromotionBenefit.giftCoupon("6元便利店商品券", new BigDecimal("6.00"), 1,
                        new BigDecimal("30.00"), 60), "coupon_gift", true);

        CalculationResult gasolineHit = engine.calculate(order(List.of(), FuelType.GASOLINE, "230.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), true,
                "gold", "gas_station", List.of()), List.of(gasoline, diesel));
        CalculationResult dieselHit = engine.calculate(order(List.of(), FuelType.DIESEL, "280.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), true,
                "gold", "gas_station", List.of()), List.of(gasoline, diesel));
        CalculationResult below = engine.calculate(order(List.of(), FuelType.GASOLINE, "229.00",
                BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), true,
                "gold", "gas_station", List.of()), List.of(gasoline));

        assertThat(gasolineHit.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-e1-gasoline-gift-coupon");
        assertThat(dieselHit.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-e1-diesel-gift-coupon");
        assertThat(below.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品消费金额"));
    }

    @Test
    void e2ProductGiftCouponHonorsSpecificProductAndQuantityThreshold() {
        PromotionRule rule = rule("e2-ilite-case-gift-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of("ilite-250"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, new BigDecimal("116.00"), BigDecimal.ZERO, true, BigDecimal.ZERO,
                        null, null, Set.of(), Set.of(), false, BigDecimal.ZERO, Set.of(), 10),
                PromotionBenefit.giftCoupon("100元汽油券", new BigDecimal("100.00"), 2,
                        new BigDecimal("201.00"), 60), "coupon_gift", true);

        CalculationResult hit = engine.calculate(order(List.of(item("ilite-250", "酒类", 10, "11.60")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(rule));
        CalculationResult belowQuantity = engine.calculate(order(List.of(item("ilite-250", "酒类", 9, "11.60")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(rule));

        assertThat(candidate(hit, "cand-e2-ilite-case-gift-coupon").coupons().getFirst().quantity()).isEqualTo(2);
        assertThat(belowQuantity.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("数量"));
    }

    @Test
    void f1FuelGiftItemSupportsCngAndBlocksInsufficientFuel() {
        PromotionRule cngRule = rule("f1-cng-gift-water", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.CNG), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("50.00"), false, BigDecimal.ZERO),
                PromotionBenefit.giftItem("70545526", "格桑泉500ml矿泉水", 2), "gift", true);

        CalculationResult hit = engine.calculate(order(List.of(), FuelType.CNG, "50.00", BigDecimal.ZERO,
                LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), false, "guest",
                "gas_cng", List.of()), List.of(cngRule));
        CalculationResult below = engine.calculate(order(List.of(), FuelType.CNG, "49.00", BigDecimal.ZERO,
                LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), false, "guest",
                "gas_cng", List.of()), List.of(cngRule));

        assertThat(candidate(hit, "cand-f1-cng-gift-water").gifts().getFirst().quantity()).isEqualTo(2);
        assertThat(below.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品消费金额"));
    }

    @Test
    void g1AndG2DiscountsRespectDateStationAndExcludedCategories() {
        PromotionRule daySevenGasStation = discountRule("g1-day7-cng-discount",
                DateCondition.monthlyDates(Set.of(7, 17, 27)), Set.of("gas_cng"));
        PromotionRule dayNineFuelStation = discountRule("g2-day9-fuel-discount",
                DateCondition.monthlyDates(Set.of(9, 19, 29)), Set.of("gas_station"));

        CalculationResult gasStationHit = engine.calculate(order(List.of(item("snack", "零食", 1, "100.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 7), LocalTime.of(10, 0),
                true, "gold", "gas_cng", List.of()), List.of(daySevenGasStation));
        CalculationResult fuelStationHit = engine.calculate(order(List.of(item("snack", "零食", 1, "100.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(dayNineFuelStation));
        CalculationResult cigaretteBlocked = engine.calculate(order(List.of(item("cigarette", "香烟", 1, "100.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(dayNineFuelStation));

        assertThat(gasStationHit.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(fuelStationHit.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(cigaretteBlocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("排除品类"));
    }

    @Test
    void g4WorldCupNightDiscountUsesDateRangeTimeRangeAndIncludedCategories() {
        PromotionRule nightDiscount = rule("g4-worldcup-night-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), Set.of(), Set.of(),
                        LocalDate.of(2026, 6, 12), LocalDate.of(2026, 8, 9), BigDecimal.ZERO, BigDecimal.ZERO,
                        false, BigDecimal.ZERO, DateCondition.dateRange(LocalDate.of(2026, 6, 12),
                        LocalDate.of(2026, 8, 9)), new TimeRangeCondition(LocalTime.of(18, 0),
                        LocalTime.of(2, 0)), Set.of(), Set.of(), false, BigDecimal.ZERO,
                        Set.of("咖啡", "啤酒", "瓜子", "雪糕", "膨化", "肉脯"), 0),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.88")), "direct_discount");

        CalculationResult nightHit = engine.calculate(order(List.of(item("coffee", "咖啡", 1, "20.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 15), LocalTime.of(23, 0),
                false, "guest", "gas_station", List.of()), List.of(nightDiscount));
        CalculationResult daytimeBlocked = engine.calculate(order(List.of(item("coffee", "咖啡", 1, "20.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0),
                false, "guest", "gas_station", List.of()), List.of(nightDiscount));

        assertThat(nightHit.discountAmount()).isEqualByComparingTo("2.40");
        assertThat(daytimeBlocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("活动时段"));
    }

    @Test
    void g5MidAutumnAmountOffUsesDateRangeAndMoonCakeCategory() {
        PromotionRule rule = rule("g5-mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), new BigDecimal("326.00"),
                        BigDecimal.ZERO, false, BigDecimal.ZERO, DateCondition.dateRange(LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)), null, Set.of(), Set.of(), false, BigDecimal.ZERO,
                        Set.of("月饼礼盒"), 0),
                PromotionBenefit.amountOff(new BigDecimal("66.00")), "direct_discount");

        CalculationResult hit = engine.calculate(order(List.of(item("moon-cake", "月饼礼盒", 1, "326.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 9, 15), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(rule));
        CalculationResult outsideDate = engine.calculate(order(List.of(item("moon-cake", "月饼礼盒", 1, "326.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 8, 31), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of()), List.of(rule));

        assertThat(hit.discountAmount()).isEqualByComparingTo("66.00");
        assertThat(outsideDate.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("活动"));
    }

    @Test
    void jProvinceSpecialCouponRedeemSupportsDiscountRateAndProductScope() {
        PromotionRule couponRedeem = rule("j-province-coupon-redeem", PromotionRuleType.COUPON_REDEEM,
                PromotionCondition.empty(), PromotionBenefit.couponRedeem(), "direct_discount");
        Coupon halfPriceCoupon = coupon("province-half", "省区特色5折券", "0.00",
                "15.00", "0.50", List.of("70539251"));

        CalculationResult hit = engine.calculate(order(List.of(item("70539251", "包装饮料", 1, "20.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of(halfPriceCoupon)), List.of(couponRedeem));
        CalculationResult scopeBlocked = engine.calculate(order(List.of(item("other", "包装饮料", 1, "20.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                true, "gold", "gas_station", List.of(halfPriceCoupon)), List.of(couponRedeem));

        assertThat(candidate(hit, "cand-j-province-coupon-redeem-province-half").discountAmount())
                .isEqualByComparingTo("10.00");
        assertThat(scopeBlocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("适用范围"));
    }

    @Test
    void g6CigaretteGiftChoiceReturnsSeparateGiftCandidates() {
        PromotionRule rule = rule("abv2-g6-cigarette-200-gift-choice", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, new BigDecimal("200.00"), BigDecimal.ZERO, false, BigDecimal.ZERO,
                        null, null, Set.of(), Set.of(), false, BigDecimal.ZERO,
                        Set.of("cigarette"), 0),
                PromotionBenefit.giftItemOptions(List.of(
                        List.of(new GiftItem("70727875", "juice", 2)),
                        List.of(new GiftItem("70559364", "milk", 2))
                )), "gift", true);

        CalculationResult result = engine.calculate(order(List.of(item("70030041", "cigarette", 1, "200.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                false, "guest", "gas_station", List.of()), List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-abv2-g6-cigarette-200-gift-choice-option1",
                        "cand-abv2-g6-cigarette-200-gift-choice-option2");
        assertThat(candidate(result, "cand-abv2-g6-cigarette-200-gift-choice-option1").gifts())
                .extracting(GiftItem::productCode)
                .containsExactly("70727875");
    }

    @Test
    void g6CottonFilmGiftPackReturnsMultipleGiftsInSingleCandidate() {
        PromotionRule rule = rule("abv2-g6-cotton-film-9-gift-pack", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of("demo-cotton-film"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        null, null, Set.of(), Set.of(), false, BigDecimal.ZERO, Set.of(), 9),
                PromotionBenefit.giftItemOptions(List.of(List.of(
                        new GiftItem("70690981", "ilite250", 2),
                        new GiftItem("70559368", "milk", 1),
                        new GiftItem("70356177", "redbull", 4),
                        new GiftItem("70657932", "gloves", 100)
                ))), "gift", true);

        CalculationResult result = engine.calculate(order(List.of(item("demo-cotton-film", "agri", 9, "2000.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                false, "guest", "gas_station", List.of()), List.of(rule));

        PromotionCandidate candidate = candidate(result, "cand-abv2-g6-cotton-film-9-gift-pack-option1");
        assertThat(candidate.gifts()).hasSize(4);
        assertThat(candidate.gifts())
                .anySatisfy(gift -> {
                    assertThat(gift.productCode()).isEqualTo("70657932");
                    assertThat(gift.quantity()).isEqualTo(100);
                });
    }

    @Test
    void g6GiftChoiceKeepsAvailableOptionAndReportsBlockedShortage() {
        PromotionEngine inventoryAwareEngine = engineWithGiftInventory(productCode ->
                "70559364".equals(productCode) ? BigDecimal.ZERO : new BigDecimal("999"));
        PromotionRule rule = rule("abv2-g6-cigarette-200-gift-choice", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, new BigDecimal("200.00"), BigDecimal.ZERO, false, BigDecimal.ZERO,
                        null, null, Set.of(), Set.of(), false, BigDecimal.ZERO,
                        Set.of("cigarette"), 0),
                PromotionBenefit.giftItemOptions(List.of(
                        List.of(new GiftItem("70727875", "juice", 2)),
                        List.of(new GiftItem("70559364", "milk", 2))
                )), "gift", true);

        CalculationResult result = inventoryAwareEngine.calculate(order(List.of(item("70030041", "cigarette", 1, "200.00")),
                FuelType.NONE, "0", BigDecimal.ZERO, LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                false, "guest", "gas_station", List.of()), List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-abv2-g6-cigarette-200-gift-choice-option1")
                .doesNotContain("cand-abv2-g6-cigarette-200-gift-choice-option2");
        assertThat(result.blockedPromotions())
                .anySatisfy(blocked -> assertThat(blocked.reasons())
                        .anySatisfy(reason -> assertThat(reason.message()).contains("70559364")));
    }

    private static PromotionRule discountRule(String id, DateCondition dateCondition, Set<String> stationTypes) {
        return rule(id, PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), stationTypes, Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        dateCondition, null, Set.of(), Set.of(), false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")), "direct_discount");
    }

    private static List<GiftCouponTier> daySevenGasCouponTiers() {
        return List.of(
                new GiftCouponTier(new BigDecimal("500.00"), List.of(
                        new GiftCoupon("10元LNG券", new BigDecimal("10.00"), 1, new BigDecimal("500.00"), 15),
                        new GiftCoupon("6元便利店商品券", new BigDecimal("6.00"), 2, new BigDecimal("30.00"), 60))),
                new GiftCouponTier(new BigDecimal("1000.00"), List.of(
                        new GiftCoupon("30元LNG券", new BigDecimal("30.00"), 1, new BigDecimal("1000.00"), 15),
                        new GiftCoupon("12元便利店商品券", new BigDecimal("12.00"), 1, new BigDecimal("50.00"), 60))),
                new GiftCouponTier(new BigDecimal("1500.00"), List.of(
                        new GiftCoupon("60元LNG券", new BigDecimal("60.00"), 1, new BigDecimal("1500.00"), 15),
                        new GiftCoupon("12元便利店商品券", new BigDecimal("12.00"), 2, new BigDecimal("50.00"), 60))),
                new GiftCouponTier(new BigDecimal("2000.00"), List.of(
                        new GiftCoupon("100元LNG券", new BigDecimal("100.00"), 1, new BigDecimal("2000.00"), 15),
                        new GiftCoupon("12元便利店商品券", new BigDecimal("12.00"), 3, new BigDecimal("50.00"), 60)))
        );
    }

    private static List<BenefitCalculator> calculators() {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(),
                new GiftCouponBenefitCalculator(),
                new CouponRedeemBenefitCalculator(),
                new FuelVolumeDiscountBenefitCalculator()
        );
    }

    private static PromotionEngine engineWithGiftInventory(
            com.cnpc.promoretail.inventory.InventoryQueryService inventoryQueryService) {
        return new DefaultPromotionEngine(
                new DefaultConditionMatcher(),
                List.of(
                        new FixedPriceBenefitCalculator(),
                        new PercentageDiscountBenefitCalculator(),
                        new AmountOffBenefitCalculator(),
                        new ExchangePurchaseBenefitCalculator(),
                        new GiftItemBenefitCalculator(inventoryQueryService),
                        new GiftCouponBenefitCalculator(),
                        new CouponRedeemBenefitCalculator(),
                        new FuelVolumeDiscountBenefitCalculator()
                ),
                new DefaultConflictResolver(),
                new DefaultCandidateRanker(),
                new DefaultExplanationBuilder()
        );
    }

    private static PromotionRule rule(String id, PromotionRuleType type, PromotionCondition condition,
                                      PromotionBenefit benefit, String exclusiveGroup) {
        return rule(id, type, condition, benefit, exclusiveGroup, false);
    }

    private static PromotionRule rule(String id, PromotionRuleType type, PromotionCondition condition,
                                      PromotionBenefit benefit, String exclusiveGroup, boolean stackable) {
        return new PromotionRule(id, id, type, 90, exclusiveGroup, stackable,
                PromotionRuleStatus.CONFIRMED, condition, benefit, "activity-board-test-v1");
    }

    private static OrderContext order(List<CartItem> items, FuelType fuelType, String fuelAmount,
                                      BigDecimal fuelVolume, LocalDate date, LocalTime time,
                                      boolean member, String memberLevel, String stationType,
                                      List<Coupon> coupons) {
        return new OrderContext(
                new StationContext("station-001", stationType, "新疆"),
                new CustomerContext(member, memberLevel, List.of(), 7),
                new FuelContext(fuelType, fuelType.name(), new BigDecimal(fuelAmount), fuelVolume),
                items,
                date,
                time,
                coupons
        );
    }

    private static CartItem item(String productCode, String category, int quantity, String unitPrice) {
        return new CartItem("line-" + productCode, productCode, "barcode-" + productCode, productCode,
                quantity, new BigDecimal(unitPrice), category, new BigDecimal("999"));
    }

    private static Coupon coupon(String id, String name, String faceValue, String minSpend,
                                 String discountRate, List<String> productCodes) {
        return new Coupon(id, "template-" + id, name, new BigDecimal(faceValue), new BigDecimal(minSpend),
                List.of(), List.of("香烟", "化肥"), productCodes, List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), true, false, CouponStatus.AVAILABLE,
                LocalDateTime.of(2026, 7, 1, 10, 0), null, "operator", new BigDecimal(discountRate));
    }

    private static PromotionCandidate candidate(CalculationResult result, String candidateId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.candidateId().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }
}
