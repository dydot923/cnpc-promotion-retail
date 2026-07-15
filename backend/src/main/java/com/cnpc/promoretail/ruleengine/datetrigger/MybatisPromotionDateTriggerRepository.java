package com.cnpc.promoretail.ruleengine.datetrigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.ruleengine.datetrigger.persistence.PromotionDateTriggerEntity;
import com.cnpc.promoretail.ruleengine.datetrigger.persistence.PromotionDateTriggerMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPromotionDateTriggerRepository implements PromotionDateTriggerRepository {

    private final PromotionDateTriggerMapper mapper;
    private volatile List<PromotionDateTrigger> cachedTriggers;
    private volatile Map<String, List<PromotionDateTrigger>> cachedByRuleId;

    public MybatisPromotionDateTriggerRepository(PromotionDateTriggerMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PromotionDateTrigger> findByRuleId(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return List.of();
        }
        ensureCache();
        return cachedByRuleId.getOrDefault(normalize(ruleId), List.of());
    }

    @Override
    public List<PromotionDateTrigger> findAllEnabled() {
        ensureCache();
        return cachedTriggers;
    }

    private void ensureCache() {
        if (cachedTriggers != null) {
            return;
        }
        synchronized (this) {
            if (cachedTriggers != null) {
                return;
            }
            List<PromotionDateTrigger> loaded = mapper.selectList(
                            new LambdaQueryWrapper<PromotionDateTriggerEntity>()
                                    .orderByAsc(PromotionDateTriggerEntity::getId))
                    .stream()
                    .map(PromotionDateTriggerEntity::toTrigger)
                    .filter(PromotionDateTrigger::enabled)
                    .toList();
            cachedTriggers = loaded;
            cachedByRuleId = loaded.stream()
                    .filter(trigger -> !trigger.ruleId().isBlank())
                    .collect(Collectors.groupingBy(
                            trigger -> normalize(trigger.ruleId()),
                            ConcurrentHashMap::new,
                            Collectors.toUnmodifiableList()
                    ));
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }
}
