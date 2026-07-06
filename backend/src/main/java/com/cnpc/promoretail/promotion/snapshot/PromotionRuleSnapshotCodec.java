package com.cnpc.promoretail.promotion.snapshot;

import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PromotionRuleSnapshotCodec {

    private final ObjectMapper objectMapper;

    public PromotionRuleSnapshotCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    public String toJson(PromotionRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException exception) {
            throw new RuleSnapshotSerializationException("规则快照序列化失败: " + rule.ruleId(), exception);
        }
    }

    public PromotionRule fromJson(String json) {
        try {
            return objectMapper.readValue(json, PromotionRule.class);
        } catch (JsonProcessingException exception) {
            throw new RuleSnapshotSerializationException("规则快照反序列化失败", exception);
        }
    }
}
