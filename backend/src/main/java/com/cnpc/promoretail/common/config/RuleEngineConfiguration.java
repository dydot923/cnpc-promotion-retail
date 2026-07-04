package com.cnpc.promoretail.common.config;

import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftItemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.ConflictResolver;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.explanation.ExplanationBuilder;
import com.cnpc.promoretail.ruleengine.ranking.CandidateRanker;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineConfiguration {

    @Bean
    public ConditionMatcher conditionMatcher() {
        return new DefaultConditionMatcher();
    }

    @Bean
    public List<BenefitCalculator> benefitCalculators() {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator()
        );
    }

    @Bean
    public ConflictResolver conflictResolver() {
        return new DefaultConflictResolver();
    }

    @Bean
    public CandidateRanker candidateRanker() {
        return new DefaultCandidateRanker();
    }

    @Bean
    public ExplanationBuilder explanationBuilder() {
        return new DefaultExplanationBuilder();
    }

    @Bean
    public PromotionEngine promotionEngine(
            ConditionMatcher conditionMatcher,
            List<BenefitCalculator> benefitCalculators,
            ConflictResolver conflictResolver,
            CandidateRanker candidateRanker,
            ExplanationBuilder explanationBuilder
    ) {
        return new DefaultPromotionEngine(conditionMatcher, benefitCalculators, conflictResolver,
                candidateRanker, explanationBuilder);
    }
}

