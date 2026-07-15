package com.cnpc.promoretail.promotion.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.promotion.coupon.CouponResponse;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OperationCouponServiceTest {

    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
    private final InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
    private final OperationCouponService service =
            new OperationCouponService(memberRepository, couponRepository, AuditLogService.noop());

    @Test
    void rfmRecoveryIssuesDieselPackageIdempotentlyByHalfMonthCycle() {
        RfmRecoveryRewardRequest request =
                new RfmRecoveryRewardRequest("member-002", "DIESEL", LocalDate.of(2026, 7, 16), "tester", "Tester");

        OperationCouponIssueResponse first = service.issueRfmRecovery(request);
        OperationCouponIssueResponse second = service.issueRfmRecovery(request);

        assertThat(first.activityCode()).isEqualTo("activity-board-v2-rfm-recovery");
        assertThat(first.eventKey()).isEqualTo("2026-07-cycle-2");
        assertThat(first.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly("rfm-diesel-20", "rfm-diesel-20", "rfm-diesel-20", "rfm-store-12");
        assertThat(second.coupons()).extracting(CouponResponse::couponId)
                .containsExactlyElementsOf(first.coupons().stream().map(CouponResponse::couponId).toList());
        assertThat(couponRepository.findByHolderMemberId("member-002")).hasSize(4);
    }

    @Test
    void birthdayPackageRequiresBirthMonthAndIssuesFullPackage() {
        OperationCouponIssueResponse response = service.issueBirthday(
                new OperationRewardRequest("member-001", LocalDate.of(2026, 7, 8), null, "tester", "Tester"));

        assertThat(response.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly(
                        "birthday-gasoline-10",
                        "birthday-store-12",
                        "birthday-store-12",
                        "birthday-store-12",
                        "birthday-store-12",
                        "birthday-store-12",
                        "birthday-carwash-10"
                );
        assertThat(response.coupons()).allSatisfy(coupon -> assertThat(coupon.holderMemberId()).isEqualTo("member-001"));

        assertThatThrownBy(() -> service.issueBirthday(
                new OperationRewardRequest("member-001", LocalDate.of(2026, 8, 8), null, "tester", "Tester")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birthday coupons can only be issued");
    }

    @Test
    void signInAndGroupBuyUseActivityBoardTiers() {
        OperationCouponIssueResponse signIn = service.issueSignIn(
                new SignInRewardRequest("member-001", 10, LocalDate.of(2026, 7, 10), "tester", "Tester"));
        assertThat(signIn.eventKey()).isEqualTo("2026-07");
        assertThat(signIn.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly(
                        "signin-gasoline-2",
                        "signin-store-2",
                        "signin-gasoline-5",
                        "signin-store-6",
                        "signin-gasoline-8",
                        "signin-store-12"
                );

        OperationCouponIssueResponse groupBuy = service.issueGroupBuy(
                new GroupBuyRewardRequest("member-001", "group-202607-a", 8,
                        "NEW_MEMBER", LocalDate.of(2026, 7, 10), "tester", "Tester"));

        assertThat(groupBuy.eventKey()).isEqualTo("group-202607-a-tier8");
        assertThat(groupBuy.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly(
                        "group-buy-gasoline-12",
                        "group-buy-store-12",
                        "group-buy-store-12",
                        "group-buy-store-12"
                );
    }

    @Test
    void industryAndEcommerceOperationalRewardsAreBoundedAndIssuedToMember() {
        OperationCouponIssueResponse industry = service.issueIndustryCertification(
                new QualificationRewardRequest("member-001", "teacher", LocalDate.of(2026, 7, 5),
                        "tester", "Tester"));
        assertThat(industry.eventKey()).isEqualTo("2026-07-TEACHER");
        assertThat(industry.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly("industry-gasoline-10", "industry-gasoline-10", "industry-store-6");

        OperationCouponIssueResponse ecommerce = service.issueEcommerce(
                new EcommerceRewardRequest("member-001", "STORE_12", 2, LocalDate.of(2026, 7, 5),
                        "douyin-order-1", "tester", "Tester"));
        assertThat(ecommerce.eventKey()).isEqualTo("douyin-order-1");
        assertThat(ecommerce.coupons()).extracting(CouponResponse::couponTemplateId)
                .containsExactly("ecommerce-store-12", "ecommerce-store-12");
        assertThat(ecommerce.coupons()).extracting(CouponResponse::holderMemberId)
                .containsOnly("member-001");
    }
}
