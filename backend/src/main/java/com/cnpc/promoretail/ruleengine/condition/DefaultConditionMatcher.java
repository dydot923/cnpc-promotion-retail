package com.cnpc.promoretail.ruleengine.condition;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTriggerRepository;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DefaultConditionMatcher implements ConditionMatcher {

    private final DateConditionMatcher dateConditionMatcher;
    private final TimeRangeConditionMatcher timeRangeConditionMatcher = new TimeRangeConditionMatcher();
    private final StationTypeConditionMatcher stationTypeConditionMatcher = new StationTypeConditionMatcher();
    private final ProvinceConditionMatcher provinceConditionMatcher = new ProvinceConditionMatcher();
    private final MemberConditionMatcher memberConditionMatcher = new MemberConditionMatcher();
    private final RechargeAmountConditionMatcher rechargeAmountConditionMatcher = new RechargeAmountConditionMatcher();

    public DefaultConditionMatcher() {
        this(PromotionDateTriggerRepository.empty());
    }

    public DefaultConditionMatcher(PromotionDateTriggerRepository dateTriggerRepository) {
        this.dateConditionMatcher = new DateConditionMatcher(dateTriggerRepository);
    }

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

        reasons.addAll(dateConditionMatcher.mismatchReasons(context, rule));
        reasons.addAll(timeRangeConditionMatcher.mismatchReasons(context, condition));
        reasons.addAll(stationTypeConditionMatcher.mismatchReasons(context, condition));
        reasons.addAll(provinceConditionMatcher.mismatchReasons(context, condition));
        reasons.addAll(memberConditionMatcher.mismatchReasons(context, condition));
        reasons.addAll(rechargeAmountConditionMatcher.mismatchReasons(context, condition));

        if (!condition.fuelTypes().isEmpty()
                && !condition.fuelTypes().contains(context.fuel().fuelType())) {
            reasons.add("当前油品类型不满足活动要求。");
        }

        if (condition.minFuelAmount().compareTo(BigDecimal.ZERO) > 0
                && context.fuel().amount().compareTo(condition.minFuelAmount()) < 0) {
            reasons.add("当前油品消费金额未满 "
                    + condition.minFuelAmount().stripTrailingZeros().toPlainString() + " 元。");
        }

        if (condition.minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && CartTotals.from(context).originalAmount().compareTo(condition.minCartAmount()) < 0) {
            reasons.add("当前购物车金额未达到活动门槛。");
        }

        if (!condition.productCodes().isEmpty() && context.cartItems().stream()
                .noneMatch(item -> item.matchesProductScope(condition.productCodes()))) {
            reasons.add("商品不在活动范围内。");
        }

        if (!condition.includedCategories().isEmpty() && context.cartItems().stream()
                .filter(item -> item.matchesProductScope(condition.productCodes()))
                .noneMatch(item -> item.includedByCategory(condition.includedCategories()))) {
            reasons.add("商品品类不在活动范围内。");
        }

        boolean allEligibleItemsExcluded = context.cartItems().stream()
                .filter(item -> item.matchesProductScope(condition.productCodes()))
                .filter(item -> item.includedByCategory(condition.includedCategories()))
                .allMatch(item -> item.excludedByCategory(condition.excludedCategories()));
        if (!context.cartItems().isEmpty() && allEligibleItemsExcluded) {
            reasons.add("活动适用商品均属于排除品类。");
        }

        if (condition.minProductQuantity() > 0) {
            int scopedQuantity = context.cartItems().stream()
                    .filter(item -> item.matchesProductScope(condition.productCodes()))
                    .filter(item -> item.includedByCategory(condition.includedCategories()))
                    .filter(item -> !item.excludedByCategory(condition.excludedCategories()))
                    .mapToInt(CartItem::quantity)
                    .sum();
            if (scopedQuantity < condition.minProductQuantity()) {
                reasons.add("适用商品数量未达到活动门槛。");
            }
        }

        for (CartItem item : context.cartItems()) {
            if (!item.matchesProductScope(condition.productCodes())) {
                continue;
            }
            if (!item.includedByCategory(condition.includedCategories())) {
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
