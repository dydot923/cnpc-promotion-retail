package com.cnpc.promoretail.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.member.repository.InMemoryMemberRepository;
import com.cnpc.promoretail.member.repository.InMemoryMemberPointsChangeRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberServiceTest {

    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
    private final InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
    private final InMemoryMemberPointsChangeRepository pointsChangeRepository =
            new InMemoryMemberPointsChangeRepository();
    private final InMemoryAuditLogRepository auditLogRepository = new InMemoryAuditLogRepository();
    private final MemberService memberService = new MemberService(
            memberRepository, couponRepository, pointsChangeRepository, new DefaultAuditLogService(auditLogRepository));

    @Test
    void memberCanBeCreatedListedUpdatedAndChangedPoints() {
        MemberResponse created = memberService.create(new MemberCreateRequest(
                "member-new",
                "New Member",
                "13900009999",
                "silver",
                100L,
                80L,
                LocalDate.of(1998, 8, 8),
                "新疆",
                "ACTIVE"
        ));

        assertThat(created.memberCode()).isEqualTo("member-new");
        assertThat(created.levelName()).isEqualTo("银卡会员");
        assertThat(memberService.members()).extracting(MemberResponse::memberCode).contains("member-new");

        MemberResponse updated = memberService.update("member-new", new MemberUpdateRequest(
                "Updated Member",
                "13900008888",
                "gold",
                LocalDate.of(1998, 9, 9),
                "北京",
                "ACTIVE"
        ));
        assertThat(updated.memberName()).isEqualTo("Updated Member");
        assertThat(updated.level()).isEqualTo("gold");
        assertThat(updated.levelName()).isEqualTo("金卡会员");

        PointsChangeResponse added = memberService.changePoints("member-new",
                new PointsChangeRequest("ADD", 30, "checkout"));
        assertThat(added.totalPoints()).isEqualTo(130);
        assertThat(added.availablePoints()).isEqualTo(110);

        PointsChangeResponse subtracted = memberService.changePoints("member-new",
                new PointsChangeRequest("SUBTRACT", 10, "redeem"));
        assertThat(subtracted.change()).isEqualTo(-10);
        assertThat(subtracted.availablePoints()).isEqualTo(100);
        assertThat(memberService.pointsHistory("member-new", 10))
                .extracting(MemberPointsChangeResponse::pointsChange)
                .containsExactlyInAnyOrder(-10L, 30L);
    }

    @Test
    void newMemberCardOpeningIssuesCouponPackageAndAuditLogs() {
        MemberResponse created = memberService.create(new MemberCreateRequest(
                "member-coupon-package",
                "Coupon Member",
                "13900006666",
                "normal",
                null,
                null,
                null,
                "新疆",
                "EJOY-NEW-001",
                "新疆",
                null,
                null,
                "ACTIVE",
                List.of("gasoline_customer"),
                true
        ));

        MemberCouponListResponse response = memberService.coupons(created.memberCode());

        assertThat(created.eEnjoyCardNo()).isEqualTo("EJOY-NEW-001");
        assertThat(created.usualProvince()).isEqualTo("新疆");
        assertThat(created.registeredAt()).isNotNull();
        assertThat(created.cardOpenedAt()).isNotNull();
        assertThat(response.coupons())
                .extracting(MemberCouponResponse::couponTemplateId)
                .containsExactlyInAnyOrder(
                        "new-member-gasoline-10",
                        "new-member-highgrade-gasoline-15",
                        "new-member-store-12",
                        "new-member-carwash-10"
                );
        assertThat(response.coupons())
                .filteredOn(coupon -> "new-member-store-12".equals(coupon.couponTemplateId()))
                .singleElement()
                .extracting(MemberCouponResponse::excludedCategories)
                .isEqualTo(List.of("cigarette"));
        assertThat(auditLogRepository.search(new AuditLogQuery("COUPON_ISSUE", "COUPON", null, "system", 10)))
                .hasSize(4);
    }

    @Test
    void memberUpdateOpeningCardIssuesNewMemberPackageOnlyOnce() {
        memberService.create(new MemberCreateRequest(
                "member-open-later",
                "Open Later",
                "13900005555",
                "normal",
                null,
                null,
                null,
                "新疆",
                "ACTIVE",
                List.of("gasoline_customer"),
                false
        ));
        assertThat(memberService.coupons("member-open-later").coupons()).isEmpty();

        MemberUpdateRequest openCard = new MemberUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        );
        memberService.update("member-open-later", openCard);
        memberService.update("member-open-later", openCard);

        assertThat(memberService.coupons("member-open-later").coupons())
                .extracting(MemberCouponResponse::couponTemplateId)
                .containsExactlyInAnyOrder(
                        "new-member-gasoline-10",
                        "new-member-highgrade-gasoline-15",
                        "new-member-store-12",
                        "new-member-carwash-10"
                );
        assertThat(auditLogRepository.search(new AuditLogQuery("COUPON_ISSUE", "COUPON", null, "system", 20)))
                .hasSize(4);
    }

    @Test
    void activationCouponsFollowFuelPreferenceAndAreIdempotent() {
        MemberCouponListResponse first = memberService.issueActivationCoupons("member-002");
        MemberCouponListResponse second = memberService.issueActivationCoupons("member-002");

        assertThat(first.coupons())
                .extracting(MemberCouponResponse::couponTemplateId)
                .containsExactly("activation-diesel-20", "activation-diesel-20", "activation-diesel-20");
        assertThat(second.coupons())
                .extracting(MemberCouponResponse::couponTemplateId)
                .containsExactly("activation-diesel-20", "activation-diesel-20", "activation-diesel-20");
        assertThat(first.coupons())
                .allSatisfy(coupon -> {
                    assertThat(coupon.faceValue()).isEqualByComparingTo("20.00");
                    assertThat(coupon.minSpendAmount()).isEqualByComparingTo("400.00");
                });
        assertThat(couponRepository.findByHolderMemberId("member-002").stream()
                .filter(coupon -> coupon.couponTemplateId().startsWith("activation-")))
                .hasSize(3);
        assertThat(auditLogRepository.search(new AuditLogQuery("COUPON_ISSUE", "COUPON", null, "system", 10)))
                .hasSize(3);
    }

    @Test
    void duplicateMemberCodeIsRejected() {
        assertThatThrownBy(() -> memberService.create(new MemberCreateRequest(
                "member-001",
                "Duplicate",
                "13900007777",
                "gold",
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(MemberAlreadyExistsException.class);
    }

    @Test
    void memberCouponsReturnOnlyAvailableCoupons() {
        Coupon available = coupon("coupon-available", CouponStatus.AVAILABLE);
        Coupon used = coupon("coupon-used", CouponStatus.USED);
        couponRepository.save(available);
        couponRepository.save(used);

        MemberCouponListResponse response = memberService.coupons("member-001");

        assertThat(response.coupons()).extracting(MemberCouponResponse::couponId)
                .containsExactly("coupon-available");
    }

    private Coupon coupon(String couponId, CouponStatus status) {
        return new Coupon(
                couponId,
                "template-001",
                "会员券",
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                true,
                false,
                status,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                null,
                "issuer",
                BigDecimal.ZERO,
                "",
                null,
                "member-001"
        );
    }
}
