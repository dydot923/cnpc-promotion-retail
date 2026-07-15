package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.station.StationRepository;
import com.cnpc.promoretail.station.model.Station;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultBenefitPackageService implements BenefitPackageService {

    private static final int DEFAULT_COUPON_VALID_DAYS = 365;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\u5143");
    private static final Pattern DISCOUNT_PATTERN = Pattern.compile("(\\d(?:\\.\\d)?)\\s*\\u6298");
    private static final Pattern MIN_SPEND_PATTERN = Pattern.compile("\\u6ee1\\s*(\\d+(?:\\.\\d+)?)\\s*\\u5143?");
    private static final Pattern MONTHLY_BATCH_PATTERN = Pattern.compile("\\u6bcf\\u6708\\u751f\\u6548\\s*(\\d+)\\s*\\u5f20");

    private final BenefitPackageRepository packageRepository;
    private final BenefitPackagePurchaseRepository purchaseRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final StationRepository stationRepository;
    private final AuditLogService auditLogService;

    public DefaultBenefitPackageService(
            BenefitPackageRepository packageRepository,
            BenefitPackagePurchaseRepository purchaseRepository,
            MemberRepository memberRepository
    ) {
        this(packageRepository, purchaseRepository, memberRepository, null, null, AuditLogService.noop());
    }

    public DefaultBenefitPackageService(
            BenefitPackageRepository packageRepository,
            BenefitPackagePurchaseRepository purchaseRepository,
            MemberRepository memberRepository,
            CouponRepository couponRepository,
            AuditLogService auditLogService
    ) {
        this(packageRepository, purchaseRepository, memberRepository, couponRepository, null, auditLogService);
    }

    @Autowired
    public DefaultBenefitPackageService(
            BenefitPackageRepository packageRepository,
            BenefitPackagePurchaseRepository purchaseRepository,
            MemberRepository memberRepository,
            CouponRepository couponRepository,
            StationRepository stationRepository,
            AuditLogService auditLogService
    ) {
        this.packageRepository = packageRepository;
        this.purchaseRepository = purchaseRepository;
        this.memberRepository = memberRepository;
        this.couponRepository = couponRepository;
        this.stationRepository = stationRepository;
        this.auditLogService = auditLogService == null ? AuditLogService.noop() : auditLogService;
    }

    @Override
    public List<BenefitPackageResponse> packages() {
        return packageRepository.findActive().stream()
                .sorted(Comparator.comparing(BenefitPackage::packageCode))
                .map(BenefitPackageResponse::from)
                .toList();
    }

    @Override
    public BenefitPackageResponse getPackage(String packageCode) {
        return BenefitPackageResponse.from(activePackage(packageCode));
    }

    @Override
    @Transactional
    public BenefitPackagePurchaseResponse purchase(String packageCode, BenefitPackagePurchaseRequest request) {
        Member member = memberRepository.findByMemberCode(required(request.memberCode(), "memberCode"))
                .filter(Member::active)
                .orElseThrow(() -> new MemberNotFoundException(request.memberCode()));
        BenefitPackage benefitPackage = activePackage(packageCode);
        validateTourCardSalesStation(benefitPackage, request.stationCode());
        BigDecimal paymentAmount = request.paymentAmount() == null
                ? benefitPackage.salePrice()
                : request.paymentAmount();
        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("paymentAmount must be greater than or equal to 0");
        }
        Instant now = Instant.now();
        BenefitPackagePurchase purchase = new BenefitPackagePurchase(
                "benefit-purchase-" + UUID.randomUUID(),
                member.memberCode(),
                benefitPackage.packageCode(),
                benefitPackage.packageName(),
                benefitPackage.salePrice(),
                paymentAmount,
                blankToEmpty(request.stationCode()),
                blankToEmpty(request.checkoutTransactionNo()),
                "PURCHASED",
                benefitPackage.items(),
                now,
                now,
                null,
                blankToEmpty(request.operatorId()),
                blankToEmpty(request.operatorName())
        );
        BenefitPackagePurchase saved = purchaseRepository.save(purchase);
        issueCoupons(saved);
        return BenefitPackagePurchaseResponse.from(saved);
    }

    @Override
    public List<BenefitPackagePurchaseResponse> memberPurchases(String memberCode) {
        Member member = memberRepository.findByMemberCode(required(memberCode, "memberCode"))
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        return purchaseRepository.findByMemberCode(member.memberCode()).stream()
                .map(BenefitPackagePurchaseResponse::from)
                .toList();
    }

    private BenefitPackage activePackage(String packageCode) {
        return packageRepository.findActiveByPackageCode(required(packageCode, "packageCode"))
                .filter(BenefitPackage::active)
                .orElseThrow(() -> new BenefitPackageNotFoundException(packageCode));
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void issueCoupons(BenefitPackagePurchase purchase) {
        if (couponRepository == null) {
            return;
        }
        LocalDate issuedDate = LocalDate.now();
        LocalDateTime issuedAt = LocalDateTime.now();
        for (int itemIndex = 0; itemIndex < purchase.entitlementSnapshot().size(); itemIndex++) {
            var item = purchase.entitlementSnapshot().get(itemIndex);
            Optional<EntitlementCouponSpec> optionalSpec = couponSpec(purchase, item.itemName(), item.remark(), item.sourceRowNumber());
            if (optionalSpec.isEmpty()) {
                continue;
            }
            EntitlementCouponSpec spec = optionalSpec.get();
            int quantity = couponQuantity(item.quantity());
            for (int sequence = 1; sequence <= quantity; sequence++) {
                LocalDate validFrom = validFrom(issuedDate, sequence, spec.monthlyBatchSize());
                Coupon coupon = couponRepository.save(new Coupon(
                        "coupon-" + purchase.purchaseId() + "-" + (itemIndex + 1) + "-" + sequence,
                        spec.templateId(),
                        spec.couponName(),
                        spec.faceValue(),
                        spec.minSpendAmount(),
                        spec.applicableCategories(),
                        spec.excludedCategories(),
                        List.of(),
                        List.of(),
                        validFrom,
                        validFrom.plusDays(DEFAULT_COUPON_VALID_DAYS - 1L),
                        true,
                        false,
                        CouponStatus.AVAILABLE,
                        issuedAt,
                        null,
                        purchase.operatorId().isBlank() ? "system" : purchase.operatorId(),
                        spec.discountRate(),
                        spec.monthlyBatchSize() > 0 ? purchase.purchaseId() + "-" + (itemIndex + 1) : "",
                        spec.monthlyBatchSize() > 0 ? sequence : null,
                        purchase.memberCode()
                ));
                auditLogService.record("COUPON_ISSUE", "COUPON", coupon.couponId(), null,
                        new BenefitPackageCouponIssueAudit(
                                purchase.purchaseId(),
                                purchase.packageCode(),
                                purchase.memberCode(),
                                coupon.couponTemplateId()
                        ),
                        purchase.operatorId().isBlank() ? "system" : purchase.operatorId(),
                        purchase.operatorName(),
                        "Benefit package entitlement coupon issued");
            }
        }
    }

    private Optional<EntitlementCouponSpec> couponSpec(
            BenefitPackagePurchase purchase,
            String itemName,
            String remark,
            Integer sourceRowNumber
    ) {
        String text = blankToEmpty(itemName) + " " + blankToEmpty(remark);
        if (!containsCouponWord(text)) {
            return Optional.empty();
        }
        BigDecimal faceValue = amount(text).orElse(BigDecimal.ZERO);
        BigDecimal discountRate = discountRate(text).orElse(BigDecimal.ZERO);
        if (faceValue.compareTo(BigDecimal.ZERO) <= 0 && discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        BigDecimal minSpend = minSpend(text).orElse(BigDecimal.ZERO);
        String templateId = "benefit-" + purchase.packageCode() + "-item-"
                + (sourceRowNumber == null ? Math.abs(itemName.hashCode()) : sourceRowNumber);
        return Optional.of(new EntitlementCouponSpec(
                templateId,
                itemName,
                faceValue,
                minSpend,
                applicableCategories(text),
                excludedCategories(text),
                discountRate,
                monthlyBatchSize(text)
        ));
    }

    private void validateTourCardSalesStation(BenefitPackage benefitPackage, String stationCode) {
        if (!tourCardPackage(benefitPackage)) {
            return;
        }
        String effectiveStationCode = required(stationCode, "stationCode");
        if (stationRepository == null) {
            throw new IllegalArgumentException("Tour card purchase requires configured sales station validation");
        }
        Station station = stationRepository.findByStationCode(effectiveStationCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tour card can only be sold at configured one-card sales stations: " + effectiveStationCode));
        if (!oneCardSalesStation(station)) {
            throw new IllegalArgumentException(
                    "Tour card can only be sold at configured one-card sales stations: " + effectiveStationCode);
        }
    }

    private boolean tourCardPackage(BenefitPackage benefitPackage) {
        String marker = (benefitPackage.packageCode() + " "
                + benefitPackage.packageName() + " "
                + benefitPackage.salesChannel() + " "
                + benefitPackage.sourceSheetName()).toLowerCase();
        return marker.contains("xinjiang-tour-card")
                || marker.contains("travel card")
                || marker.contains("tour-card")
                || marker.contains("one-card")
                || marker.contains("\u4e00\u5361\u901a");
    }

    private boolean oneCardSalesStation(Station station) {
        String marker = (station.sourceSheetName() + " "
                + station.remark() + " "
                + String.join(" ", station.salesScope())).toLowerCase();
        return marker.contains("\u4e00\u5361\u901a")
                || marker.contains("one-card")
                || marker.contains("tour-card")
                || marker.contains("travel card");
    }

    private boolean containsCouponWord(String value) {
        return value.contains("\u5238");
    }

    private Optional<BigDecimal> amount(String value) {
        Matcher matcher = AMOUNT_PATTERN.matcher(value);
        return matcher.find() ? Optional.of(money(matcher.group(1))) : Optional.empty();
    }

    private Optional<BigDecimal> discountRate(String value) {
        Matcher matcher = DISCOUNT_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(money(matcher.group(1)).divide(new BigDecimal("10.00"), 4, RoundingMode.HALF_UP));
    }

    private Optional<BigDecimal> minSpend(String value) {
        Matcher matcher = MIN_SPEND_PATTERN.matcher(value);
        return matcher.find() ? Optional.of(money(matcher.group(1))) : Optional.empty();
    }

    private int monthlyBatchSize(String value) {
        Matcher matcher = MONTHLY_BATCH_PATTERN.matcher(value);
        if (!matcher.find()) {
            return 0;
        }
        return Math.max(1, Integer.parseInt(matcher.group(1)));
    }

    private LocalDate validFrom(LocalDate issuedDate, int sequence, int monthlyBatchSize) {
        if (monthlyBatchSize <= 0) {
            return issuedDate;
        }
        int monthOffset = (sequence - 1) / monthlyBatchSize;
        return issuedDate.plusMonths(monthOffset);
    }

    private int couponQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        return Math.max(1, Math.min(120, quantity.setScale(0, RoundingMode.DOWN).intValue()));
    }

    private List<String> applicableCategories(String value) {
        if (value.contains("LNG")) {
            return List.of("fuel_lng", "LNG");
        }
        if (value.contains("CNG")) {
            return List.of("fuel_cng", "CNG");
        }
        if (value.contains("\u67f4\u6cb9")) {
            return List.of("fuel_diesel");
        }
        if (value.contains("\u9ad8\u6807\u53f7") || value.contains("95") || value.contains("98")) {
            return List.of("fuel_high_grade_gasoline");
        }
        if (value.contains("\u6c7d\u6cb9")) {
            return List.of("fuel_gasoline");
        }
        if (value.contains("\u6d17\u8f66")) {
            return List.of("car_wash");
        }
        return List.of("store");
    }

    private List<String> excludedCategories(String value) {
        if (value.contains("\u9999\u70df") || value.contains("\u5316\u80a5")) {
            return List.of("cigarette", "fertilizer", "\u9999\u70df", "\u5316\u80a5");
        }
        return List.of();
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record EntitlementCouponSpec(
            String templateId,
            String couponName,
            BigDecimal faceValue,
            BigDecimal minSpendAmount,
            List<String> applicableCategories,
            List<String> excludedCategories,
            BigDecimal discountRate,
            int monthlyBatchSize
    ) {
    }

    private record BenefitPackageCouponIssueAudit(
            String purchaseId,
            String packageCode,
            String memberCode,
            String couponTemplateId
    ) {
    }
}
