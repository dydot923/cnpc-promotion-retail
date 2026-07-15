package com.cnpc.promoretail.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import com.cnpc.promoretail.checkout.repository.InMemoryCheckoutCalculationRecordRepository;
import com.cnpc.promoretail.checkout.repository.InMemoryCheckoutConfirmationRepository;
import com.cnpc.promoretail.checkout.repository.InMemoryCheckoutTransactionRepository;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.member.repository.InMemoryMemberPointsChangeRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.promotion.points.InMemoryPointsActivityRepository;
import com.cnpc.promoretail.promotion.points.PointsActivity;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
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
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.DateCondition;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.GiftCouponTier;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import com.cnpc.promoretail.station.InMemoryStationRepository;
import com.cnpc.promoretail.station.model.Station;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CheckoutApplicationServiceTest {

    private final InMemoryPromotionRuleRepository repository = new InMemoryPromotionRuleRepository();
    private final InMemoryCheckoutCalculationRecordRepository calculationRecordRepository =
            new InMemoryCheckoutCalculationRecordRepository();
    private final InMemoryCheckoutConfirmationRepository confirmationRepository =
            new InMemoryCheckoutConfirmationRepository();
    private final InMemoryCheckoutTransactionRepository transactionRepository =
            new InMemoryCheckoutTransactionRepository();
    private final InMemoryAuditLogRepository auditLogRepository = new InMemoryAuditLogRepository();
    private final InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
    private final InMemoryMemberPointsChangeRepository pointsChangeRepository =
            new InMemoryMemberPointsChangeRepository();
    private final InMemoryPointsActivityRepository pointsActivityRepository =
            new InMemoryPointsActivityRepository();
    private final InMemoryStationRepository stationRepository = new InMemoryStationRepository();
    private final DefaultAuditLogService auditLogService = new DefaultAuditLogService(auditLogRepository);
    private final PromotionRuleGovernanceService governanceService = new PromotionRuleGovernanceService(repository);
    private final CheckoutApplicationService checkoutApplicationService =
            new CheckoutApplicationService(engine(), repository, calculationRecordRepository,
                    confirmationRepository, transactionRepository, couponRepository, memberRepository,
                    pointsChangeRepository, pointsActivityRepository, auditLogService, stationRepository);

    @Test
    void calculateFallsBackToOriginalPriceWhenNoRuleIsConfirmed() {
        governanceService.createDraft(importedFixedPriceRule(), "importer");

        CheckoutCalculateResponse result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        assertThat(result.calculationId()).startsWith("calc-");
        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.payableAmount()).isEqualByComparingTo("12.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.ruleVersionIds()).isEmpty();
        assertThat(result.availableCandidates().getFirst().ruleVersionId()).isEqualTo("original");
        assertThat(calculationRecordRepository.findAll()).hasSize(1);
        assertThat(calculationRecordRepository.findAll().getFirst().resultSnapshot().recommendedCandidateId())
                .isEqualTo("original-price");
    }

    @Test
    void calculateLoadsOnlyConfirmedRulesAndReturnsRuleVersionIds() {
        PromotionRuleDraft draft = governanceService.createDraft(importedFixedPriceRule(), "importer");
        PromotionRuleVersion version = governanceService.confirmDraft(draft.draftId(), "manager", "confirm fixed price");

        CheckoutCalculateResponse result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        assertThat(result.recommendedCandidateId()).isEqualTo("cand-import-fixed-9_9-70424725");
        assertThat(result.payableAmount()).isEqualByComparingTo("9.90");
        assertThat(result.discountAmount()).isEqualByComparingTo("2.10");
        assertThat(result.ruleVersion()).isEqualTo(version.versionId());
        assertThat(result.ruleVersionIds()).containsExactly(version.versionId());
        assertThat(result.availableCandidates()).anySatisfy(candidate -> {
            assertThat(candidate.candidateId()).isEqualTo("cand-import-fixed-9_9-70424725");
            assertThat(candidate.ruleVersionId()).isEqualTo(version.versionId());
            assertThat(candidate.status()).isEqualTo("AVAILABLE");
        });
        assertThat(calculationRecordRepository.findAll()).hasSize(1);
        assertThat(calculationRecordRepository.findAll().getFirst().ruleVersionIds()).containsExactly(version.versionId());
        assertThat(calculationRecordRepository.findAll().getFirst().requestSnapshot().cartItems().getFirst().productCode())
                .isEqualTo("70424725");
    }

    @Test
    void calculateLoadsConfirmedAmountOffRuleFromRepository() {
        PromotionRuleDraft draft = governanceService.createDraft(importedAmountOffRule(), "manager");
        PromotionRuleVersion version = governanceService.confirmDraft(draft.draftId(), "manager", "confirm amount off");

        CheckoutCalculateResponse result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                order(List.of(new CartItem("line-moon", "moon-cake", "barcode-moon",
                        "月饼礼盒", 1, new BigDecimal("120.00"), "家庭食品", new BigDecimal("20"))))));

        assertThat(result.recommendedCandidateId()).isEqualTo("cand-manual-amount-off");
        assertThat(result.payableAmount()).isEqualByComparingTo("100.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(result.ruleVersionIds()).containsExactly(version.versionId());
        assertThat(calculationRecordRepository.findAll()).hasSize(1);
    }

    @Test
    void confirmStoresSelectedCandidateSnapshotAndAuditLog() {
        PromotionRuleDraft draft = governanceService.createDraft(importedFixedPriceRule(), "importer");
        governanceService.confirmDraft(draft.draftId(), "manager", "confirm fixed price");
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        CheckoutConfirmationResponse confirmation = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-001",
                calculation.calculationId(),
                calculation.recommendedCandidateId(),
                false,
                "cashier-001",
                "Cashier A"
        ));

        assertThat(confirmation.confirmationId()).startsWith("confirm-");
        assertThat(confirmation.calculationId()).isEqualTo(calculation.calculationId());
        assertThat(confirmation.selectedCandidateId()).isEqualTo(calculation.recommendedCandidateId());
        assertThat(confirmation.selectedCandidateSnapshot().payableAmount()).isEqualByComparingTo("9.90");
        assertThat(confirmation.selectedCandidateSnapshot().discountAmount()).isEqualByComparingTo("2.10");
        assertThat(confirmation.skipped()).isFalse();
        assertThat(confirmationRepository.findByConfirmationId(confirmation.confirmationId())).isPresent();
        CheckoutTransaction transaction = transactionRepository.findByTxnNo("order-001").orElseThrow();
        assertThat(transaction.confirmationId()).isEqualTo(confirmation.confirmationId());
        assertThat(transaction.calculationId()).isEqualTo(calculation.calculationId());
        assertThat(transaction.selectedCandidateId()).isEqualTo(calculation.recommendedCandidateId());
        assertThat(transaction.totalAmount()).isEqualByComparingTo("12.00");
        assertThat(transaction.discountAmount()).isEqualByComparingTo("2.10");
        assertThat(transaction.payableAmount()).isEqualByComparingTo("9.90");
        assertThat(transaction.stationCode()).isEqualTo("station-001");
        assertThat(transaction.memberCode()).isEqualTo("member-001");
        assertThat(transaction.items()).hasSize(1);
        assertThat(transaction.items().getFirst().productCode()).isEqualTo("70424725");
        assertThat(transaction.items().getFirst().subtotal()).isEqualByComparingTo("12.00");
        assertThat(checkoutApplicationService.getTransaction("order-001").txnNo()).isEqualTo("order-001");
        assertThat(checkoutApplicationService.findRecentTransactions(10)).extracting(CheckoutTransactionResponse::txnNo)
                .contains("order-001");
        assertThat(checkoutApplicationService.findTransactions(
                new CheckoutTransactionQuery("member-001", "station-001", null, null, 10)
        )).extracting(CheckoutTransactionResponse::txnNo).containsExactly("order-001");
        assertThat(memberRepository.findByMemberCode("member-001")).hasValueSatisfying(member -> {
            assertThat(member.totalPoints()).isEqualTo(5219);
            assertThat(member.availablePoints()).isEqualTo(1219);
        });
        assertThat(pointsChangeRepository.findByMemberCode("member-001", 10))
                .extracting(change -> change.pointsChange())
                .containsExactly(19L);
        assertThat(auditLogRepository.findByEntity("MEMBER", "member-001"))
                .extracting(log -> log.actionType())
                .containsExactly("MEMBER_POINTS_ADD");
        assertThat(auditLogRepository.findByEntity("CHECKOUT_CONFIRMATION", confirmation.confirmationId()))
                .extracting(log -> log.actionType())
                .containsExactly("CHECKOUT_CONFIRM");
    }

    @Test
    void confirmUsesPromotionPointsMultiplierAsTotalMultiplier() {
        PromotionRule rule = new PromotionRule("day9-store-points", "Day9 store points",
                PromotionRuleType.PERCENTAGE_DISCOUNT, 50, "storewide-discount", false,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of("gas_station"), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90"), 3),
                "day9-store-points-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-day9-store-points", rule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-day9-points",
                calculation.calculationId(),
                "cand-day9-store-points",
                false,
                "cashier-points",
                "Cashier Points"
        ));

        assertThat(memberRepository.findByMemberCode("member-001")).hasValueSatisfying(member -> {
            assertThat(member.totalPoints()).isEqualTo(5232);
            assertThat(member.availablePoints()).isEqualTo(1232);
        });
        assertThat(pointsChangeRepository.findByMemberCode("member-001", 10))
                .first()
                .satisfies(change -> {
                    assertThat(change.pointsChange()).isEqualTo(32);
                    assertThat(change.ruleId()).isEqualTo("day9-store-points");
                    assertThat(change.reason()).contains("multiplier 3");
                });
    }

    @Test
    void confirmUsesConfiguredPointsActivityForFuelOnlyCheckout() {
        pointsActivityRepository.save(new PointsActivity(
                "points-day7-gas",
                "abv2-a1-day7-gas-points",
                "Day7 gas points",
                new BigDecimal("3.0000"),
                true,
                null,
                null,
                Set.of(7, 17, 27),
                Set.of(),
                Set.of(),
                Set.of("CNG", "LNG"),
                Set.of(),
                Set.of(),
                "ACTIVE"
        ));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                fuelOrder(FuelType.LNG, new BigDecimal("500.00"), LocalDate.of(2026, 7, 17))));

        assertThat(calculation.originalAmount()).isEqualByComparingTo("500.00");
        assertThat(calculation.pointsPreview()).isNotNull();
        assertThat(calculation.pointsPreview().activityId()).isEqualTo("points-day7-gas");
        assertThat(calculation.pointsPreview().multiplier()).isEqualByComparingTo("3.0000");
        assertThat(calculation.pointsPreview().estimatedPoints()).isEqualTo(1500);

        checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-day7-gas-points",
                calculation.calculationId(),
                "original-price",
                false,
                "cashier-fuel",
                "Cashier Fuel"
        ));

        assertThat(memberRepository.findByMemberCode("member-001")).hasValueSatisfying(member -> {
            assertThat(member.totalPoints()).isEqualTo(6700);
            assertThat(member.availablePoints()).isEqualTo(2700);
        });
        assertThat(pointsChangeRepository.findByMemberCode("member-001", 10))
                .first()
                .satisfies(change -> {
                    assertThat(change.pointsChange()).isEqualTo(1500);
                    assertThat(change.ruleId()).isEqualTo("original-price");
                    assertThat(change.stationCode()).isEqualTo("station-001");
                });
    }

    @Test
    void calculateResolvesStationMetadataFromStationCode() {
        stationRepository.save(new Station(
                "station-auto",
                "hos-auto",
                "Resolved station",
                "branch",
                "prefecture",
                "Xinjiang",
                "Urumqi",
                "Tianshan",
                "address",
                null,
                null,
                "contact",
                "phone",
                "gas_filling_station",
                List.of("fuel", "store"),
                "",
                "test",
                1,
                false
        ));

        checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                order(),
                null,
                null,
                null,
                null,
                null,
                "station-auto",
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                "member-001"
        ));

        StationContext resolved = calculationRecordRepository.findAll().getFirst().requestSnapshot().station();
        assertThat(resolved.stationId()).isEqualTo("station-auto");
        assertThat(resolved.stationType()).isEqualTo("gas_filling_station");
        assertThat(resolved.province()).isEqualTo("Xinjiang");
        assertThat(resolved.city()).isEqualTo("Urumqi");
        assertThat(resolved.district()).isEqualTo("Tianshan");
    }

    @Test
    void confirmOriginalPriceFallbackCanBeStoredAsSkippedPromotion() {
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        CheckoutConfirmationResponse confirmation = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-002",
                calculation.calculationId(),
                "original-price",
                true,
                "cashier-002",
                "Cashier B"
        ));

        assertThat(confirmation.selectedCandidateSnapshot().ruleType()).isEqualTo(PromotionRuleType.ORIGINAL_PRICE);
        assertThat(confirmation.selectedCandidateSnapshot().payableAmount()).isEqualByComparingTo("12.00");
        assertThat(confirmation.skipped()).isTrue();
    }

    @Test
    void confirmationCanBeQueriedByConfirmationId() {
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));
        CheckoutConfirmationResponse saved = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-003",
                calculation.calculationId(),
                "original-price",
                true,
                "cashier-003",
                "Cashier C"
        ));

        CheckoutConfirmationResponse loaded = checkoutApplicationService.getConfirmation(saved.confirmationId());

        assertThat(loaded.confirmationId()).isEqualTo(saved.confirmationId());
        assertThat(loaded.selectedCandidateSnapshot().candidateId()).isEqualTo("original-price");
        assertThat(loaded.cartItems()).extracting(CartItem::productCode).containsExactly("70424725");
        assertThat(loaded.operatorId()).isEqualTo("cashier-003");
    }

    @Test
    void confirmationsCanBeQueriedByCalculationIdWithCartSnapshot() {
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));
        CheckoutConfirmationResponse saved = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-003-1",
                calculation.calculationId(),
                "original-price",
                true,
                "cashier-003",
                "Cashier C"
        ));

        List<CheckoutConfirmationResponse> loaded =
                checkoutApplicationService.findConfirmationsByCalculationId(calculation.calculationId());

        assertThat(loaded).hasSize(1);
        assertThat(loaded.getFirst().confirmationId()).isEqualTo(saved.confirmationId());
        assertThat(loaded.getFirst().cartItems()).extracting(CartItem::productCode).containsExactly("70424725");
    }

    @Test
    void duplicateConfirmationForSameCalculationIsRejected() {
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));
        CheckoutConfirmRequest request = new CheckoutConfirmRequest(
                "order-004",
                calculation.calculationId(),
                "original-price",
                true,
                "cashier-004",
                "Cashier D"
        );
        checkoutApplicationService.confirm(request);

        assertThatThrownBy(() -> checkoutApplicationService.confirm(request))
                .isInstanceOf(CheckoutCalculationAlreadyConfirmedException.class)
                .hasMessageContaining(calculation.calculationId());
    }

    @Test
    void confirmCouponRedeemMarksCouponUsedAndWritesAuditLog() {
        Coupon coupon = new Coupon("coupon-001", "template-001", "便利店5元券",
                new BigDecimal("5.00"), new BigDecimal("40.00"), List.of("闆堕"), List.of(),
                List.of(), List.of(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                false, false, CouponStatus.AVAILABLE, LocalDateTime.of(2026, 7, 1, 8, 0),
                null, "issuer");
        couponRepository.save(coupon);
        PromotionRule couponRule = new PromotionRule("coupon-redeem", "券核销",
                PromotionRuleType.COUPON_REDEEM, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(), PromotionBenefit.couponRedeem(),
                "coupon-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-coupon-redeem", couponRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                orderWithCoupons(List.of(coupon))));

        CheckoutConfirmationResponse confirmation = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-coupon",
                calculation.calculationId(),
                "cand-coupon-redeem-coupon-001",
                false,
                "cashier-005",
                "Cashier E"
        ));

        assertThat(confirmation.selectedCandidateSnapshot().consumedCouponIds()).containsExactly("coupon-001");
        assertThat(couponRepository.findByCouponId("coupon-001")).isPresent()
                .get()
                .extracting(Coupon::status)
                .isEqualTo(CouponStatus.USED);
        assertThat(auditLogRepository.findByEntity("COUPON", "coupon-001"))
                .extracting(log -> log.actionType())
                .containsExactly("COUPON_REDEEM");
    }

    @Test
    void confirmCouponRedeemRejectsAlreadyRedeemedCoupon() {
        Coupon coupon = memberCoupon("coupon-race", CouponStatus.AVAILABLE, "member-001");
        couponRepository.save(coupon);
        PromotionRule couponRule = new PromotionRule("coupon-redeem-race", "Coupon redeem race",
                PromotionRuleType.COUPON_REDEEM, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(), PromotionBenefit.couponRedeem(),
                "coupon-race-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-coupon-redeem-race", couponRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                order(), null, null, null, null, true, null, null, null,
                null, null, null, List.of(), List.of("coupon-race"), "member-001"
        ));
        couponRepository.redeemIfAvailable("coupon-race", "member-001", LocalDate.of(2026, 7, 9),
                LocalDateTime.of(2026, 7, 9, 20, 40), "other-cashier");

        assertThatThrownBy(() -> checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-coupon-race",
                calculation.calculationId(),
                "cand-coupon-redeem-race-coupon-race",
                false,
                "cashier-006",
                "Cashier F"
        ))).isInstanceOf(CheckoutCouponException.class)
                .hasMessageContaining("not available");
        assertThat(confirmationRepository.findByCalculationId(calculation.calculationId())).isEmpty();
        assertThat(transactionRepository.findByTxnNo("order-coupon-race")).isEmpty();
    }

    @Test
    void confirmCouponRedeemRejectsCouponBelongingToAnotherMember() {
        Coupon coupon = memberCoupon("coupon-owner", CouponStatus.AVAILABLE, "member-001");
        couponRepository.save(coupon);
        PromotionRule couponRule = new PromotionRule("coupon-redeem-owner", "Coupon redeem owner",
                PromotionRuleType.COUPON_REDEEM, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(), PromotionBenefit.couponRedeem(),
                "coupon-owner-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-coupon-redeem-owner", couponRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                order(), null, null, null, null, true, null, null, null,
                null, null, null, List.of(), List.of("coupon-owner"), "member-001"
        ));
        couponRepository.save(memberCoupon("coupon-owner", CouponStatus.AVAILABLE, "member-002"));

        assertThatThrownBy(() -> checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-coupon-owner",
                calculation.calculationId(),
                "cand-coupon-redeem-owner-coupon-owner",
                false,
                "cashier-007",
                "Cashier G"
        ))).isInstanceOf(CheckoutCouponException.class)
                .hasMessageContaining("does not belong");
        assertThat(confirmationRepository.findByCalculationId(calculation.calculationId())).isEmpty();
        assertThat(transactionRepository.findByTxnNo("order-coupon-owner")).isEmpty();
    }

    @Test
    void confirmGiftCouponCandidateIssuesCouponsToMemberAndWritesAuditLogs() {
        PromotionRule giftRule = new PromotionRule("gift-coupon", "Gift coupon",
                PromotionRuleType.GIFT_COUPON, 70, "coupon_gift", true,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(),
                PromotionBenefit.giftCoupon("6 yuan coupon", new BigDecimal("6.00"), 2,
                        new BigDecimal("20.00"), 30),
                "gift-coupon-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-gift-coupon", giftRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));
        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        CheckoutConfirmationResponse confirmation = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "order-gift-coupon",
                calculation.calculationId(),
                "cand-gift-coupon",
                false,
                "cashier-008",
                "Cashier H"
        ));

        assertThat(confirmation.selectedCandidateSnapshot().coupons()).hasSize(1);
        assertThat(couponRepository.findAll())
                .filteredOn(couponItem -> couponItem.couponId().startsWith("gift-gift-coupon-"))
                .hasSize(2)
                .allSatisfy(couponItem -> {
                    assertThat(couponItem.holderMemberId()).isEqualTo("member-001");
                    assertThat(couponItem.status()).isEqualTo(CouponStatus.AVAILABLE);
                    assertThat(couponItem.faceValue()).isEqualByComparingTo("6.00");
                    assertThat(couponItem.minSpendAmount()).isEqualByComparingTo("20.00");
                    assertThat(couponItem.validFrom()).isEqualTo(LocalDate.of(2026, 7, 9));
                    assertThat(couponItem.validUntil()).isEqualTo(LocalDate.of(2026, 8, 8));
                });
        assertThat(auditLogRepository.search(new AuditLogQuery("COUPON_ISSUE", "COUPON", null, null, 10)))
                .hasSize(2);
    }

    @Test
    void confirmRechargeGiftCouponIssuesTemplateCouponsAndBlocksDuplicateOrder() {
        PromotionRule rechargeRule = new PromotionRule("a5-recharge-1000-gold", "A5 recharge 1000 gold",
                PromotionRuleType.GIFT_COUPON, 35, "recharge_coupon", false,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO,
                        DateCondition.monthlyDates(Set.of(10, 20, 30)), null, Set.of(), Set.of("gold"), false,
                        Set.of(), BigDecimal.ZERO, Set.of(), 0, new BigDecimal("1000.00")),
                PromotionBenefit.tieredGiftCoupons(List.of(new GiftCouponTier(new BigDecimal("1000.00"), List.of(
                        new GiftCoupon("A5 Day10 12 yuan gasoline coupon", new BigDecimal("12.00"),
                                2, new BigDecimal("200.00"), 60, "a5-day10-gasoline-12"),
                        new GiftCoupon("A5 Day10 12 yuan convenience store coupon", new BigDecimal("12.00"),
                                3, new BigDecimal("50.00"), 60, "a5-day10-store-12"),
                        new GiftCoupon("A5 Day10 10 yuan car wash coupon", new BigDecimal("10.00"),
                                3, new BigDecimal("11.00"), 30, "a5-day10-carwash-10"),
                        new GiftCoupon("A5 Day10 15 yuan high-grade gasoline coupon", new BigDecimal("15.00"),
                                1, new BigDecimal("200.00"), 60, "a5-day10-highgrade-gasoline-15")
                )))),
                "a5-recharge-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-a5-recharge-1000-gold", rechargeRule,
                "manual", "manual", 1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));

        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(rechargeRequest("1000.00"));
        assertThat(calculation.recommendedCandidateId()).isEqualTo("cand-a5-recharge-1000-gold");
        assertThat(calculation.originalAmount()).isEqualByComparingTo("1000.00");

        CheckoutConfirmationResponse confirmation = checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "recharge-order-1000",
                calculation.calculationId(),
                calculation.recommendedCandidateId(),
                false,
                "cashier-recharge",
                "Cashier Recharge"
        ));

        assertThat(confirmation.selectedCandidateSnapshot().coupons()).hasSize(4);
        assertThat(transactionRepository.findByTxnNo("recharge-order-1000")).hasValueSatisfying(transaction -> {
            assertThat(transaction.totalAmount()).isEqualByComparingTo("1000.00");
            assertThat(transaction.payableAmount()).isEqualByComparingTo("1000.00");
        });
        assertThat(couponRepository.findAll())
                .filteredOn(coupon -> coupon.couponId().startsWith("gift-a5-recharge-1000-gold-"))
                .hasSize(9)
                .extracting(Coupon::couponTemplateId)
                .contains(
                        "a5-day10-gasoline-12",
                        "a5-day10-store-12",
                        "a5-day10-carwash-10",
                        "a5-day10-highgrade-gasoline-15"
                );

        CheckoutCalculateResponse duplicatedCalculation = checkoutApplicationService.calculate(rechargeRequest("1000.00"));
        assertThatThrownBy(() -> checkoutApplicationService.confirm(new CheckoutConfirmRequest(
                "recharge-order-1000",
                duplicatedCalculation.calculationId(),
                duplicatedCalculation.recommendedCandidateId(),
                false,
                "cashier-recharge",
                "Cashier Recharge"
        ))).isInstanceOf(CheckoutTransactionAlreadyExistsException.class)
                .hasMessageContaining("recharge-order-1000");
        assertThat(couponRepository.findAll())
                .filteredOn(coupon -> coupon.couponId().startsWith("gift-a5-recharge-1000-gold-"))
                .hasSize(9);
    }

    @Test
    void calculateWithMemberCodeLoadsCouponsFromRepository() {
        Coupon coupon = new Coupon("member-coupon-001", "template-001", "会员5元券",
                new BigDecimal("5.00"), BigDecimal.ZERO, List.of("零食"), List.of(),
                List.of(), List.of(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                true, false, CouponStatus.AVAILABLE, LocalDateTime.of(2026, 7, 1, 8, 0),
                null, "issuer", BigDecimal.ZERO, "", null, "member-001");
        couponRepository.save(coupon);
        PromotionRule couponRule = new PromotionRule("coupon-redeem-member", "会员券核销",
                PromotionRuleType.COUPON_REDEEM, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(), PromotionBenefit.couponRedeem(),
                "coupon-member-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-coupon-redeem-member", couponRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));

        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                order(), null, null, null, null, true, null, null, null,
                null, null, null, List.of(), List.of("member-coupon-001"), "member-001"
        ));

        assertThat(calculation.availableCandidates())
                .extracting(CheckoutCalculateResponse.CandidateResponse::candidateId)
                .contains("cand-coupon-redeem-member-member-coupon-001");
        assertThat(calculationRecordRepository.findAll().getLast().requestSnapshot().customer().memberCode())
                .isEqualTo("member-001");
        assertThat(calculationRecordRepository.findAll().getLast().requestSnapshot().availableCoupons())
                .extracting(Coupon::couponId)
                .containsExactly("member-coupon-001");
    }

    @Test
    void selectedSequenceCouponKeepsUsedPredecessorsAndPaymentMethodInCalculationContext() {
        Coupon firstUsed = sequenceCoupon("shake-1", 1, CouponStatus.USED);
        Coupon secondUsed = sequenceCoupon("shake-2", 2, CouponStatus.USED);
        Coupon thirdAvailable = sequenceCoupon("shake-3", 3, CouponStatus.AVAILABLE);
        couponRepository.save(firstUsed);
        couponRepository.save(secondUsed);
        couponRepository.save(thirdAvailable);
        PromotionRule couponRule = new PromotionRule("coupon-redeem-sequence", "序列券核销",
                PromotionRuleType.COUPON_REDEEM, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(), PromotionBenefit.couponRedeem(),
                "coupon-sequence-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-coupon-redeem-sequence", couponRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));

        CheckoutCalculateResponse calculation = checkoutApplicationService.calculate(new CheckoutCalculateRequest(
                orderWithCoupons(List.of()),
                null, null, null, null, true, "gold", 7, "E_ENJOY_CARD",
                null, null, null, List.of(), List.of("shake-3"), "member-001"
        ));

        assertThat(calculation.availableCandidates()).extracting(CheckoutCalculateResponse.CandidateResponse::candidateId)
                .contains("cand-coupon-redeem-sequence-shake-3");
        assertThat(calculationRecordRepository.findAll().getFirst().requestSnapshot().availableCoupons())
                .extracting(Coupon::couponId)
                .containsExactly("shake-1", "shake-2", "shake-3");
        assertThat(calculationRecordRepository.findAll().getFirst().requestSnapshot().customer().paymentMethod())
                .isEqualTo("E_ENJOY_CARD");
    }

    @Test
    void calculateLoadsMemberTagsAndLevelPriorityFromMemberProfile() {
        PromotionRule taggedRule = new PromotionRule("member-tag-fixed", "汽油会员专属价",
                PromotionRuleType.FIXED_PRICE, 60, "direct_discount", false,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        null, null, Set.of(), Set.of("gold"), false, Set.of("gasoline_customer"),
                        BigDecimal.ZERO, Set.of(), 0),
                PromotionBenefit.fixedPrice(new BigDecimal("9.00")),
                "member-condition-v1");
        repository.saveDraft(new PromotionRuleDraft("draft-member-tag-fixed", taggedRule, "manual", "manual",
                1, PromotionRuleStatus.CONFIRMED, true, null, null, "tester"));

        CheckoutCalculateResponse result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        assertThat(result.availableCandidates())
                .extracting(CheckoutCalculateResponse.CandidateResponse::candidateId)
                .contains("cand-member-tag-fixed");
        CustomerContext customer = calculationRecordRepository.findAll().getFirst().requestSnapshot().customer();
        assertThat(customer.memberTags()).contains("gasoline_customer");
        assertThat(customer.memberLevelPriority()).isEqualTo(3);
    }

    private ImportedPromotionRule importedFixedPriceRule() {
        return new ImportedPromotionRule(new ImportVersion("import-v1"), "参考2-9.9元商品专区", 4,
                new PromotionRule("import-fixed-9_9-70424725", "9.9专区 奥利奥",
                        PromotionRuleType.FIXED_PRICE, 50, "direct_discount", false,
                        PromotionRuleStatus.PENDING_CONFIRMATION,
                        new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(),
                                null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ONE),
                        PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                        "import-v1"));
    }

    private ImportedPromotionRule importedAmountOffRule() {
        PromotionRule rule = new PromotionRule("manual-amount-off", "满100减20",
                PromotionRuleType.AMOUNT_OFF, 60, "direct_discount", false,
                PromotionRuleStatus.PENDING_CONFIRMATION,
                new PromotionCondition(Set.of("moon-cake"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.00")),
                "manual-import-v1");
        return new ImportedPromotionRule(new ImportVersion("manual-import-v1"), "manual", 1, rule);
    }

    private OrderContext order() {
        return order(List.of(new CartItem("line-1", "70424725", "barcode-70424725",
                "奥利奥 0糖夹心饼干 97g", 1, new BigDecimal("12.00"), "零食", new BigDecimal("20"))));
    }

    private OrderContext order(List<CartItem> items) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                new CustomerContext(true, "gold", List.of(), null, "", "member-001"),
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                LocalTime.of(20, 30)
        );
    }

    private OrderContext fuelOrder(FuelType fuelType, BigDecimal amount, LocalDate businessDate) {
        return new OrderContext(
                new StationContext("station-001", "gas_filling_station", "新疆"),
                new CustomerContext(true, "gold", List.of(), null, "", "member-001"),
                new FuelContext(fuelType, null, amount, BigDecimal.ZERO),
                List.of(),
                businessDate,
                LocalTime.of(10, 30)
        );
    }

    private CheckoutCalculateRequest rechargeRequest(String rechargeAmount) {
        return new CheckoutCalculateRequest(
                order(List.of()),
                LocalDate.of(2026, 7, 10),
                LocalTime.of(10, 30),
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "E_ENJOY_CARD",
                null,
                null,
                null,
                new BigDecimal(rechargeAmount),
                List.of(),
                List.of(),
                "member-001"
        );
    }

    private PromotionEngine engine() {
        return new DefaultPromotionEngine(
                new DefaultConditionMatcher(),
                calculators(),
                new DefaultConflictResolver(),
                new DefaultCandidateRanker(),
                new DefaultExplanationBuilder()
        );
    }

    private List<BenefitCalculator> calculators() {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator(),
                new CouponRedeemBenefitCalculator(),
                new FuelVolumeDiscountBenefitCalculator()
        );
    }

    private OrderContext orderWithCoupons(List<Coupon> coupons) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "鏂扮枂"),
                new CustomerContext(true, "gold", List.of(), 7),
                FuelContext.empty(),
                List.of(new CartItem("line-1", "70424725", "barcode-70424725",
                        "濂ュ埄濂?0绯栧す蹇冮ゼ骞?97g", 4, new BigDecimal("12.00"), "闆堕", new BigDecimal("20"))),
                LocalDate.of(2026, 7, 9),
                LocalTime.of(20, 30),
                coupons
        );
    }

    private Coupon memberCoupon(String id, CouponStatus status, String holderMemberId) {
        return new Coupon(id, "template-" + id, "Member coupon " + id,
                new BigDecimal("5.00"), BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                true, false, status,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                status == CouponStatus.USED ? LocalDateTime.of(2026, 7, 9, 20, 0) : null,
                "operator", BigDecimal.ZERO, "", null, holderMemberId);
    }

    private Coupon sequenceCoupon(String id, int sequenceOrder, CouponStatus status) {
        return new Coupon(id, "template-shake", "微信摇一摇序列券" + sequenceOrder,
                new BigDecimal("5.00"), new BigDecimal("40.00"),
                List.of(), List.of(), List.of(), List.of(),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                true, false, status,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                status == CouponStatus.USED ? LocalDateTime.of(2026, 7, sequenceOrder, 9, 0) : null,
                "operator", BigDecimal.ZERO, "wechat-shake-2026", sequenceOrder, "member-001");
    }
}
