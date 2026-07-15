package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.inventory.InventoryQueryService;
import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.BundleDefinition;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BundlePriceBenefitCalculator extends AbstractBenefitCalculator {

    private final InventoryQueryService inventoryQueryService;
    private final BundleDefinitionProvider bundleDefinitionProvider;

    public BundlePriceBenefitCalculator() {
        this(productCode -> new BigDecimal("999999"));
    }

    public BundlePriceBenefitCalculator(InventoryQueryService inventoryQueryService) {
        this(inventoryQueryService, BundleDefinitionProvider.empty());
    }

    public BundlePriceBenefitCalculator(
            InventoryQueryService inventoryQueryService,
            BundleDefinitionProvider bundleDefinitionProvider
    ) {
        this.inventoryQueryService = inventoryQueryService;
        this.bundleDefinitionProvider = bundleDefinitionProvider == null
                ? BundleDefinitionProvider.empty()
                : bundleDefinitionProvider;
    }

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.BUNDLE_PRICE;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        BundleDefinition definition = resolveDefinition(rule);
        if (definition == null
                && rule.benefit().bundleId() != null
                && !rule.benefit().bundleId().isBlank()
                && rule.benefit().bundleItems().isEmpty()) {
            return BenefitCalculation.blocked(List.of("未找到组合包定义 " + rule.benefit().bundleId() + "。"));
        }
        BigDecimal bundlePrice = definition == null
                ? money(rule.benefit().bundlePrice())
                : definition.bundlePrice();
        if (bundlePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("组合包价格必须大于 0。"));
        }
        BigDecimal thresholdAmount = definition == null ? BigDecimal.ZERO : definition.thresholdAmount();
        if (thresholdAmount.compareTo(BigDecimal.ZERO) > 0
                && context.fuel().amount().compareTo(thresholdAmount) < 0) {
            return BenefitCalculation.blocked(List.of("油品消费未满门槛，当前 "
                    + money(context.fuel().amount()) + " 元，未满 " + thresholdAmount + " 元。"));
        }

        List<BundleItem> bundleItems = definition == null ? rule.benefit().bundleItems() : definition.items();
        if (bundleItems.isEmpty()) {
            return calculateByEligibleSubtotal(context, rule, totals, bundlePrice);
        }
        return calculateByBundleItems(context, rule, totals, bundlePrice, bundleItems);
    }

    private BundleDefinition resolveDefinition(PromotionRule rule) {
        String bundleId = rule.benefit().bundleId();
        if (bundleId == null || bundleId.isBlank() || !rule.benefit().bundleItems().isEmpty()) {
            return null;
        }
        return bundleDefinitionProvider.findActiveBundle(bundleId).orElse(null);
    }

    private BenefitCalculation calculateByEligibleSubtotal(
            OrderContext context,
            PromotionRule rule,
            CartTotals totals,
            BigDecimal bundlePrice
    ) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("购物车中没有组合包适用商品。"));
        }

        BigDecimal subtotal = eligibleSubtotal(items);
        BigDecimal discount = subtotal.subtract(bundlePrice).max(BigDecimal.ZERO);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("组合包价未低于组合商品原价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        return BenefitCalculation.available(candidate(rule, totals.originalAmount(), payable, discount,
                "命中组合包价，组合商品按 " + bundlePrice + " 元结算。"));
    }

    private BenefitCalculation calculateByBundleItems(
            OrderContext context,
            PromotionRule rule,
            CartTotals totals,
            BigDecimal bundlePrice,
            List<BundleItem> bundleItems
    ) {
        Map<String, CartItem> cartItemsByCode = context.cartItems().stream()
                .collect(Collectors.toMap(CartItem::productCode, Function.identity(), this::mergeQuantities));

        for (BundleItem bundleItem : bundleItems) {
            CartItem cartItem = cartItemsByCode.get(bundleItem.productCode());
            if (cartItem == null) {
                return BenefitCalculation.blocked(List.of("组合包缺少商品 " + bundleItem.productCode() + "。"));
            }
            if (cartItem.quantity() < bundleItem.quantity()) {
                return BenefitCalculation.blocked(List.of("组合包商品 " + bundleItem.productCode() + " 数量不足。"));
            }
            BigDecimal availableQuantity = inventoryQueryService.getAvailableQuantity(bundleItem.productCode());
            if (availableQuantity.compareTo(BigDecimal.valueOf(bundleItem.quantity())) < 0) {
                return BenefitCalculation.blocked(List.of("组合包商品库存不足，无法组装：商品 "
                        + bundleItem.productCode() + " 库存 " + availableQuantity.stripTrailingZeros().toPlainString()
                        + " 件，需要 " + bundleItem.quantity() + " 件。"));
            }
        }

        int bundleCount = bundleItems.stream()
                .mapToInt(bundleItem -> Math.min(
                        cartItemsByCode.get(bundleItem.productCode()).quantity() / bundleItem.quantity(),
                        inventoryQueryService.getAvailableQuantity(bundleItem.productCode())
                                .divideToIntegralValue(BigDecimal.valueOf(bundleItem.quantity()))
                                .intValue()
                ))
                .min()
                .orElse(0);
        if (bundleCount <= 0) {
            return BenefitCalculation.blocked(List.of("组合包商品库存不足，无法组装。"));
        }

        BigDecimal oneSetOriginal = bundleItems.stream()
                .map(bundleItem -> cartItemsByCode.get(bundleItem.productCode()).unitPrice()
                        .multiply(BigDecimal.valueOf(bundleItem.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBundleOriginal = oneSetOriginal.multiply(BigDecimal.valueOf(bundleCount));
        BigDecimal totalBundlePrice = bundlePrice.multiply(BigDecimal.valueOf(bundleCount));
        BigDecimal discount = money(totalBundleOriginal.subtract(totalBundlePrice).max(BigDecimal.ZERO));
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("组合包价未低于组合商品原价。"));
        }

        BigDecimal payable = totals.originalAmount().subtract(discount);
        Set<String> consumedCodes = bundleItems.stream()
                .map(BundleItem::productCode)
                .collect(Collectors.toUnmodifiableSet());
        return BenefitCalculation.available(new com.cnpc.promoretail.ruleengine.model.PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                payable,
                discount,
                List.of(),
                List.of(),
                "命中组合包价，可组成 " + bundleCount + " 套，优惠 " + discount + " 元。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority(),
                consumedCodes,
                Set.of()
        ));
    }

    private CartItem mergeQuantities(CartItem left, CartItem right) {
        CartItem first = List.of(left, right).stream()
                .min(Comparator.comparing(CartItem::lineId))
                .orElse(left);
        return new CartItem(first.lineId(), first.productCode(), first.barcode(), first.name(),
                left.quantity() + right.quantity(), first.unitPrice(), first.category(), first.inventoryQuantity());
    }
}
