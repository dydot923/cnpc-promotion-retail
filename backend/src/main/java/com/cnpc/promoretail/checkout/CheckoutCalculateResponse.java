package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.BlockedReason;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.CompositeBenefitComponent;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.GiftItem;
import com.cnpc.promoretail.ruleengine.model.InventoryWarning;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutCalculateResponse(
        String calculationId,
        BigDecimal originalAmount,
        BigDecimal payableAmount,
        BigDecimal discountAmount,
        String recommendedCandidateId,
        List<CandidateResponse> availableCandidates,
        List<BlockedPromotionResponse> blockedPromotions,
        List<String> warnings,
        List<String> explanations,
        String ruleVersion,
        List<String> ruleVersionIds,
        List<InventoryWarning> inventoryWarnings,
        PointsPreview pointsPreview,
        CandidateResponse originalPriceFallback
) {

    public static CheckoutCalculateResponse from(String calculationId, CalculationResult result) {
        return from(calculationId, result, null);
    }

    public static CheckoutCalculateResponse from(
            String calculationId,
            CalculationResult result,
            PointsPreview pointsPreview
    ) {
        return new CheckoutCalculateResponse(
                calculationId,
                result.originalAmount(),
                result.payableAmount(),
                result.discountAmount(),
                result.recommendedCandidateId(),
                result.availableCandidates().stream().map(CandidateResponse::from).toList(),
                result.blockedPromotions().stream().map(BlockedPromotionResponse::from).toList(),
                List.of(),
                result.explanations(),
                result.ruleVersion(),
                result.ruleVersionIds(),
                result.inventoryWarnings(),
                pointsPreview,
                CandidateResponse.from(result.originalPriceFallback())
        );
    }

    public record PointsPreview(
            String activityId,
            String ruleId,
            String activityName,
            BigDecimal multiplier,
            long estimatedPoints
    ) {
    }

    public record CandidateResponse(
            String candidateId,
            String ruleId,
            String title,
            PromotionRuleType ruleType,
            String status,
            BigDecimal originalAmount,
            BigDecimal payableAmount,
            BigDecimal discountAmount,
            List<GiftItem> gifts,
            List<GiftCoupon> coupons,
            String explanation,
            String ruleVersionId,
            boolean stackable,
            String exclusiveGroup,
            List<String> consumedCouponIds,
            List<CompositeBenefitComponent> compositeComponents,
            int pointsMultiplier
    ) {

        private static CandidateResponse from(PromotionCandidate candidate) {
            return new CandidateResponse(
                    candidate.candidateId(),
                    candidate.ruleId(),
                    candidate.title(),
                    candidate.ruleType(),
                    "AVAILABLE",
                    candidate.originalAmount(),
                    candidate.payableAmount(),
                    candidate.discountAmount(),
                    candidate.gifts(),
                    candidate.coupons(),
                    candidate.explanation(),
                    candidate.ruleVersion(),
                    candidate.stackable(),
                    candidate.exclusiveGroup(),
                    candidate.consumedCouponIds().stream().toList(),
                    candidate.compositeComponents(),
                    candidate.pointsMultiplier()
            );
        }
    }

    public record BlockedPromotionResponse(
            String ruleId,
            PromotionRuleType ruleType,
            String title,
            List<BlockedReason> blockedReasons,
            String ruleVersionId
    ) {

        private static BlockedPromotionResponse from(BlockedPromotion promotion) {
            return new BlockedPromotionResponse(
                    promotion.ruleId(),
                    promotion.ruleType(),
                    promotion.title(),
                    promotion.reasons(),
                    promotion.ruleVersion()
            );
        }
    }
}
