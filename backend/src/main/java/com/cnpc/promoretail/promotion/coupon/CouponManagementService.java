package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CouponManagementService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    public CouponManagementService(
            CouponTemplateRepository couponTemplateRepository,
            CouponRepository couponRepository,
            MemberRepository memberRepository,
            AuditLogService auditLogService
    ) {
        this.couponTemplateRepository = couponTemplateRepository;
        this.couponRepository = couponRepository;
        this.memberRepository = memberRepository;
        this.auditLogService = auditLogService;
    }

    public List<CouponTemplateResponse> templates() {
        return couponTemplateRepository.findAll().stream()
                .map(CouponTemplateResponse::from)
                .toList();
    }

    public CouponTemplateResponse template(String couponTemplateId) {
        return CouponTemplateResponse.from(findTemplate(couponTemplateId));
    }

    public CouponTemplateResponse saveTemplate(String couponTemplateId, CouponTemplateRequest request) {
        CouponTemplate before = couponTemplateId == null || couponTemplateId.isBlank()
                ? null
                : couponTemplateRepository.findByTemplateId(couponTemplateId).orElse(null);
        CouponTemplate saved = couponTemplateRepository.save(request.toTemplate(couponTemplateId));
        auditLogService.record(
                before == null ? "COUPON_TEMPLATE_CREATE" : "COUPON_TEMPLATE_UPDATE",
                "COUPON_TEMPLATE",
                saved.couponTemplateId(),
                before,
                saved,
                "coupon-management",
                "coupon-management",
                before == null ? "Create coupon template" : "Update coupon template"
        );
        return CouponTemplateResponse.from(saved);
    }

    public List<CouponResponse> issue(CouponIssueRequest request) {
        CouponTemplate template = findTemplate(request.couponTemplateId());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        validateHolderMember(request.holderMemberId());
        ensureIssueLimits(template, request.holderMemberId(), quantity);
        LocalDate validFrom = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        LocalDate validUntil = validUntil(template, validFrom, request.validUntil());
        LocalDateTime issuedAt = LocalDateTime.now();

        return java.util.stream.IntStream.range(0, quantity)
                .mapToObj(index -> issueOne(template, request, validFrom, validUntil, issuedAt))
                .map(CouponResponse::from)
                .toList();
    }

    public CouponResponse redeem(CouponRedeemRequest request) {
        Coupon before = couponRepository.findByCouponId(request.couponId())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + request.couponId()));
        LocalDate businessDate = request.businessDate() == null ? LocalDate.now() : request.businessDate();
        Coupon redeemed = couponRepository.redeemIfAvailable(
                        request.couponId(),
                        request.holderMemberId(),
                        businessDate,
                        LocalDateTime.now(),
                        defaultOperator(request.operatorId()))
                .orElseThrow(() -> new IllegalArgumentException("Coupon is not available for redeem: "
                        + request.couponId()));
        auditLogService.record(
                "COUPON_REDEEM",
                "COUPON",
                redeemed.couponId(),
                before,
                redeemed,
                defaultOperator(request.operatorId()),
                request.operatorName(),
                defaultReason(request.reason(), "Manual coupon redeem")
        );
        return CouponResponse.from(redeemed);
    }

    public CouponResponse coupon(String couponId) {
        return CouponResponse.from(couponRepository.findByCouponId(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId)));
    }

    public CouponStatsResponse stats(String couponTemplateId, String holderMemberId) {
        List<Coupon> coupons = couponRepository.findAll().stream()
                .filter(coupon -> couponTemplateId == null || couponTemplateId.isBlank()
                        || couponTemplateId.equals(coupon.couponTemplateId()))
                .filter(coupon -> holderMemberId == null || holderMemberId.isBlank()
                        || holderMemberId.equals(coupon.holderMemberId()))
                .toList();
        return new CouponStatsResponse(
                coupons.size(),
                count(coupons, CouponStatus.AVAILABLE),
                count(coupons, CouponStatus.USED),
                count(coupons, CouponStatus.EXPIRED),
                count(coupons, CouponStatus.DISABLED)
        );
    }

    private CouponTemplate findTemplate(String couponTemplateId) {
        if (couponTemplateId == null || couponTemplateId.isBlank()) {
            throw new IllegalArgumentException("couponTemplateId is required");
        }
        return couponTemplateRepository.findByTemplateId(couponTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon template not found: " + couponTemplateId));
    }

    private void validateHolderMember(String memberCode) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        if (!member.active()) {
            throw new IllegalArgumentException("Member is not active: " + memberCode);
        }
    }

    private void ensureIssueLimits(CouponTemplate template, String holderMemberId, int quantity) {
        if (template.issueQuantity() > 0) {
            long issuedCount = couponRepository.findAll().stream()
                    .filter(coupon -> template.couponTemplateId().equals(coupon.couponTemplateId()))
                    .count();
            if (issuedCount + quantity > template.issueQuantity()) {
                throw new IllegalArgumentException("Coupon template issue quantity exceeded: "
                        + template.couponTemplateId());
            }
        }
        if (template.perCustomerLimit() > 0) {
            long memberIssuedCount = couponRepository.findByHolderMemberId(holderMemberId).stream()
                    .filter(coupon -> template.couponTemplateId().equals(coupon.couponTemplateId()))
                    .count();
            if (memberIssuedCount + quantity > template.perCustomerLimit()) {
                throw new IllegalArgumentException("Coupon per-customer limit exceeded: "
                        + template.couponTemplateId());
            }
        }
    }

    private Coupon issueOne(
            CouponTemplate template,
            CouponIssueRequest request,
            LocalDate validFrom,
            LocalDate validUntil,
            LocalDateTime issuedAt
    ) {
        Coupon coupon = new Coupon(
                "c-" + UUID.randomUUID(),
                template.couponTemplateId(),
                template.couponName(),
                template.faceValue(),
                template.minSpendAmount(),
                template.applicableCategories(),
                template.excludedCategories(),
                template.applicableProductCodes(),
                template.excludedProductCodes(),
                validFrom,
                validUntil,
                template.memberOnly(),
                template.stackable(),
                CouponStatus.AVAILABLE,
                issuedAt,
                null,
                defaultOperator(request.operatorId()),
                template.discountRate(),
                "",
                null,
                request.holderMemberId()
        );
        Coupon saved = couponRepository.save(coupon);
        auditLogService.record(
                "COUPON_ISSUE",
                "COUPON",
                saved.couponId(),
                null,
                saved,
                defaultOperator(request.operatorId()),
                request.operatorName(),
                defaultReason(request.reason(), "Manual coupon issue")
        );
        return saved;
    }

    private LocalDate validUntil(CouponTemplate template, LocalDate validFrom, LocalDate requestValidUntil) {
        LocalDate validUntil = requestValidUntil;
        if (validUntil == null && template.validDays() > 0) {
            validUntil = validFrom.plusDays(template.validDays() - 1L);
        }
        if (validUntil != null && validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("validUntil must not be before validFrom");
        }
        return validUntil;
    }

    private String defaultOperator(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? "coupon-management" : operatorId;
    }

    private String defaultReason(String reason, String defaultReason) {
        return reason == null || reason.isBlank() ? defaultReason : reason;
    }

    private int count(List<Coupon> coupons, CouponStatus status) {
        return (int) coupons.stream()
                .filter(coupon -> coupon.status() == status)
                .count();
    }
}
