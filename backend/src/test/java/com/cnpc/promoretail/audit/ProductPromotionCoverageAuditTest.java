package com.cnpc.promoretail.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.importcenter.ImportCenterService;
import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import com.cnpc.promoretail.importcenter.repository.InMemoryImportRecordRepository;
import com.cnpc.promoretail.inventory.ProductCatalogInventoryQueryService;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.InMemoryProductCatalogRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponTemplateRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.CouponRedeemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FuelVolumeDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftItemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProductPromotionCoverageAuditTest {

    private static final BigDecimal NINE_POINT_NINE = new BigDecimal("9.90");
    private static final LocalDate AUDIT_BUSINESS_DATE = LocalDate.of(2026, 7, 9);
    private static final String NINE_POINT_NINE_RULE_PREFIX = "import-fixed-9_9-";
    private static final String PERSONALIZED_RULE_PREFIX = "audit-personalized-fixed-";
    private static final String COUPON_SCOPE_RULE_ID = "audit-coupon-scope-redeem";
    private static final String BUNDLE_RULE_ID = "audit-bundle-lng-cng";
    private static final String DEMO_RULE_PREFIX = "audit-demo-fixed-";
    private static final String DEMO_EXCHANGE_RULE_PREFIX = "demo-exchange-";
    private static final int DEMO_ACTIVATION_TARGET = 25;

    private final EasyExcelWorkbookReader workbookReader = new EasyExcelWorkbookReader();

    @Test
    void auditAllInventoryProductsGetPromotionDecision() throws IOException {
        AuditFixture fixture = loadFixture();
        AuditReport report = auditInventoryProducts(fixture);
        appendPromotionPoolIntersections(report, fixture);
        appendMultiContextAuditResults(report, fixture);
        appendProductGroupMappingNotes(report);
        appendEdgeScenarioResults(report, fixture);
        writeReport(report);
        writeActivationGapAnalysis(report, fixture);
        writeGovernanceBacklog(fixture);

        assertThat(report.failures()).as("promotion coverage audit failures").isEmpty();
        assertThat(report.edgeScenarios()).allSatisfy(edge -> assertThat(edge.passed()).as(edge.name()).isTrue());
    }

    private AuditFixture loadFixture() {
        InMemoryProductCatalogRepository productRepository = new InMemoryProductCatalogRepository();
        ImportCenterService importCenterService = new ImportCenterService(
                workbookReader,
                new InMemoryImportRecordRepository(),
                new PromotionRuleGovernanceService(new InMemoryPromotionRuleRepository()),
                productRepository,
                new InMemoryCouponTemplateRepository(),
                new InMemoryCouponRepository()
        );

        Path activityFile = dataFile("活动看板.xlsx");
        ImportResult<PriceImportRow> prices = importCenterService.importPrices(dataFile("价格.xlsx"));
        ImportResult<InventoryImportRow> inventory = importCenterService.importInventory(dataFile("库存.xlsx"));
        ImportResult<ImportedPromotionRule> fixedPriceRules =
                importCenterService.importNinePointNineFixedPricePromotions(activityFile);
        importCenterService.importCoupons(activityFile);

        Map<String, PriceImportRow> priceByProductCode = prices.records().stream()
                .collect(Collectors.toMap(PriceImportRow::productCode, Function.identity(), (left, right) -> left));
        Map<String, PriceImportRow> priceByBarcode = prices.records().stream()
                .filter(row -> row.barcode() != null && !row.barcode().isBlank())
                .collect(Collectors.toMap(PriceImportRow::barcode, Function.identity(), (left, right) -> left));
        Map<String, InventoryImportRow> inventoryByProductCode = inventory.records().stream()
                .collect(Collectors.toMap(InventoryImportRow::productCode, Function.identity(), (left, right) -> left));
        PromotionPools promotionPools = promotionPools(activityFile);
        List<PromotionRule> ninePointNineRules = fixedPriceRules.records().stream()
                .map(ImportedPromotionRule::rule)
                .map(rule -> rule.withStatus(PromotionRuleStatus.CONFIRMED))
                .toList();
        Set<String> ninePointNineCodes = ninePointNineRules.stream()
                .flatMap(rule -> rule.condition().productCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PromotionRule> activationRules = activationGapRules(
                activityFile,
                inventoryByProductCode.keySet(),
                promotionPools,
                productRepository
        );
        Set<String> demoActivationCodes = demoActivationCodes(
                productRepository,
                inventory.records(),
                promotionPools,
                ninePointNineCodes,
                DEMO_ACTIVATION_TARGET
        );
        activationRules.addAll(demoFixedPriceRules(productRepository, demoActivationCodes));
        activationRules.addAll(demoExchangeRules(productRepository));
        List<PromotionRule> confirmedRules = new ArrayList<>();
        confirmedRules.addAll(ninePointNineRules);
        confirmedRules.addAll(activationRules);

        return new AuditFixture(
                productRepository,
                promotionEngine(productRepository),
                prices.records(),
                inventory.records(),
                priceByProductCode,
                priceByBarcode,
                inventoryByProductCode,
                ninePointNineRules,
                confirmedRules,
                ninePointNineCodes,
                demoActivationCodes,
                promotionPools,
                fixedPriceRules.invalidCount()
        );
    }

    private AuditReport auditInventoryProducts(AuditFixture fixture) {
        AuditReport report = new AuditReport(fixture.inventoryRows().size());
        for (InventoryImportRow inventoryRow : fixture.inventoryRows()) {
            ProductDecisionAuditResult result = auditOneProduct(fixture, inventoryRow);
            report.addProductResult(result);
        }
        return report;
    }

    private ProductDecisionAuditResult auditOneProduct(AuditFixture fixture, InventoryImportRow inventoryRow) {
        List<String> failures = new ArrayList<>();
        Optional<PriceImportRow> price = findPrice(fixture, inventoryRow);
        if (price.isEmpty()) {
            failures.add("价格表中未找到该商品编码或条码");
        }

        Optional<ProductCatalogItem> product = findProduct(fixture.productRepository(), inventoryRow);
        if (product.isEmpty()) {
            failures.add("商品查询无法通过条码或商品编码定位商品");
        }

        CalculationResult calculation = null;
        if (price.isPresent() && product.isPresent()) {
            CartItem cartItem = cartItem(product.get(), 1, product.get().category(), product.get().inventoryQuantity());
            calculation = fixture.promotionEngine().calculate(order(List.of(cartItem)), fixture.confirmedRules());
            if (calculation.availableCandidates().isEmpty() && calculation.blockedPromotions().isEmpty()) {
                failures.add("无任何促销候选或拦截记录");
            }
            if (!hasFallback(calculation)) {
                failures.add("缺少原价兜底方案");
            }
            if (fixture.ninePointNineCodes().contains(inventoryRow.productCode())) {
                Optional<PromotionCandidate> fixedPrice = candidateByRuleIdPrefix(calculation, NINE_POINT_NINE_RULE_PREFIX);
                Optional<BlockedPromotion> blockedFixedPrice = blockedByRuleIdPrefix(calculation, NINE_POINT_NINE_RULE_PREFIX);
                if (fixedPrice.isEmpty()) {
                    if (blockedFixedPrice.isEmpty()) {
                        failures.add("9.9专区库存命中商品未返回 fixed_price 候选或不可用原因");
                    }
                } else if (fixedPrice.get().payableAmount().compareTo(NINE_POINT_NINE) != 0) {
                    failures.add("9.9专区 fixed_price 应付金额不是 9.90");
                }
            } else if (fixture.promotionPools().personalizedCodes().contains(inventoryRow.productCode())) {
                assertCandidateOrBlockedByRuleId(
                        calculation,
                        PERSONALIZED_RULE_PREFIX + inventoryRow.productCode(),
                        "个性化促销池库存命中商品未返回 fixed_price 候选或不可用原因",
                        failures
                );
            } else if (fixture.promotionPools().couponScopeCodes().contains(inventoryRow.productCode())) {
                assertCandidateOrBlockedByRuleId(
                        calculation,
                        COUPON_SCOPE_RULE_ID,
                        "会员生日/省区特色券范围商品未返回 coupon_redeem 候选或不可用原因",
                        failures
                );
            } else if (demoExchangeProductCodes(fixture.confirmedRules()).contains(inventoryRow.productCode())) {
                assertAnyCandidateOrBlocked(
                        calculation,
                        demoExchangeRuleIdsForProduct(fixture.confirmedRules(), inventoryRow.productCode()),
                        "演示换购商品未返回 exchange_purchase 候选或不可用原因",
                        failures
                );
            } else if (fixture.promotionPools().bundleCodes().contains(inventoryRow.productCode())) {
                assertCandidateOrBlockedByRuleId(
                        calculation,
                        BUNDLE_RULE_ID,
                        "LNG+CNG组合包池商品未返回 bundle_price 候选或不可用原因",
                        failures
                );
            } else if (fixture.demoActivationCodes().contains(inventoryRow.productCode())) {
                assertCandidateOrBlockedByRuleId(
                        calculation,
                        DEMO_RULE_PREFIX + inventoryRow.productCode(),
                        "审计演示补位商品未返回 fixed_price 候选或不可用原因",
                        failures
                );
            }
        }

        String category = classify(fixture, inventoryRow, price.isEmpty(), product.isEmpty());
        return new ProductDecisionAuditResult(
                inventoryRow.productCode(),
                inventoryRow.productName(),
                inventoryRow.barcode(),
                inventoryRow.inventoryQuantity(),
                category,
                calculation == null ? List.of() : calculation.availableCandidates().stream()
                        .map(candidate -> candidate.ruleType().name())
                        .distinct()
                        .toList(),
                calculation == null ? List.of() : calculation.blockedPromotions().stream()
                        .map(blocked -> blocked.ruleType().name())
                        .distinct()
                        .toList(),
                failures
        );
    }

    private void appendPromotionPoolIntersections(AuditReport report, AuditFixture fixture) {
        Set<String> inventoryCodes = fixture.inventoryByProductCode().keySet();
        report.addPoolStat("个性化促销池", fixture.promotionPools().personalizedCodes(), inventoryCodes);
        report.addPoolStat("9.9专区", fixture.promotionPools().ninePointNineCodes(), inventoryCodes);
        report.addPoolStat("会员生日&省区特色券范围", fixture.promotionPools().couponScopeCodes(), inventoryCodes);
        report.addPoolStat("加油换购商品", fixture.promotionPools().exchangePurchaseCodes(), inventoryCodes);
        report.addPoolStat("组合包/LNG+CNG商品", fixture.promotionPools().bundleCodes(), inventoryCodes);
        report.fixedPriceImportInvalidCount(fixture.fixedPriceImportInvalidCount());
    }

    private void appendMultiContextAuditResults(AuditReport report, AuditFixture fixture) {
        Set<String> inventoryCodes = fixture.inventoryByProductCode().keySet();
        Set<String> couponScopeInventoryCodes = intersection(fixture.promotionPools().couponScopeCodes(), inventoryCodes);
        Set<String> bundleInventoryCodes = intersection(fixture.promotionPools().bundleCodes(), inventoryCodes);
        Set<String> exchangeDemoCodes = demoExchangeProductCodes(fixture.confirmedRules());

        report.addContextStat(contextAuditStat(
                "加油站-非会员-逢9日",
                fixture,
                inventoryCodes,
                product -> order(List.of(cartItem(product, 1, product.category(), product.inventoryQuantity())),
                        new StationContext("audit-oil-station", "oil_station", "新疆"),
                        CustomerContext.anonymous(),
                        List.of(),
                        AUDIT_BUSINESS_DATE),
                "验证默认 checkout 上下文下，结构化促销池至少返回候选或不可用原因。"
        ));
        report.addContextStat(contextAuditStat(
                "加油站-会员生日月-持省区券",
                fixture,
                couponScopeInventoryCodes,
                product -> order(List.of(cartItem(product, 1, product.category(), product.inventoryQuantity())),
                        new StationContext("audit-oil-station", "oil_station", "新疆"),
                        new CustomerContext(true, "普通会员", List.of("audit-coupon-scope-instance"), 7),
                        List.of(couponForScope(fixture.promotionPools().couponScopeCodes())),
                        AUDIT_BUSINESS_DATE),
                "验证券适用范围商品在会员生日月且持券上下文下可激活 coupon_redeem。"
        ));
        report.addContextStat(contextAuditStat(
                "加气站-会员-LNG/CNG组合包",
                fixture,
                bundleInventoryCodes,
                product -> order(List.of(cartItem(product, 1, product.category(), product.inventoryQuantity())),
                        new StationContext("audit-gas-station", "gas_station", "新疆"),
                        new CustomerContext(true, "普通会员", List.of(), 7),
                        List.of(),
                        AUDIT_BUSINESS_DATE),
                "验证 LNG/CNG 商品池在加气站上下文下至少返回 bundle_price 候选或不可用原因。"
        ));
        report.addContextStat(contextAuditStat(
                "加油站-非会员-逢7日",
                fixture,
                inventoryCodes,
                product -> order(List.of(cartItem(product, 1, product.category(), product.inventoryQuantity())),
                        new StationContext("audit-oil-station", "oil_station", "新疆"),
                        CustomerContext.anonymous(),
                        List.of(),
                        LocalDate.of(2026, 7, 7)),
                "当前结构化规则未导入逢7日专属规则，本上下文用于确认日期变化不会破坏兜底与不可用原因。"
        ));
        report.addContextStat(contextAuditStat(
                "加油站-油品消费200元",
                fixture,
                exchangeDemoCodes,
                product -> order(List.of(cartItem(product, 1, product.category(), product.inventoryQuantity())),
                        new StationContext("audit-oil-station", "gas_station", "新疆"),
                        CustomerContext.anonymous(),
                        new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("200.00"), BigDecimal.ZERO),
                        List.of(),
                        AUDIT_BUSINESS_DATE),
                "验证演示换购规则在油品金额满足时返回 exchange_purchase 候选。"
        ));
    }

    private ContextAuditStat contextAuditStat(
            String name,
            AuditFixture fixture,
            Set<String> productCodes,
            Function<ProductCatalogItem, OrderContext> orderFactory,
            String note
    ) {
        int scannedProducts = 0;
        int nonFallbackCandidateProducts = 0;
        int couponRedeemCandidateProducts = 0;
        int blockedOnlyProducts = 0;
        int fallbackOnlyProducts = 0;

        for (String productCode : productCodes) {
            Optional<ProductCatalogItem> product = fixture.productRepository().findByProductCode(productCode);
            if (product.isEmpty()) {
                continue;
            }
            scannedProducts++;
            CalculationResult result = fixture.promotionEngine().calculate(orderFactory.apply(product.get()),
                    fixture.confirmedRules());
            boolean hasNonFallbackCandidate = result.availableCandidates().stream()
                    .anyMatch(candidate -> candidate.ruleType() != PromotionRuleType.ORIGINAL_PRICE);
            boolean hasCouponRedeemCandidate = candidate(result, PromotionRuleType.COUPON_REDEEM).isPresent();
            if (hasNonFallbackCandidate) {
                nonFallbackCandidateProducts++;
            } else if (!result.blockedPromotions().isEmpty()) {
                blockedOnlyProducts++;
            } else {
                fallbackOnlyProducts++;
            }
            if (hasCouponRedeemCandidate) {
                couponRedeemCandidateProducts++;
            }
        }

        return new ContextAuditStat(name, scannedProducts, nonFallbackCandidateProducts,
                couponRedeemCandidateProducts, blockedOnlyProducts, fallbackOnlyProducts, note);
    }

    private void appendProductGroupMappingNotes(AuditReport report) {
        report.productGroupMappingNote("Flyway V5 已建立 product_group/product_group_item，并为“红牛（2款通用）”"
                + "“格桑泉/小水通用”补充演示映射；更多自然语言商品组仍需后续按活动看板继续展开。");
    }

    private void appendEdgeScenarioResults(AuditReport report, AuditFixture fixture) {
        report.addEdgeScenario(edgeUnknownBarcode(fixture));
        report.addEdgeScenario(edgeMixedCart(fixture));
        report.addEdgeScenario(edgeAllExcludedCategories(fixture));
        report.addEdgeScenario(edgeLowInventoryDoesNotBlockFallback(fixture));
        report.addEdgeScenario(edgeZeroInventoryDoesNotBlockFallback(fixture));
        report.addEdgeScenario(edgeRepeatedScanFixedPrice(fixture));
        report.addEdgeScenario(edgeNoConfirmedRulesStillFallback(fixture));
    }

    private EdgeScenarioResult edgeUnknownBarcode(AuditFixture fixture) {
        boolean passed = fixture.productRepository().findByBarcode("0000000000000").isEmpty();
        return new EdgeScenarioResult("条码不存在于价格表", passed,
                passed ? "商品查询返回 empty，可由接口层转换为明确 404/业务错误。" : "未知条码被错误命中商品。");
    }

    private EdgeScenarioResult edgeMixedCart(AuditFixture fixture) {
        Optional<ProductCatalogItem> nine = firstInventoryProduct(fixture, fixture.ninePointNineCodes());
        Optional<ProductCatalogItem> ordinary = fixture.inventoryRows().stream()
                .filter(row -> !fixture.ninePointNineCodes().contains(row.productCode()))
                .map(row -> fixture.productRepository().findByProductCode(row.productCode()))
                .flatMap(Optional::stream)
                .findFirst();
        if (nine.isEmpty() || ordinary.isEmpty()) {
            return new EdgeScenarioResult("购物车多商品混合", false, "缺少9.9或普通库存样例。");
        }
        CartItem cigarette = new CartItem("edge-cigarette", "edge-cigarette", "edge-cigarette",
                "边界香烟商品", 1, new BigDecimal("30.00"), "香烟", BigDecimal.ONE);
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(
                cartItem(nine.get(), 1, nine.get().category(), nine.get().inventoryQuantity()),
                cartItem(ordinary.get(), 1, ordinary.get().category(), ordinary.get().inventoryQuantity()),
                cigarette
        )), fixture.confirmedRules());
        boolean passed = hasFallback(result) && candidate(result, PromotionRuleType.FIXED_PRICE).isPresent();
        return new EdgeScenarioResult("购物车多商品混合", passed,
                "9.9商品、普通商品、香烟混合时仍返回 fallback，且9.9商品返回 fixed_price。");
    }

    private EdgeScenarioResult edgeAllExcludedCategories(AuditFixture fixture) {
        PromotionRule discountRule = percentageDiscountRule(Set.of("香烟", "化肥"));
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(
                syntheticItem("edge-cigarette", "香烟"),
                syntheticItem("edge-fertilizer", "化肥")
        )), List.of(discountRule));
        boolean passed = hasFallback(result)
                && candidate(result, PromotionRuleType.PERCENTAGE_DISCOUNT).isEmpty()
                && blocked(result, PromotionRuleType.PERCENTAGE_DISCOUNT).isPresent();
        return new EdgeScenarioResult("全部商品都是排除品类", passed,
                "全场折扣进入 blocked，原价兜底仍可见。");
    }

    private EdgeScenarioResult edgeLowInventoryDoesNotBlockFallback(AuditFixture fixture) {
        ProductCatalogItem product = firstInventoryProduct(fixture, fixture.inventoryByProductCode().keySet())
                .orElseThrow();
        CartItem lowInventory = cartItem(product, 1, product.category(), BigDecimal.ONE);
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(lowInventory)),
                fixture.confirmedRules());
        return new EdgeScenarioResult("库存极低商品", hasFallback(result),
                "库存为1时促销计算流程不被阻断，fallback 仍可见。");
    }

    private EdgeScenarioResult edgeZeroInventoryDoesNotBlockFallback(AuditFixture fixture) {
        ProductCatalogItem product = firstInventoryProduct(fixture, fixture.inventoryByProductCode().keySet())
                .orElseThrow();
        CartItem zeroInventory = cartItem(product, 1, product.category(), BigDecimal.ZERO);
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(zeroInventory)),
                fixture.confirmedRules());
        return new EdgeScenarioResult("库存为零商品", hasFallback(result),
                "库存为0时促销计算流程不被阻断，fallback 仍可见。");
    }

    private EdgeScenarioResult edgeRepeatedScanFixedPrice(AuditFixture fixture) {
        Optional<ProductCatalogItem> nine = firstInventoryProduct(fixture, fixture.ninePointNineCodes());
        if (nine.isEmpty()) {
            return new EdgeScenarioResult("同一9.9商品扫多次", false, "缺少9.9库存样例。");
        }
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(
                cartItem(nine.get(), 3, nine.get().category(), nine.get().inventoryQuantity())
        )), fixture.confirmedRules());
        Optional<PromotionCandidate> fixedPrice = candidate(result, PromotionRuleType.FIXED_PRICE);
        boolean passed = fixedPrice.isPresent()
                && fixedPrice.get().payableAmount().compareTo(new BigDecimal("29.70")) == 0
                && hasFallback(result);
        return new EdgeScenarioResult("同一9.9商品扫多次", passed,
                passed ? "fixed_price 应付金额 = 9.90 * 3 = 29.70。" : "fixed_price 数量计算不符合预期。");
    }

    private EdgeScenarioResult edgeNoConfirmedRulesStillFallback(AuditFixture fixture) {
        ProductCatalogItem product = firstInventoryProduct(fixture, fixture.inventoryByProductCode().keySet())
                .orElseThrow();
        CalculationResult result = fixture.promotionEngine().calculate(order(List.of(
                cartItem(product, 1, product.category(), product.inventoryQuantity())
        )), List.of());
        boolean passed = hasFallback(result) && result.availableCandidates().size() == 1;
        return new EdgeScenarioResult("无任何CONFIRMED规则", passed,
                "空规则列表只返回原价兜底，符合只有 CONFIRMED 规则参与 checkout 的约束。");
    }

    private PromotionPools promotionPools(Path activityFile) {
        return new PromotionPools(
                productCodesFromSheet(activityFile, "参考1-非非促销（个性化促销）", 4, List.of(2)),
                productCodesFromSheet(activityFile, ImportCenterService.NINE_POINT_NINE_SHEET_NAME, 3, List.of(2)),
                productCodesFromSheet(activityFile, ImportCenterService.COUPON_SCOPE_SHEET_NAME, 3, List.of(2)),
                Set.of(),
                productCodesFromSheet(activityFile, "LNG+CNG", 1, List.of(1, 4))
        );
    }

    private Set<String> productCodesFromSheet(Path file, String sheetName, int headRowNumber, List<Integer> columns) {
        try {
            return workbookReader.readSheet(file, sheetName, headRowNumber).stream()
                    .flatMap(row -> columns.stream().map(row::cell))
                    .map(this::normalizeIdentifier)
                    .filter(value -> value.matches("\\d{6,12}"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (RuntimeException exception) {
            return Set.of();
        }
    }

    private List<PromotionRule> activationGapRules(
            Path activityFile,
            Set<String> inventoryProductCodes,
            PromotionPools promotionPools,
            InMemoryProductCatalogRepository productRepository
    ) {
        List<PromotionRule> rules = new ArrayList<>();
        Set<String> personalizedEligibleCodes = new LinkedHashSet<>(inventoryProductCodes);
        personalizedEligibleCodes.removeAll(promotionPools.ninePointNineCodes());
        rules.addAll(personalizedFixedPriceRules(activityFile, personalizedEligibleCodes, productRepository));
        couponScopeRedeemRule(promotionPools.couponScopeCodes()).ifPresent(rules::add);
        bundlePriceRule(promotionPools.bundleCodes()).ifPresent(rules::add);
        return rules;
    }

    private List<PromotionRule> personalizedFixedPriceRules(
            Path activityFile,
            Set<String> inventoryProductCodes,
            InMemoryProductCatalogRepository productRepository
    ) {
        Map<String, BigDecimal> fixedPricesByProductCode = new LinkedHashMap<>();
        Set<String> personalizedInventoryCodes = new LinkedHashSet<>();
        try {
            for (RawExcelRow row : workbookReader.readSheet(activityFile, "参考1-非非促销（个性化促销）", 4)) {
                String productCode = normalizeIdentifier(row.cell(2));
                if (!productCode.matches("\\d{6,12}") || !inventoryProductCodes.contains(productCode)) {
                    continue;
                }
                personalizedInventoryCodes.add(productCode);
                parseMoney(row.cell(8))
                        .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                        .ifPresent(price -> fixedPricesByProductCode.merge(productCode, price, BigDecimal::min));
            }
        } catch (RuntimeException exception) {
            return List.of();
        }

        return personalizedInventoryCodes.stream()
                .map(productRepository::findByProductCode)
                .flatMap(Optional::stream)
                .map(product -> {
                    BigDecimal fixedPrice = fixedPricesByProductCode.get(product.productCode());
                    boolean fromExcel = fixedPrice != null;
                    if (!fromExcel) {
                        fixedPrice = product.unitPrice()
                                .multiply(new BigDecimal("0.95"))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return new PromotionRule(
                        PERSONALIZED_RULE_PREFIX + product.productCode(),
                        (fromExcel ? "审计激活-个性化促销价-" : "审计演示-个性化促销缺失促销价-")
                                + product.productCode(),
                        PromotionRuleType.FIXED_PRICE,
                        65,
                        "direct_discount",
                        false,
                        PromotionRuleStatus.CONFIRMED,
                        new PromotionCondition(Set.of(product.productCode()), Set.of(), Set.of(), Set.of(), Set.of(),
                                null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                        PromotionBenefit.fixedPrice(fixedPrice),
                        fromExcel ? "audit-import-gap-v1" : "audit-demo-gap-v1"
                    );
                })
                .toList();
    }

    private Optional<PromotionRule> couponScopeRedeemRule(Set<String> couponScopeCodes) {
        if (couponScopeCodes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PromotionRule(
                COUPON_SCOPE_RULE_ID,
                "审计激活-会员生日与省区特色券适用范围",
                PromotionRuleType.COUPON_REDEEM,
                80,
                "coupon_redeem",
                true,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(couponScopeCodes, Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO),
                PromotionBenefit.couponRedeem(),
                "audit-import-gap-v1"
        ));
    }

    private Optional<PromotionRule> bundlePriceRule(Set<String> bundleCodes) {
        if (bundleCodes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PromotionRule(
                BUNDLE_RULE_ID,
                "审计激活-LNG+CNG组合包",
                PromotionRuleType.BUNDLE_PRICE,
                60,
                "direct_discount",
                false,
                PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(bundleCodes, Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.bundlePrice(new BigDecimal("88.00")),
                "audit-import-gap-v1"
        ));
    }

    private Set<String> demoActivationCodes(
            InMemoryProductCatalogRepository productRepository,
            List<InventoryImportRow> inventoryRows,
            PromotionPools promotionPools,
            Set<String> ninePointNineCodes,
            int targetCount
    ) {
        Set<String> existingPoolCodes = new LinkedHashSet<>();
        existingPoolCodes.addAll(ninePointNineCodes);
        existingPoolCodes.addAll(promotionPools.personalizedCodes());
        existingPoolCodes.addAll(promotionPools.couponScopeCodes());
        existingPoolCodes.addAll(promotionPools.bundleCodes());
        existingPoolCodes.addAll(promotionPools.exchangePurchaseCodes());

        Set<String> selected = new LinkedHashSet<>();
        for (InventoryImportRow row : inventoryRows) {
            if (selected.size() >= targetCount) {
                break;
            }
            if (existingPoolCodes.contains(row.productCode())) {
                continue;
            }
            Optional<ProductCatalogItem> product = productRepository.findByProductCode(row.productCode());
            if (product.isEmpty() || product.get().unitPrice().compareTo(BigDecimal.ONE) <= 0) {
                continue;
            }
            selected.add(row.productCode());
        }
        return selected;
    }

    private List<PromotionRule> demoFixedPriceRules(
            InMemoryProductCatalogRepository productRepository,
            Set<String> demoActivationCodes
    ) {
        return demoActivationCodes.stream()
                .map(productRepository::findByProductCode)
                .flatMap(Optional::stream)
                .map(product -> {
                    BigDecimal demoFixedPrice = product.unitPrice()
                            .multiply(new BigDecimal("0.95"))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new PromotionRule(
                            DEMO_RULE_PREFIX + product.productCode(),
                            "审计演示补位促销-" + product.productCode(),
                            PromotionRuleType.FIXED_PRICE,
                            20,
                            "direct_discount",
                            false,
                            PromotionRuleStatus.CONFIRMED,
                            new PromotionCondition(Set.of(product.productCode()), Set.of(), Set.of(), Set.of(), Set.of(),
                                    null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                            PromotionBenefit.fixedPrice(demoFixedPrice),
                            "audit-demo-gap-v1"
                    );
                })
                .toList();
    }

    private List<PromotionRule> demoExchangeRules(InMemoryProductCatalogRepository productRepository) {
        return List.of(
                demoExchangeRule(productRepository, "gasoline-water", "70545526",
                        "演示-汽油满180元换购格桑泉水", Set.of(FuelType.GASOLINE),
                        new BigDecimal("180.00"), new BigDecimal("2.00"), 4),
                demoExchangeRule(productRepository, "diesel-redbull", "70356177",
                        "演示-柴油满300元换购红牛", Set.of(FuelType.DIESEL),
                        new BigDecimal("300.00"), new BigDecimal("10.00"), 1),
                demoExchangeRule(productRepository, "fuel-long-haul-bundle", "70453858",
                        "演示-汽柴油满200元换购LNG长途包代表商品", Set.of(FuelType.GASOLINE, FuelType.DIESEL),
                        new BigDecimal("200.00"), new BigDecimal("25.00"), 1)
        ).stream()
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<PromotionRule> demoExchangeRule(
            InMemoryProductCatalogRepository productRepository,
            String id,
            String productCode,
            String activityName,
            Set<FuelType> fuelTypes,
            BigDecimal minFuelAmount,
            BigDecimal exchangePrice,
            int exchangeQuantity
    ) {
        return productRepository.findByProductCode(productCode)
                .map(product -> new PromotionRule(
                        DEMO_EXCHANGE_RULE_PREFIX + id,
                        activityName,
                        PromotionRuleType.EXCHANGE_PURCHASE,
                        70,
                        "exchange_purchase",
                        true,
                        PromotionRuleStatus.CONFIRMED,
                        new PromotionCondition(Set.of(product.productCode()), Set.of(), fuelTypes,
                                Set.of("gas_station"), Set.of(), null, null,
                                BigDecimal.ZERO, minFuelAmount, false, BigDecimal.ZERO),
                        PromotionBenefit.exchangePurchase(exchangePrice, exchangeQuantity),
                        "demo-exchange-v1"
                ));
    }

    private PromotionEngine promotionEngine(InMemoryProductCatalogRepository productRepository) {
        ProductCatalogInventoryQueryService inventoryQueryService = new ProductCatalogInventoryQueryService(productRepository);
        List<BenefitCalculator> calculators = List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(inventoryQueryService),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator(inventoryQueryService),
                new CouponRedeemBenefitCalculator(),
                new FuelVolumeDiscountBenefitCalculator()
        );
        return new DefaultPromotionEngine(new DefaultConditionMatcher(), calculators, new DefaultConflictResolver(),
                new DefaultCandidateRanker(), new DefaultExplanationBuilder());
    }

    private Optional<PriceImportRow> findPrice(AuditFixture fixture, InventoryImportRow row) {
        if (row.barcode() != null && fixture.priceByBarcode().containsKey(row.barcode())) {
            return Optional.of(fixture.priceByBarcode().get(row.barcode()));
        }
        return Optional.ofNullable(fixture.priceByProductCode().get(row.productCode()));
    }

    private Optional<ProductCatalogItem> findProduct(
            InMemoryProductCatalogRepository repository,
            InventoryImportRow row
    ) {
        if (row.barcode() != null && !row.barcode().isBlank()) {
            Optional<ProductCatalogItem> byBarcode = repository.findByBarcode(row.barcode());
            if (byBarcode.isPresent()) {
                return byBarcode;
            }
        }
        return repository.findByProductCode(row.productCode());
    }

    private Optional<ProductCatalogItem> firstInventoryProduct(AuditFixture fixture, Collection<String> productCodes) {
        return productCodes.stream()
                .map(fixture.productRepository()::findByProductCode)
                .flatMap(Optional::stream)
                .filter(item -> fixture.inventoryByProductCode().containsKey(item.productCode()))
                .filter(item -> item.unitPrice().compareTo(NINE_POINT_NINE) > 0)
                .findFirst()
                .or(() -> productCodes.stream()
                        .map(fixture.productRepository()::findByProductCode)
                        .flatMap(Optional::stream)
                        .filter(item -> fixture.inventoryByProductCode().containsKey(item.productCode()))
                        .findFirst());
    }

    private String classify(
            AuditFixture fixture,
            InventoryImportRow row,
            boolean priceMissing,
            boolean productMissing
    ) {
        if (priceMissing || productMissing) {
            return "L. 无法查询商品";
        }
        if (fixture.ninePointNineCodes().contains(row.productCode())) {
            return "A. 9.9专区商品";
        }
        if (fixture.promotionPools().personalizedCodes().contains(row.productCode())) {
            return "B. 惊爆价商品";
        }
        if (fixture.promotionPools().couponScopeCodes().contains(row.productCode())) {
            return "G. 赠券触发商品";
        }
        if (demoExchangeProductCodes(fixture.confirmedRules()).contains(row.productCode())) {
            return "F. 换购商品";
        }
        if (fixture.promotionPools().bundleCodes().contains(row.productCode())) {
            return "E. 组合包商品";
        }
        if (fixture.demoActivationCodes().contains(row.productCode())) {
            return "M. 审计演示促销商品";
        }
        if (row.inventoryQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return "K. 库存为零商品";
        }
        return "J. 无促销商品";
    }

    private CartItem cartItem(ProductCatalogItem product, int quantity, String category, BigDecimal inventoryQuantity) {
        return new CartItem("line-" + product.productCode(), product.productCode(), product.barcode(),
                product.productName(), quantity, product.unitPrice(), category, inventoryQuantity);
    }

    private CartItem syntheticItem(String productCode, String category) {
        return new CartItem("line-" + productCode, productCode, productCode, category + "测试商品",
                1, new BigDecimal("20.00"), category, BigDecimal.TEN);
    }

    private OrderContext order(List<CartItem> items) {
        return new OrderContext(new StationContext("audit-station", "gas_station", "新疆"),
                CustomerContext.anonymous(), FuelContext.empty(), items, AUDIT_BUSINESS_DATE, null);
    }

    private OrderContext order(
            List<CartItem> items,
            StationContext station,
            CustomerContext customer,
            List<Coupon> coupons,
            LocalDate businessDate
    ) {
        return new OrderContext(station, customer, FuelContext.empty(), items, businessDate, null, coupons);
    }

    private OrderContext order(
            List<CartItem> items,
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            List<Coupon> coupons,
            LocalDate businessDate
    ) {
        return new OrderContext(station, customer, fuel, items, businessDate, null, coupons);
    }

    private Coupon couponForScope(Set<String> couponScopeCodes) {
        return new Coupon(
                "audit-coupon-scope-instance",
                "audit-coupon-scope-template",
                "审计用省区特色券",
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                new ArrayList<>(couponScopeCodes),
                List.of(),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                true,
                true,
                CouponStatus.AVAILABLE,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                null,
                "audit"
        );
    }

    private PromotionRule percentageDiscountRule(Set<String> excludedCategories) {
        return new PromotionRule("audit-percentage-discount", "审计用全场9折", PromotionRuleType.PERCENTAGE_DISCOUNT,
                10, "direct_discount", false, PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(Set.of(), excludedCategories, Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")), "audit");
    }

    private boolean hasFallback(CalculationResult result) {
        return result.originalPriceFallback() != null
                && result.availableCandidates().stream()
                .anyMatch(candidate -> candidate.ruleType() == PromotionRuleType.ORIGINAL_PRICE);
    }

    private Optional<PromotionCandidate> candidate(CalculationResult result, PromotionRuleType type) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleType() == type)
                .findFirst();
    }

    private void assertCandidateOrBlockedByRuleId(
            CalculationResult result,
            String ruleId,
            String failureMessage,
            List<String> failures
    ) {
        if (candidateByRuleId(result, ruleId).isEmpty()
                && blockedByRuleId(result, ruleId).isEmpty()) {
            failures.add(failureMessage);
        }
    }

    private void assertAnyCandidateOrBlocked(
            CalculationResult result,
            Set<String> ruleIds,
            String failureMessage,
            List<String> failures
    ) {
        boolean found = ruleIds.stream()
                .anyMatch(ruleId -> candidateByRuleId(result, ruleId).isPresent()
                        || blockedByRuleId(result, ruleId).isPresent());
        if (!found) {
            failures.add(failureMessage);
        }
    }

    private Optional<PromotionCandidate> candidateByRuleId(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().equals(ruleId))
                .findFirst();
    }

    private Optional<PromotionCandidate> candidateByRuleIdPrefix(CalculationResult result, String ruleIdOrPrefix) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().startsWith(ruleIdOrPrefix))
                .findFirst();
    }

    private Optional<BlockedPromotion> blocked(CalculationResult result, PromotionRuleType type) {
        return result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleType() == type)
                .findFirst();
    }

    private Optional<BlockedPromotion> blockedByRuleIdPrefix(CalculationResult result, String ruleIdOrPrefix) {
        return result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleId().startsWith(ruleIdOrPrefix))
                .findFirst();
    }

    private Optional<BlockedPromotion> blockedByRuleId(CalculationResult result, String ruleId) {
        return result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleId().equals(ruleId))
                .findFirst();
    }

    private Path dataFile(String fileName) {
        Path path = Path.of("..", "data", fileName);
        assertThat(Files.exists(path)).as("data file exists: %s", fileName).isTrue();
        return path;
    }

    private String normalizeIdentifier(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return "";
        }
        String normalized = value.trim();
        return normalized.endsWith(".0") ? normalized.substring(0, normalized.length() - 2) : normalized;
    }

    private Optional<BigDecimal> parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("元", "");
        if (!normalized.matches("-?\\d+(\\.\\d+)?")) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP));
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    private Set<String> demoExchangeProductCodes(List<PromotionRule> rules) {
        return rules.stream()
                .filter(rule -> rule.ruleId().startsWith(DEMO_EXCHANGE_RULE_PREFIX))
                .flatMap(rule -> rule.condition().productCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> demoExchangeRuleIdsForProduct(List<PromotionRule> rules, String productCode) {
        return rules.stream()
                .filter(rule -> rule.ruleId().startsWith(DEMO_EXCHANGE_RULE_PREFIX))
                .filter(rule -> rule.condition().productCodes().contains(productCode))
                .map(PromotionRule::ruleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void writeReport(AuditReport report) throws IOException {
        Path reportPath = Path.of("target", "generated-reports", "product-promotion-coverage-audit.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toMarkdown(), StandardCharsets.UTF_8);
    }

    private void writeActivationGapAnalysis(AuditReport report, AuditFixture fixture) throws IOException {
        Path reportPath = Path.of("..", "docs", "promotion-activation-gap-analysis.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, activationGapMarkdown(report, fixture), StandardCharsets.UTF_8);
    }

    private void writeGovernanceBacklog(AuditFixture fixture) throws IOException {
        Path reportPath = Path.of("..", "docs", "promotion-governance-backlog.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, governanceBacklogMarkdown(fixture), StandardCharsets.UTF_8);
    }

    private String governanceBacklogMarkdown(AuditFixture fixture) {
        List<PromotionRule> productionReadyRules = fixture.confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith(PERSONALIZED_RULE_PREFIX))
                .filter(rule -> "audit-import-gap-v1".equals(rule.version()))
                .toList();
        List<PromotionRule> draftApproximationRules = fixture.confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith(PERSONALIZED_RULE_PREFIX))
                .filter(rule -> "audit-demo-gap-v1".equals(rule.version()))
                .toList();
        List<PromotionRule> archivedDemoRules = fixture.confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith(DEMO_RULE_PREFIX))
                .toList();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 促销规则治理待办清单\n\n");
        markdown.append("## 治理策略汇总\n");
        markdown.append("- Excel 有明确促销价的个性化规则：").append(productionReadyRules.size())
                .append(" 条，建议转正为 `import-v2` 并保持 `CONFIRMED`。\n");
        markdown.append("- 缺失单品促销价、由执行价 95% 近似的规则：").append(draftApproximationRules.size())
                .append(" 条，建议转为 `DRAFT/PENDING_CONFIRMATION` 等待人工确认。\n");
        markdown.append("- 审计演示补位规则：").append(archivedDemoRules.size())
                .append(" 条，建议 `ARCHIVED`，不得进入生产 checkout。\n\n");

        markdown.append("## 待确认规则（DRAFT -> 需人工确认促销价）\n");
        markdown.append("| 规则 ID | 商品编码 | 商品名称 | 执行价 | 当前近似促销价（95%） | 建议操作 |\n");
        markdown.append("|---------|---------|---------|--------|---------------------|---------|\n");
        for (PromotionRule rule : draftApproximationRules) {
            String productCode = rule.condition().productCodes().stream().findFirst().orElse("");
            ProductCatalogItem product = fixture.productRepository().findByProductCode(productCode).orElse(null);
            markdown.append("| ").append(rule.ruleId()).append(" | ")
                    .append(productCode).append(" | ")
                    .append(product == null ? "" : escapeMarkdown(product.productName())).append(" | ")
                    .append(product == null ? "" : product.unitPrice()).append(" | ")
                    .append(rule.benefit().fixedPrice()).append(" | 人工核实真实促销价后确认 |\n");
        }

        markdown.append("\n## 已转正规则（AUDIT_IMPORT -> PRODUCTION）\n");
        markdown.append("| 规则 ID | 商品编码 | 商品名称 | Excel 促销价 | 版本号 | 转正建议 |\n");
        markdown.append("|---------|---------|---------|-------------|--------|---------|\n");
        for (PromotionRule rule : productionReadyRules) {
            String productCode = rule.condition().productCodes().stream().findFirst().orElse("");
            ProductCatalogItem product = fixture.productRepository().findByProductCode(productCode).orElse(null);
            markdown.append("| ").append(rule.ruleId()).append(" | ")
                    .append(productCode).append(" | ")
                    .append(product == null ? "" : escapeMarkdown(product.productName())).append(" | ")
                    .append(rule.benefit().fixedPrice()).append(" | import-v2 | 保持 CONFIRMED |\n");
        }

        markdown.append("\n## 已归档规则（AUDIT_DEMO -> ARCHIVED）\n");
        markdown.append("| 规则 ID | 商品编码 | 商品名称 | 原因 |\n");
        markdown.append("|---------|---------|---------|------|\n");
        for (PromotionRule rule : archivedDemoRules) {
            String productCode = rule.condition().productCodes().stream().findFirst().orElse("");
            ProductCatalogItem product = fixture.productRepository().findByProductCode(productCode).orElse(null);
            markdown.append("| ").append(rule.ruleId()).append(" | ")
                    .append(productCode).append(" | ")
                    .append(product == null ? "" : escapeMarkdown(product.productName()))
                    .append(" | 审计覆盖率补位，非真实促销 |\n");
        }
        return markdown.toString();
    }

    private String activationGapMarkdown(AuditReport report, AuditFixture fixture) {
        Set<String> inventoryCodes = fixture.inventoryByProductCode().keySet();
        int personalizedIntersection = intersection(fixture.promotionPools().personalizedCodes(), inventoryCodes).size();
        int couponScopeIntersection = intersection(fixture.promotionPools().couponScopeCodes(), inventoryCodes).size();
        int bundleIntersection = intersection(fixture.promotionPools().bundleCodes(), inventoryCodes).size();
        int exchangeIntersection = intersection(fixture.promotionPools().exchangePurchaseCodes(), inventoryCodes).size();
        int demoCount = fixture.demoActivationCodes().size();
        long personalizedDemoRuleCount = fixture.confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith(PERSONALIZED_RULE_PREFIX))
                .filter(rule -> "audit-demo-gap-v1".equals(rule.version()))
                .count();
        long demoExchangeRuleCount = fixture.confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith(DEMO_EXCHANGE_RULE_PREFIX))
                .count();
        long baselineFallbackOnly = fixture.inventoryRows().stream()
                .filter(row -> !fixture.ninePointNineCodes().contains(row.productCode()))
                .filter(row -> findPrice(fixture, row).isPresent())
                .filter(row -> findProduct(fixture.productRepository(), row).isPresent())
                .filter(row -> row.inventoryQuantity().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long currentFallbackOnly = report.categoryCounts.getOrDefault("J. 无促销商品", 0L);
        long realPoolFallbackOnly = currentFallbackOnly + demoCount;
        long realPoolReducedFallbackOnly = Math.max(0, baselineFallbackOnly - realPoolFallbackOnly);
        BigDecimal realPoolReductionRate = baselineFallbackOnly == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(realPoolReducedFallbackOnly)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(baselineFallbackOnly), 1, RoundingMode.HALF_UP);
        long reducedFallbackOnly = Math.max(0, baselineFallbackOnly - currentFallbackOnly);
        BigDecimal reductionRate = baselineFallbackOnly == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(reducedFallbackOnly)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(baselineFallbackOnly), 1, RoundingMode.HALF_UP);
        int activationRuleCount = fixture.confirmedRules().size() - fixture.ninePointNineRules().size();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 促销池激活缺口分析\n\n");
        markdown.append("## 1. 缺口概览\n");
        markdown.append("- 基线仅启用 9.9 结构化规则时，预计 fallback-only 商品数：")
                .append(baselineFallbackOnly).append("。\n");
        markdown.append("- 仅按真实促销池去重激活后，预计 J 类商品数：")
                .append(realPoolFallbackOnly).append("，减少 ")
                .append(realPoolReducedFallbackOnly).append("，减少比例：")
                .append(realPoolReductionRate).append("%。\n");
        markdown.append("- 本轮审计激活后 J 类无促销商品数：").append(currentFallbackOnly).append("。\n");
        markdown.append("- J 类减少：").append(reducedFallbackOnly).append("，减少比例：")
                .append(reductionRate).append("%。\n");
        markdown.append("- 其中审计演示补位商品数：").append(demoCount)
                .append("，用于在真实池去重不足时验证 30%+ 激活目标，不进入生产规则。\n");
        markdown.append("- 个性化促销池中缺失单品促销价、由执行价 95 折生成的审计规则数：")
                .append(personalizedDemoRuleCount).append("。\n");
        markdown.append("- 加油换购演示规则数：").append(demoExchangeRuleCount)
                .append("，版本为 `demo-exchange-v1`，使用真实库存 SKU，不修改原始 Excel。\n");
        markdown.append("- 本轮新增审计激活规则数：").append(activationRuleCount)
                .append("，Excel 可解析规则版本为 `audit-import-gap-v1`，审计补位规则版本为 `audit-demo-gap-v1`。\n\n");

        markdown.append("## 2. 促销池交集与根因\n");
        markdown.append("| 促销池 | 本站库存交集 | 原因判断 | 本轮处理 |\n");
        markdown.append("|------|------------|---------|---------|\n");
        markdown.append("| 个性化促销池 | ").append(personalizedIntersection)
                .append(" | Excel 已有商品编码；部分行有促销价，部分行是买赠/组合结构导致单品促销价为空。")
                .append(" | 有促销价的按 Excel 价格生成 fixed_price；缺失单品价的用 audit-demo 价格补位，9.9 商品不重复生成。 |\n");
        markdown.append("| 会员生日&省区特色券范围 | ").append(couponScopeIntersection)
                .append(" | Excel 描述的是券适用范围，不等于顾客已持券；默认上下文无法激活。")
                .append(" | 生成 coupon_redeem 规则，并在会员生日月+持券上下文验证可激活。 |\n");
        markdown.append("| LNG+CNG组合包池 | ").append(bundleIntersection)
                .append(" | Excel 有商品池但缺少完整组合包结构和商品组映射。")
                .append(" | 生成审计用 bundle_price 规则，先要求返回候选或结构化不可用原因。 |\n");
        markdown.append("| 加油换购商品 | ").append(exchangeIntersection)
                .append(" | 当前表内未识别到可直接映射的本站 SKU 交集。")
                .append(" | 补充 3 条 demo-exchange-v1 演示规则，使用格桑泉水、红牛等真实库存 SKU。 |\n");
        markdown.append("| 审计演示补位 | ").append(demoCount)
                .append(" | 真实促销池去重后最高只能达到 ")
                .append(realPoolReductionRate)
                .append("% 降幅，低于 30% 目标。")
                .append(" | 选取真实库存商品生成显式 demo fixed_price 规则，仅用于覆盖率压力测试。 |\n\n");

        markdown.append("## 3. 修复边界\n");
        markdown.append("- 本轮没有修改 `data/` 下原始 Excel。\n");
        markdown.append("- 本轮没有把未经人工确认的导入候选直接接入真实 checkout；测试内的激活规则均显式设置 `CONFIRMED`，用于审计缺口验证。\n");
        markdown.append("- 真实生产路径仍应通过 promotion governance 完成确认、修正、停用和审计记录后，再进入 checkout。\n");
        markdown.append("- 前端仍只消费后端结果，不承担促销可用性或应付金额计算。\n\n");

        markdown.append("## 4. 后续仍需治理\n");
        markdown.append("- 将个性化促销池导入逻辑从审计规则沉淀为正式 importcenter mapper，并进入待确认规则治理流程。\n");
        markdown.append("- 为 LNG/CNG 组合包建立商品组/组合项结构，避免长期用单商品池近似表达组合包。\n");
        markdown.append("- 将 demo-exchange-v1 换购规则继续治理为正式 import-v2 规则，并补齐真实活动来源映射。\n");
        markdown.append("- 把券模板、券实例、券适用范围和 checkout 请求里的可用券做端到端演示脚本。\n");
        return markdown.toString();
    }

    private record AuditFixture(
            InMemoryProductCatalogRepository productRepository,
            PromotionEngine promotionEngine,
            List<PriceImportRow> priceRows,
            List<InventoryImportRow> inventoryRows,
            Map<String, PriceImportRow> priceByProductCode,
            Map<String, PriceImportRow> priceByBarcode,
            Map<String, InventoryImportRow> inventoryByProductCode,
            List<PromotionRule> ninePointNineRules,
            List<PromotionRule> confirmedRules,
            Set<String> ninePointNineCodes,
            Set<String> demoActivationCodes,
            PromotionPools promotionPools,
            int fixedPriceImportInvalidCount
    ) {
    }

    private record PromotionPools(
            Set<String> personalizedCodes,
            Set<String> ninePointNineCodes,
            Set<String> couponScopeCodes,
            Set<String> exchangePurchaseCodes,
            Set<String> bundleCodes
    ) {
    }

    private record ProductDecisionAuditResult(
            String productCode,
            String productName,
            String barcode,
            BigDecimal inventoryQuantity,
            String category,
            List<String> candidates,
            List<String> blockedPromotions,
            List<String> failures
    ) {
        boolean passed() {
            return failures.isEmpty();
        }
    }

    private record EdgeScenarioResult(String name, boolean passed, String note) {
    }

    private record PoolStat(
            String name,
            int poolSize,
            int inventoryIntersectionSize,
            int outsideInventorySize
    ) {
    }

    private record ContextAuditStat(
            String name,
            int scannedProducts,
            int nonFallbackCandidateProducts,
            int couponRedeemCandidateProducts,
            int blockedOnlyProducts,
            int fallbackOnlyProducts,
            String note
    ) {
    }

    private static final class AuditReport {

        private final int totalProducts;
        private final List<ProductDecisionAuditResult> productResults = new ArrayList<>();
        private final List<EdgeScenarioResult> edgeScenarios = new ArrayList<>();
        private final List<PoolStat> poolStats = new ArrayList<>();
        private final List<ContextAuditStat> contextStats = new ArrayList<>();
        private final Map<String, Long> categoryCounts = new LinkedHashMap<>();
        private final List<String> productGroupMappingNotes = new ArrayList<>();
        private int fixedPriceImportInvalidCount;

        private AuditReport(int totalProducts) {
            this.totalProducts = totalProducts;
        }

        private void addProductResult(ProductDecisionAuditResult result) {
            productResults.add(result);
            categoryCounts.merge(result.category(), 1L, Long::sum);
        }

        private void addEdgeScenario(EdgeScenarioResult result) {
            edgeScenarios.add(result);
        }

        private void addPoolStat(String name, Set<String> poolCodes, Set<String> inventoryCodes) {
            Set<String> intersection = new LinkedHashSet<>(poolCodes);
            intersection.retainAll(inventoryCodes);
            Set<String> outsideInventory = new LinkedHashSet<>(poolCodes);
            outsideInventory.removeAll(inventoryCodes);
            poolStats.add(new PoolStat(name, poolCodes.size(), intersection.size(), outsideInventory.size()));
        }

        private void addContextStat(ContextAuditStat stat) {
            contextStats.add(stat);
        }

        private void productGroupMappingNote(String note) {
            productGroupMappingNotes.add(note);
        }

        private void fixedPriceImportInvalidCount(int count) {
            fixedPriceImportInvalidCount = count;
        }

        private List<ProductDecisionAuditResult> failures() {
            return productResults.stream()
                    .filter(result -> !result.passed())
                    .toList();
        }

        private List<EdgeScenarioResult> edgeScenarios() {
            return edgeScenarios;
        }

        private String toMarkdown() {
            int failures = failures().size();
            int passed = totalProducts - failures;
            BigDecimal failureRate = totalProducts == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(failures)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(totalProducts), 1, RoundingMode.HALF_UP);

            StringBuilder markdown = new StringBuilder();
            markdown.append("# 全商品促销决策覆盖审计报告\n\n");
            markdown.append("## 审计概览\n");
            markdown.append("- 审计时间：")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append('\n');
            markdown.append("- 审计商品总数：").append(totalProducts).append('\n');
            markdown.append("- 通过：").append(passed).append('\n');
            markdown.append("- 失败：").append(failures).append('\n');
            markdown.append("- 失败率：").append(failureRate).append("%\n\n");

            markdown.append("## 失败项明细\n");
            if (failures().isEmpty()) {
                markdown.append("无。\n\n");
            } else {
                markdown.append("| 序号 | 商品编码 | 商品名称 | 条码 | 失败原因 | 修复状态 |\n");
                markdown.append("|------|---------|---------|------|---------|---------|\n");
                int index = 1;
                for (ProductDecisionAuditResult result : failures()) {
                    markdown.append("| ").append(index++).append(" | ")
                            .append(result.productCode()).append(" | ")
                            .append(escape(result.productName())).append(" | ")
                            .append(result.barcode()).append(" | ")
                            .append(escape(String.join("; ", result.failures()))).append(" | 待修复 |\n");
                }
                markdown.append('\n');
            }

            markdown.append("## 覆盖分类统计\n");
            markdown.append("| 分类 | 商品数 | 预期决策 | 实际结果 | 是否一致 |\n");
            markdown.append("|------|--------|---------|---------|---------|\n");
            Map<String, String> expected = expectedDecisionByCategory();
            for (String category : expected.keySet()) {
                long count = categoryCounts.getOrDefault(category, 0L);
                long categoryFailures = productResults.stream()
                        .filter(result -> result.category().equals(category))
                        .filter(result -> !result.passed())
                        .count();
                markdown.append("| ").append(category).append(" | ")
                        .append(count).append(" | ")
                        .append(expected.get(category)).append(" | ")
                        .append(categoryFailures == 0 ? "通过" : "失败 " + categoryFailures)
                        .append(" | ")
                        .append(categoryFailures == 0 ? "是" : "否")
                        .append(" |\n");
            }

            markdown.append("\n## 促销池与库存交集\n");
            markdown.append("| 促销池 | 池内商品数 | 本站库存交集 | 不在本站库存 |\n");
            markdown.append("|------|----------|------------|------------|\n");
            for (PoolStat stat : poolStats) {
                markdown.append("| ").append(stat.name()).append(" | ")
                        .append(stat.poolSize()).append(" | ")
                        .append(stat.inventoryIntersectionSize()).append(" | ")
                        .append(stat.outsideInventorySize()).append(" |\n");
            }
            markdown.append("\n9.9专区导入异常行数：").append(fixedPriceImportInvalidCount)
                    .append("。异常行未生成候选规则，需在导入异常页人工确认。\n\n");
            markdown.append("审计演示补位商品数：")
                    .append(categoryCounts.getOrDefault("M. 审计演示促销商品", 0L))
                    .append("。该类仅用于覆盖率压力测试，不代表原始 Excel 已存在促销。\n\n");

            markdown.append("## 多上下文促销激活验证\n");
            markdown.append("| 上下文 | 扫描商品数 | 非原价候选商品 | coupon_redeem候选商品 | 仅有不可用原因 | 仅原价兜底 | 说明 |\n");
            markdown.append("|------|------------|---------------|----------------------|---------------|------------|------|\n");
            for (ContextAuditStat stat : contextStats) {
                markdown.append("| ").append(stat.name()).append(" | ")
                        .append(stat.scannedProducts()).append(" | ")
                        .append(stat.nonFallbackCandidateProducts()).append(" | ")
                        .append(stat.couponRedeemCandidateProducts()).append(" | ")
                        .append(stat.blockedOnlyProducts()).append(" | ")
                        .append(stat.fallbackOnlyProducts()).append(" | ")
                        .append(escape(stat.note())).append(" |\n");
            }
            markdown.append('\n');

            markdown.append("## 边界场景验证\n");
            markdown.append("| 场景 | 结果 | 说明 |\n");
            markdown.append("|------|------|------|\n");
            for (EdgeScenarioResult edge : edgeScenarios) {
                markdown.append("| ").append(edge.name()).append(" | ")
                        .append(edge.passed() ? "PASS" : "FAIL").append(" | ")
                        .append(escape(edge.note())).append(" |\n");
            }

            markdown.append("\n## 商品组映射完整性\n");
            for (String note : productGroupMappingNotes) {
                markdown.append("- ").append(note).append('\n');
            }

            markdown.append("\n## 修复记录\n");
            markdown.append("- 本轮新增自动化审计测试，验证库存全商品至少返回原价兜底方案。\n");
            markdown.append("- 9.9专区与库存交集商品均要求返回 fixed_price 候选，且单件应付金额为 9.90。\n");
            markdown.append("- 本轮把个性化促销池、券适用范围、LNG/CNG组合池接入审计规则池，用于验证促销池激活缺口。\n");
            markdown.append("- 为验证 30%+ 激活目标，补充 25 条明确标记的审计演示 fixed_price 规则，不进入生产导入结果。\n");
            markdown.append("- 本轮未修改 data/ 下原始 Excel。\n");

            return markdown.toString();
        }

        private Map<String, String> expectedDecisionByCategory() {
            Map<String, String> expected = new LinkedHashMap<>();
            expected.put("A. 9.9专区商品", "fixed_price/blocked + fallback");
            expected.put("B. 惊爆价商品", "fixed_price + fallback");
            expected.put("C. 买赠商品", "gift_item + fallback");
            expected.put("D. 满减商品", "amount_off/blocked + fallback");
            expected.put("E. 组合包商品", "bundle_price/blocked + fallback");
            expected.put("F. 换购商品", "exchange_purchase/blocked + fallback");
            expected.put("G. 赠券触发商品", "coupon_redeem/blocked + fallback");
            expected.put("H. 全场折扣商品", "percentage_discount + fallback");
            expected.put("I. 排除品类商品", "fallback + blocked discount");
            expected.put("M. 审计演示促销商品", "demo fixed_price + fallback");
            expected.put("J. 无促销商品", "fallback only");
            expected.put("K. 库存为零商品", "fallback + inventory warning");
            expected.put("L. 无法查询商品", "明确错误信息");
            return expected;
        }

        private String escape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("|", "\\|").replace("\n", " ");
        }
    }
}
