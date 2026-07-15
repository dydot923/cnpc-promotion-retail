package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.promotion.bundle.MybatisBundleDefinitionProvider;
import com.cnpc.promoretail.promotion.productgroup.ProductGroupMapping;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.GiftItem;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportedPromotionEndToEndTest extends PostgresIntegrationTestSupport {

    private static final StationContext GAS_STATION = new StationContext("station-001", "gas_station", "新疆");
    private static final LocalTime DAYTIME = LocalTime.of(10, 0);

    @Autowired
    private MybatisBundleDefinitionProvider bundleDefinitionProvider;

    @Test
    void fiveImportedNinePointNineRulesProduceFixedPriceCandidatesAndFallback() {
        List<PromotionRule> sampledRules = confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith("abv2-99-zone-"))
                .filter(rule -> !rule.condition().productCodes().isEmpty())
                .filter(rule -> productCatalogRepository.findByProductCode(
                        rule.condition().productCodes().iterator().next()).isPresent())
                .limit(5)
                .toList();

        assertThat(sampledRules).hasSize(5);
        for (PromotionRule rule : sampledRules) {
            String productCode = rule.condition().productCodes().iterator().next();
            ProductCatalogItem product = productCatalogRepository.findByProductCode(productCode).orElseThrow();
            CalculationResult result = calculate(order(
                    GAS_STATION,
                    CustomerContext.anonymous(),
                    FuelContext.empty(),
                    List.of(item(product, 1, new BigDecimal("19.90"))),
                    LocalDate.of(2026, 7, 9),
                    DAYTIME,
                    List.of()
            ));

            PromotionCandidate fixedPrice = candidate(result, rule.ruleId());
            assertThat(fixedPrice.ruleType()).isEqualTo(PromotionRuleType.FIXED_PRICE);
            assertThat(fixedPrice.payableAmount()).isEqualByComparingTo("9.90");
            assertThat(fixedPrice.explanation()).isNotBlank();
            assertThat(result.originalPriceFallback()).isNotNull();
            assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                    .contains("original-price");
        }
    }

    @Test
    void provinceCouponProducesHalfPriceCandidateForThreeImportedSkuAndBlocksNonMember() {
        Coupon coupon = couponRepository.findByCouponId("demo-province-half-001").orElseThrow();
        List<String> productCodes = coupon.applicableProductCodes().stream().limit(3).toList();
        assertThat(productCodes).hasSize(3);
        List<CartItem> items = productCodes.stream()
                .map(code -> syntheticItem(code, "province-coupon-item", 1, "20.00", "便利店商品"))
                .toList();

        CalculationResult memberResult = calculate(order(
                GAS_STATION,
                new CustomerContext(true, "GOLD", List.of(coupon.couponId())),
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                DAYTIME,
                List.of(coupon)
        ));

        PromotionCandidate couponCandidate = memberResult.availableCandidates().stream()
                .filter(candidate -> candidate.ruleType() == PromotionRuleType.COUPON_REDEEM)
                .filter(candidate -> candidate.consumedCouponIds().contains(coupon.couponId()))
                .findFirst()
                .orElseThrow();
        assertThat(coupon.discountRate()).isEqualByComparingTo("0.50");
        assertThat(couponCandidate.discountAmount()).isEqualByComparingTo("30.00");
        assertThat(couponCandidate.explanation()).contains("0.50");

        CalculationResult anonymousResult = calculate(order(
                GAS_STATION,
                CustomerContext.anonymous(),
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                DAYTIME,
                List.of(coupon)
        ));
        assertThat(anonymousResult.availableCandidates())
                .noneMatch(candidate -> candidate.consumedCouponIds().contains(coupon.couponId()));
        assertThat(blockedReasons(anonymousResult)).anyMatch(reason -> reason.contains("会员专属"));
    }

    @Test
    void exchangeBundlesProduceCandidateOrExplicitInventoryAndThresholdReasons() {
        PromotionRule drivingRule = rule("abv2-bundle-abv2-driving-package");
        PromotionRule waterRule = rule("abv2-bundle-abv2-water-drink-package");
        PromotionRule longHaulRule = rule("abv2-bundle-abv2-long-haul-package");

        CalculationResult driving = calculateBundle(drivingRule, FuelType.GASOLINE, "200.00");
        CalculationResult water = calculateBundle(waterRule, FuelType.GASOLINE, "200.00");
        assertThat(candidate(driving, drivingRule.ruleId()).payableAmount()).isEqualByComparingTo("225.00");
        assertThat(candidatesByRule(water, waterRule.ruleId())).isEmpty();
        assertThat(blockedReasons(water, waterRule.ruleId()))
                .anyMatch(reason -> reason.contains("库存不足") && reason.contains("70655834"));

        CalculationResult belowThreshold = calculateBundle(drivingRule, FuelType.GASOLINE, "199.00");
        assertThat(belowThreshold.availableCandidates())
                .noneMatch(candidate -> candidate.ruleId().equals(drivingRule.ruleId()));
        assertThat(blockedReasons(belowThreshold, drivingRule.ruleId()))
                .anyMatch(reason -> reason.contains("未满 200"));

        CalculationResult longHaul = calculateBundle(longHaulRule, FuelType.DIESEL, "500.00");
        if (candidatesByRule(longHaul, longHaulRule.ruleId()).isEmpty()) {
            assertThat(blockedReasons(longHaul, longHaulRule.ruleId()))
                    .anyMatch(reason -> reason.contains("库存不足"));
        } else {
            assertThat(candidate(longHaul, longHaulRule.ruleId()).payableAmount()).isEqualByComparingTo("525.00");
        }
    }

    @Test
    void g6ImportedRulesProduceChoiceCandidatesGiftPackAndThresholdTiers() {
        CalculationResult cigarette200 = calculate(order(
                GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                List.of(syntheticItem("70030041", "cigarette", 8, "28.00", "香烟")),
                LocalDate.of(2026, 7, 9), DAYTIME, List.of()));
        assertThat(candidatesByRule(cigarette200, "abv2-g6-cigarette-200-gift-choice")).hasSize(2)
                .allSatisfy(candidate -> assertThat(candidate.explanation()).isNotBlank());

        CalculationResult store36 = calculate(order(
                GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                List.of(syntheticItem("70356177", "red bull", 6, "6.00", "包装饮料")),
                LocalDate.of(2026, 7, 9), DAYTIME, List.of()));
        assertThat(candidatesByRule(store36, "abv2-g6-store-36-gift-choice")).hasSize(2);

        CalculationResult cottonFilm = calculate(order(
                GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                List.of(syntheticItem("demo-cotton-film", "cotton film", 9, "2000.00", "化工农资")),
                LocalDate.of(2026, 7, 9), DAYTIME, List.of()));
        assertThat(candidate(cottonFilm, "abv2-g6-cotton-film-9-gift-pack").gifts())
                .extracting(GiftItem::quantity)
                .containsExactly(2, 1, 4, 100);

        CalculationResult ilite = calculate(order(
                GAS_STATION, new CustomerContext(true, "GOLD", List.of()), FuelContext.empty(),
                List.of(syntheticItem("70690981", "ilite 250", 2, "68.00", "酒类")),
                LocalDate.of(2026, 7, 9), DAYTIME, List.of()));
        assertThat(candidate(ilite, "abv2-g6-ilite-250-fixed").payableAmount())
                .isEqualByComparingTo("116.00");
        assertThat(candidate(ilite, "abv2-g6-ilite-250-coupon").coupons())
                .singleElement().satisfies(gift -> assertThat(gift.amount()).isEqualByComparingTo("12.00"));

        CalculationResult cigarette300 = cigaretteAmount("300.00");
        assertThat(ruleIds(cigarette300)).contains("abv2-g6-cigarette-200-gift-choice")
                .doesNotContain("abv2-g6-cigarette-555-gift-ilite250", "abv2-g6-cigarette-888-gift-ilite500");
        CalculationResult cigarette600 = cigaretteAmount("600.00");
        assertThat(ruleIds(cigarette600)).contains(
                        "abv2-g6-cigarette-200-gift-choice", "abv2-g6-cigarette-555-gift-ilite250")
                .doesNotContain("abv2-g6-cigarette-888-gift-ilite500");
    }

    @Test
    void sequenceCouponsEnforceOrderAndEEnjoyCardPayment() {
        Coupon migrated = couponRepository.findByCouponId("demo-wechat-shake-003").orElseThrow();
        Coupon firstAvailable = sequenceCoupon(migrated, "sequence-test-1", 1, CouponStatus.AVAILABLE);
        Coupon secondAvailable = sequenceCoupon(migrated, "sequence-test-2", 2, CouponStatus.AVAILABLE);
        Coupon firstUsed = sequenceCoupon(migrated, "sequence-test-1", 1, CouponStatus.USED);
        Coupon secondUsed = sequenceCoupon(migrated, "sequence-test-2", 2, CouponStatus.USED);
        Coupon thirdAvailable = sequenceCoupon(migrated, "sequence-test-3", 3, CouponStatus.AVAILABLE);
        List<CartItem> cart = List.of(syntheticItem("sequence-drink", "drink", 1, "50.00", "饮料"));

        CalculationResult first = sequenceResult(cart, new CustomerContext(true, "GOLD", List.of()),
                List.of(firstAvailable));
        assertThat(first.availableCandidates()).anyMatch(candidate ->
                candidate.consumedCouponIds().contains(firstAvailable.couponId()));

        CalculationResult secondBlocked = sequenceResult(cart, new CustomerContext(true, "GOLD", List.of()),
                List.of(firstAvailable, secondAvailable));
        assertThat(blockedReasons(secondBlocked)).anyMatch(reason -> reason.contains("需先核销第1张券"));

        CalculationResult thirdWrongPayment = sequenceResult(cart,
                new CustomerContext(true, "GOLD", List.of(), null, "CASH"),
                List.of(firstUsed, secondUsed, thirdAvailable));
        assertThat(blockedReasons(thirdWrongPayment)).anyMatch(reason -> reason.contains("第3张及以后"));

        CalculationResult thirdAllowed = sequenceResult(cart,
                new CustomerContext(true, "GOLD", List.of(), null, "E_ENJOY_CARD"),
                List.of(firstUsed, secondUsed, thirdAvailable));
        assertThat(thirdAllowed.availableCandidates()).anyMatch(candidate ->
                candidate.consumedCouponIds().contains(thirdAvailable.couponId()));
    }

    @Test
    void productGroupsAndBundleItemsResolveToConcreteSku() {
        ProductGroupMapping redBull = productGroupService.findByGroupId("RED_BULL_2").orElseThrow();
        ProductGroupMapping gesang = productGroupService.findByGroupId("GESANG_3").orElseThrow();
        assertThat(redBull.groupName()).isEqualTo("红牛2款通用");
        assertThat(redBull.productCodes()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(gesang.groupName()).isEqualTo("格桑泉3款通用");
        assertThat(gesang.productCodes()).hasSizeGreaterThanOrEqualTo(3);

        assertThat(bundleDefinitionProvider.findActiveBundle("bundle-abv2-water-drink-package"))
                .hasValueSatisfying(bundle -> {
                    assertThat(bundle.items()).isNotEmpty();
                    assertThat(bundle.items()).allSatisfy(item -> {
                        assertThat(item.productCode()).isNotBlank();
                        assertThat(productCatalogRepository.findByProductCode(item.productCode())).isPresent();
                    });
                });
    }

    private CalculationResult calculateBundle(PromotionRule rule, FuelType fuelType, String amount) {
        List<CartItem> cart = new ArrayList<>();
        bundleDefinitionProvider.findActiveBundle(rule.benefit().bundleId()).orElseThrow().items()
                .forEach(bundleItem -> {
                    ProductCatalogItem product = productCatalogRepository.findByProductCode(bundleItem.productCode())
                            .orElseThrow();
                    cart.add(item(product, bundleItem.quantity(), product.unitPrice().max(new BigDecimal("10.00"))));
                });
        return calculate(order(
                GAS_STATION,
                new CustomerContext(true, "GOLD", List.of()),
                new FuelContext(fuelType, null, new BigDecimal(amount), BigDecimal.ZERO),
                cart,
                LocalDate.of(2026, 7, 9),
                DAYTIME,
                List.of()
        ));
    }

    private CalculationResult cigaretteAmount(String amount) {
        return calculate(order(
                GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                List.of(syntheticItem("70030041", "cigarette", 1, amount, "香烟")),
                LocalDate.of(2026, 7, 9), DAYTIME, List.of()));
    }

    private CalculationResult sequenceResult(
            List<CartItem> cart,
            CustomerContext customer,
            List<Coupon> coupons
    ) {
        return calculate(order(GAS_STATION, customer, FuelContext.empty(), cart,
                LocalDate.of(2026, 7, 9), DAYTIME, coupons));
    }

    private Coupon sequenceCoupon(Coupon source, String couponId, int order, CouponStatus status) {
        return new Coupon(
                couponId,
                source.couponTemplateId(),
                source.couponName(),
                source.faceValue(),
                source.minSpendAmount(),
                source.applicableCategories(),
                source.excludedCategories(),
                source.applicableProductCodes(),
                source.excludedProductCodes(),
                source.validFrom(),
                source.validUntil(),
                source.memberOnly(),
                source.stackable(),
                status,
                source.issuedAt(),
                status == CouponStatus.USED ? source.issuedAt() : null,
                "integration-test",
                source.discountRate(),
                "sequence-integration-test",
                order
        );
    }

    private PromotionRule rule(String ruleId) {
        return confirmedRules().stream()
                .filter(rule -> rule.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("confirmed rule not found: " + ruleId));
    }

    private List<PromotionCandidate> candidatesByRule(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().equals(ruleId))
                .toList();
    }

    private List<String> ruleIds(CalculationResult result) {
        return result.availableCandidates().stream().map(PromotionCandidate::ruleId).toList();
    }

    private List<String> blockedReasons(CalculationResult result) {
        return result.blockedPromotions().stream()
                .map(BlockedPromotion::reasons)
                .flatMap(List::stream)
                .map(reason -> reason.message())
                .toList();
    }

    private List<String> blockedReasons(CalculationResult result, String ruleId) {
        return result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleId().equals(ruleId))
                .map(BlockedPromotion::reasons)
                .flatMap(List::stream)
                .map(reason -> reason.message())
                .toList();
    }
}
