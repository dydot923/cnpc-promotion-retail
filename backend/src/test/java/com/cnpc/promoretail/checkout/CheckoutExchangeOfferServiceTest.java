package com.cnpc.promoretail.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.model.BundleDefinition;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CheckoutExchangeOfferServiceTest {

    @Test
    void returnsBoardBundlesAndSingleItemOffersInOneList() {
        PromotionRule bundle = new PromotionRule(
                "abv2-bundle-driving", "驾驶包", PromotionRuleType.BUNDLE_PRICE, 20,
                "H1_EXCHANGE", false, PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(java.util.Set.of("701"), java.util.Set.of(), java.util.Set.of(com.cnpc.promoretail.ruleengine.context.FuelType.GASOLINE), java.util.Set.of(), java.util.Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.bundlePrice("bundle-driving", List.of(), new BigDecimal("25")), "v2");
        PromotionRule item = new PromotionRule(
                "abv2-h2-water", "小水4瓶", PromotionRuleType.EXCHANGE_PURCHASE, 20,
                "H2_EXCHANGE", false, PromotionRuleStatus.CONFIRMED,
                new PromotionCondition(java.util.Set.of("701"), java.util.Set.of(), java.util.Set.of(com.cnpc.promoretail.ruleengine.context.FuelType.GASOLINE), java.util.Set.of(), java.util.Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("180"), false, BigDecimal.ZERO),
                PromotionBenefit.exchangePurchase(new BigDecimal("2"), 4), "v2");
        ProductCatalogRepository catalog = new TestCatalog();
        BundleDefinitionProvider bundles = bundleId -> Optional.of(new BundleDefinition(
                "bundle-driving", "驾驶包", new BigDecimal("25"), new BigDecimal("200"),
                "activity-board", List.of(new BundleItem("701", 1))));
        CheckoutExchangeOfferService service = new CheckoutExchangeOfferService(
                new TestRules(List.of(bundle, item)), catalog,
                new DefaultConditionMatcher(), bundles);

        List<CheckoutExchangeOfferResponse> offers = service.findOffers(
                com.cnpc.promoretail.ruleengine.context.FuelType.GASOLINE,
                new BigDecimal("200"), null, "gas_station", "新疆");

        assertThat(offers).extracting(CheckoutExchangeOfferResponse::offerType)
                .contains("BUNDLE", "ITEM");
        assertThat(offers.stream().filter(offer -> "BUNDLE".equals(offer.offerType())).findFirst().orElseThrow()
                .bundleItems()).hasSize(1);
    }

    private static final class TestCatalog implements ProductCatalogRepository {
        @Override public void savePriceRows(com.cnpc.promoretail.importcenter.model.ImportVersion v, List<com.cnpc.promoretail.importcenter.model.PriceImportRow> r) {}
        @Override public void saveInventoryRows(com.cnpc.promoretail.importcenter.model.ImportVersion v, List<com.cnpc.promoretail.importcenter.model.InventoryImportRow> r) {}
        @Override public Optional<ProductCatalogItem> findByProductCode(String code) { return Optional.of(new ProductCatalogItem(code, null, code, "饮料", new BigDecimal("30"), new BigDecimal("20"), false)); }
        @Override public Optional<ProductCatalogItem> findByBarcode(String barcode) { return Optional.empty(); }
        @Override public List<ProductCatalogItem> search(String keyword, int limit) { return List.of(); }
        @Override public List<ProductCatalogItem> searchInventory(String keyword, int limit) { return List.of(); }
        @Override public List<ProductCatalogItem> findByProductCodes(java.util.Collection<String> codes) { return codes.stream().map(code -> findByProductCode(code).orElseThrow()).toList(); }
        @Override public Optional<BigDecimal> findInventoryQuantity(String code) { return Optional.of(new BigDecimal("20")); }
        @Override public void saveInventoryQuantity(String code, BigDecimal quantity, String importVersion) { }
    }

    private static final class TestRules implements PromotionRuleRepository {
        private final List<PromotionRule> rules;

        private TestRules(List<PromotionRule> rules) {
            this.rules = rules;
        }

        @Override public PromotionRuleDraft saveDraft(PromotionRuleDraft draft) { throw new UnsupportedOperationException(); }
        @Override public PromotionRuleDraft saveDraft(PromotionRuleDraft draft, boolean overwriteManualLocked) { throw new UnsupportedOperationException(); }
        @Override public Optional<PromotionRuleDraft> findDraftById(String draftId) { return Optional.empty(); }
        @Override public Optional<PromotionRuleDraft> findDraftByRuleId(String ruleId) { return Optional.empty(); }
        @Override public List<PromotionRuleDraft> findDraftsByStatus(PromotionRuleStatus status) { return List.of(); }
        @Override public PromotionRuleVersion saveVersion(PromotionRuleVersion version) { throw new UnsupportedOperationException(); }
        @Override public List<PromotionRule> findConfirmedRules() { return rules; }
        @Override public void appendAuditLog(PromotionRuleAuditLog auditLog) { }
        @Override public List<PromotionRuleAuditLog> findAuditLogsByRuleId(String ruleId) { return List.of(); }
    }
}
