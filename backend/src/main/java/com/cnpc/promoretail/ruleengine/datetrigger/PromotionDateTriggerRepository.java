package com.cnpc.promoretail.ruleengine.datetrigger;

import java.util.List;

public interface PromotionDateTriggerRepository {

    List<PromotionDateTrigger> findByRuleId(String ruleId);

    List<PromotionDateTrigger> findAllEnabled();

    static PromotionDateTriggerRepository empty() {
        return new PromotionDateTriggerRepository() {
            @Override
            public List<PromotionDateTrigger> findByRuleId(String ruleId) {
                return List.of();
            }

            @Override
            public List<PromotionDateTrigger> findAllEnabled() {
                return List.of();
            }
        };
    }
}
