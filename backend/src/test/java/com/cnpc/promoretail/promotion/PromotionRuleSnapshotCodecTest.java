package com.cnpc.promoretail.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.promotion.snapshot.PromotionRuleSnapshotCodec;
import com.cnpc.promoretail.promotion.snapshot.RuleSnapshotSerializationException;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromotionRuleSnapshotCodecTest {

    private final PromotionRuleSnapshotCodec codec = new PromotionRuleSnapshotCodec(new ObjectMapper());

    @Test
    void promotionRuleSnapshotCanRoundTripAsStrongType() {
        PromotionRule rule = new PromotionRule(
                "amount-off-001",
                "满100减20",
                PromotionRuleType.AMOUNT_OFF,
                60,
                "direct_discount",
                false,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(Set.of("70424725"), Set.of("香烟", "化肥"), Set.of(), Set.of(), Set.of(),
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                        new BigDecimal("100.00"), BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.amountOff(new BigDecimal("20.00")),
                "rule-version-001");

        PromotionRule restored = codec.fromJson(codec.toJson(rule));

        assertThat(restored).isEqualTo(rule);
        assertThat(restored.condition().startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(restored.benefit().amountOff()).isEqualByComparingTo("20.00");
    }

    @Test
    void invalidRuleSnapshotJsonReturnsClearError() {
        assertThatThrownBy(() -> codec.fromJson("{broken-json"))
                .isInstanceOf(RuleSnapshotSerializationException.class)
                .hasMessageContaining("规则快照反序列化失败");
    }
}
