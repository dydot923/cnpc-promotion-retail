package com.cnpc.promoretail.promotion.excludedcategory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.excludedcategory.persistence.PromotionExcludedCategoryEntity;
import com.cnpc.promoretail.promotion.excludedcategory.persistence.PromotionExcludedCategoryMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPromotionExcludedCategoryRepository implements PromotionExcludedCategoryRepository {

    private final PromotionExcludedCategoryMapper mapper;

    public MybatisPromotionExcludedCategoryRepository(PromotionExcludedCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PromotionExcludedCategory> findByRuleId(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<PromotionExcludedCategoryEntity>()
                        .eq(PromotionExcludedCategoryEntity::getRuleId, ruleId)
                        .orderByAsc(PromotionExcludedCategoryEntity::getCategoryName))
                .stream()
                .map(PromotionExcludedCategoryEntity::toExcludedCategory)
                .toList();
    }

    @Override
    public Map<String, Set<String>> categoryNamesByRuleIds(Collection<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Map.of();
        }
        Set<String> normalizedRuleIds = ruleIds.stream()
                .filter(ruleId -> ruleId != null && !ruleId.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (normalizedRuleIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        mapper.selectList(new LambdaQueryWrapper<PromotionExcludedCategoryEntity>()
                        .in(PromotionExcludedCategoryEntity::getRuleId, normalizedRuleIds)
                        .orderByAsc(PromotionExcludedCategoryEntity::getRuleId)
                        .orderByAsc(PromotionExcludedCategoryEntity::getCategoryName))
                .forEach(entity -> result
                        .computeIfAbsent(entity.getRuleId(), ignored -> new LinkedHashSet<>())
                        .add(entity.getCategoryName()));
        return result.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> Set.copyOf(entry.getValue())
        ));
    }
}
