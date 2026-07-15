package com.cnpc.promoretail.ruleengine;

import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculation;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatchResult;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.ConflictResolver;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.explanation.ExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.BlockedReason;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.MoneySummary;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.ranking.CandidateRanker;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultPromotionEngine implements PromotionEngine {

    private final ConditionMatcher conditionMatcher;
    private final List<BenefitCalculator> benefitCalculators;
    private final ConflictResolver conflictResolver;
    private final CandidateRanker candidateRanker;
    private final ExplanationBuilder explanationBuilder;

    public DefaultPromotionEngine(
            ConditionMatcher conditionMatcher,
            List<BenefitCalculator> benefitCalculators,
            ConflictResolver conflictResolver,
            CandidateRanker candidateRanker,
            ExplanationBuilder explanationBuilder
    ) {
        this.conditionMatcher = conditionMatcher;
        this.benefitCalculators = List.copyOf(benefitCalculators);
        this.conflictResolver = conflictResolver;
        this.candidateRanker = candidateRanker;
        this.explanationBuilder = explanationBuilder;
    }

    @Override
    public CalculationResult calculate(OrderContext context, List<PromotionRule> rules) {
        List<PromotionRule> activeRules = rules == null ? List.of() : rules;
        CartTotals totals = CartTotals.from(context);
        PromotionCandidate fallback = PromotionCandidate.originalPrice(totals.originalAmount());
        List<PromotionCandidate> candidates = new ArrayList<>();
        List<BlockedPromotion> blockedPromotions = new ArrayList<>();

        for (PromotionRule rule : activeRules) {
            if (!rule.active()) {
                blockedPromotions.add(blocked(rule, List.of("促销规则未启用或仍处于待确认状态。")));
                continue;
            }

            ConditionMatchResult conditionResult = conditionMatcher.match(context, rule);
            if (!conditionResult.matched()) {
                blockedPromotions.add(blocked(rule, conditionResult.blockedReasons()));
                continue;
            }

            BenefitCalculator calculator = benefitCalculators.stream()
                    .filter(item -> item.supports(rule.ruleType()))
                    .findFirst()
                    .orElse(null);
            if (calculator == null) {
                blockedPromotions.add(blocked(rule, List.of("当前促销类型尚未接入优惠计算器。")));
                continue;
            }

            BenefitCalculation benefit = calculator.calculate(context, rule, totals);
            if (benefit.hasCandidates()) {
                candidates.addAll(benefit.candidates());
            }
            if (!benefit.blockedReasons().isEmpty()) {
                blockedPromotions.add(blocked(rule, benefit.blockedReasons()));
            }
        }

        List<PromotionCandidate> resolvedCandidates = new ArrayList<>();
        resolvedCandidates.add(fallback);
        resolvedCandidates.addAll(conflictResolver.resolve(candidates));
        PromotionCandidate recommended = candidateRanker.recommend(resolvedCandidates).orElse(fallback);
        List<String> ruleVersionIds = resolvedCandidates.stream()
                .map(PromotionCandidate::ruleVersion)
                .filter(Objects::nonNull)
                .filter(version -> !"original".equals(version))
                .distinct()
                .toList();
        return new CalculationResult(
                totals.originalAmount(),
                recommended.payableAmount(),
                recommended.discountAmount(),
                MoneySummary.of(totals.originalAmount(), recommended.payableAmount(), recommended.discountAmount()),
                recommended.candidateId(),
                resolvedCandidates,
                blockedPromotions,
                explanationBuilder.summarize(recommended, blockedPromotions),
                recommended.ruleVersion(),
                ruleVersionIds,
                List.of(),
                fallback
        );
    }

    private BlockedPromotion blocked(PromotionRule rule, List<String> reasons) {
        return new BlockedPromotion(rule.ruleId(), rule.activityName(), rule.ruleType(),
                reasons.stream().map(BlockedReason::of).toList(), rule.version());
    }
}
