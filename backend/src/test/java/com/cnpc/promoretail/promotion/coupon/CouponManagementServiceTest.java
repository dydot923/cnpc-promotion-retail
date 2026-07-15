package com.cnpc.promoretail.promotion.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CouponManagementServiceTest {

    @Test
    void createsTemplateIssuesCouponsAndRedeemsOnce() {
        CouponManagementService service = service();
        service.saveTemplate(null, storeCouponTemplate(10, 2));

        List<CouponResponse> issued = service.issue(new CouponIssueRequest(
                "tmpl-store-12", "member-001", 2,
                LocalDate.of(2026, 7, 1), null,
                "operator-001", "cashier", "manual test issue"));

        assertThat(issued).hasSize(2)
                .allSatisfy(coupon -> {
                    assertThat(coupon.holderMemberId()).isEqualTo("member-001");
                    assertThat(coupon.validUntil()).isEqualTo(LocalDate.of(2026, 8, 29));
                    assertThat(coupon.status()).isEqualTo(CouponStatus.AVAILABLE);
                });

        CouponResponse redeemed = service.redeem(new CouponRedeemRequest(
                issued.getFirst().couponId(), "member-001", LocalDate.of(2026, 7, 2),
                "operator-001", "cashier", "manual test redeem"));

        assertThat(redeemed.status()).isEqualTo(CouponStatus.USED);
        assertThat(service.stats("tmpl-store-12", "member-001"))
                .isEqualTo(new CouponStatsResponse(2, 1, 1, 0, 0));
        assertThatThrownBy(() -> service.redeem(new CouponRedeemRequest(
                issued.getFirst().couponId(), "member-001", LocalDate.of(2026, 7, 2),
                "operator-001", "cashier", "double redeem")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void issueChecksTemplateAndPerCustomerLimits() {
        CouponManagementService service = service();
        service.saveTemplate(null, storeCouponTemplate(3, 2));

        service.issue(new CouponIssueRequest("tmpl-store-12", "member-001", 2,
                LocalDate.of(2026, 7, 1), null, null, null, null));

        assertThatThrownBy(() -> service.issue(new CouponIssueRequest("tmpl-store-12", "member-001", 1,
                LocalDate.of(2026, 7, 1), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("per-customer");
        assertThatThrownBy(() -> service.issue(new CouponIssueRequest("tmpl-store-12", "member-002", 2,
                LocalDate.of(2026, 7, 1), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issue quantity");
    }

    private CouponManagementService service() {
        return new CouponManagementService(
                new InMemoryCouponTemplateRepository(),
                new InMemoryCouponRepository(),
                new InMemoryMemberRepository(),
                AuditLogService.noop()
        );
    }

    private CouponTemplateRequest storeCouponTemplate(int issueQuantity, int perCustomerLimit) {
        return new CouponTemplateRequest(
                "tmpl-store-12",
                "12 yuan store coupon",
                new BigDecimal("12.00"),
                new BigDecimal("50.00"),
                List.of("store"),
                List.of("cigarette", "fertilizer"),
                List.of(),
                List.of(),
                60,
                issueQuantity,
                perCustomerLimit,
                List.of("checkout"),
                true,
                false,
                BigDecimal.ZERO
        );
    }
}
