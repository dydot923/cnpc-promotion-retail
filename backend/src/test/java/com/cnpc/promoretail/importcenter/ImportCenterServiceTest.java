package com.cnpc.promoretail.importcenter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.CouponImportRecord;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.model.ImportType;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import com.cnpc.promoretail.importcenter.repository.InMemoryImportRecordRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponTemplateRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportCenterServiceTest {

    private final InMemoryImportRecordRepository importRecordRepository = new InMemoryImportRecordRepository();
    private final InMemoryPromotionRuleRepository promotionRuleRepository = new InMemoryPromotionRuleRepository();
    private final PromotionRuleGovernanceService governanceService =
            new PromotionRuleGovernanceService(promotionRuleRepository);
    private final ImportCenterService importCenterService = new ImportCenterService(
            new EasyExcelWorkbookReader(), importRecordRepository, governanceService);

    @Test
    void importsPriceRowsFromRealExcel() {
        ImportResult<PriceImportRow> result = importCenterService.importPrices(dataFile("价格.xlsx"));

        assertThat(result.importType()).isEqualTo(ImportType.PRICE);
        assertThat(result.insertedCount()).isGreaterThan(12_000);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.invalidCount()).isZero();
        assertThat(result.warningCount()).isEqualTo(64);
        assertThat(result.importBatch().warningCount()).isEqualTo(64);
        assertThat(result.errors()).hasSize(64);
        assertThat(result.errors()).allSatisfy(error -> {
            assertThat(error.severity()).isEqualTo(ImportErrorSeverity.WARNING);
            assertThat(error.errorCode()).isEqualTo(ImportErrorCode.MISSING_BARCODE);
            assertThat(error.columnName()).isEqualTo("商品条码");
        });
        assertThat(result.records().getFirst().productCode()).isEqualTo("454566");
        assertThat(result.records().getFirst().barcode()).isEqualTo("2000004545666");
        assertThat(result.records().getFirst().executionPrice()).isEqualByComparingTo("2800.00");
        assertThat(result.records().getFirst().executionPrice().scale()).isEqualTo(2);
        assertThat(importRecordRepository.findAllBatches()).hasSize(1);
        assertThat(importRecordRepository.findErrorRowsByImportId(result.importVersion())).hasSize(64);
    }

    @Test
    void importsInventoryRowsFromRealExcel() {
        ImportResult<InventoryImportRow> result = importCenterService.importInventory(dataFile("库存.xlsx"));

        assertThat(result.importType()).isEqualTo(ImportType.INVENTORY);
        assertThat(result.insertedCount()).isGreaterThan(400);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.invalidCount()).isZero();
        assertThat(result.errors()).isEmpty();
        assertThat(result.records().getFirst().productCode()).isEqualTo("70647044");
        assertThat(result.records().getFirst().barcode()).isEqualTo("6978082410274");
        assertThat(result.records().getFirst().inventoryQuantity()).isEqualByComparingTo("5.00");
        assertThat(importRecordRepository.findAllBatches()).hasSize(1);
        assertThat(importRecordRepository.findErrorRowsByImportId(result.importVersion())).isEmpty();
    }

    @Test
    void importsNinePointNineSheetAsPendingFixedPriceCandidateRules() {
        ImportResult<ImportedPromotionRule> result = importCenterService.importNinePointNineFixedPricePromotions(dataFile("活动看板.xlsx"));

        assertThat(result.importType()).isEqualTo(ImportType.PROMOTION);
        assertThat(result.insertedCount()).isGreaterThan(100);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.invalidCount()).isEqualTo(4);
        assertThat(result.errors()).hasSize(4);
        assertThat(result.errors()).allSatisfy(error -> {
            assertThat(error.severity()).isEqualTo(ImportErrorSeverity.ERROR);
            assertThat(error.errorCode()).isEqualTo(ImportErrorCode.MISSING_PRODUCT_CODE);
            assertThat(error.columnName()).isEqualTo("商品编码");
            assertThat(error.errorMessage()).contains("商品编码");
        });
        assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("候选规则"));

        ImportedPromotionRule firstImportedRule = result.records().getFirst();
        PromotionRule firstRule = firstImportedRule.rule();
        assertThat(firstImportedRule.importId()).isEqualTo(result.importVersion());
        assertThat(firstImportedRule.sourceSheetName()).isEqualTo(ImportCenterService.NINE_POINT_NINE_SHEET_NAME);
        assertThat(firstImportedRule.sourceRowNumber()).isEqualTo(4);
        assertThat(firstRule.ruleId()).isEqualTo("import-fixed-9_9-70424725");
        assertThat(firstRule.ruleType()).isEqualTo(PromotionRuleType.FIXED_PRICE);
        assertThat(firstRule.status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
        assertThat(firstRule.condition().productCodes()).containsExactly("70424725");
        assertThat(firstRule.benefit().fixedPrice()).isEqualByComparingTo("9.90");
        assertThat(firstRule.version()).isEqualTo(result.importVersion().value());
        assertThat(importRecordRepository.findAllBatches()).hasSize(1);
        assertThat(importRecordRepository.findErrorRowsByImportId(result.importVersion())).hasSize(4);
        assertThat(promotionRuleRepository.findDraftByRuleId("import-fixed-9_9-70424725"))
                .hasValueSatisfying(draft -> {
                    assertThat(draft.status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
                    assertThat(draft.rule().status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
                    assertThat(draft.sourceImportId()).isEqualTo(result.importVersion().value());
                });
        assertThat(promotionRuleRepository.findConfirmedRules()).isEmpty();
    }

    @Test
    void importsCouponTemplatesFromRealActivityWorkbookWithoutCreatingCustomerInstances() {
        InMemoryImportRecordRepository importRepository = new InMemoryImportRecordRepository();
        InMemoryCouponTemplateRepository couponTemplateRepository = new InMemoryCouponTemplateRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        ImportCenterService service = new ImportCenterService(new EasyExcelWorkbookReader(), importRepository,
                governanceService, new com.cnpc.promoretail.product.repository.InMemoryProductCatalogRepository(),
                couponTemplateRepository, couponRepository);

        ImportResult<CouponImportRecord> result = service.importCoupons(dataFile("活动看板.xlsx"));

        assertThat(result.importType()).isEqualTo(ImportType.COUPON);
        assertThat(result.insertedCount()).isGreaterThan(20);
        assertThat(result.warningCount()).isGreaterThan(0);
        assertThat(result.invalidCount()).isZero();
        assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("券模板"));
        assertThat(couponTemplateRepository.findAll()).hasSize(result.insertedCount());
        assertThat(couponRepository.findAll()).isEmpty();

        assertThat(result.records())
                .filteredOn(record -> record.couponTemplate().couponName().contains("便利店通用券"))
                .anySatisfy(record -> {
                    assertThat(record.couponTemplate().faceValue()).isEqualByComparingTo("12.00");
                    assertThat(record.couponTemplate().minSpendAmount()).isEqualByComparingTo("50.00");
                    assertThat(record.couponTemplate().applicableCategories()).contains("便利店商品");
                    assertThat(record.couponTemplate().excludedCategories()).contains("香烟", "化肥");
                    assertThat(record.couponTemplate().issueQuantity()).isGreaterThan(0);
                    assertThat(record.couponTemplate().memberOnly()).isTrue();
                    assertThat(record.couponInstances()).isEmpty();
                    assertThat(record.mappingNote()).contains("未提供顾客券号");
                });
        assertThat(result.errors())
                .anySatisfy(error -> {
                    assertThat(error.severity()).isEqualTo(ImportErrorSeverity.WARNING);
                    assertThat(error.errorCode()).isEqualTo(ImportErrorCode.UNPARSEABLE_COUPON_TEMPLATE);
                });
        assertThat(importRepository.findAllBatches()).hasSize(1);
        assertThat(importRepository.findErrorRowsByImportId(result.importVersion())).hasSize(result.warningCount());
    }

    @Test
    void importsExplicitCouponInstanceWhenSourceRowContainsCouponId() throws Exception {
        InMemoryImportRecordRepository importRepository = new InMemoryImportRecordRepository();
        InMemoryCouponTemplateRepository couponTemplateRepository = new InMemoryCouponTemplateRepository();
        InMemoryCouponRepository couponRepository = new InMemoryCouponRepository();
        ImportCenterService service = new ImportCenterService(couponWorkbookReaderWithExplicitCouponId(), importRepository,
                governanceService, new com.cnpc.promoretail.product.repository.InMemoryProductCatalogRepository(),
                couponTemplateRepository, couponRepository);

        ImportResult<CouponImportRecord> result = service.importCoupons(Path.of("coupon-instance-test.xlsx"));

        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isZero();
        assertThat(couponTemplateRepository.findAll()).hasSize(1);
        assertThat(couponRepository.findByCouponId("coupon-001"))
                .hasValueSatisfying(coupon -> {
                    assertThat(coupon.couponName()).contains("便利店券");
                    assertThat(coupon.faceValue()).isEqualByComparingTo("5.00");
                    assertThat(coupon.minSpendAmount()).isEqualByComparingTo("40.00");
                    assertThat(coupon.excludedCategories()).contains("香烟");
                });
        assertThat(result.records().getFirst().couponInstances())
                .extracting(coupon -> coupon.couponId())
                .containsExactly("coupon-001");
    }

    @Test
    void importErrorApplicationServiceFiltersExportsAndWritesAudit() {
        InMemoryImportRecordRepository repository = new InMemoryImportRecordRepository();
        InMemoryAuditLogRepository auditRepository = new InMemoryAuditLogRepository();
        ImportRecordApplicationService service =
                new ImportRecordApplicationService(repository, new DefaultAuditLogService(auditRepository));
        ImportVersion importId = new ImportVersion("import-test");
        repository.saveImportBatch(new ImportBatch(importId, ImportType.PRICE, "price.xlsx",
                1, 0, 0, 1, 1, Instant.now()));
        repository.saveErrorRows(List.of(
                new ImportErrorRow(importId, "sheet-a", 2, "商品条码", "",
                        ImportErrorCode.MISSING_BARCODE, List.of(), "missing barcode", ImportErrorSeverity.WARNING),
                new ImportErrorRow(importId, "sheet-b", 3, "商品编码", "",
                        ImportErrorCode.MISSING_PRODUCT_CODE, List.of(), "missing product", ImportErrorSeverity.ERROR)
        ));

        List<ImportErrorRow> filtered = service.errors(importId.value(), ImportErrorSeverity.ERROR, "sheet-b",
                ImportErrorCode.MISSING_PRODUCT_CODE);
        String csv = new String(service.exportErrorsCsv(importId.value(), ImportErrorSeverity.ERROR, "sheet-b",
                ImportErrorCode.MISSING_PRODUCT_CODE, "auditor"), StandardCharsets.UTF_8);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.getFirst().rowNumber()).isEqualTo(3);
        assertThat(csv).contains("importId,sheetName,rowNumber,columnName,rawValue,errorCode,errorMessage,severity");
        assertThat(csv).contains("missing product");
        assertThat(auditRepository.findByEntity("IMPORT_BATCH", importId.value()))
                .extracting(log -> log.actionType())
                .containsExactly("IMPORT_ERRORS_EXPORT");
    }

    private Path dataFile(String fileName) {
        Path path = Path.of("..", "data", fileName);
        assertThat(Files.exists(path)).as("data file exists: %s", fileName).isTrue();
        return path;
    }

    private EasyExcelWorkbookReader couponWorkbookReaderWithExplicitCouponId() {
        return new EasyExcelWorkbookReader() {
            @Override
            public List<RawExcelRow> readSheet(Path file, String sheetName, int headRowNumber) {
                if (ImportCenterService.MEMBER_BENEFIT_SHEET_NAME.equals(sheetName)) {
                    return List.of(new RawExcelRow(sheetName, 4, List.of(
                            "油惠新疆", "", "测试权益包", "", "5元便利店券（满40元可用）", "1",
                            "除香烟；有效期30天；券号: coupon-001"
                    )));
                }
                if (ImportCenterService.COUPON_SCOPE_SHEET_NAME.equals(sheetName)) {
                    return List.of(new RawExcelRow(sheetName, 4, List.of(
                            "", "便利店商品", "70000001", "", "", "", "", "", "便利店券可核"
                    )));
                }
                return List.of();
            }
        };
    }
}
