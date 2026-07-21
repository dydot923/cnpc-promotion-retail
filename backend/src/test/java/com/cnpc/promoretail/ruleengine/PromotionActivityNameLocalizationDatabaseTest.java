package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PromotionActivityNameLocalizationDatabaseTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyStoredRuleNameContainsChineseAndGeneratedEnglishNamesAreGone() {
        Integer namesWithoutChinese = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select rule_json ->> 'activityName' as activity_name from promotion_rule_draft
                    union all
                    select rule_json ->> 'activityName' as activity_name from promotion_rule_version
                ) names
                where activity_name !~ '[一-龥]'
                """, Integer.class);
        Integer generatedEnglishNames = jdbcTemplate.queryForObject("""
                select count(*)
                from (
                    select rule_json ->> 'activityName' as activity_name from promotion_rule_draft
                    union all
                    select rule_json ->> 'activityName' as activity_name from promotion_rule_version
                ) names
                where activity_name ~* '(safe-price|single item promotion|super recharge|small recharge)'
                   or activity_name ~ '-(GASOLINE|DIESEL)$'
                """, Integer.class);

        assertThat(namesWithoutChinese).isZero();
        assertThat(generatedEnglishNames).isZero();
    }

    @Test
    void g7RulesUseProductNamesAndOtherGeneratedActivitiesAreLocalized() {
        Map<String, String> namesByRuleId = promotionRuleRepository.findConfirmedRules().stream()
                .collect(Collectors.toMap(rule -> rule.ruleId(), rule -> rule.activityName(), (left, right) -> left));

        assertThat(namesByRuleId.get("audit-personalized-fixed-70001573"))
                .isEqualTo("非非促销-单品安全价-可口可乐 汽水 500ML");
        assertThat(namesByRuleId.get("audit-personalized-fixed-70235652"))
                .isEqualTo("非非促销-单品安全价-东鹏 特饮罐装 250ML");
        assertThat(namesByRuleId.get("abv2-a5-day10-super-1000-gold"))
                .isEqualTo("超级十惠-黄金及以上客户单笔充值1000元");
        assertThat(namesByRuleId.get("abv2-a6-small-recharge-666"))
                .isEqualTo("非十惠日小额充值666元赠券包");
        assertThat(namesByRuleId.get("abv2-h2-small-water-gasoline"))
                .isEqualTo("加油换购-小水4瓶-汽油");
    }
}
