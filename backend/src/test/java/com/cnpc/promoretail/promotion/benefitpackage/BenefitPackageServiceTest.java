package com.cnpc.promoretail.promotion.benefitpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.station.InMemoryStationRepository;
import com.cnpc.promoretail.station.model.Station;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BenefitPackageServiceTest {

    @Test
    void activePackagesCanBeListedAndLoadedWithItems() {
        Fixture fixture = fixture();

        List<BenefitPackageResponse> packages = fixture.service().packages();
        BenefitPackageResponse loaded = fixture.service().getPackage("benefit-package-test");

        assertThat(packages).hasSize(1);
        assertThat(packages.getFirst().packageCode()).isEqualTo("benefit-package-test");
        assertThat(loaded.items())
                .extracting(BenefitPackageItem::itemName)
                .containsExactly("Fuel coupon", "Car wash");
    }

    @Test
    void purchaseCreatesRecordWithEntitlementSnapshot() {
        Fixture fixture = fixture();

        BenefitPackagePurchaseResponse response = fixture.service().purchase(
                "benefit-package-test",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-001",
                        new BigDecimal("88.00"),
                        "checkout-001",
                        "operator-001",
                        "Operator"
                )
        );

        assertThat(response.purchaseId()).startsWith("benefit-purchase-");
        assertThat(response.memberCode()).isEqualTo("member-001");
        assertThat(response.packageCode()).isEqualTo("benefit-package-test");
        assertThat(response.salePrice()).isEqualByComparingTo("99.00");
        assertThat(response.paymentAmount()).isEqualByComparingTo("88.00");
        assertThat(response.status()).isEqualTo("PURCHASED");
        assertThat(response.entitlementSnapshot())
                .extracting(BenefitPackageItem::itemName)
                .containsExactly("Fuel coupon", "Car wash");
        assertThat(response.purchasedAt()).isNotNull();
        assertThat(response.activatedAt()).isNotNull();
        assertThat(fixture.service().memberPurchases("member-001"))
                .extracting(BenefitPackagePurchaseResponse::purchaseId)
                .containsExactly(response.purchaseId());
    }

    @Test
    void purchaseRejectsUnknownPackage() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().purchase(
                "missing-package",
                new BenefitPackagePurchaseRequest("member-001", null, null, null, null, null)
        )).isInstanceOf(BenefitPackageNotFoundException.class);
    }

    @Test
    void purchaseTourCardIssuesTwoGasolineCoupons() {
        InMemoryBenefitPackageRepository packageRepository = new InMemoryBenefitPackageRepository();
        InMemoryBenefitPackagePurchaseRepository purchaseRepository = new InMemoryBenefitPackagePurchaseRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        InMemoryStationRepository stationRepository = new InMemoryStationRepository();
        stationRepository.save(station("station-001", List.of("\u4e00\u5361\u901a\u9500\u552e\u7ad9\u70b9"),
                "\u53c2\u80034-\u201c\u4e00\u5361\u901a\u201d\u9500\u552e\u7ad9\u70b9\u660e\u7ec6"));
        packageRepository.save(new BenefitPackage(
                "benefit-package-xinjiang-tour-card-2026",
                "Xinjiang travel card",
                "station",
                BigDecimal.ZERO,
                "ACTIVE",
                "activity-board",
                9001,
                List.of(new BenefitPackageItem("100元汽油券（满200元使用）", new BigDecimal("2"), "赠券", 9002))
        ));
        DefaultBenefitPackageService service = new DefaultBenefitPackageService(
                packageRepository,
                purchaseRepository,
                new InMemoryMemberRepository(),
                couponRepository,
                stationRepository,
                new DefaultAuditLogService(new InMemoryAuditLogRepository())
        );

        service.purchase(
                "benefit-package-xinjiang-tour-card-2026",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-001",
                        new BigDecimal("0.00"),
                        "checkout-tour-card",
                        "operator-001",
                        "Operator"
                )
        );

        assertThat(couponRepository.findByHolderMemberId("member-001"))
                .hasSize(2)
                .allSatisfy(coupon -> {
                    assertThat(coupon.faceValue()).isEqualByComparingTo("100.00");
                    assertThat(coupon.minSpendAmount()).isEqualByComparingTo("200.00");
                    assertThat(coupon.applicableCategories()).contains("fuel_gasoline");
                });
    }

    @Test
    void purchaseTourCardRejectsStationOutsideOneCardSalesScope() {
        InMemoryBenefitPackageRepository packageRepository = new InMemoryBenefitPackageRepository();
        InMemoryBenefitPackagePurchaseRepository purchaseRepository = new InMemoryBenefitPackagePurchaseRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        InMemoryStationRepository stationRepository = new InMemoryStationRepository();
        stationRepository.save(station("station-002", List.of("fuel", "store"), "regular-station-list"));
        packageRepository.save(new BenefitPackage(
                "benefit-package-xinjiang-tour-card-2026",
                "Xinjiang travel card",
                "station",
                BigDecimal.ZERO,
                "ACTIVE",
                "activity-board",
                9001,
                List.of(new BenefitPackageItem("100\u5143\u6c7d\u6cb9\u5238\uff08\u6ee1200\u5143\u4f7f\u7528\uff09",
                        new BigDecimal("2"), "\u8d60\u5238", 9002))
        ));
        DefaultBenefitPackageService service = new DefaultBenefitPackageService(
                packageRepository,
                purchaseRepository,
                new InMemoryMemberRepository(),
                couponRepository,
                stationRepository,
                new DefaultAuditLogService(new InMemoryAuditLogRepository())
        );

        assertThatThrownBy(() -> service.purchase(
                "benefit-package-xinjiang-tour-card-2026",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-002",
                        BigDecimal.ZERO,
                        "checkout-tour-card",
                        "operator-001",
                        "Operator"
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one-card sales stations");
    }

    @Test
    void purchaseMonthlyCouponsAreActivatedInMonthlyBatches() {
        InMemoryBenefitPackageRepository packageRepository = new InMemoryBenefitPackageRepository();
        InMemoryBenefitPackagePurchaseRepository purchaseRepository = new InMemoryBenefitPackagePurchaseRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        packageRepository.save(new BenefitPackage(
                "benefit-package-monthly",
                "Monthly benefit package",
                "rpos",
                new BigDecimal("88.00"),
                "ACTIVE",
                "activity-board",
                3001,
                List.of(new BenefitPackageItem(
                        "12\u5143\u6c7d\u6cb9\u5238\uff08\u6ee1200\u5143\u4f7f\u7528\uff09",
                        new BigDecimal("4"),
                        "\u6ee1200\u5143\u53ef\u7528\uff08\u6bcf\u6708\u751f\u65482\u5f20\uff09",
                        3002
                ))
        ));
        DefaultBenefitPackageService service = new DefaultBenefitPackageService(
                packageRepository,
                purchaseRepository,
                new InMemoryMemberRepository(),
                couponRepository,
                new DefaultAuditLogService(new InMemoryAuditLogRepository())
        );

        service.purchase(
                "benefit-package-monthly",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-001",
                        null,
                        "checkout-monthly-package",
                        "operator-001",
                        "Operator"
                )
        );

        LocalDate today = LocalDate.now();
        assertThat(couponRepository.findByHolderMemberId("member-001"))
                .hasSize(4)
                .extracting(Coupon::validFrom)
                .containsExactly(today, today, today.plusMonths(1), today.plusMonths(1));
        assertThat(couponRepository.findByHolderMemberId("member-001"))
                .extracting(Coupon::sequenceOrder)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void purchaseLngCngPackageIssuesFuelCouponsFromEntitlementItems() {
        InMemoryBenefitPackageRepository packageRepository = new InMemoryBenefitPackageRepository();
        InMemoryBenefitPackagePurchaseRepository purchaseRepository = new InMemoryBenefitPackagePurchaseRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        packageRepository.save(new BenefitPackage(
                "benefit-package-010",
                "LNG package",
                "rpos",
                new BigDecimal("88.00"),
                "ACTIVE",
                "activity-board",
                110,
                List.of(
                        new BenefitPackageItem("15元LNG券（满1000元使用）", new BigDecimal("2"), "满1000元使用", 120),
                        new BenefitPackageItem("6元红牛", new BigDecimal("6"), "权益商品", 110)
                )
        ));
        DefaultBenefitPackageService service = new DefaultBenefitPackageService(
                packageRepository,
                purchaseRepository,
                new InMemoryMemberRepository(),
                couponRepository,
                new DefaultAuditLogService(new InMemoryAuditLogRepository())
        );

        service.purchase(
                "benefit-package-010",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-001",
                        null,
                        "checkout-lng-package",
                        "operator-001",
                        "Operator"
                )
        );

        assertThat(couponRepository.findByHolderMemberId("member-001"))
                .hasSize(2)
                .extracting(Coupon::couponTemplateId)
                .allMatch(templateId -> templateId.contains("benefit-package-010"));
        assertThat(couponRepository.findByHolderMemberId("member-001"))
                .allSatisfy(coupon -> assertThat(coupon.applicableCategories()).contains("LNG"));
    }

    private Fixture fixture() {
        InMemoryBenefitPackageRepository packageRepository = new InMemoryBenefitPackageRepository();
        InMemoryBenefitPackagePurchaseRepository purchaseRepository = new InMemoryBenefitPackagePurchaseRepository();
        packageRepository.save(new BenefitPackage(
                "benefit-package-test",
                "Test package",
                "cashier",
                new BigDecimal("99.00"),
                "ACTIVE",
                "activity-board",
                1,
                List.of(
                        new BenefitPackageItem("Fuel coupon", new BigDecimal("1"), "test", 1),
                        new BenefitPackageItem("Car wash", new BigDecimal("2"), "test", 2)
                )
        ));
        return new Fixture(
                new DefaultBenefitPackageService(
                        packageRepository,
                        purchaseRepository,
                        new InMemoryMemberRepository()
                )
        );
    }

    private Station station(String stationCode, List<String> salesScope, String sourceSheetName) {
        return new Station(
                stationCode,
                "hos-" + stationCode,
                "Station " + stationCode,
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
                "gas_station",
                salesScope,
                "",
                sourceSheetName,
                1,
                false
        );
    }

    private record Fixture(DefaultBenefitPackageService service) {
    }
}
