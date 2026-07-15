package com.cnpc.promoretail.ruleengine.datetrigger;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPromotionDateTriggerRepository implements PromotionDateTriggerRepository {

    private final List<PromotionDateTrigger> triggers = new CopyOnWriteArrayList<>();

    @Override
    public List<PromotionDateTrigger> findByRuleId(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return List.of();
        }
        return triggers.stream()
                .filter(PromotionDateTrigger::enabled)
                .filter(trigger -> ruleId.equalsIgnoreCase(trigger.ruleId()))
                .sorted(triggerComparator())
                .toList();
    }

    @Override
    public List<PromotionDateTrigger> findAllEnabled() {
        return triggers.stream()
                .filter(PromotionDateTrigger::enabled)
                .sorted(triggerComparator())
                .toList();
    }

    public PromotionDateTrigger save(PromotionDateTrigger trigger) {
        triggers.add(trigger);
        return trigger;
    }

    private Comparator<PromotionDateTrigger> triggerComparator() {
        return Comparator.comparing(PromotionDateTrigger::ruleId)
                .thenComparing(PromotionDateTrigger::activityCode)
                .thenComparing(PromotionDateTrigger::triggerType);
    }
}
