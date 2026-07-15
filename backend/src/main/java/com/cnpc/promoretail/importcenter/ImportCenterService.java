package com.cnpc.promoretail.importcenter;

import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.importcenter.model.CouponImportRecord;
import com.cnpc.promoretail.importcenter.model.FixedPricePromotionImportRow;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.model.ImportType;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import com.cnpc.promoretail.importcenter.repository.ImportRecordRepository;
import com.cnpc.promoretail.product.repository.InMemoryProductCatalogRepository;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.promotion.coupon.CouponTemplateRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponRepository;
import com.cnpc.promoretail.promotion.coupon.InMemoryCouponTemplateRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImportCenterService {

    public static final String NINE_POINT_NINE_SHEET_NAME = "参考2-9.9元商品专区";
    public static final String MEMBER_BENEFIT_SHEET_NAME = "会员权益包";
    public static final String COUPON_SCOPE_SHEET_NAME = "参考3-会员生日&省区特色专用券范围";

    private final EasyExcelWorkbookReader workbookReader;
    private final CouponExcelImportMapper couponExcelImportMapper;
    private final ImportRecordRepository importRecordRepository;
    private final PromotionRuleGovernanceService promotionRuleGovernanceService;
    private final ProductCatalogRepository productCatalogRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponRepository couponRepository;

    public ImportCenterService(
            EasyExcelWorkbookReader workbookReader,
            ImportRecordRepository importRecordRepository,
            PromotionRuleGovernanceService promotionRuleGovernanceService
    ) {
        this(workbookReader, importRecordRepository, promotionRuleGovernanceService,
                new InMemoryProductCatalogRepository(), new InMemoryCouponTemplateRepository(),
                new InMemoryCouponRepository());
    }

    @Autowired
    public ImportCenterService(
            EasyExcelWorkbookReader workbookReader,
            ImportRecordRepository importRecordRepository,
            PromotionRuleGovernanceService promotionRuleGovernanceService,
            ProductCatalogRepository productCatalogRepository,
            CouponTemplateRepository couponTemplateRepository,
            CouponRepository couponRepository
    ) {
        this.workbookReader = workbookReader;
        this.couponExcelImportMapper = new CouponExcelImportMapper(workbookReader);
        this.importRecordRepository = importRecordRepository;
        this.promotionRuleGovernanceService = promotionRuleGovernanceService;
        this.productCatalogRepository = productCatalogRepository;
        this.couponTemplateRepository = couponTemplateRepository;
        this.couponRepository = couponRepository;
    }

    public ImportResult<PriceImportRow> importPrices(Path file) {
        ImportVersion version = ImportVersion.newVersion(ImportType.PRICE);
        List<RawExcelRow> rows = workbookReader.readSheet(file, 0, 1);
        List<PriceImportRow> records = new ArrayList<>();
        List<ImportErrorRow> errors = new ArrayList<>();
        int skipped = 0;

        for (RawExcelRow row : rows) {
            if (blank(row.cell(0)) && blank(row.cell(1)) && blank(row.cell(2)) && blank(row.cell(3))) {
                skipped++;
                continue;
            }
            try {
                if (blank(row.cell(2))) {
                    errors.add(warning(version, row, "商品条码", row.cell(2), ImportErrorCode.MISSING_BARCODE,
                            "商品条码为空，已按空条码导入，后续需人工补齐。"));
                }
                records.add(new PriceImportRow(
                        identifier(row.cell(0), "商品编码"),
                        required(row.cell(1), "商品名称"),
                        optionalIdentifier(row.cell(2)),
                        money(row.cell(3), "执行价"),
                        row.sheetName(),
                        row.rowNumber()
                ));
            } catch (ImportRowException exception) {
                errors.add(error(version, row, exception));
            }
        }

        productCatalogRepository.savePriceRows(version, records);
        return persist(result(version, ImportType.PRICE, file, records, skipped, errors, List.of()));
    }

    public ImportResult<InventoryImportRow> importInventory(Path file) {
        ImportVersion version = ImportVersion.newVersion(ImportType.INVENTORY);
        List<RawExcelRow> rows = workbookReader.readSheet(file, 0, 1);
        List<InventoryImportRow> records = new ArrayList<>();
        List<ImportErrorRow> errors = new ArrayList<>();
        int skipped = 0;

        for (RawExcelRow row : rows) {
            if (blank(row.cell(0)) && blank(row.cell(1)) && blank(row.cell(2)) && blank(row.cell(3))) {
                skipped++;
                continue;
            }
            try {
                records.add(new InventoryImportRow(
                        identifier(row.cell(0), "商品编码"),
                        required(row.cell(1), "商品名称"),
                        optionalIdentifier(row.cell(2)),
                        quantity(row.cell(3), "库存数量"),
                        row.sheetName(),
                        row.rowNumber()
                ));
            } catch (ImportRowException exception) {
                errors.add(error(version, row, exception));
            }
        }

        productCatalogRepository.saveInventoryRows(version, records);
        return persist(result(version, ImportType.INVENTORY, file, records, skipped, errors, List.of()));
    }

    public ImportResult<ImportedPromotionRule> importNinePointNineFixedPricePromotions(Path file) {
        ImportVersion version = ImportVersion.newVersion(ImportType.PROMOTION);
        List<ImportErrorRow> errors = new ArrayList<>();
        List<RawExcelRow> rows;
        try {
            rows = workbookReader.readSheet(file, NINE_POINT_NINE_SHEET_NAME, 3);
        } catch (RuntimeException exception) {
            errors.add(blocker(version, NINE_POINT_NINE_SHEET_NAME, "工作表不存在或读取失败: " + exception.getMessage()));
            return persist(result(version, ImportType.PROMOTION, file, List.of(), 0, errors, List.of()));
        }

        List<ImportedPromotionRule> records = new ArrayList<>();
        int skipped = 0;
        String inheritedCategory = "";

        for (RawExcelRow row : rows) {
            if (blank(row.cell(2)) && blank(row.cell(3)) && blank(row.cell(9))) {
                skipped++;
                continue;
            }
            try {
                if (!blank(row.cell(1))) {
                    inheritedCategory = row.cell(1);
                } else if (!blank(inheritedCategory)) {
                    errors.add(warning(version, row, "商品品类", row.cell(1), ImportErrorCode.INHERITED_BLANK_VALUE,
                            "商品品类为空，已按上一条非空商品品类继承。"));
                }
                FixedPricePromotionImportRow parsedRow = new FixedPricePromotionImportRow(
                        identifier(row.cell(2), "商品编码"),
                        required(row.cell(3), "商品名称"),
                        inheritedCategory,
                        money(row.cell(6), "挂牌零售价"),
                        integer(row.cell(8), "数量"),
                        money(row.cell(9), "促销金额"),
                        decimal(row.cell(10), "促销后毛利率"),
                        row.sheetName(),
                        row.rowNumber()
                );
                records.add(toFixedPriceCandidateRule(parsedRow, version));
            } catch (ImportRowException exception) {
                errors.add(error(version, row, exception));
            }
        }

        List<String> warnings = List.of(
                "本阶段仅生成 fixed_price 候选规则，未落库，未覆盖人工修正规则。",
                "商品品类为空时按上一条非空商品品类继承；商品编码、商品名称和促销金额不继承。"
        );
        ImportResult<ImportedPromotionRule> result = persist(
                result(version, ImportType.PROMOTION, file, records, skipped, errors, warnings));
        result.records().forEach(rule -> promotionRuleGovernanceService.createDraft(rule, "importcenter"));
        return result;
    }

    public ImportResult<CouponImportRecord> importCoupons(Path file) {
        ImportVersion version = ImportVersion.newVersion(ImportType.COUPON);
        List<ImportErrorRow> errors = new ArrayList<>();
        List<RawExcelRow> rows;
        try {
            rows = workbookReader.readSheet(file, MEMBER_BENEFIT_SHEET_NAME, 3);
        } catch (RuntimeException exception) {
            errors.add(blocker(version, MEMBER_BENEFIT_SHEET_NAME, "工作表不存在或读取失败: " + exception.getMessage()));
            return persist(result(version, ImportType.COUPON, file, List.of(), 0, errors, List.of()));
        }

        Map<String, List<String>> scopedProductCodes =
                couponExcelImportMapper.readCouponScopeProductCodes(file, version, errors);
        List<CouponImportRecord> records = new ArrayList<>();
        int skipped = 0;
        String inheritedSalesChannel = "";
        String inheritedPackageType = "";

        for (RawExcelRow row : rows) {
            if (!blank(row.cell(0))) {
                inheritedSalesChannel = row.cell(0);
            }
            if (!blank(row.cell(2))) {
                inheritedPackageType = row.cell(2);
            }
            try {
                var mapping = couponExcelImportMapper.mapMemberBenefitRow(row, version, inheritedSalesChannel,
                        inheritedPackageType, scopedProductCodes);
                if (mapping.isEmpty()) {
                    skipped++;
                    continue;
                }
                couponTemplateRepository.save(mapping.get().couponTemplate());
                mapping.get().couponInstances().forEach(couponRepository::save);
                records.add(new CouponImportRecord(version, row.sheetName(), row.rowNumber(),
                        mapping.get().couponTemplate(), mapping.get().couponInstances(), mapping.get().mappingNote()));
            } catch (CouponExcelImportMapper.CouponImportRowException exception) {
                if (exception.severity() == ImportErrorSeverity.WARNING) {
                    skipped++;
                }
                errors.add(exception.toError(version, row));
            }
        }

        List<String> warnings = List.of(
                "已从“会员权益包”导入可解析面额的券模板；无法可靠解析面额的折扣券/自然语言券进入 warning，不静默丢弃。",
                "已从“参考3-会员生日&省区特色专用券范围”聚合可核销商品编码，并回填到匹配券模板的 applicableProductCodes。",
                "活动看板源数据通常只描述券模板和发放数量；只有显式出现券号/券实例ID/couponId 的行才生成 coupon 实例。"
        );
        return persist(result(version, ImportType.COUPON, file, records, skipped, errors, warnings));
    }

    private <T> ImportResult<T> persist(ImportResult<T> result) {
        importRecordRepository.saveImportBatch(result.importBatch());
        importRecordRepository.saveErrorRows(result.errors());
        return result;
    }

    private ImportedPromotionRule toFixedPriceCandidateRule(FixedPricePromotionImportRow row, ImportVersion version) {
        PromotionRule rule = new PromotionRule(
                "import-fixed-9_9-" + row.productCode(),
                "9.9元专区-" + row.productName(),
                PromotionRuleType.FIXED_PRICE,
                50,
                "direct_discount",
                false,
                PromotionRuleStatus.PENDING_CONFIRMATION,
                new PromotionCondition(Set.of(row.productCode()), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ONE),
                PromotionBenefit.fixedPrice(row.fixedPrice()),
                version.value()
        );
        return new ImportedPromotionRule(version, row.sheetName(), row.rowNumber(), rule);
    }

    private <T> ImportResult<T> result(
            ImportVersion version,
            ImportType importType,
            Path sourceFile,
            List<T> records,
            int skipped,
            List<ImportErrorRow> errors,
            List<String> warnings
    ) {
        int invalidCount = (int) errors.stream()
                .filter(error -> error.severity() == ImportErrorSeverity.ERROR
                        || error.severity() == ImportErrorSeverity.BLOCKER)
                .count();
        int warningCount = (int) errors.stream()
                .filter(error -> error.severity() == ImportErrorSeverity.WARNING)
                .count();
        ImportBatch batch = new ImportBatch(version, importType, sourceFile.toString(), records.size(), 0,
                skipped, invalidCount, warningCount, null);
        return new ImportResult<>(version, importType, batch, records.size(), 0, skipped, invalidCount, warningCount,
                records, errors, warnings);
    }

    private ImportErrorRow error(ImportVersion version, RawExcelRow row, ImportRowException exception) {
        return new ImportErrorRow(version, row.sheetName(), row.rowNumber(), exception.columnName(),
                exception.rawValue(), exception.errorCode(), row.cells(), exception.getMessage(), exception.severity());
    }

    private ImportErrorRow warning(
            ImportVersion version,
            RawExcelRow row,
            String columnName,
            String rawValue,
            ImportErrorCode errorCode,
            String message
    ) {
        return new ImportErrorRow(version, row.sheetName(), row.rowNumber(), columnName, rawValue,
                errorCode, row.cells(), message, ImportErrorSeverity.WARNING);
    }

    private ImportErrorRow blocker(ImportVersion version, String sheetName, String message) {
        return new ImportErrorRow(version, sheetName, 0, "", "", ImportErrorCode.SHEET_NOT_FOUND,
                List.of(), message, ImportErrorSeverity.BLOCKER);
    }

    private String required(String value, String fieldName) {
        if (blank(value)) {
            throw new ImportRowException(fieldName, value, missingCode(fieldName), fieldName + "不能为空");
        }
        return value.trim();
    }

    private String identifier(String value, String fieldName) {
        String text = required(value, fieldName);
        return normalizeIdentifier(text);
    }

    private String optionalIdentifier(String value) {
        if (blank(value)) {
            return null;
        }
        return normalizeIdentifier(value.trim());
    }

    private String normalizeIdentifier(String text) {
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    private BigDecimal money(String value, String fieldName) {
        return decimal(value, fieldName).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(String value, String fieldName) {
        return decimal(value, fieldName).setScale(2, RoundingMode.HALF_UP);
    }

    private int integer(String value, String fieldName) {
        BigDecimal decimal = decimal(value, fieldName);
        try {
            return decimal.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException exception) {
            throw new ImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT, fieldName + "必须是整数");
        }
    }

    private BigDecimal decimal(String value, String fieldName) {
        if (blank(value)) {
            throw new ImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT, fieldName + "不能为空");
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("%", "");
        if (normalized.startsWith("#")) {
            throw new ImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT,
                    fieldName + "不是可解析数值: " + value);
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new ImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT,
                    fieldName + "不是可解析数值: " + value);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ImportErrorCode missingCode(String fieldName) {
        return "商品编码".equals(fieldName) ? ImportErrorCode.MISSING_PRODUCT_CODE : ImportErrorCode.MISSING_REQUIRED_FIELD;
    }

    private static final class ImportRowException extends RuntimeException {

        private final String columnName;
        private final String rawValue;
        private final ImportErrorCode errorCode;
        private final ImportErrorSeverity severity;

        private ImportRowException(String columnName, String rawValue, ImportErrorCode errorCode, String message) {
            this(columnName, rawValue, errorCode, message, ImportErrorSeverity.ERROR);
        }

        private ImportRowException(
                String columnName,
                String rawValue,
                ImportErrorCode errorCode,
                String message,
                ImportErrorSeverity severity
        ) {
            super(message);
            this.columnName = columnName;
            this.rawValue = rawValue == null ? "" : rawValue;
            this.errorCode = errorCode;
            this.severity = severity == null ? ImportErrorSeverity.ERROR : severity;
        }

        private String columnName() {
            return columnName;
        }

        private String rawValue() {
            return rawValue;
        }

        private ImportErrorCode errorCode() {
            return errorCode;
        }

        private ImportErrorSeverity severity() {
            return severity;
        }
    }
}
