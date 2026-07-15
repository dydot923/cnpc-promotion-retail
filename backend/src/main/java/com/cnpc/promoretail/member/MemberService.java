package com.cnpc.promoretail.member;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberLevel;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import com.cnpc.promoretail.member.repository.MemberPointsChangeRepository;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private static final String MEMBER_LIFECYCLE_CAMPAIGN = "activity-board-v2-member-lifecycle";
    private static final String SYSTEM_OPERATOR = "system";
    private static final String XINJIANG = "\u65B0\u7586";
    private static final LifecycleCouponSpec NEW_MEMBER_GASOLINE_COUPON = new LifecycleCouponSpec(
            "new-member-gasoline-10",
            "New member 10 yuan gasoline coupon",
            new BigDecimal("10.00"),
            new BigDecimal("200.00"),
            List.of("fuel_gasoline"),
            List.of(),
            60
    );
    private static final LifecycleCouponSpec NEW_MEMBER_HIGH_GRADE_GASOLINE_COUPON = new LifecycleCouponSpec(
            "new-member-highgrade-gasoline-15",
            "New member 15 yuan high-grade gasoline coupon",
            new BigDecimal("15.00"),
            new BigDecimal("200.00"),
            List.of("fuel_high_grade_gasoline"),
            List.of(),
            60
    );
    private static final LifecycleCouponSpec NEW_MEMBER_STORE_COUPON = new LifecycleCouponSpec(
            "new-member-store-12",
            "New member 12 yuan convenience store coupon",
            new BigDecimal("12.00"),
            new BigDecimal("50.00"),
            List.of("store"),
            List.of("cigarette"),
            60
    );
    private static final LifecycleCouponSpec NEW_MEMBER_CAR_WASH_COUPON = new LifecycleCouponSpec(
            "new-member-carwash-10",
            "New member 10 yuan car wash coupon",
            new BigDecimal("10.00"),
            new BigDecimal("11.00"),
            List.of("car_wash"),
            List.of(),
            30
    );
    private static final LifecycleCouponSpec ACTIVATION_GASOLINE_COUPON = new LifecycleCouponSpec(
            "activation-gasoline-10",
            "Potential member 10 yuan gasoline coupon",
            new BigDecimal("10.00"),
            new BigDecimal("200.00"),
            List.of("fuel_gasoline"),
            List.of(),
            60
    );
    private static final LifecycleCouponSpec ACTIVATION_DIESEL_COUPON = new LifecycleCouponSpec(
            "activation-diesel-10",
            "Potential member 10 yuan diesel coupon",
            new BigDecimal("10.00"),
            new BigDecimal("200.00"),
            List.of("fuel_diesel"),
            List.of(),
            60
    );
    private static final List<LifecycleCouponSpec> NEW_MEMBER_COUPON_PACKAGE = List.of(
            NEW_MEMBER_GASOLINE_COUPON,
            NEW_MEMBER_HIGH_GRADE_GASOLINE_COUPON,
            NEW_MEMBER_STORE_COUPON,
            NEW_MEMBER_CAR_WASH_COUPON
    );

    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final MemberPointsChangeRepository memberPointsChangeRepository;
    private final AuditLogService auditLogService;

    public MemberService(
            MemberRepository memberRepository,
            CouponRepository couponRepository,
            MemberPointsChangeRepository memberPointsChangeRepository
    ) {
        this(memberRepository, couponRepository, memberPointsChangeRepository, AuditLogService.noop());
    }

    @Autowired
    public MemberService(
            MemberRepository memberRepository,
            CouponRepository couponRepository,
            MemberPointsChangeRepository memberPointsChangeRepository,
            AuditLogService auditLogService
    ) {
        this.memberRepository = memberRepository;
        this.couponRepository = couponRepository;
        this.memberPointsChangeRepository = memberPointsChangeRepository;
        this.auditLogService = auditLogService == null ? AuditLogService.noop() : auditLogService;
    }

    public MemberResponse identify(MemberIdentifyRequest request) {
        String identifier = request.identifier();
        Member member = switch (normalizeType(request.identifyType())) {
            case "PHONE" -> memberRepository.findByPhone(identifier)
                    .orElseThrow(() -> new MemberNotFoundException(identifier));
            default -> memberRepository.findByMemberCode(identifier)
                    .orElseGet(() -> memberRepository.findByPhone(identifier)
                            .orElseThrow(() -> new MemberNotFoundException(identifier)));
        };
        return MemberResponse.from(member, level(member.levelCode()));
    }

    public MemberResponse getMember(String memberCode) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        return MemberResponse.from(member, level(member.levelCode()));
    }

    public List<MemberResponse> members() {
        return memberRepository.findAll().stream()
                .sorted(Comparator.comparing(Member::memberCode))
                .map(member -> MemberResponse.from(member, level(member.levelCode())))
                .toList();
    }

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        String memberCode = request.memberCode() == null || request.memberCode().isBlank()
                ? "member-" + UUID.randomUUID().toString().substring(0, 8)
                : request.memberCode().trim();
        memberRepository.findByMemberCode(memberCode)
                .ifPresent(existing -> {
                    throw new MemberAlreadyExistsException(existing.memberCode());
                });
        if (request.phone() != null && !request.phone().isBlank()) {
            memberRepository.findByPhone(request.phone())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Member phone already exists: " + request.phone());
                    });
        }
        String levelCode = normalizeLevelCode(request.levelCode());
        ensureLevel(levelCode);
        long totalPoints = request.totalPoints() == null ? 0 : request.totalPoints();
        long availablePoints = request.availablePoints() == null ? totalPoints : request.availablePoints();
        boolean issueNewMemberCoupons = shouldIssueNewMemberCoupons(request.openedCard());
        Instant registeredAt = request.registeredAt() == null ? Instant.now() : request.registeredAt();
        Instant cardOpenedAt = request.cardOpenedAt();
        if (issueNewMemberCoupons && cardOpenedAt == null) {
            cardOpenedAt = Instant.now();
        }
        Member member = new Member(
                memberCode,
                request.memberName(),
                request.phone(),
                levelCode,
                totalPoints,
                availablePoints,
                request.birthday(),
                request.province(),
                request.eEnjoyCardNo(),
                request.usualProvince(),
                registeredAt,
                cardOpenedAt,
                request.status(),
                request.memberTags()
        );
        Member saved = memberRepository.save(member);
        if (issueNewMemberCoupons) {
            issueLifecycleCoupons(saved, NEW_MEMBER_COUPON_PACKAGE, "new-member",
                    "NEW_MEMBER_CARD_OPENED", "New member e-enjoy card opening");
        }
        return MemberResponse.from(saved, level(levelCode));
    }

    @Transactional
    public MemberResponse update(String memberCode, MemberUpdateRequest request) {
        Member existing = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        String levelCode = request.levelCode() == null || request.levelCode().isBlank()
                ? existing.levelCode()
                : normalizeLevelCode(request.levelCode());
        ensureLevel(levelCode);
        boolean issueNewMemberCoupons = Boolean.TRUE.equals(request.openedCard());
        Instant cardOpenedAt = request.cardOpenedAt();
        if (issueNewMemberCoupons && cardOpenedAt == null && existing.cardOpenedAt() == null) {
            cardOpenedAt = Instant.now();
        }
        Member updated = existing.withProfile(
                request.memberName(),
                request.phone(),
                levelCode,
                request.birthday(),
                request.province(),
                request.eEnjoyCardNo(),
                request.usualProvince(),
                request.registeredAt(),
                cardOpenedAt,
                request.status()
        );
        if (request.memberTags() != null) {
            updated = updated.withTags(request.memberTags());
        }
        Member saved = memberRepository.update(updated);
        if (issueNewMemberCoupons) {
            issueLifecycleCoupons(saved, NEW_MEMBER_COUPON_PACKAGE, "new-member",
                    "NEW_MEMBER_CARD_OPENED", "New member e-enjoy card opening");
        }
        return MemberResponse.from(saved, level(levelCode));
    }

    @Transactional
    public MemberCouponListResponse issueActivationCoupons(String memberCode) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        if (!member.active()) {
            throw new IllegalArgumentException("Member is not active: " + memberCode);
        }
        if (!isXinjiangMember(member)) {
            throw new IllegalArgumentException("Potential member activation coupons require Xinjiang province: "
                    + memberCode);
        }
        List<Coupon> coupons = issueLifecycleCoupons(member, activationCouponPackage(member), "activation",
                "POTENTIAL_MEMBER_ACTIVATION", "Potential member activation coupons");
        return new MemberCouponListResponse(member.memberCode(), coupons.stream()
                .map(MemberCouponResponse::from)
                .toList());
    }

    public PointsChangeResponse changePoints(String memberCode, PointsChangeRequest request) {
        memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        long change = switch (normalizeType(request.changeType())) {
            case "SUBTRACT", "DEDUCT", "USE" -> -request.amount();
            case "ADD", "INCREASE" -> request.amount();
            default -> throw new IllegalArgumentException("Unsupported points change type: " + request.changeType());
        };
        Member updated = memberRepository.adjustPoints(memberCode, change);
        memberPointsChangeRepository.save(new MemberPointsChange(
                "points-" + UUID.randomUUID(),
                updated.memberCode(),
                change >= 0 ? "ADD" : "SUBTRACT",
                change,
                updated.totalPoints(),
                updated.availablePoints(),
                "MANUAL",
                "",
                "",
                "",
                "system",
                "",
                request.reason(),
                Instant.now()
        ));
        return new PointsChangeResponse(updated.memberCode(), change, updated.totalPoints(),
                updated.availablePoints(), request.reason());
    }

    public List<MemberPointsChangeResponse> pointsHistory(String memberCode, int limit) {
        memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        return memberPointsChangeRepository.findByMemberCode(memberCode, limit).stream()
                .map(MemberPointsChangeResponse::from)
                .toList();
    }

    public MemberCouponListResponse coupons(String memberCode) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        List<MemberCouponResponse> coupons = couponRepository
                .findAvailableByHolderMemberId(member.memberCode(), LocalDate.now())
                .stream()
                .map(MemberCouponResponse::from)
                .toList();
        return new MemberCouponListResponse(member.memberCode(), coupons);
    }

    private MemberLevel level(String levelCode) {
        return memberRepository.findLevelByCode(levelCode).orElse(null);
    }

    private void ensureLevel(String levelCode) {
        if (memberRepository.findLevelByCode(levelCode).isEmpty()) {
            throw new IllegalArgumentException("Member level not found: " + levelCode);
        }
    }

    private String normalizeLevelCode(String levelCode) {
        if (levelCode == null || levelCode.isBlank()) {
            return "normal";
        }
        String normalized = levelCode.trim().toLowerCase();
        return "ordinary".equals(normalized) ? "normal" : normalized;
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    private boolean shouldIssueNewMemberCoupons(Boolean openedCard) {
        return openedCard == null || openedCard;
    }

    private List<Coupon> issueLifecycleCoupons(
            Member member,
            List<LifecycleCouponSpec> specs,
            String packageCode,
            String sourceEventId,
            String reason
    ) {
        if (!member.active() || specs.isEmpty()) {
            return List.of();
        }
        Set<String> templateIds = new HashSet<>(specs.stream()
                .map(LifecycleCouponSpec::templateId)
                .toList());
        List<Coupon> existing = couponRepository.findByHolderMemberId(member.memberCode()).stream()
                .filter(coupon -> templateIds.contains(coupon.couponTemplateId()))
                .toList();
        Set<String> existingTemplateIds = new HashSet<>(existing.stream()
                .map(Coupon::couponTemplateId)
                .toList());
        List<Coupon> result = new ArrayList<>(existing);
        LocalDate validFrom = LocalDate.now();
        LocalDateTime issuedAt = LocalDateTime.now();
        for (LifecycleCouponSpec spec : specs) {
            if (existingTemplateIds.contains(spec.templateId())) {
                continue;
            }
            Coupon coupon = new Coupon(
                    couponId(packageCode, member.memberCode(), spec.templateId()),
                    spec.templateId(),
                    spec.couponName(),
                    spec.faceValue(),
                    spec.minSpendAmount(),
                    spec.applicableCategories(),
                    spec.excludedCategories(),
                    List.of(),
                    List.of(),
                    validFrom,
                    validFrom.plusDays(spec.validDays() - 1L),
                    true,
                    false,
                    CouponStatus.AVAILABLE,
                    issuedAt,
                    null,
                    SYSTEM_OPERATOR,
                    BigDecimal.ZERO,
                    "",
                    null,
                    member.memberCode()
            );
            Coupon saved = couponRepository.save(coupon);
            auditCouponIssue(saved, member.memberCode(), sourceEventId, reason);
            result.add(saved);
        }
        return result.stream()
                .sorted(Comparator.comparing(Coupon::couponTemplateId).thenComparing(Coupon::couponId))
                .toList();
    }

    private void auditCouponIssue(Coupon coupon, String memberCode, String sourceEventId, String reason) {
        auditLogService.record(
                "COUPON_ISSUE",
                "COUPON",
                coupon.couponId(),
                null,
                new CouponIssueAudit(
                        sourceEventId,
                        MEMBER_LIFECYCLE_CAMPAIGN,
                        memberCode,
                        coupon.couponTemplateId(),
                        coupon.couponId(),
                        coupon.validFrom(),
                        coupon.validUntil()
                ),
                SYSTEM_OPERATOR,
                SYSTEM_OPERATOR,
                reason
        );
    }

    private String couponId(String packageCode, String memberCode, String templateId) {
        UUID uuid = UUID.nameUUIDFromBytes((packageCode + ":" + memberCode + ":" + templateId)
                .getBytes(StandardCharsets.UTF_8));
        return "coupon-" + packageCode + "-" + uuid;
    }

    private List<LifecycleCouponSpec> activationCouponPackage(Member member) {
        boolean gasolineCustomer = hasTag(member, "gasoline_customer");
        boolean dieselCustomer = hasTag(member, "diesel_customer");
        if (gasolineCustomer && dieselCustomer) {
            return List.of(ACTIVATION_GASOLINE_COUPON, ACTIVATION_DIESEL_COUPON);
        }
        if (dieselCustomer) {
            return List.of(ACTIVATION_DIESEL_COUPON);
        }
        return List.of(ACTIVATION_GASOLINE_COUPON);
    }

    private boolean hasTag(Member member, String tag) {
        return member.memberTags().stream()
                .anyMatch(value -> tag.equalsIgnoreCase(value == null ? "" : value.trim()));
    }

    private boolean isXinjiangMember(Member member) {
        String province = member.usualProvince() == null || member.usualProvince().isBlank()
                ? member.province()
                : member.usualProvince();
        return province != null && province.contains(XINJIANG);
    }

    private record LifecycleCouponSpec(
            String templateId,
            String couponName,
            BigDecimal faceValue,
            BigDecimal minSpendAmount,
            List<String> applicableCategories,
            List<String> excludedCategories,
            int validDays
    ) {
    }

    private record CouponIssueAudit(
            String sourceEventId,
            String campaignVersion,
            String memberCode,
            String couponTemplateId,
            String couponId,
            LocalDate validFrom,
            LocalDate validUntil
    ) {
    }
}
