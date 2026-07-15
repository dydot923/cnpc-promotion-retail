package com.cnpc.promoretail.promotion.excludedcategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPromotionExcludedCategoryRepository implements PromotionExcludedCategoryRepository {

    private final ConcurrentMap<String, List<PromotionExcludedCategory>> rowsByRuleId = new ConcurrentHashMap<>();

    @Override
    public List<PromotionExcludedCategory> findByRuleId(String ruleId) {
        return rowsByRuleId.getOrDefault(normalize(ruleId), List.of());
    }

    public void save(PromotionExcludedCategory excludedCategory) {
        rowsByRuleId.compute(normalize(excludedCategory.ruleId()), (ignored, rows) -> {
            List<PromotionExcludedCategory> mutable = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            mutable.removeIf(row -> row.categoryName().equals(excludedCategory.categoryName()));
            mutable.add(excludedCategory);
            mutable.sort(Comparator.comparing(PromotionExcludedCategory::categoryName));
            return List.copyOf(mutable);
        });
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
