package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.checkout.model.CheckoutCalculationRecord;
import com.cnpc.promoretail.checkout.model.CheckoutConfirmation;
import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import com.cnpc.promoretail.checkout.model.CheckoutTransactionItem;
import com.cnpc.promoretail.checkout.repository.CheckoutCalculationRecordRepository;
import com.cnpc.promoretail.checkout.repository.CheckoutConfirmationRepository;
import com.cnpc.promoretail.checkout.repository.CheckoutTransactionRepository;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import com.cnpc.promoretail.member.repository.MemberPointsChangeRepository;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.promotion.points.PointsActivity;
import com.cnpc.promoretail.promotion.points.PointsActivityRepository;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.station.StationNotFoundException;
import com.cnpc.promoretail.station.StationRepository;
import com.cnpc.promoretail.station.model.Station;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutApplicationService {

    private final PromotionEngine promotionEngine;
    private final PromotionRuleRepository promotionRuleRepository;
    private final CheckoutCalculationRecordRepository checkoutCalculationRecordRepository;
    private final CheckoutConfirmationRepository checkoutConfirmationRepository;
    private final CheckoutTransactionRepository checkoutTransactionRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final MemberPointsChangeRepository memberPointsChangeRepository;
    private final PointsActivityRepository pointsActivityRepository;
    private final AuditLogService auditLogService;
    private final StationRepository stationRepository;

    public CheckoutApplicationService(
            PromotionEngine promotionEngine,
            PromotionRuleRepository promotionRuleRepository,
            CheckoutCalculationRecordRepository checkoutCalculationRecordRepository,
            CheckoutConfirmationRepository checkoutConfirmationRepository,
            CheckoutTransactionRepository checkoutTransactionRepository,
            CouponRepository couponRepository,
            MemberRepository memberRepository,
            MemberPointsChangeRepository memberPointsChangeRepository,
            PointsActivityRepository pointsActivityRepository,
            AuditLogService auditLogService,
            StationRepository stationRepository
    ) {
        this.promotionEngine = promotionEngine;
        this.promotionRuleRepository = promotionRuleRepository;
        this.checkoutCalculationRecordRepository = checkoutCalculationRecordRepository;
        this.checkoutConfirmationRepository = checkoutConfirmationRepository;
        this.checkoutTransactionRepository = checkoutTransactionRepository;
        this.couponRepository = couponRepository;
        this.memberRepository = memberRepository;
        this.memberPointsChangeRepository = memberPointsChangeRepository;
        this.pointsActivityRepository = pointsActivityRepository;
        this.auditLogService = auditLogService;
        this.stationRepository = stationRepository;
    }

    public CheckoutCalculateResponse calculate(CheckoutCalculateRequest request) {
        OrderContext orderContext = effectiveOrderContext(request);
        CalculationResult result = promotionEngine.calculate(orderContext, promotionRuleRepository.findConfirmedRules());
        String calculationId = "calc-" + UUID.randomUUID();
        checkoutCalculationRecordRepository.save(new CheckoutCalculationRecord(
                calculationId,
                orderContext,
                result,
                result.ruleVersionIds(),
                Instant.now()
        ));
        return CheckoutCalculateResponse.from(calculationId, result);
    }

    @Transactional
    public CheckoutConfirmationResponse confirm(CheckoutConfirmRequest request) {
        CheckoutCalculationRecord calculationRecord = checkoutCalculationRecordRepository
                .findByCalculationId(request.calculationId())
                .orElseThrow(() -> new CheckoutCalculationNotFoundException(request.calculationId()));
        if (!checkoutConfirmationRepository.findByCalculationId(request.calculationId()).isEmpty()) {
            throw new CheckoutCalculationAlreadyConfirmedException(request.calculationId());
        }
        preventDuplicateTransaction(request);
        PromotionCandidate selectedCandidate = findSelectedCandidate(
                calculationRecord.resultSnapshot(),
                request.selectedCandidateId()
        );

        Instant now = Instant.now();
        redeemConsumedCoupons(selectedCandidate, calculationRecord.requestSnapshot(), request, now);
        issueGiftCoupons(selectedCandidate, calculationRecord.requestSnapshot(), request, now);
        CheckoutConfirmation confirmation = new CheckoutConfirmation(
                "confirm-" + UUID.randomUUID(),
                calculationRecord.calculationId(),
                selectedCandidate.candidateId(),
                selectedCandidate,
                request.operatorId(),
                request.operatorName(),
                request.skippedPromotion() || selectedCandidate.ruleType() == PromotionRuleType.ORIGINAL_PRICE,
                now,
                now,
                now
        );
        checkoutConfirmationRepository.save(confirmation);
        CheckoutTransaction transaction = buildTransaction(request, calculationRecord, confirmation, selectedCandidate);
        checkoutTransactionRepository.save(transaction);
        awardMemberPoints(calculationRecord.requestSnapshot(), selectedCandidate, request, transaction.txnNo());
        auditLogService.record(
                "CHECKOUT_CONFIRM",
                "CHECKOUT_CONFIRMATION",
                confirmation.confirmationId(),
                null,
                confirmation,
                confirmation.operatorId(),
                confirmation.operatorName(),
                request.skippedPromotion() ? "Skipped promotion" : "Selected checkout candidate"
        );
        return CheckoutConfirmationResponse.from(confirmation, calculationRecord.requestSnapshot().cartItems());
    }

    public CheckoutTransactionResponse getTransaction(String txnNo) {
        return checkoutTransactionRepository.findByTxnNo(txnNo)
                .map(CheckoutTransactionResponse::from)
                .orElseThrow(() -> new CheckoutTransactionNotFoundException(txnNo));
    }

    public List<CheckoutTransactionResponse> findRecentTransactions(int limit) {
        return checkoutTransactionRepository.findRecent(limit).stream()
                .map(CheckoutTransactionResponse::from)
                .toList();
    }

    public List<CheckoutTransactionResponse> findTransactions(CheckoutTransactionQuery query) {
        return checkoutTransactionRepository.findByQuery(query).stream()
                .map(CheckoutTransactionResponse::from)
                .toList();
    }

    public CheckoutConfirmationResponse getConfirmation(String confirmationId) {
        CheckoutConfirmation confirmation = checkoutConfirmationRepository.findByConfirmationId(confirmationId)
                .orElseThrow(() -> new CheckoutConfirmationNotFoundException(confirmationId));
        return CheckoutConfirmationResponse.from(confirmation, cartItems(confirmation.calculationId()));
    }

    public List<CheckoutConfirmationResponse> findConfirmationsByCalculationId(String calculationId) {
        return checkoutConfirmationRepository.findByCalculationId(calculationId).stream()
                .map(confirmation -> CheckoutConfirmationResponse.from(confirmation, cartItems(confirmation.calculationId())))
                .toList();
    }

    private PromotionCandidate findSelectedCandidate(CalculationResult result, String selectedCandidateId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.candidateId().equals(selectedCandidateId))
                .findFirst()
                .or(() -> {
                    PromotionCandidate fallback = result.originalPriceFallback();
                    return fallback != null && fallback.candidateId().equals(selectedCandidateId)
                            ? java.util.Optional.of(fallback)
                            : java.util.Optional.empty();
                })
                .orElseThrow(() -> new CheckoutCandidateNotFoundException(selectedCandidateId));
    }

    private List<com.cnpc.promoretail.ruleengine.context.CartItem> cartItems(String calculationId) {
        return checkoutCalculationRecordRepository.findByCalculationId(calculationId)
                .map(record -> record.requestSnapshot().cartItems())
                .orElse(List.of());
    }

    private CheckoutTransaction buildTransaction(
            CheckoutConfirmRequest request,
            CheckoutCalculationRecord calculationRecord,
            CheckoutConfirmation confirmation,
            PromotionCandidate selectedCandidate
    ) {
        OrderContext orderContext = calculationRecord.requestSnapshot();
        return new CheckoutTransaction(
                transactionNo(request.orderNo()),
                confirmation.confirmationId(),
                calculationRecord.calculationId(),
                selectedCandidate.candidateId(),
                selectedCandidate.originalAmount(),
                selectedCandidate.discountAmount(),
                selectedCandidate.payableAmount(),
                orderContext.customer().paymentMethod(),
                confirmation.operatorId(),
                confirmation.operatorName(),
                orderContext.customer().memberCode(),
                orderContext.station().stationId(),
                "CONFIRMED",
                confirmation.confirmedAt(),
                orderContext.cartItems().stream()
                        .map(item -> toTransactionItem(item, selectedCandidate))
                        .toList()
        );
    }

    private CheckoutTransactionItem toTransactionItem(CartItem item, PromotionCandidate selectedCandidate) {
        return new CheckoutTransactionItem(
                item.productCode(),
                item.name(),
                item.barcode(),
                item.category(),
                item.unitPrice(),
                item.unitPrice(),
                item.quantity(),
                item.lineAmount(),
                appliedPromotionId(item, selectedCandidate),
                appliedCouponCodes(selectedCandidate)
        );
    }

    private String appliedPromotionId(CartItem item, PromotionCandidate selectedCandidate) {
        if (selectedCandidate.ruleType() == PromotionRuleType.ORIGINAL_PRICE) {
            return null;
        }
        if (selectedCandidate.consumedProductCodes().isEmpty()
                || selectedCandidate.consumedProductCodes().contains(item.productCode())) {
            return selectedCandidate.ruleId();
        }
        return null;
    }

    private String appliedCouponCodes(PromotionCandidate selectedCandidate) {
        if (selectedCandidate.consumedCouponIds().isEmpty()) {
            return null;
        }
        return selectedCandidate.consumedCouponIds().stream()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String transactionNo(String orderNo) {
        return orderNo == null || orderNo.isBlank()
                ? "txn-" + UUID.randomUUID()
                : orderNo;
    }

    private void preventDuplicateTransaction(CheckoutConfirmRequest request) {
        if (request.orderNo() == null || request.orderNo().isBlank()) {
            return;
        }
        checkoutTransactionRepository.findByTxnNo(request.orderNo()).ifPresent(existing -> {
            throw new CheckoutTransactionAlreadyExistsException(request.orderNo());
        });
    }

    private void awardMemberPoints(
            OrderContext orderContext,
            PromotionCandidate selectedCandidate,
            CheckoutConfirmRequest request,
            String sourceId
    ) {
        String memberCode = orderContext.customer().memberCode();
        if (memberCode == null || memberCode.isBlank()) {
            return;
        }
        Member member = memberRepository.findByMemberCode(memberCode).orElse(null);
        if (member == null || !member.active()) {
            return;
        }
        BigDecimal multiplier = memberRepository.findLevelByCode(member.levelCode())
                .map(level -> level.pointsMultiplier())
                .orElse(BigDecimal.ONE);
        PointsActivity matchedPointsActivity = bestPointsActivity(orderContext).orElse(null);
        multiplier = max(multiplier, BigDecimal.valueOf(selectedCandidate.pointsMultiplier()));
        if (matchedPointsActivity != null) {
            multiplier = max(multiplier, matchedPointsActivity.pointsMultiplier());
        }
        BigDecimal pointsBasis = pointsBasis(orderContext, selectedCandidate, matchedPointsActivity);
        long points = pointsBasis
                .multiply(multiplier)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        if (points <= 0) {
            return;
        }
        Member before = member;
        Member after = memberRepository.adjustPoints(member.memberCode(), points);
        memberPointsChangeRepository.save(new MemberPointsChange(
                "points-" + UUID.randomUUID(),
                after.memberCode(),
                "ADD",
                points,
                after.totalPoints(),
                after.availablePoints(),
                "CHECKOUT",
                sourceId,
                selectedCandidate.ruleId(),
                orderContext.station().stationId(),
                request.operatorId(),
                request.operatorName(),
                "Checkout confirmation awarded points at multiplier "
                        + multiplier.stripTrailingZeros().toPlainString(),
                Instant.now()
        ));
        auditLogService.record(
                "MEMBER_POINTS_ADD",
                "MEMBER",
                member.memberCode(),
                before,
                after,
                request.operatorId(),
                request.operatorName(),
                "Checkout confirmation awarded points"
        );
    }

    private Optional<PointsActivity> bestPointsActivity(OrderContext orderContext) {
        return pointsActivityRepository.findActive().stream()
                .filter(activity -> matchesPointsActivity(activity, orderContext))
                .max(Comparator.comparing(PointsActivity::pointsMultiplier));
    }

    private boolean matchesPointsActivity(PointsActivity activity, OrderContext orderContext) {
        if (activity.memberRequired() && !orderContext.customer().member()) {
            return false;
        }
        LocalDate businessDate = orderContext.businessDate();
        if (businessDate != null) {
            if (activity.startDate() != null && businessDate.isBefore(activity.startDate())) {
                return false;
            }
            if (activity.endDate() != null && businessDate.isAfter(activity.endDate())) {
                return false;
            }
            if (!activity.daysOfMonth().isEmpty()
                    && !activity.daysOfMonth().contains(businessDate.getDayOfMonth())) {
                return false;
            }
        }
        if (!matchesText(activity.stationTypes(), orderContext.station().stationType())) {
            return false;
        }
        if (!matchesText(activity.stationProvinces(), orderContext.station().region())) {
            return false;
        }
        if (!activity.fuelTypes().isEmpty()) {
            if (orderContext.fuel().amount().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (!containsText(activity.fuelTypes(), orderContext.fuel().fuelType().name())) {
                return false;
            }
        }
        if (!activity.includedCategories().isEmpty() || !activity.excludedCategories().isEmpty()) {
            return orderContext.cartItems().stream()
                    .anyMatch(item -> matchesItemCategory(activity, item));
        }
        return true;
    }

    private boolean matchesItemCategory(PointsActivity activity, CartItem item) {
        return (activity.includedCategories().isEmpty()
                || containsText(activity.includedCategories(), item.category()))
                && !containsText(activity.excludedCategories(), item.category());
    }

    private boolean matchesText(Set<String> expectedValues, String actualValue) {
        return expectedValues.isEmpty() || containsText(expectedValues, actualValue);
    }

    private boolean containsText(Set<String> expectedValues, String actualValue) {
        if (actualValue == null || actualValue.isBlank()) {
            return false;
        }
        return expectedValues.stream()
                .anyMatch(expected -> expected.equalsIgnoreCase(actualValue));
    }

    private BigDecimal pointsBasis(
            OrderContext orderContext,
            PromotionCandidate selectedCandidate,
            PointsActivity matchedPointsActivity
    ) {
        if (matchedPointsActivity != null
                && !matchedPointsActivity.fuelTypes().isEmpty()
                && orderContext.fuel().amount().compareTo(BigDecimal.ZERO) > 0) {
            return orderContext.fuel().amount();
        }
        if (selectedCandidate.payableAmount().compareTo(BigDecimal.ZERO) > 0) {
            return selectedCandidate.payableAmount();
        }
        return orderContext.fuel().amount();
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private OrderContext effectiveOrderContext(CheckoutCalculateRequest request) {
        OrderContext base = request.orderContext();
        StationContext station = buildStationContext(request, base);
        String memberCode = valueOrDefault(request.memberCode(), base.customer().memberCode());
        Member member = findMember(memberCode);
        String effectiveMemberLevel = valueOrDefault(request.memberLevel(), member == null
                ? base.customer().memberLevel()
                : member.levelCode());
        CustomerContext customer = new CustomerContext(
                member != null || (request.isMember() == null ? base.customer().member() : request.isMember()),
                effectiveMemberLevel,
                base.customer().availableCouponIds(),
                request.memberBirthMonth() == null
                        ? (member == null ? base.customer().memberBirthMonth() : member.birthMonth())
                        : request.memberBirthMonth(),
                valueOrDefault(request.paymentMethod(), base.customer().paymentMethod()),
                memberCode,
                member == null ? base.customer().memberTags() : member.memberTags(),
                memberLevelPriority(effectiveMemberLevel, base.customer().memberLevelPriority())
        );
        FuelContext fuel = new FuelContext(
                request.fuelType() == null ? base.fuel().fuelType() : request.fuelType(),
                base.fuel().fuelGrade(),
                request.fuelAmount() == null ? base.fuel().amount() : request.fuelAmount(),
                request.fuelVolume() == null ? base.fuel().volume() : request.fuelVolume()
        );
        List<Coupon> coupons = effectiveCoupons(request, base, memberCode);
        return new OrderContext(
                station,
                customer,
                fuel,
                base.cartItems(),
                request.transactionDate() == null ? base.businessDate() : request.transactionDate(),
                request.transactionTime() == null ? base.businessTime() : request.transactionTime(),
                coupons,
                request.rechargeAmount().compareTo(BigDecimal.ZERO) > 0
                        ? request.rechargeAmount()
                        : base.rechargeAmount()
        );
    }

    private StationContext buildStationContext(CheckoutCalculateRequest request, OrderContext base) {
        String requestedStationCode = blankToNull(request.stationCode());
        if (requestedStationCode != null) {
            Station station = stationRepository.findByStationCode(requestedStationCode)
                    .orElseThrow(() -> new StationNotFoundException(requestedStationCode));
            return new StationContext(
                    station.stationCode(),
                    valueOrDefault(request.stationType(), station.stationType()),
                    valueOrDefault(request.stationProvince(), station.province()),
                    valueOrDefault(request.stationCity(), station.city()),
                    station.district()
            );
        }
        return new StationContext(
                base.station().stationId(),
                valueOrDefault(request.stationType(), base.station().stationType()),
                valueOrDefault(request.stationProvince(), base.station().province()),
                valueOrDefault(request.stationCity(), base.station().city()),
                base.station().district()
        );
    }

    private Member findMember(String memberCode) {
        if (memberCode == null || memberCode.isBlank()) {
            return null;
        }
        return memberRepository.findByMemberCode(memberCode)
                .filter(Member::active)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
    }

    private Integer memberLevelPriority(String memberLevel, Integer fallback) {
        if (memberLevel == null || memberLevel.isBlank()) {
            return fallback;
        }
        return memberRepository.findLevelByCode(memberLevel)
                .map(level -> level.priority())
                .orElse(fallback);
    }

    private List<Coupon> effectiveCoupons(CheckoutCalculateRequest request, OrderContext base, String memberCode) {
        LocalDate businessDate = request.transactionDate() == null ? base.businessDate() : request.transactionDate();
        Set<String> selectedCouponIds = selectedCouponIds(request, base);
        List<Coupon> coupons;
        if (memberCode != null && !memberCode.isBlank()) {
            coupons = couponRepository.findByHolderMemberId(memberCode);
            return coupons.stream()
                    .filter(coupon -> usableForMemberCalculation(coupon, selectedCouponIds, businessDate))
                    .sorted(Comparator.comparing(Coupon::couponId))
                    .toList();
        }
        if (!selectedCouponIds.isEmpty()) {
            coupons = selectedCouponIds.stream()
                    .map(couponRepository::findByCouponId)
                    .flatMap(Optional::stream)
                    .filter(coupon -> usableForAnonymousCalculation(coupon, businessDate))
                    .toList();
        } else {
            coupons = request.availableCoupons().isEmpty()
                    ? base.availableCoupons()
                    : request.availableCoupons();
        }
        if (!selectedCouponIds.isEmpty()) {
            coupons = coupons.stream()
                    .filter(coupon -> selectedCouponIds.contains(coupon.couponId()))
                    .toList();
        }
        return coupons.stream()
                .filter(coupon -> coupon.status() == CouponStatus.AVAILABLE)
                .filter(coupon -> validOn(coupon, businessDate))
                .sorted(Comparator.comparing(Coupon::couponId))
                .toList();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void redeemConsumedCoupons(
            PromotionCandidate selectedCandidate,
            OrderContext requestSnapshot,
            CheckoutConfirmRequest request,
            Instant now
    ) {
        if (selectedCandidate.consumedCouponIds().isEmpty()) {
            return;
        }
        String memberCode = requestSnapshot.customer().memberCode();
        LocalDate businessDate = requestSnapshot.businessDate();
        LocalDateTime usedAt = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        for (String couponId : selectedCandidate.consumedCouponIds()) {
            Coupon before = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new CheckoutCouponException("Coupon not found: " + couponId));
            validateCouponRedeem(before, memberCode, businessDate);
            Coupon after = couponRepository.redeemIfAvailable(
                            couponId,
                            before.holderMemberId(),
                            businessDate,
                            usedAt,
                            request.operatorId()
                    )
                    .orElseThrow(() -> new CheckoutCouponException(
                            "Coupon is unavailable or already redeemed: " + couponId));
            auditLogService.record(
                    "COUPON_REDEEM",
                    "COUPON",
                    couponId,
                    before,
                    after,
                    request.operatorId(),
                    request.operatorName(),
                    "Checkout confirmation redeemed coupon"
            );
        }
    }

    private void issueGiftCoupons(
            PromotionCandidate selectedCandidate,
            OrderContext requestSnapshot,
            CheckoutConfirmRequest request,
            Instant now
    ) {
        if (selectedCandidate.coupons().isEmpty()) {
            return;
        }
        String memberCode = requestSnapshot.customer().memberCode();
        if (memberCode == null || memberCode.isBlank()) {
            throw new CheckoutCouponException("Gift coupons require a member code");
        }
        Member member = memberRepository.findByMemberCode(memberCode)
                .filter(Member::active)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        LocalDate validFrom = requestSnapshot.businessDate() == null
                ? LocalDate.ofInstant(now, ZoneId.systemDefault())
                : requestSnapshot.businessDate();
        LocalDateTime issuedAt = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        for (Coupon coupon : giftCoupons(selectedCandidate, member.memberCode(), validFrom, issuedAt, request)) {
            couponRepository.save(coupon);
            auditLogService.record(
                    "COUPON_ISSUE",
                    "COUPON",
                    coupon.couponId(),
                    null,
                    coupon,
                    request.operatorId(),
                    request.operatorName(),
                    "Checkout confirmation issued gift coupon"
            );
        }
    }

    private List<Coupon> giftCoupons(
            PromotionCandidate selectedCandidate,
            String memberCode,
            LocalDate validFrom,
            LocalDateTime issuedAt,
            CheckoutConfirmRequest request
    ) {
        List<Coupon> issued = new ArrayList<>();
        for (GiftCoupon giftCoupon : selectedCandidate.coupons()) {
            for (int index = 0; index < giftCoupon.quantity(); index++) {
                issued.add(new Coupon(
                        "gift-" + selectedCandidate.ruleId() + "-" + UUID.randomUUID(),
                        giftCoupon.couponTemplateId().isBlank()
                                ? "gift-" + normalizedCouponName(giftCoupon.couponName())
                                : giftCoupon.couponTemplateId(),
                        giftCoupon.couponName(),
                        giftCoupon.amount(),
                        giftCoupon.useThreshold(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        validFrom,
                        giftCoupon.validDays() <= 0 ? null : validFrom.plusDays(giftCoupon.validDays()),
                        true,
                        false,
                        CouponStatus.AVAILABLE,
                        issuedAt,
                        null,
                        request.operatorId(),
                        BigDecimal.ZERO,
                        "",
                        null,
                        memberCode
                ));
            }
        }
        return issued;
    }

    private void validateCouponRedeem(Coupon coupon, String memberCode, LocalDate businessDate) {
        if (coupon.status() != CouponStatus.AVAILABLE) {
            throw new CheckoutCouponException("Coupon is not available: " + coupon.couponId());
        }
        if (!validOn(coupon, businessDate)) {
            throw new CheckoutCouponException("Coupon is outside validity period: " + coupon.couponId());
        }
        if (coupon.memberOnly() && (memberCode == null || memberCode.isBlank())) {
            throw new CheckoutCouponException("Member coupon requires a member code: " + coupon.couponId());
        }
        if (!coupon.holderMemberId().isBlank() && !coupon.holderMemberId().equals(memberCode)) {
            throw new CheckoutCouponException("Coupon does not belong to member: " + coupon.couponId());
        }
    }

    private boolean usableForMemberCalculation(Coupon coupon, Set<String> selectedCouponIds, LocalDate businessDate) {
        if (coupon.status() == CouponStatus.USED) {
            return !selectedCouponIds.isEmpty();
        }
        if (coupon.status() != CouponStatus.AVAILABLE || !validOn(coupon, businessDate)) {
            return false;
        }
        return selectedCouponIds.isEmpty() || selectedCouponIds.contains(coupon.couponId());
    }

    private boolean usableForAnonymousCalculation(Coupon coupon, LocalDate businessDate) {
        return (coupon.status() == CouponStatus.AVAILABLE || coupon.status() == CouponStatus.USED)
                && validOn(coupon, businessDate);
    }

    private boolean validOn(Coupon coupon, LocalDate businessDate) {
        if (businessDate == null) {
            return true;
        }
        return (coupon.validFrom() == null || !businessDate.isBefore(coupon.validFrom()))
                && (coupon.validUntil() == null || !businessDate.isAfter(coupon.validUntil()));
    }

    private Set<String> selectedCouponIds(CheckoutCalculateRequest request, OrderContext base) {
        LinkedHashSet<String> couponIds = new LinkedHashSet<>();
        couponIds.addAll(request.selectedCouponIds());
        couponIds.addAll(base.customer().availableCouponIds());
        return couponIds.stream()
                .filter(couponId -> couponId != null && !couponId.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizedCouponName(String couponName) {
        String normalized = couponName == null ? "coupon" : couponName.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "coupon" : normalized;
    }
}
