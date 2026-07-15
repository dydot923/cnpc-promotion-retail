package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MemberConditionMatcher {

    private static final Map<String, Integer> LEVEL_PRIORITY = Map.ofEntries(
            Map.entry("normal", 1),
            Map.entry("ordinary", 1),
            Map.entry("普通", 1),
            Map.entry("普通会员", 1),
            Map.entry("silver", 2),
            Map.entry("银卡", 2),
            Map.entry("银卡会员", 2),
            Map.entry("gold", 3),
            Map.entry("金卡", 3),
            Map.entry("金卡会员", 3),
            Map.entry("黄金", 3),
            Map.entry("platinum", 4),
            Map.entry("铂金", 4),
            Map.entry("铂金会员", 4)
    );

    public List<String> mismatchReasons(OrderContext context, PromotionCondition condition) {
        List<String> reasons = new ArrayList<>();
        if (!condition.memberLevels().isEmpty() && !matchesMemberLevel(context, condition.memberLevels())) {
            reasons.add("会员等级不满足活动要求");
        }
        if (condition.birthdayMonthRequired()) {
            Integer birthMonth = context.customer().memberBirthMonth();
            if (!context.customer().member()) {
                reasons.add("当前顾客不是会员，不能参与会员生日活动");
            } else if (birthMonth == null || context.transactionDate() == null
                    || birthMonth != context.transactionDate().getMonthValue()) {
                reasons.add("当前月份不是会员生日月");
            }
        }
        if (!condition.memberTags().isEmpty() && !matchesMemberTags(context, condition.memberTags())) {
            reasons.add("会员标签不满足活动要求");
        }
        return reasons;
    }

    private boolean matchesMemberLevel(OrderContext context, Set<String> requiredLevels) {
        if (context.customer() == null || !context.customer().member()) {
            return false;
        }
        String currentLevel = normalize(context.customer().memberLevel());
        if (currentLevel.isBlank()) {
            return false;
        }
        Set<String> normalizedRequired = requiredLevels.stream()
                .map(this::normalize)
                .filter(level -> !level.isBlank())
                .collect(Collectors.toSet());
        if (normalizedRequired.contains(currentLevel)) {
            return true;
        }

        Integer currentPriority = levelPriority(currentLevel, context.customer().memberLevelPriority());
        if (currentPriority == null) {
            return false;
        }
        if (normalizedRequired.size() == 1) {
            return normalizedRequired.stream()
                    .map(this::minimumLevelCode)
                    .map(required -> levelPriority(required, null))
                    .anyMatch(requiredPriority -> requiredPriority != null && currentPriority >= requiredPriority);
        }
        return normalizedRequired.stream()
                .filter(this::isMinimumLevelExpression)
                .map(this::minimumLevelCode)
                .map(required -> levelPriority(required, null))
                .anyMatch(requiredPriority -> requiredPriority != null && currentPriority >= requiredPriority);
    }

    private boolean matchesMemberTags(OrderContext context, Set<String> requiredTags) {
        if (context.customer() == null || !context.customer().member()) {
            return false;
        }
        Set<String> memberTags = context.customer().memberTags().stream()
                .map(this::normalize)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toSet());
        if (memberTags.isEmpty()) {
            return false;
        }
        return requiredTags.stream()
                .map(this::normalize)
                .anyMatch(memberTags::contains);
    }

    private Integer levelPriority(String levelCode, Integer fallback) {
        Integer priority = LEVEL_PRIORITY.get(normalize(levelCode));
        return priority == null ? fallback : priority;
    }

    private boolean isMinimumLevelExpression(String levelCode) {
        return levelCode.endsWith("+")
                || levelCode.startsWith("min:")
                || levelCode.startsWith("at_least:")
                || levelCode.endsWith("_and_above")
                || levelCode.endsWith("-and-above")
                || levelCode.endsWith("及以上");
    }

    private String minimumLevelCode(String levelCode) {
        String normalized = normalize(levelCode);
        if (normalized.endsWith("+")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("min:")) {
            return normalized.substring("min:".length());
        }
        if (normalized.startsWith("at_least:")) {
            return normalized.substring("at_least:".length());
        }
        if (normalized.endsWith("_and_above")) {
            return normalized.substring(0, normalized.length() - "_and_above".length());
        }
        if (normalized.endsWith("-and-above")) {
            return normalized.substring(0, normalized.length() - "-and-above".length());
        }
        if (normalized.endsWith("及以上")) {
            return normalized.substring(0, normalized.length() - "及以上".length());
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
