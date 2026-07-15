package com.cnpc.promoretail.promotion.points;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.member.MemberCouponResponse;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import com.cnpc.promoretail.member.repository.MemberPointsChangeRepository;
import com.cnpc.promoretail.member.repository.MemberRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsPromotionService {

    private static final String POINTS_EXCHANGE_RULE_ID = "abv2-g2-points-exchange-90-off";
    private static final String POINTS_EXCHANGE_TEMPLATE_ID = "points-exchange-90-off";
    private static final String POINTS_LOTTERY_ACTIVITY_CODE = PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE;
    private static final String POINTS_LOTTERY_RULE_ID = "abv2-g2-points-lottery";
    private static final int LOTTERY_POINTS_COST = 500;
    private static final Set<Integer> EXCHANGE_DAYS = Set.of(9, 19, 29);

    private final MemberRepository memberRepository;
    private final MemberPointsChangeRepository pointsChangeRepository;
    private final CouponRepository couponRepository;
    private final PointsLotteryDrawRepository drawRepository;
    private final PointsLotteryPrizeConfigRepository prizeConfigRepository;
    private final AuditLogService auditLogService;

    public PointsPromotionService(
            MemberRepository memberRepository,
            MemberPointsChangeRepository pointsChangeRepository,
            CouponRepository couponRepository,
            PointsLotteryDrawRepository drawRepository,
            AuditLogService auditLogService
    ) {
        this(memberRepository, pointsChangeRepository, couponRepository, drawRepository,
                new InMemoryPointsLotteryPrizeConfigRepository(), auditLogService);
    }

    @Autowired
    public PointsPromotionService(
            MemberRepository memberRepository,
            MemberPointsChangeRepository pointsChangeRepository,
            CouponRepository couponRepository,
            PointsLotteryDrawRepository drawRepository,
            PointsLotteryPrizeConfigRepository prizeConfigRepository,
            AuditLogService auditLogService
    ) {
        this.memberRepository = memberRepository;
        this.pointsChangeRepository = pointsChangeRepository;
        this.couponRepository = couponRepository;
        this.drawRepository = drawRepository;
        this.prizeConfigRepository = prizeConfigRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PointsExchangeResponse exchangeDiscount(String memberCode, PointsExchangeRequest request) {
        LocalDate businessDate = businessDate(request.businessDate());
        requireExchangeDay(businessDate);
        long pointsUsed = request.pointsUsed();
        Member member = activeMember(memberCode);
        requireEnoughPoints(member, pointsUsed);

        Member updated = memberRepository.adjustPoints(member.memberCode(), -pointsUsed);
        String exchangeId = "points-exchange-" + UUID.randomUUID();
        Coupon coupon = couponRepository.save(new Coupon(
                "coupon-" + exchangeId,
                POINTS_EXCHANGE_TEMPLATE_ID,
                "Points exchange 90% off coupon",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of("store"),
                List.of("cigarette", "fertilizer", "\u9999\u70df", "\u5316\u80a5"),
                List.of(),
                List.of(),
                businessDate,
                businessDate.plusDays(29),
                true,
                false,
                CouponStatus.AVAILABLE,
                LocalDateTime.now(),
                null,
                operatorId(request.operatorId()),
                new BigDecimal("0.90"),
                "",
                null,
                member.memberCode()
        ));
        pointsChangeRepository.save(pointsChange(
                updated,
                "SUBTRACT",
                -pointsUsed,
                "POINTS_EXCHANGE",
                exchangeId,
                POINTS_EXCHANGE_RULE_ID,
                request.stationCode(),
                request.operatorId(),
                request.operatorName(),
                "Points exchange 90% off coupon"
        ));
        auditLogService.record("POINTS_EXCHANGE", "MEMBER", member.memberCode(), null,
                new PointsExchangeAudit(exchangeId, POINTS_EXCHANGE_RULE_ID, coupon.couponId(), pointsUsed,
                        updated.availablePoints()),
                operatorId(request.operatorId()), blankToEmpty(request.operatorName()),
                "Points exchange 90% off coupon");
        auditLogService.record("COUPON_ISSUE", "COUPON", coupon.couponId(), null,
                new CouponIssueAudit(exchangeId, POINTS_EXCHANGE_RULE_ID, member.memberCode(), coupon.couponTemplateId()),
                operatorId(request.operatorId()), blankToEmpty(request.operatorName()),
                "Points exchange coupon issued");
        return new PointsExchangeResponse(exchangeId, member.memberCode(), pointsUsed,
                updated.availablePoints(), businessDate, MemberCouponResponse.from(coupon));
    }

    @Transactional
    public PointsLotteryDrawResponse draw(String memberCode, PointsLotteryDrawRequest request) {
        LocalDate businessDate = businessDate(request.businessDate());
        requireLotteryDay(businessDate);
        Member member = activeMember(memberCode);
        requireEnoughPoints(member, LOTTERY_POINTS_COST);

        Member updated = memberRepository.adjustPoints(member.memberCode(), -LOTTERY_POINTS_COST);
        String drawId = "points-draw-" + UUID.randomUUID();
        PointsLotteryPrizeConfig prizeConfig = selectPrize(member.memberCode(), businessDate, drawId);
        Coupon prizeCoupon = prizeConfig.couponPrize()
                ? issueLotteryCoupon(member, businessDate, drawId, request, prizeConfig)
                : null;
        PointsLotteryDraw draw = new PointsLotteryDraw(
                drawId,
                member.memberCode(),
                POINTS_LOTTERY_ACTIVITY_CODE,
                LOTTERY_POINTS_COST,
                prizeConfig.prizeType(),
                prizeCoupon == null ? "" : prizeCoupon.couponId(),
                prizeConfig.prizeName(),
                businessDate,
                blankToEmpty(request.stationCode()),
                operatorId(request.operatorId()),
                blankToEmpty(request.operatorName()),
                Instant.now()
        );
        drawRepository.save(draw);
        pointsChangeRepository.save(pointsChange(
                updated,
                "SUBTRACT",
                -LOTTERY_POINTS_COST,
                "POINTS_LOTTERY",
                drawId,
                POINTS_LOTTERY_RULE_ID,
                request.stationCode(),
                request.operatorId(),
                request.operatorName(),
                "Points lottery draw"
        ));
        auditLogService.record("POINTS_LOTTERY_DRAW", "MEMBER", member.memberCode(), null,
                draw, operatorId(request.operatorId()), blankToEmpty(request.operatorName()),
                "Points lottery draw");
        return PointsLotteryDrawResponse.from(draw, updated.availablePoints(),
                prizeCoupon == null ? null : MemberCouponResponse.from(prizeCoupon));
    }

    public List<PointsLotteryDrawResponse> lotteryDraws(String memberCode, int limit) {
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
        return drawRepository.findByMemberCode(member.memberCode(), limit).stream()
                .map(draw -> PointsLotteryDrawResponse.from(draw, member.availablePoints(),
                        draw.prizeCouponId().isBlank()
                                ? null
                                : couponRepository.findByCouponId(draw.prizeCouponId())
                                        .map(MemberCouponResponse::from)
                                        .orElse(null)))
                .toList();
    }

    private Coupon issueLotteryCoupon(
            Member member,
            LocalDate businessDate,
            String drawId,
            PointsLotteryDrawRequest request,
            PointsLotteryPrizeConfig prizeConfig
    ) {
        Coupon coupon = couponRepository.save(new Coupon(
                "coupon-" + drawId,
                prizeConfig.couponTemplateId(),
                prizeConfig.couponName(),
                prizeConfig.faceValue(),
                prizeConfig.minSpendAmount(),
                prizeConfig.applicableCategories(),
                prizeConfig.excludedCategories(),
                List.of(),
                List.of(),
                businessDate,
                businessDate.plusDays(prizeConfig.validDays() - 1L),
                true,
                false,
                CouponStatus.AVAILABLE,
                LocalDateTime.now(),
                null,
                operatorId(request.operatorId()),
                BigDecimal.ZERO,
                "",
                null,
                member.memberCode()
        ));
        auditLogService.record("COUPON_ISSUE", "COUPON", coupon.couponId(), null,
                new CouponIssueAudit(drawId, POINTS_LOTTERY_RULE_ID, member.memberCode(), coupon.couponTemplateId()),
                operatorId(request.operatorId()), blankToEmpty(request.operatorName()),
                "Points lottery prize coupon issued");
        return coupon;
    }

    private PointsLotteryPrizeConfig selectPrize(String memberCode, LocalDate businessDate, String drawId) {
        List<PointsLotteryPrizeConfig> activeConfigs = prizeConfigRepository
                .findActiveByActivityCode(POINTS_LOTTERY_ACTIVITY_CODE)
                .stream()
                .filter(PointsLotteryPrizeConfig::active)
                .sorted(Comparator.comparing(PointsLotteryPrizeConfig::prizeId))
                .toList();
        if (activeConfigs.isEmpty()) {
            activeConfigs = List.of(PointsLotteryPrizeConfig.defaultNoPrize(), PointsLotteryPrizeConfig.defaultStoreCoupon());
        }
        int totalWeight = activeConfigs.stream()
                .mapToInt(PointsLotteryPrizeConfig::weight)
                .sum();
        if (totalWeight <= 0) {
            return PointsLotteryPrizeConfig.defaultNoPrize();
        }
        int ticket = Math.floorMod((memberCode + ":" + businessDate + ":" + drawId).hashCode(), totalWeight);
        int cursor = 0;
        for (PointsLotteryPrizeConfig config : activeConfigs) {
            cursor += config.weight();
            if (ticket < cursor) {
                return config;
            }
        }
        return activeConfigs.getLast();
    }

    private Member activeMember(String memberCode) {
        return memberRepository.findByMemberCode(memberCode)
                .filter(Member::active)
                .orElseThrow(() -> new MemberNotFoundException(memberCode));
    }

    private void requireEnoughPoints(Member member, long points) {
        if (member.availablePoints() < points) {
            throw new IllegalArgumentException("Member points are insufficient: " + member.memberCode());
        }
    }

    private void requireExchangeDay(LocalDate businessDate) {
        if (!EXCHANGE_DAYS.contains(businessDate.getDayOfMonth())) {
            throw new IllegalArgumentException("Points exchange is only available on days 9, 19 and 29");
        }
    }

    private void requireLotteryDay(LocalDate businessDate) {
        int day = businessDate.getDayOfMonth();
        boolean active = day == 1 || (day >= 9 && day <= 11) || (day >= 19 && day <= 21) || day >= 29;
        if (!active) {
            throw new IllegalArgumentException("Points lottery is only available on days 9-11, 19-21, 29-31 or day 1");
        }
    }

    private LocalDate businessDate(LocalDate value) {
        return value == null ? LocalDate.now() : value;
    }

    private MemberPointsChange pointsChange(
            Member member,
            String changeType,
            long pointsChange,
            String sourceType,
            String sourceId,
            String ruleId,
            String stationCode,
            String operatorId,
            String operatorName,
            String reason
    ) {
        return new MemberPointsChange(
                "points-" + UUID.randomUUID(),
                member.memberCode(),
                changeType,
                pointsChange,
                member.totalPoints(),
                member.availablePoints(),
                sourceType,
                sourceId,
                ruleId,
                blankToEmpty(stationCode),
                operatorId(operatorId),
                blankToEmpty(operatorName),
                reason,
                Instant.now()
        );
    }

    private String operatorId(String value) {
        return value == null || value.isBlank() ? "system" : value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record PointsExchangeAudit(
            String exchangeId,
            String ruleId,
            String couponId,
            long pointsUsed,
            long availablePointsAfter
    ) {
    }

    private record CouponIssueAudit(
            String sourceEventId,
            String campaignVersion,
            String memberCode,
            String couponTemplateId
    ) {
    }
}
