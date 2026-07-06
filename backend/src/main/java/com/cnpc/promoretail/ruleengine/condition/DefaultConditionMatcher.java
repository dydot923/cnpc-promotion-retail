package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DefaultConditionMatcher implements ConditionMatcher {

    @Override
    public ConditionMatchResult match(OrderContext context, PromotionRule rule) {
        PromotionCondition condition = rule.condition();
        List<String> reasons = new ArrayList<>();

        if (context.businessDate() != null) {
            if (condition.startDate() != null && context.businessDate().isBefore(condition.startDate())) {
                reasons.add("当前营业日期早于活动开始日期。");
            }
            if (condition.endDate() != null && context.businessDate().isAfter(condition.endDate())) {
                reasons.add("当前营业日期晚于活动结束日期。");
            }
            if (!condition.daysOfMonth().isEmpty()
                    && !condition.daysOfMonth().contains(context.businessDate().getDayOfMonth())) {
                reasons.add("当前日期不在活动指定日期内。");
            }
        }

        if (condition.memberRequired() && !context.customer().member()) {
            reasons.add("当前顾客不是会员，未满足会员专享条件。");
        }

        if (!condition.stationTypes().isEmpty()
                && !condition.stationTypes().contains(context.station().stationType())) {
            reasons.add("当前站点类型不在活动适用范围内。");
        }

        if (!condition.fuelTypes().isEmpty()
                && !condition.fuelTypes().contains(context.fuel().fuelType())) {
            reasons.add("当前油品类型不满足活动要求。");
        }

        if (condition.minFuelAmount().compareTo(BigDecimal.ZERO) > 0
                && context.fuel().amount().compareTo(condition.minFuelAmount()) < 0) {
            reasons.add("当前油品消费金额未达到活动门槛。");
        }

        if (condition.minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && CartTotals.from(context).originalAmount().compareTo(condition.minCartAmount()) < 0) {
            reasons.add("当前购物车金额未达到活动门槛。");
        }

        if (!condition.productCodes().isEmpty() && context.cartItems().stream()
                .noneMatch(item -> item.matchesProductScope(condition.productCodes()))) {
            reasons.add("商品不在活动范围内。");
        }

        boolean allEligibleItemsExcluded = context.cartItems().stream()
                .filter(item -> item.matchesProductScope(condition.productCodes()))
                .allMatch(item -> item.excludedByCategory(condition.excludedCategories()));
        if (!context.cartItems().isEmpty() && allEligibleItemsExcluded) {
            reasons.add("活动适用商品均属于排除品类。");
        }

        for (CartItem item : context.cartItems()) {
            if (!item.matchesProductScope(condition.productCodes())) {
                continue;
            }
            if (condition.minInventoryQuantity().compareTo(BigDecimal.ZERO) > 0
                    && item.inventoryQuantity() != null
                    && item.inventoryQuantity().compareTo(condition.minInventoryQuantity()) < 0) {
                reasons.add("商品 " + item.productCode() + " 当前库存不足，不能推荐执行该促销。");
            }
        }

        return reasons.isEmpty() ? ConditionMatchResult.success() : ConditionMatchResult.blocked(reasons);
    }
}
