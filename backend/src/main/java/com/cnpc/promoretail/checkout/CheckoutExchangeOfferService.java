package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
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

    public CheckoutExchangeOfferService(
            PromotionRuleRepository promotionRuleRepository,
            ProductCatalogRepository productCatalogRepository,
            ConditionMatcher conditionMatcher
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.productCatalogRepository = productCatalogRepository;
        this.conditionMatcher = conditionMatcher;
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
                .filter(rule -> rule.ruleType() == PromotionRuleType.EXCHANGE_PURCHASE)
                .toList();
        Map<String, ProductCatalogItem> products = productsByCode(rules);

        return rules.stream()
                .flatMap(rule -> rule.condition().productCodes().stream()
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
                List.copyOf(blockedReasons)
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
