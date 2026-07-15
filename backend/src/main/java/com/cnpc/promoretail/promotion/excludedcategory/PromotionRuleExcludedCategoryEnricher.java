package com.cnpc.promoretail.promotion.excludedcategory;

import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PromotionRuleExcludedCategoryEnricher {

    private final PromotionExcludedCategoryRepository repository;

    public PromotionRuleExcludedCategoryEnricher(PromotionExcludedCategoryRepository repository) {
        this.repository = repository;
    }

    public PromotionRule enrich(PromotionRule rule) {
        if (rule == null) {
            return null;
        }
        return enrich(rule, repository.categoryNamesByRuleId(rule.ruleId()));
    }

    public List<PromotionRule> enrichAll(List<PromotionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        Map<String, Set<String>> categoriesByRuleId = repository.categoryNamesByRuleIds(
                rules.stream().map(PromotionRule::ruleId).toList());
        return rules.stream()
                .map(rule -> enrich(rule, categoriesByRuleId.getOrDefault(rule.ruleId(), Set.of())))
                .toList();
    }

    private PromotionRule enrich(PromotionRule rule, Set<String> excludedCategories) {
        return new PromotionRule(
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                rule.priority(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.status(),
                rule.condition().withAdditionalExcludedCategories(excludedCategories),
                rule.benefit(),
                rule.version()
        );
    }
}
