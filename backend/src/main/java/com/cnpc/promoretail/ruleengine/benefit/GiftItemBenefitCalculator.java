package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.inventory.InventoryQueryService;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.GiftItem;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;

public class GiftItemBenefitCalculator extends AbstractBenefitCalculator {

    private final InventoryQueryService inventoryQueryService;

    public GiftItemBenefitCalculator() {
        this(productCode -> new BigDecimal("999999"));
    }

    public GiftItemBenefitCalculator(InventoryQueryService inventoryQueryService) {
        this.inventoryQueryService = inventoryQueryService;
    }

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.GIFT_ITEM;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty() && requiresCartTrigger(rule.condition())) {
            return BenefitCalculation.blocked(List.of("没有满足买赠条件的商品。"));
        }
        BigDecimal eligibleSubtotal = eligibleSubtotal(items);
        if (!items.isEmpty()
                && rule.condition().minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && eligibleSubtotal.compareTo(rule.condition().minCartAmount()) < 0) {
            return BenefitCalculation.blocked(List.of("适用商品金额未达到买赠门槛。"));
        }
        if (!rule.benefit().giftItemOptions().isEmpty()) {
            return calculateOptions(rule, totals, items);
        }
        if (rule.benefit().giftItemCode() == null || rule.benefit().giftItemQuantity() <= 0) {
            return BenefitCalculation.blocked(List.of("赠品信息不完整。"));
        }
        if (!inventoryQueryService.hasEnough(rule.benefit().giftItemCode(),
                BigDecimal.valueOf(rule.benefit().giftItemQuantity()))) {
            return BenefitCalculation.blocked(List.of("赠品 " + rule.benefit().giftItemCode() + " 库存不足。"));
        }

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                totals.originalAmount(),
                BigDecimal.ZERO,
                List.of(new GiftItem(rule.benefit().giftItemCode(), rule.benefit().giftItemName(),
                        rule.benefit().giftItemQuantity())),
                List.of(),
                "满足买赠条件，应赠送指定赠品。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority(),
                consumedProductCodes(rule, items),
                java.util.Set.of()
        );
        return BenefitCalculation.available(candidate);
    }

    private BenefitCalculation calculateOptions(PromotionRule rule, CartTotals totals, List<CartItem> items) {
        java.util.List<PromotionCandidate> candidates = new java.util.ArrayList<>();
        java.util.List<String> blockedReasons = new java.util.ArrayList<>();
        int optionIndex = 1;
        for (List<GiftItem> option : rule.benefit().giftItemOptions()) {
            if (option.isEmpty()) {
                blockedReasons.add("赠品选项 " + optionIndex + " 为空。");
                optionIndex++;
                continue;
            }
            List<String> shortageReasons = option.stream()
                    .filter(gift -> !inventoryQueryService.hasEnough(gift.productCode(), BigDecimal.valueOf(gift.quantity())))
                    .map(gift -> "赠品 " + gift.productCode() + " 库存不足。")
                    .toList();
            if (!shortageReasons.isEmpty()) {
                blockedReasons.addAll(shortageReasons);
                optionIndex++;
                continue;
            }
            candidates.add(new PromotionCandidate(
                    "cand-" + rule.ruleId() + "-option" + optionIndex,
                    rule.ruleId(),
                    rule.activityName(),
                    rule.ruleType(),
                    totals.originalAmount(),
                    totals.originalAmount(),
                    BigDecimal.ZERO,
                    option,
                    List.of(),
                    "满足买赠条件，可选择赠品方案 " + optionIndex + "。",
                    rule.version(),
                    rule.exclusiveGroup(),
                    rule.stackable(),
                    rule.priority(),
                    consumedProductCodes(rule, items),
                    java.util.Set.of()
            ));
            optionIndex++;
        }
        if (candidates.isEmpty()) {
            return BenefitCalculation.blocked(blockedReasons);
        }
        return BenefitCalculation.mixed(candidates, blockedReasons);
    }

    private boolean requiresCartTrigger(PromotionCondition condition) {
        return !condition.productCodes().isEmpty()
                || !condition.includedCategories().isEmpty()
                || condition.minCartAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
