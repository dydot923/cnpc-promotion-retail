package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.ConditionMatchResult;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CheckoutExchangeOfferService {

    private final PromotionRuleRepository promotionRuleRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final ConditionMatcher conditionMatcher;
    private final BundleDefinitionProvider bundleDefinitionProvider;

    public CheckoutExchangeOfferService(
            PromotionRuleRepository promotionRuleRepository,
            ProductCatalogRepository productCatalogRepository,
            ConditionMatcher conditionMatcher,
            BundleDefinitionProvider bundleDefinitionProvider
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.productCatalogRepository = productCatalogRepository;
        this.conditionMatcher = conditionMatcher;
        this.bundleDefinitionProvider = bundleDefinitionProvider == null
                ? BundleDefinitionProvider.empty()
                : bundleDefinitionProvider;
    }

    public List<CheckoutExchangeOfferResponse> findOffers(
            FuelType fuelType,
            BigDecimal fuelAmount,
            LocalDate businessDate,
            String stationType,
            String stationProvince
    ) {
        FuelType effectiveFuelType = fuelType == null ? FuelType.GASOLINE : fuelType;
        BigDecimal effectiveFuelAmount = money(fuelAmount);
        LocalDate effectiveBusinessDate = businessDate == null ? LocalDate.now() : businessDate;

        List<PromotionRule> rules = promotionRuleRepository.findConfirmedRules().stream()
                .filter(rule -> (rule.ruleType() == PromotionRuleType.EXCHANGE_PURCHASE
                        && rule.ruleId().startsWith("abv2-h2-"))
                        || (rule.ruleType() == PromotionRuleType.BUNDLE_PRICE
                        && rule.ruleId().startsWith("abv2-bundle-")))
                .toList();
        Map<String, ProductCatalogItem> products = productsByCode(rules);

        return rules.stream()
                .flatMap(rule -> rule.ruleType() == PromotionRuleType.BUNDLE_PRICE
                        ? java.util.stream.Stream.of(toBundleOffer(rule, products, effectiveFuelType,
                                effectiveFuelAmount, effectiveBusinessDate, stationType, stationProvince))
                        : rule.condition().productCodes().stream()
                                .map(productCode -> toOffer(rule, productCode, products.get(productCode), effectiveFuelType,
                                        effectiveFuelAmount, effectiveBusinessDate, stationType, stationProvince)))
                .sorted(Comparator.comparing(CheckoutExchangeOfferResponse::eligible).reversed()
                        .thenComparing(CheckoutExchangeOfferResponse::minFuelAmount)
                        .thenComparing(CheckoutExchangeOfferResponse::productCode))
                .toList();
    }

    private CheckoutExchangeOfferResponse toOffer(
            PromotionRule rule,
            String productCode,
            ProductCatalogItem product,
            FuelType fuelType,
            BigDecimal fuelAmount,
            LocalDate businessDate,
            String stationType,
            String stationProvince
    ) {
        int exchangeQuantity = rule.benefit().exchangeQuantity() <= 0 ? 1 : rule.benefit().exchangeQuantity();
        BigDecimal unitPrice = product == null ? BigDecimal.ZERO : money(product.unitPrice());
        BigDecimal exchangePrice = money(rule.benefit().exchangePrice());
        BigDecimal inventoryQuantity = product == null ? BigDecimal.ZERO : product.inventoryQuantity();
        List<String> blockedReasons = new ArrayList<>();

        if (product == null) {
            blockedReasons.add("商品目录缺少换购商品：" + productCode);
        }

        OrderContext context = new OrderContext(
                new StationContext("station-001", blankDefault(stationType, "gas_station"),
                        blankDefault(stationProvince, "新疆")),
                CustomerContext.anonymous(),
                new FuelContext(fuelType, "", fuelAmount, BigDecimal.ZERO),
                List.of(new CartItem("offer-" + productCode, productCode, product == null ? null : product.barcode(),
                        product == null ? productCode : product.productName(), exchangeQuantity, unitPrice,
                        product == null ? null : product.category(), inventoryQuantity)),
                businessDate,
                LocalTime.now()
        );

        ConditionMatchResult matchResult = conditionMatcher.match(context, rule);
        blockedReasons.addAll(matchResult.blockedReasons());

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            blockedReasons.add("商品当前执行价缺失，不能形成真实换购优惠。");
        }
        boolean packagePrice = rule.benefit().exchangeQuantity() > 1
                && exchangePrice.compareTo(unitPrice) >= 0;
        if (!packagePrice && exchangePrice.compareTo(unitPrice) >= 0) {
            blockedReasons.add("换购价未低于当前执行价。");
        }
        if (inventoryQuantity.compareTo(BigDecimal.valueOf(exchangeQuantity)) < 0) {
            blockedReasons.add("换购商品库存不足，需要 " + exchangeQuantity + " 件。");
        }

        BigDecimal estimatedDiscount = (packagePrice
                ? unitPrice.multiply(BigDecimal.valueOf(exchangeQuantity)).subtract(exchangePrice)
                : unitPrice.subtract(exchangePrice).max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(exchangeQuantity)))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new CheckoutExchangeOfferResponse(
                rule.ruleId(),
                rule.activityName(),
                rule.version(),
                "ITEM",
                productCode,
                product == null ? productCode : product.productName(),
                product == null ? null : product.barcode(),
                product == null ? null : product.category(),
                unitPrice,
                exchangePrice,
                exchangeQuantity,
                money(rule.condition().minFuelAmount()),
                rule.condition().fuelTypes().stream().sorted().toList(),
                estimatedDiscount,
                inventoryQuantity,
                blockedReasons.isEmpty(),
                List.copyOf(blockedReasons),
                List.of()
        );
    }

    private CheckoutExchangeOfferResponse toBundleOffer(
            PromotionRule rule,
            Map<String, ProductCatalogItem> products,
            FuelType fuelType,
            BigDecimal fuelAmount,
            LocalDate businessDate,
            String stationType,
            String stationProvince
    ) {
        List<String> blockedReasons = new ArrayList<>();
        var definition = bundleDefinitionProvider.findActiveBundle(rule.benefit().bundleId()).orElse(null);
        if (definition == null || definition.items().isEmpty()) {
            blockedReasons.add("组合包配置缺失：" + rule.benefit().bundleId());
        }

        List<CheckoutExchangeOfferResponse.CheckoutExchangeOfferItem> items = new ArrayList<>();
        BigDecimal originalPrice = BigDecimal.ZERO;
        BigDecimal availableSets = null;
        if (definition != null) {
            for (var bundleItem : definition.items()) {
                ProductCatalogItem product = products.get(bundleItem.productCode());
                if (product == null) {
                    blockedReasons.add("商品目录缺少组合包商品：" + bundleItem.productCode());
                    continue;
                }
                BigDecimal inventory = product.inventoryQuantity() == null
                        ? BigDecimal.ZERO
                        : product.inventoryQuantity();
                int quantity = bundleItem.quantity();
                items.add(new CheckoutExchangeOfferResponse.CheckoutExchangeOfferItem(
                        product.productCode(), product.productName(), product.barcode(), product.category(),
                        money(product.unitPrice()), quantity, inventory));
                originalPrice = originalPrice.add(money(product.unitPrice())
                        .multiply(BigDecimal.valueOf(quantity)));
                BigDecimal productSets = inventory.divide(BigDecimal.valueOf(quantity), 0, RoundingMode.DOWN);
                availableSets = availableSets == null ? productSets : availableSets.min(productSets);
                // A bundle is presented even when one item is temporarily short so the cashier can
                // see the exact board rule and the reason it cannot be added right now.
                if (inventory.compareTo(BigDecimal.valueOf(quantity)) < 0) {
                    blockedReasons.add("组合包商品库存不足：" + product.productCode()
                            + "，需要 " + quantity + " 件。");
                }
            }
        }

        BigDecimal minFuelAmount = definition == null
                ? money(rule.condition().minFuelAmount())
                : money(definition.thresholdAmount().max(rule.condition().minFuelAmount()));
        if (!rule.condition().fuelTypes().isEmpty() && !rule.condition().fuelTypes().contains(fuelType)) {
            blockedReasons.add("当前油品类型不满足活动要求。");
        }
        if (fuelAmount.compareTo(minFuelAmount) < 0) {
            blockedReasons.add("当前油品消费金额未满 " + minFuelAmount.stripTrailingZeros().toPlainString() + " 元。");
        }
        BigDecimal exchangePrice = definition == null ? money(rule.benefit().bundlePrice()) : money(definition.bundlePrice());
        BigDecimal estimatedDiscount = originalPrice.subtract(exchangePrice).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (estimatedDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            blockedReasons.add("组合包价未低于组合商品原价。");
        }
        return new CheckoutExchangeOfferResponse(
                rule.ruleId(), rule.activityName(), rule.version(), "BUNDLE",
                rule.benefit().bundleId(), definition == null ? rule.activityName() : definition.name(),
                null, "组合包", originalPrice, exchangePrice,
                1, minFuelAmount, rule.condition().fuelTypes().stream().sorted().toList(),
                estimatedDiscount, availableSets == null ? BigDecimal.ZERO : availableSets,
                blockedReasons.isEmpty(), List.copyOf(blockedReasons), List.copyOf(items)
        );
    }

    private Map<String, ProductCatalogItem> productsByCode(List<PromotionRule> rules) {
        List<String> productCodes = rules.stream()
                .flatMap(rule -> rule.condition().productCodes().stream())
                .distinct()
                .toList();
        Map<String, ProductCatalogItem> products = new HashMap<>();
        productCatalogRepository.findByProductCodes(productCodes)
                .forEach(product -> products.put(product.productCode(), product));
        return products;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
