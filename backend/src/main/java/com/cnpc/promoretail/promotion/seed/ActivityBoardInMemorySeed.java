package com.cnpc.promoretail.promotion.seed;

import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev-db & !postgres")
public class ActivityBoardInMemorySeed implements ApplicationRunner {

    private static final String SOURCE_IMPORT_ID = "activity-board-v2-in-memory";

    private final ProductCatalogRepository productCatalogRepository;
    private final PromotionRuleRepository promotionRuleRepository;

    public ActivityBoardInMemorySeed(
            ProductCatalogRepository productCatalogRepository,
            PromotionRuleRepository promotionRuleRepository
    ) {
        this.productCatalogRepository = productCatalogRepository;
        this.promotionRuleRepository = promotionRuleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProducts();
        seedExchangeRules();
    }

    private void seedProducts() {
        ImportVersion priceVersion = new ImportVersion("activity-board-v2-in-memory-price");
        ImportVersion inventoryVersion = new ImportVersion("activity-board-v2-in-memory-inventory");
        List<ProductSeed> products = List.of(
                new ProductSeed("70545523", "格桑泉 蓝格饮用天然矿泉水 330ML", "6944483900089", "2.50", "30"),
                new ProductSeed("70545526", "格桑泉 蓝格天然饮用水 500ML", "6944483933322", "3.00", "403"),
                new ProductSeed("70356177", "红牛 维生素风味饮料 250ML", "6970640429988", "6.00", "5747"),
                new ProductSeed("70727173", "优斯麦尔 香梨复合果汁饮品 1L", "6972515373471", "15.50", "30"),
                new ProductSeed("70727875", "优斯麦尔 香梨复合果汁饮品 0.3L", "6972515373457", "5.50", "30"),
                new ProductSeed("70559364", "天润 优斯麦尔佳丽新疆纯牛奶康美砖 200G", "6970372689490", "3.00", "30")
        );
        productCatalogRepository.savePriceRows(priceVersion, products.stream()
                .map(product -> new PriceImportRow(product.productCode(), product.productName(), product.barcode(),
                        money(product.price()), "加油换购（统建）", 1))
                .toList());
        productCatalogRepository.saveInventoryRows(inventoryVersion, products.stream()
                .map(product -> new InventoryImportRow(product.productCode(), product.productName(), product.barcode(),
                        money(product.quantity()), "加油换购（统建）", 1))
                .toList());
    }

    private void seedExchangeRules() {
        List<ExchangeRuleSeed> rules = List.of(
                new ExchangeRuleSeed("abv2-h2-small-water-gasoline", "加油换购-小水4瓶-汽油",
                        "70545523", FuelType.GASOLINE, "180.00", "2.00", 4, 13),
                new ExchangeRuleSeed("abv2-h2-small-water-diesel", "加油换购-小水4瓶-柴油",
                        "70545523", FuelType.DIESEL, "300.00", "2.00", 4, 13),
                new ExchangeRuleSeed("abv2-h2-big-water-gasoline", "加油换购-大水4瓶-汽油",
                        "70545526", FuelType.GASOLINE, "180.00", "4.00", 4, 14),
                new ExchangeRuleSeed("abv2-h2-big-water-diesel", "加油换购-大水4瓶-柴油",
                        "70545526", FuelType.DIESEL, "300.00", "4.00", 4, 14),
                new ExchangeRuleSeed("abv2-h2-redbull-gasoline", "加油换购-红牛3罐-汽油",
                        "70356177", FuelType.GASOLINE, "180.00", "12.00", 3, 15),
                new ExchangeRuleSeed("abv2-h2-redbull-diesel", "加油换购-红牛3罐-柴油",
                        "70356177", FuelType.DIESEL, "300.00", "12.00", 3, 15),
                new ExchangeRuleSeed("abv2-h2-juice-1l-gasoline", "加油换购-优斯麦尔果汁1L-汽油",
                        "70727173", FuelType.GASOLINE, "180.00", "9.90", 1, 16),
                new ExchangeRuleSeed("abv2-h2-juice-1l-diesel", "加油换购-优斯麦尔果汁1L-柴油",
                        "70727173", FuelType.DIESEL, "300.00", "9.90", 1, 16),
                new ExchangeRuleSeed("abv2-h2-juice-03l-gasoline", "加油换购-优斯麦尔果汁0.3L两瓶-汽油",
                        "70727875", FuelType.GASOLINE, "180.00", "9.90", 2, 17),
                new ExchangeRuleSeed("abv2-h2-juice-03l-diesel", "加油换购-优斯麦尔果汁0.3L两瓶-柴油",
                        "70727875", FuelType.DIESEL, "300.00", "9.90", 2, 17),
                new ExchangeRuleSeed("abv2-h2-milk-200-gasoline", "加油换购-自有牛奶200g-汽油",
                        "70559364", FuelType.GASOLINE, "180.00", "19.90", 1, 19),
                new ExchangeRuleSeed("abv2-h2-milk-200-diesel", "加油换购-自有牛奶200g-柴油",
                        "70559364", FuelType.DIESEL, "300.00", "19.90", 1, 19)
        );
        rules.forEach(this::saveRule);
    }

    private void saveRule(ExchangeRuleSeed seed) {
        PromotionCondition condition = new PromotionCondition(
                Set.of(seed.productCode()),
                Set.of(),
                Set.of(seed.fuelType()),
                Set.of(),
                Set.of(),
                null,
                null,
                BigDecimal.ZERO,
                money(seed.minFuelAmount()),
                false,
                BigDecimal.ZERO
        );
        PromotionRule rule = new PromotionRule(
                seed.ruleId(),
                seed.activityName(),
                PromotionRuleType.EXCHANGE_PURCHASE,
                76,
                "exchange_purchase",
                true,
                PromotionRuleStatus.CONFIRMED,
                condition,
                PromotionBenefit.exchangePurchase(money(seed.exchangePrice()), seed.exchangeQuantity()),
                "activity-board-v2"
        );
        promotionRuleRepository.saveDraft(new PromotionRuleDraft(
                "draft-" + seed.ruleId(),
                rule,
                SOURCE_IMPORT_ID,
                "加油换购（统建）",
                seed.sourceRowNumber(),
                PromotionRuleStatus.CONFIRMED,
                true,
                Instant.now(),
                Instant.now(),
                "in-memory-seed"
        ), true);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private record ProductSeed(
            String productCode,
            String productName,
            String barcode,
            String price,
            String quantity
    ) {
    }

    private record ExchangeRuleSeed(
            String ruleId,
            String activityName,
            String productCode,
            FuelType fuelType,
            String minFuelAmount,
            String exchangePrice,
            int exchangeQuantity,
            int sourceRowNumber
    ) {
    }
}
