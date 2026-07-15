package com.cnpc.promoretail.promotion.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import com.cnpc.promoretail.member.repository.InMemoryMemberPointsChangeRepository;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PointsPromotionServiceTest {

    @Test
    void exchangeDiscountDeductsPointsIssuesCouponAndWritesLedger() {
        Fixture fixture = fixture();

        PointsExchangeResponse response = fixture.service().exchangeDiscount(
                "member-001",
                new PointsExchangeRequest(100L, LocalDate.of(2026, 7, 9),
                        "station-001", "operator-001", "Operator")
        );

        Coupon coupon = fixture.couponRepository().findByCouponId(response.coupon().couponId()).orElseThrow();
        assertThat(response.availablePointsAfter()).isEqualTo(1100);
        assertThat(coupon.couponTemplateId()).isEqualTo("points-exchange-90-off");
        assertThat(coupon.discountRate()).isEqualByComparingTo("0.90");
        assertThat(coupon.holderMemberId()).isEqualTo("member-001");
        assertThat(fixture.pointsChangeRepository().findByMemberCode("member-001", 10))
                .extracting(MemberPointsChange::sourceType)
                .containsExactly("POINTS_EXCHANGE");
        assertThat(fixture.auditLogRepository()
                .search(new AuditLogQuery("POINTS_EXCHANGE", "MEMBER", "member-001", null, 10)))
                .hasSize(1);
    }

    @Test
    void exchangeDiscountRejectsNonActivityDay() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().exchangeDiscount(
                "member-001",
                new PointsExchangeRequest(100L, LocalDate.of(2026, 7, 15),
                        "station-001", "operator-001", "Operator")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9, 19 and 29");
    }

    @Test
    void lotteryDrawCostsFiveHundredPointsAndStoresDrawRecord() {
        Fixture fixture = fixture();

        PointsLotteryDrawResponse response = fixture.service().draw(
                "member-001",
                new PointsLotteryDrawRequest(LocalDate.of(2026, 7, 9),
                        "station-001", "operator-001", "Operator")
        );

        assertThat(response.pointsCost()).isEqualTo(500);
        assertThat(response.availablePointsAfter()).isEqualTo(700);
        assertThat(fixture.drawRepository().findByMemberCode("member-001", 10))
                .extracting(PointsLotteryDraw::drawId)
                .containsExactly(response.drawId());
        assertThat(fixture.pointsChangeRepository().findByMemberCode("member-001", 10))
                .extracting(MemberPointsChange::sourceType)
                .containsExactly("POINTS_LOTTERY");
    }

    @Test
    void lotteryDrawUsesBackendPrizePoolConfigToIssueCoupon() {
        Fixture fixture = fixture();
        fixture.prizeConfigRepository().save(new PointsLotteryPrizeConfig(
                "g2-lottery-no-prize",
                PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE,
                "No prize",
                PointsLotteryPrizeConfig.PRIZE_TYPE_NO_PRIZE,
                "",
                "",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                30,
                0,
                "INACTIVE",
                null,
                null
        ));
        fixture.prizeConfigRepository().save(new PointsLotteryPrizeConfig(
                "g2-lottery-store-10",
                PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE,
                "Configured 5 yuan store coupon",
                PointsLotteryPrizeConfig.PRIZE_TYPE_COUPON,
                "configured-lottery-store-5",
                "Configured lottery 5 yuan store coupon",
                new BigDecimal("5.00"),
                new BigDecimal("30.00"),
                List.of("store"),
                List.of("cigarette"),
                15,
                100,
                "ACTIVE",
                null,
                null
        ));

        PointsLotteryDrawResponse response = fixture.service().draw(
                "member-001",
                new PointsLotteryDrawRequest(LocalDate.of(2026, 7, 9),
                        "station-001", "operator-001", "Operator")
        );

        Coupon coupon = fixture.couponRepository().findByCouponId(response.prizeCoupon().couponId()).orElseThrow();
        assertThat(response.prizeType()).isEqualTo(PointsLotteryPrizeConfig.PRIZE_TYPE_COUPON);
        assertThat(response.resultLabel()).isEqualTo("Configured 5 yuan store coupon");
        assertThat(coupon.couponTemplateId()).isEqualTo("configured-lottery-store-5");
        assertThat(coupon.faceValue()).isEqualByComparingTo("5.00");
        assertThat(coupon.validUntil()).isEqualTo(LocalDate.of(2026, 7, 23));
    }

    @Test
    void lotteryDrawRejectsInsufficientPoints() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().draw(
                "member-002",
                new PointsLotteryDrawRequest(LocalDate.of(2026, 7, 9),
                        "station-001", "operator-001", "Operator")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("insufficient");
    }

    private Fixture fixture() {
        InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
        InMemoryMemberPointsChangeRepository pointsChangeRepository = new InMemoryMemberPointsChangeRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        InMemoryPointsLotteryDrawRepository drawRepository = new InMemoryPointsLotteryDrawRepository();
        InMemoryPointsLotteryPrizeConfigRepository prizeConfigRepository = new InMemoryPointsLotteryPrizeConfigRepository();
        InMemoryAuditLogRepository auditLogRepository = new InMemoryAuditLogRepository();
        return new Fixture(
                new PointsPromotionService(
                        memberRepository,
                        pointsChangeRepository,
                        couponRepository,
                        drawRepository,
                        prizeConfigRepository,
                        new DefaultAuditLogService(auditLogRepository)
                ),
                pointsChangeRepository,
                couponRepository,
                drawRepository,
                prizeConfigRepository,
                auditLogRepository
        );
    }

    private record Fixture(
            PointsPromotionService service,
            InMemoryMemberPointsChangeRepository pointsChangeRepository,
            InMemoryCouponRepository couponRepository,
            InMemoryPointsLotteryDrawRepository drawRepository,
            InMemoryPointsLotteryPrizeConfigRepository prizeConfigRepository,
            InMemoryAuditLogRepository auditLogRepository
    ) {
    }
}
