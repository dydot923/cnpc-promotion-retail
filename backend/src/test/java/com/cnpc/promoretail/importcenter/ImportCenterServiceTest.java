package com.cnpc.promoretail.importcenter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.model.ImportType;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.importcenter.repository.InMemoryImportRecordRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Path dataFile(String fileName) {
        Path path = Path.of("..", "data", fileName);
        assertThat(Files.exists(path)).as("data file exists: %s", fileName).isTrue();
        return path;
    }
}
