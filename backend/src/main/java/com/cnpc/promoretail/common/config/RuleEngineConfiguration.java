package com.cnpc.promoretail.common.config;

import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.CompositeBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.CouponRedeemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FuelVolumeDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftItemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.ConflictResolver;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTriggerRepository;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.explanation.ExplanationBuilder;
import com.cnpc.promoretail.ruleengine.ranking.CandidateRanker;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import com.cnpc.promoretail.inventory.InventoryQueryService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineConfiguration {

    @Bean
    public ConditionMatcher conditionMatcher(PromotionDateTriggerRepository dateTriggerRepository) {
        return new DefaultConditionMatcher(dateTriggerRepository);
    }

    @Bean
    public List<BenefitCalculator> benefitCalculators(
            InventoryQueryService inventoryQueryService,
            BundleDefinitionProvider bundleDefinitionProvider
    ) {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(inventoryQueryService),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator(inventoryQueryService, bundleDefinitionProvider),
                new CouponRedeemBenefitCalculator(),
                new FuelVolumeDiscountBenefitCalculator(),
                new CompositeBenefitCalculator()
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
