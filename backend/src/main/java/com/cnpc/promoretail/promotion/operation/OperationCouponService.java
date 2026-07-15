package com.cnpc.promoretail.promotion.operation;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.promotion.coupon.CouponResponse;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationCouponService {

    private static final String OPERATOR = "operation-campaign";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final AuditLogService auditLogService;

    public OperationCouponService(
            MemberRepository memberRepository,
            CouponRepository couponRepository,
            AuditLogService auditLogService
    ) {
        this.memberRepository = memberRepository;
        this.couponRepository = couponRepository;
        this.auditLogService = auditLogService == null ? AuditLogService.noop() : auditLogService;
    }

    @Transactional
    public OperationCouponIssueResponse issueRfmRecovery(RfmRecoveryRewardRequest request) {
        Member member = activeMember(request.memberCode());
        LocalDate date = businessDate(request.businessDate());
        String type = customerType(request.customerType(), member);
        List<CouponSpec> specs = "DIESEL".equals(type)
                ? List.of(fuel("rfm-diesel-20", "RFM 20 yuan diesel coupon", "fuel_diesel",
                        "20.00", "200.00", 60, 3), store12("rfm-store-12", 1))
                : List.of(fuel("rfm-gasoline-20", "RFM 20 yuan gasoline coupon", "fuel_gasoline",
                        "20.00", "200.00", 60, 1), store12("rfm-store-12", 1));
        return issue("activity-board-v2-rfm-recovery", member, rfmCycle(date), specs,
                date, request.operatorId(), request.operatorName(), "RFM recovery coupon issue");
    }

    @Transactional
    public OperationCouponIssueResponse issueBirthday(OperationRewardRequest request) {
        Member member = activeMember(request.memberCode());
        LocalDate date = businessDate(request.businessDate());
        Integer birthMonth = member.birthMonth();
        if (birthMonth == null || birthMonth != date.getMonthValue()) {
            throw new IllegalArgumentException("Birthday coupons can only be issued in member birth month: "
                    + member.memberCode());
        }
        List<CouponSpec> specs = List.of(
                fuel("birthday-gasoline-10", "Birthday 10 yuan gasoline coupon", "fuel_gasoline",
                        "10.00", "100.00", 30, 1),
                store12("birthday-store-12", 5),
                service("birthday-carwash-10", "Birthday 10 yuan car wash coupon", "car_wash",
                        "10.00", "11.00", 30, 1)
        );
        return issue("activity-board-v2-birthday", member, monthKey(date), specs,
                date, request.operatorId(), request.operatorName(), "Birthday coupon package issue");
    }

    @Transactional
    public OperationCouponIssueResponse issueSignIn(SignInRewardRequest request) {
        Member member = activeMember(request.memberCode());
        LocalDate date = businessDate(request.businessDate());
        List<CouponSpec> specs = new ArrayList<>();
        if (request.signInDays() >= 3) {
            specs.add(fuel("signin-gasoline-2", "Sign-in 2 yuan gasoline coupon", "fuel_gasoline",
                    "2.00", "100.00", 30, 1));
            specs.add(store("signin-store-2", "Sign-in 2 yuan store coupon", "2.00", "12.00", 30, 1));
        }
        if (request.signInDays() >= 7) {
            specs.add(fuel("signin-gasoline-5", "Sign-in 5 yuan gasoline coupon", "fuel_gasoline",
                    "5.00", "100.00", 30, 1));
            specs.add(store("signin-store-6", "Sign-in 6 yuan store coupon", "6.00", "12.00", 30, 1));
        }
        if (request.signInDays() >= 10) {
            specs.add(fuel("signin-gasoline-8", "Sign-in 8 yuan gasoline coupon", "fuel_gasoline",
                    "8.00", "100.00", 30, 1));
            specs.add(store12("signin-store-12", 1));
        }
        return issue("activity-board-v2-sign-in", member, monthKey(date), specs,
                date, request.operatorId(), request.operatorName(), "Sign-in reward coupon issue");
    }

    @Transactional
    public OperationCouponIssueResponse issueGroupBuy(GroupBuyRewardRequest request) {
        Member member = activeMember(request.memberCode());
        int tier = request.groupSize() >= 8 ? 8 : request.groupSize() >= 5 ? 5 : 2;
        boolean oldMember = "OLD_MEMBER".equals(normalize(request.memberRole()))
                || "OLD".equals(normalize(request.memberRole()));
        int fuelAmount = switch (tier) {
            case 8 -> oldMember ? 8 : 12;
            case 5 -> oldMember ? 5 : 7;
            default -> oldMember ? 2 : 3;
        };
        int storeQuantity = switch (tier) {
            case 8 -> 3;
            case 5 -> 2;
            default -> 1;
        };
        List<CouponSpec> specs = List.of(
                fuel("group-buy-gasoline-" + fuelAmount, "Group-buy " + fuelAmount + " yuan gasoline coupon",
                        "fuel_gasoline", String.valueOf(fuelAmount) + ".00", "100.00", 30, 1),
                store12("group-buy-store-12", storeQuantity)
        );
        return issue("activity-board-v2-group-buy", member, request.groupId() + "-tier" + tier, specs,
                businessDate(request.businessDate()), request.operatorId(), request.operatorName(),
                "Group-buy reward coupon issue");
    }

    @Transactional
    public OperationCouponIssueResponse issueIndustryCertification(QualificationRewardRequest request) {
        Member member = activeMember(request.memberCode());
        LocalDate date = businessDate(request.businessDate());
        List<CouponSpec> specs = List.of(
                fuel("industry-gasoline-10", "Industry certification 10 yuan gasoline coupon",
                        "fuel_gasoline", "10.00", "200.00", 30, 2),
                store("industry-store-6", "Industry certification 6 yuan store coupon",
                        "6.00", "30.00", 30, 1)
        );
        return issue("activity-board-v2-industry-certification", member,
                monthKey(date) + "-" + normalize(request.qualificationType()), specs,
                date, request.operatorId(), request.operatorName(), "Industry certification coupon issue");
    }

    @Transactional
    public OperationCouponIssueResponse issueEcommerce(EcommerceRewardRequest request) {
        Member member = activeMember(request.memberCode());
        LocalDate date = businessDate(request.businessDate());
        String rewardCode = normalize(request.rewardCode());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        CouponSpec spec = switch (rewardCode) {
            case "GASOLINE_10" -> fuel("ecommerce-gasoline-10", "E-commerce 10 yuan gasoline coupon",
                    "fuel_gasoline", "10.00", "200.00", 30, quantity);
            case "STORE_12" -> store12("ecommerce-store-12", quantity);
            default -> store("ecommerce-store-6", "E-commerce 6 yuan store coupon",
                    "6.00", "30.00", 30, quantity);
        };
        String eventKey = request.eventKey() == null || request.eventKey().isBlank()
                ? monthKey(date) + "-" + rewardCode
                : request.eventKey().trim();
        return issue("activity-board-v2-ecommerce", member, eventKey, List.of(spec),
                date, request.operatorId(), request.operatorName(), "E-commerce coupon issue");
    }

    private OperationCouponIssueResponse issue(
            String activityCode,
            Member member,
            String eventKey,
            List<CouponSpec> specs,
            LocalDate validFrom,
            String operatorId,
            String operatorName,
            String reason
    ) {
        if (specs.isEmpty()) {
            return new OperationCouponIssueResponse(activityCode, member.memberCode(), eventKey, List.of());
        }
        LocalDateTime issuedAt = LocalDateTime.now();
        List<CouponResponse> coupons = new ArrayList<>();
        for (CouponSpec spec : specs) {
            for (int index = 1; index <= spec.quantity(); index++) {
                String couponId = couponId(activityCode, member.memberCode(), eventKey, spec.templateId(), index);
                Coupon coupon = couponRepository.findByCouponId(couponId)
                        .orElseGet(() -> createCoupon(couponId, spec, member, validFrom, issuedAt,
                                defaultOperator(operatorId), activityCode, eventKey, operatorName, reason));
                coupons.add(CouponResponse.from(coupon));
            }
        }
        return new OperationCouponIssueResponse(activityCode, member.memberCode(), eventKey, coupons);
    }

    private Coupon createCoupon(
            String couponId,
            CouponSpec spec,
            Member member,
            LocalDate validFrom,
            LocalDateTime issuedAt,
            String operatorId,
            String activityCode,
            String eventKey,
            String operatorName,
            String reason
    ) {
        Coupon coupon = new Coupon(couponId, spec.templateId(), spec.name(), spec.faceValue(), spec.minSpend(),
                spec.categories(), spec.excludedCategories(), List.of(), List.of(),
                validFrom, validFrom.plusDays(spec.validDays() - 1L), true, false,
                CouponStatus.AVAILABLE, issuedAt, null, operatorId, BigDecimal.ZERO, "", null,
                member.memberCode());
        Coupon saved = couponRepository.save(coupon);
        auditLogService.record("COUPON_ISSUE", "COUPON", saved.couponId(), null,
                new OperationCouponIssueAudit(activityCode, eventKey, member.memberCode(),
                        saved.couponTemplateId(), saved.couponId()),
                operatorId, operatorName, reason);
        return saved;
    }

    private Member activeMember(String memberCode) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        if (!member.active()) {
            throw new IllegalArgumentException("Member is not active: " + memberCode);
        }
        return member;
    }

    private String customerType(String requestType, Member member) {
        String normalized = normalize(requestType);
        if ("DIESEL".equals(normalized) || "GASOLINE".equals(normalized)) {
            return normalized;
        }
        return member.memberTags().stream()
                .map(this::normalize)
                .anyMatch(tag -> tag.contains("DIESEL")) ? "DIESEL" : "GASOLINE";
    }

    private String couponId(String activityCode, String memberCode, String eventKey, String templateId, int index) {
        UUID uuid = UUID.nameUUIDFromBytes((activityCode + ":" + memberCode + ":" + eventKey + ":"
                + templateId + ":" + index).getBytes(StandardCharsets.UTF_8));
        return "op-" + activityCode + "-" + uuid;
    }

    private CouponSpec fuel(
            String templateId,
            String name,
            String category,
            String amount,
            String minSpend,
            int validDays,
            int quantity
    ) {
        return new CouponSpec(templateId, name, new BigDecimal(amount), new BigDecimal(minSpend),
                List.of(category), List.of(), validDays, quantity);
    }

    private CouponSpec store12(String templateId, int quantity) {
        return store(templateId, "12 yuan store coupon", "12.00", "50.00", 30, quantity);
    }

    private CouponSpec store(
            String templateId,
            String name,
            String amount,
            String minSpend,
            int validDays,
            int quantity
    ) {
        return new CouponSpec(templateId, name, new BigDecimal(amount), new BigDecimal(minSpend),
                List.of("store"), List.of("cigarette", "fertilizer"), validDays, quantity);
    }

    private CouponSpec service(
            String templateId,
            String name,
            String category,
            String amount,
            String minSpend,
            int validDays,
            int quantity
    ) {
        return new CouponSpec(templateId, name, new BigDecimal(amount), new BigDecimal(minSpend),
                List.of(category), List.of(), validDays, quantity);
    }

    private LocalDate businessDate(LocalDate businessDate) {
        return businessDate == null ? LocalDate.now() : businessDate;
    }

    private String monthKey(LocalDate date) {
        return date.format(MONTH);
    }

    private String rfmCycle(LocalDate date) {
        return monthKey(date) + (date.getDayOfMonth() <= 15 ? "-cycle-1" : "-cycle-2");
    }

    private String defaultOperator(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? OPERATOR : operatorId;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record CouponSpec(
            String templateId,
            String name,
            BigDecimal faceValue,
            BigDecimal minSpend,
            List<String> categories,
            List<String> excludedCategories,
            int validDays,
            int quantity
    ) {
    }

    private record OperationCouponIssueAudit(
            String activityCode,
            String eventKey,
            String memberCode,
            String couponTemplateId,
            String couponId
    ) {
    }
}
