package com.cnpc.promoretail.promotion.excludedcategory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface PromotionExcludedCategoryRepository {

    List<PromotionExcludedCategory> findByRuleId(String ruleId);

    default Set<String> categoryNamesByRuleId(String ruleId) {
        return findByRuleId(ruleId).stream()
                .map(PromotionExcludedCategory::categoryName)
                .collect(Collectors.toUnmodifiableSet());
    }

    default Map<String, Set<String>> categoryNamesByRuleIds(Collection<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        ruleIds.stream()
                .filter(ruleId -> ruleId != null && !ruleId.isBlank())
                .distinct()
                .forEach(ruleId -> result.put(ruleId, categoryNamesByRuleId(ruleId)));
        return Map.copyOf(result);
    }
}
