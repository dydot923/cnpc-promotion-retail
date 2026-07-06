package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
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
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleEngineInitialTest {

    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            calculators(),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void originalPriceFallbackIsAlwaysAvailable() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 2, "10.00", "零食", "20")));

        CalculationResult result = engine.calculate(context, List.of());

        assertThat(result.originalAmount()).isEqualByComparingTo("20.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("20.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.availableCandidates()).hasSize(1);
        assertThat(result.availableCandidates().getFirst().ruleType()).isEqualTo(PromotionRuleType.ORIGINAL_PRICE);
        assertThat(result.originalPriceFallback()).isNotNull();
        assertThat(result.explanations()).isNotEmpty();
    }

    @Test
    void fixedPriceUsesRealNinePointNineAreaSample() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "12.00", "零食", "20")));
        PromotionRule rule = rule("fixed-9_9", PromotionRuleType.FIXED_PRICE,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, new BigDecimal("1")),
                PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("12.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("9.90");
        assertThat(result.discountAmount()).isEqualByComparingTo("2.10");
        assertThat(result.moneySummary().discountAmount()).isEqualByComparingTo("2.10");
        assertThat(result.discountAmount().scale()).isEqualTo(2);
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-fixed-9_9");
        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-fixed-9_9");
        assertThat(result.explanations()).anySatisfy(text -> assertThat(text).contains("固定促销价"));
    }

    @Test
    void fixedPriceReturnsBlockedReasonWhenProductCodeDoesNotMatch() {
        OrderContext context = order(List.of(item("other-product", "非活动商品", 1, "12.00", "零食", "20")));
        PromotionRule rule = rule("fixed-9_9", PromotionRuleType.FIXED_PRICE,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, new BigDecimal("1")),
                PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price");
        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions()).hasSize(1);
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("商品不在活动范围内"));
    }

    @Test
    void dayNineFullStoreDiscountExcludesCigarettes() {
        OrderContext context = order(List.of(
                item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "100.00", "零食", "20"),
                item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 1, "28.00", "香烟", "24")
        ));
        PromotionRule rule = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), Set.of("gas_station"),
                        Set.of(9, 19, 29), null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("128.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("118.00");
    }

    @Test
    void ordinaryProductDayNineDiscountIsRecommendedAndKeepsMoneyScale() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "99.99", "零食", "20")));
        PromotionRule rule = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), Set.of("gas_station"),
                        Set.of(9, 19, 29), null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-day-9-discount");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-day-9-discount");
        assertThat(result.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("89.99");
        assertThat(result.payableAmount().scale()).isEqualTo(2);
        assertThat(result.discountAmount().scale()).isEqualTo(2);
    }

    @Test
    void dayNineFullStoreDiscountExcludesFertilizer() {
        OrderContext context = order(List.of(
                item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "100.00", "零食", "20"),
                item("fertilizer-001", "尿素化肥", 1, "50.00", "化肥", "10")
        ));
        PromotionRule rule = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), Set.of("gas_station"),
                        Set.of(9, 19, 29), null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("150.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("140.00");
    }

    @Test
    void cigaretteOnlyCartReturnsBlockedReasonForFullStoreDiscount() {
        OrderContext context = order(List.of(item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 1, "28.00", "香烟", "24")));
        PromotionRule rule = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(), Set.of("gas_station"),
                        Set.of(9, 19, 29), null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions()).hasSize(1);
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("排除品类"));
    }

    @Test
    void invalidDiscountRateIsBlockedAndOriginalFallbackRemainsRecommended() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "99.99", "零食", "20")));
        PromotionRule rule = rule("invalid-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                PromotionCondition.empty(),
                PromotionBenefit.percentageDiscount(new BigDecimal("1.00")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price");
        assertThat(result.blockedPromotions()).hasSize(1);
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("折扣率"));
    }

    @Test
    void gasolineAmountCanTriggerExchangePurchase() {
        OrderContext context = order(
                List.of(item("70545526", "格桑泉 蓝格天然饮用水 500ML", 4, "4.00", "包装饮料", "50")),
                new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("200.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("gasoline-water-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("70545526"), Set.of(), Set.of(FuelType.GASOLINE, FuelType.DIESEL),
                        Set.of(), Set.of(), null, null, BigDecimal.ZERO, new BigDecimal("180.00"),
                        false, new BigDecimal("1")),
                PromotionBenefit.exchangePurchase(new BigDecimal("2.00")),
                "exchange_purchase");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("16.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("8.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    void dieselAmountCanTriggerExchangePurchaseWithQuantityLimit() {
        OrderContext context = order(
                List.of(item("70545526", "格桑泉 蓝格天然饮用水 500ML", 6, "4.00", "包装饮料", "50")),
                new FuelContext(FuelType.DIESEL, "0", new BigDecimal("300.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("diesel-water-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("70545526"), Set.of(), Set.of(FuelType.DIESEL),
                        Set.of(), Set.of(), null, null, BigDecimal.ZERO, new BigDecimal("200.00"),
                        false, new BigDecimal("1")),
                PromotionBenefit.exchangePurchase(new BigDecimal("2.00"), 4),
                "exchange_purchase");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-diesel-water-exchange");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-diesel-water-exchange");
        assertThat(result.originalAmount()).isEqualByComparingTo("24.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("8.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("16.00");
        assertThat(result.discountAmount().scale()).isEqualTo(2);
    }

    @Test
    void insufficientFuelAmountBlocksExchangePurchase() {
        OrderContext context = order(
                List.of(item("70545526", "格桑泉 蓝格天然饮用水 500ML", 4, "4.00", "包装饮料", "50")),
                new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("150.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("gasoline-water-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("70545526"), Set.of(), Set.of(FuelType.GASOLINE),
                        Set.of(), Set.of(), null, null, BigDecimal.ZERO, new BigDecimal("180.00"),
                        false, new BigDecimal("1")),
                PromotionBenefit.exchangePurchase(new BigDecimal("2.00")),
                "exchange_purchase");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品消费金额"));
    }

    @Test
    void fuelTypeMismatchBlocksExchangePurchase() {
        OrderContext context = order(
                List.of(item("70545526", "格桑泉 蓝格天然饮用水 500ML", 4, "4.00", "包装饮料", "50")),
                new FuelContext(FuelType.CNG, "CNG", new BigDecimal("220.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("gasoline-water-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("70545526"), Set.of(), Set.of(FuelType.GASOLINE),
                        Set.of(), Set.of(), null, null, BigDecimal.ZERO, new BigDecimal("180.00"),
                        false, new BigDecimal("1")),
                PromotionBenefit.exchangePurchase(new BigDecimal("2.00"), 4),
                "exchange_purchase");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品类型"));
    }

    @Test
    void exchangePurchaseReturnsBlockedReasonWhenProductCodeDoesNotMatch() {
        OrderContext context = order(
                List.of(item("other-product", "非换购商品", 4, "4.00", "包装饮料", "50")),
                new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("220.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("gasoline-water-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("70545526"), Set.of(), Set.of(FuelType.GASOLINE),
                        Set.of(), Set.of(), null, null, BigDecimal.ZERO, new BigDecimal("180.00"),
                        false, new BigDecimal("1")),
                PromotionBenefit.exchangePurchase(new BigDecimal("2.00"), 4),
                "exchange_purchase");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("商品不在活动范围内"));
    }

    @Test
    void amountOffRuleCalculatesPayableAmount() {
        OrderContext context = order(List.of(item("moon-cake", "月饼礼盒", 1, "326.00", "家庭食品", "10")));
        PromotionRule rule = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("326.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("66.00")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.discountAmount()).isEqualByComparingTo("66.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("260.00");
    }

    @Test
    void amountOffBlocksWhenEligibleAmountIsBelowThreshold() {
        OrderContext context = order(List.of(item("moon-cake", "月饼礼盒", 1, "80.00", "家庭食品", "10")));
        PromotionRule rule = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.00")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions()).hasSize(1);
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("购物车金额"));
    }

    @Test
    void amountOffBlocksWhenProductScopeDoesNotMatch() {
        OrderContext context = order(List.of(item("snack", "普通零食", 1, "120.00", "零食", "10")));
        PromotionRule rule = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.00")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("商品不在活动范围内"));
    }

    @Test
    void amountOffExcludesConfiguredCategories() {
        OrderContext context = order(List.of(
                item("moon-cake", "月饼礼盒", 1, "120.00", "家庭食品", "10"),
                item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 1, "80.00", "香烟", "10")
        ));
        PromotionRule rule = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of(), Set.of("香烟"), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.00")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("200.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void amountOffKeepsTwoDecimalMoneyPrecision() {
        OrderContext context = order(List.of(item("moon-cake", "月饼礼盒", 1, "100.01", "家庭食品", "10")));
        PromotionRule rule = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.005")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.discountAmount()).isEqualByComparingTo("20.01");
        assertThat(result.payableAmount()).isEqualByComparingTo("80.00");
        assertThat(result.discountAmount().scale()).isEqualTo(2);
        assertThat(result.payableAmount().scale()).isEqualTo(2);
    }

    @Test
    void amountOffCompetesWithOtherDirectDiscountsInSameExclusiveGroup() {
        OrderContext context = order(List.of(item("moon-cake", "月饼礼盒", 1, "120.00", "家庭食品", "10")));
        PromotionRule amountOff = rule("mid-autumn-amount-off", PromotionRuleType.AMOUNT_OFF,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("30.00")),
                "direct_discount");
        PromotionRule percentage = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                PromotionCondition.empty(),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(percentage, amountOff));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-mid-autumn-amount-off");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-mid-autumn-amount-off");
        assertThat(result.payableAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void giftItemRuleReturnsGift() {
        OrderContext context = order(List.of(item("milk", "便利店商品", 1, "40.00", "便利店", "10")));
        PromotionRule rule = rule("buy-gift", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of("milk"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        new BigDecimal("40.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.giftItem("gift-milk", "自有奶", 1),
                "gift");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(candidate(result, "cand-buy-gift").gifts()).hasSize(1);
        assertThat(result.payableAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void giftCouponRuleReturnsCoupon() {
        OrderContext context = order(
                List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "30.00", "零食", "20")),
                new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("230.00"), BigDecimal.ZERO)
        );
        PromotionRule rule = rule("fuel-gift-coupon", PromotionRuleType.GIFT_COUPON,
                new PromotionCondition(Set.of(), Set.of("香烟", "化肥"), Set.of(FuelType.GASOLINE), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("230.00"), true, BigDecimal.ZERO),
                PromotionBenefit.giftCoupon("6元便利店商品券", new BigDecimal("6.00")),
                "coupon");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(candidate(result, "cand-fuel-gift-coupon").coupons()).hasSize(1);
        assertThat(candidate(result, "cand-fuel-gift-coupon").coupons().getFirst().couponName()).contains("便利店");
    }

    @Test
    void bundlePriceRuleCalculatesPackageDiscount() {
        OrderContext context = order(List.of(
                item("bundle-a", "组合商品A", 1, "15.00", "包装饮料", "10"),
                item("bundle-b", "组合商品B", 1, "15.00", "车辅", "10")
        ));
        PromotionRule rule = rule("bundle-driving-package", PromotionRuleType.BUNDLE_PRICE,
                new PromotionCondition(Set.of("bundle-a", "bundle-b"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.bundlePrice(new BigDecimal("25.00")),
                "bundle");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.originalAmount()).isEqualByComparingTo("30.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void inventoryShortageBlocksPromotion() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "10.00", "零食", "0")));
        PromotionRule rule = rule("fixed-9_9", PromotionRuleType.FIXED_PRICE,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, new BigDecimal("1")),
                PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(rule));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("库存不足"));
    }

    @Test
    void exclusiveDiscountsKeepBestCandidateOnly() {
        OrderContext context = order(List.of(item("70424725", "奥利奥 0糖夹心饼干 97g", 1, "20.00", "零食", "20")));
        PromotionRule fixed = rule("fixed-9_9", PromotionRuleType.FIXED_PRICE,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                "direct_discount");
        PromotionRule discount = rule("day-9-discount", PromotionRuleType.PERCENTAGE_DISCOUNT,
                PromotionCondition.empty(),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "direct_discount");

        CalculationResult result = engine.calculate(context, List.of(discount, fixed));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .containsExactly("original-price", "cand-fixed-9_9");
        assertThat(result.recommendedCandidateId()).isEqualTo("cand-fixed-9_9");
        assertThat(result.payableAmount()).isEqualByComparingTo("9.90");
    }

    private static List<BenefitCalculator> calculators() {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator()
        );
    }

    private static PromotionRule rule(
            String ruleId,
            PromotionRuleType type,
            PromotionCondition condition,
            PromotionBenefit benefit,
            String exclusiveGroup
    ) {
        return new PromotionRule(ruleId, ruleId, type, 50, exclusiveGroup, false,
                PromotionRuleStatus.CONFIRMED, condition, benefit, "test-v1");
    }

    private static OrderContext order(List<CartItem> items) {
        return order(items, FuelContext.empty());
    }

    private static OrderContext order(List<CartItem> items, FuelContext fuel) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                new CustomerContext(true, "gold", List.of()),
                fuel,
                items,
                LocalDate.of(2026, 7, 9),
                LocalTime.of(20, 30)
        );
    }

    private static CartItem item(String productCode, String name, int quantity, String unitPrice, String category, String inventory) {
        return new CartItem(productCode + "-line", productCode, "barcode-" + productCode, name, quantity,
                new BigDecimal(unitPrice), category, new BigDecimal(inventory));
    }

    private static PromotionCandidate candidate(CalculationResult result, String candidateId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.candidateId().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }
}
