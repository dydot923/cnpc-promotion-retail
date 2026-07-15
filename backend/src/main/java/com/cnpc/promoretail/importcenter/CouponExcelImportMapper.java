package com.cnpc.promoretail.importcenter;

import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.RawExcelRow;
import com.cnpc.promoretail.promotion.coupon.CouponTemplate;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CouponExcelImportMapper {

    private static final Pattern MONEY_AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*元(?=[^\\n，,；;。]*券)");
    private static final Pattern MIN_SPEND_PATTERN = Pattern.compile("满\\s*(\\d+(?:\\.\\d+)?)\\s*元?\\s*(?:可用|使用|可核)");
    private static final Pattern VALID_DAYS_PATTERN = Pattern.compile("有效期\\s*(\\d+)\\s*天");
    private static final Pattern PER_CUSTOMER_LIMIT_PATTERN = Pattern.compile("每(?:人|客户)限\\s*(\\d+)\\s*张");
    private static final Pattern EXPLICIT_COUPON_ID_PATTERN =
            Pattern.compile("(?:券实例ID|券号|couponId)[:：]\\s*([A-Za-z0-9_-]+)");
    private static final Pattern COUPON_SCOPE_NAME_PATTERN = Pattern.compile("([^、，,；;\\s]+券)");

    private final EasyExcelWorkbookReader workbookReader;

    CouponExcelImportMapper(EasyExcelWorkbookReader workbookReader) {
        this.workbookReader = workbookReader;
    }

    Map<String, List<String>> readCouponScopeProductCodes(
            Path file,
            ImportVersion version,
            List<ImportErrorRow> errors
    ) {
        List<RawExcelRow> rows;
        try {
            rows = workbookReader.readSheet(file, ImportCenterService.COUPON_SCOPE_SHEET_NAME, 3);
        } catch (RuntimeException exception) {
            errors.add(sheetWarning(version, ImportCenterService.COUPON_SCOPE_SHEET_NAME, "可核销券种", "",
                    ImportErrorCode.SHEET_NOT_FOUND, "券适用范围表读取失败，券模板将不会回填商品编码: " + exception.getMessage()));
            return Map.of();
        }

        Map<String, LinkedHashSet<String>> scopedCodes = new LinkedHashMap<>();
        for (RawExcelRow row : rows) {
            String productCode = optionalIdentifier(row.cell(2));
            String couponScope = row.cell(8);
            if (blank(productCode) || blank(couponScope)) {
                continue;
            }
            for (String couponName : extractCouponScopeNames(couponScope)) {
                scopedCodes.computeIfAbsent(couponName, ignored -> new LinkedHashSet<>()).add(productCode);
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        scopedCodes.forEach((couponName, productCodes) -> result.put(couponName, List.copyOf(productCodes)));
        return result;
    }

    Optional<CouponMapping> mapMemberBenefitRow(
            RawExcelRow row,
            ImportVersion version,
            String inheritedSalesChannel,
            String inheritedPackageType,
            Map<String, List<String>> scopedProductCodes
    ) {
        if (blank(row.cell(4)) || !row.cell(4).contains("券")) {
            return Optional.empty();
        }

        CouponTemplate template = toCouponTemplate(row, inheritedSalesChannel, inheritedPackageType,
                scopedProductCodes);
        List<Coupon> instances = toExplicitCouponInstances(row, template);
        return Optional.of(new CouponMapping(version, row, template, instances,
                instances.isEmpty()
                        ? "源行是券定义/权益包明细，未提供顾客券号，因此仅生成券模板。"
                        : "源行提供显式券号，已生成券实例。"));
    }

    private CouponTemplate toCouponTemplate(
            RawExcelRow row,
            String inheritedSalesChannel,
            String inheritedPackageType,
            Map<String, List<String>> scopedProductCodes
    ) {
        String couponName = required(row.cell(4), "券明细").replaceAll("\\s+", " ").trim();
        String sourceText = couponName + " " + row.cell(6);
        BigDecimal faceValue = parseMoneyAmount(sourceText);
        if (faceValue == null || faceValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponImportRowException("券明细", row.cell(4), ImportErrorCode.UNPARSEABLE_COUPON_TEMPLATE,
                    "无法从券明细解析券面额，已进入待确认: " + row.cell(4), ImportErrorSeverity.WARNING);
        }

        int issueQuantity = optionalInteger(row.cell(5), 1, "数量");
        List<String> applicableProductCodes = scopedProductCodesFor(couponName, scopedProductCodes);
        List<String> redeemChannels = blank(inheritedSalesChannel) ? List.of() : List.of(inheritedSalesChannel);
        String templateId = "import-coupon-template-member-benefit-r" + row.rowNumber();

        return new CouponTemplate(
                templateId,
                couponName,
                faceValue,
                parseMinSpend(sourceText),
                inferApplicableCategories(sourceText + " " + inheritedPackageType),
                inferExcludedCategories(sourceText),
                applicableProductCodes,
                List.of(),
                parseValidDays(sourceText),
                issueQuantity,
                parsePerCustomerLimit(sourceText),
                redeemChannels,
                true,
                false
        );
    }

    private List<Coupon> toExplicitCouponInstances(RawExcelRow row, CouponTemplate template) {
        String sourceText = row.cell(4) + " " + row.cell(6);
        List<String> couponIds = extractExplicitCouponIds(sourceText);
        if (couponIds.isEmpty()) {
            return List.of();
        }
        LocalDate validFrom = LocalDate.now();
        LocalDate validUntil = template.validDays() <= 0 ? null : validFrom.plusDays(template.validDays());
        LocalDateTime issuedAt = LocalDateTime.now();
        return couponIds.stream()
                .map(couponId -> template.toCoupon(couponId, validFrom, validUntil, issuedAt))
                .toList();
    }

    private List<String> extractCouponScopeNames(String value) {
        if (blank(value)) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = COUPON_SCOPE_NAME_PATTERN.matcher(value);
        while (matcher.find()) {
            String name = normalizeCouponScopeName(matcher.group(1));
            if (!blank(name)) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    private String normalizeCouponScopeName(String value) {
        if (blank(value)) {
            return "";
        }
        String normalized = value.replace("均可核", "")
                .replace("可核", "")
                .replace("均可用", "")
                .replace("可用", "")
                .trim();
        int couponIndex = normalized.indexOf("券");
        if (couponIndex >= 0) {
            normalized = normalized.substring(0, couponIndex + 1);
        }
        return normalized;
    }

    private List<String> scopedProductCodesFor(String couponName, Map<String, List<String>> scopedProductCodes) {
        if (scopedProductCodes.isEmpty()) {
            return List.of();
        }
        String normalizedCouponName = normalizeForCouponMatching(couponName);
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        scopedProductCodes.forEach((scopeName, codes) -> {
            String normalizedScopeName = normalizeForCouponMatching(scopeName);
            if (normalizedCouponName.contains(normalizedScopeName)
                    || normalizedScopeName.contains(normalizedCouponName)
                    || sameProvinceSpecialCoupon(normalizedCouponName, normalizedScopeName)) {
                productCodes.addAll(codes);
            }
        });
        return List.copyOf(productCodes);
    }

    private boolean sameProvinceSpecialCoupon(String couponName, String scopeName) {
        return couponName.contains("省区特色")
                && scopeName.contains("省区特色")
                && ((couponName.contains("5折") && scopeName.contains("5折"))
                || (couponName.contains("7折") && scopeName.contains("7折")));
    }

    private String normalizeForCouponMatching(String value) {
        return value == null ? "" : value
                .replaceAll("\\s+", "")
                .replace("商品", "")
                .replace("（", "(")
                .replace("）", ")")
                .toLowerCase(Locale.ROOT);
    }

    private BigDecimal parseMoneyAmount(String value) {
        Matcher matcher = MONEY_AMOUNT_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseMinSpend(String value) {
        Matcher matcher = MIN_SPEND_PATTERN.matcher(value);
        if (!matcher.find()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
    }

    private int parseValidDays(String value) {
        Matcher matcher = VALID_DAYS_PATTERN.matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int parsePerCustomerLimit(String value) {
        Matcher matcher = PER_CUSTOMER_LIMIT_PATTERN.matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private List<String> inferApplicableCategories(String value) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        if (containsAny(value, "汽油", "高标号")) {
            categories.add("汽油");
        }
        if (value.contains("柴油")) {
            categories.add("柴油");
        }
        if (containsAny(value, "LNG", "天然气")) {
            categories.add("LNG");
        }
        if (value.contains("CNG")) {
            categories.add("CNG");
        }
        if (containsAny(value, "便利店", "非油", "商品券", "通用券")) {
            categories.add("便利店商品");
        }
        if (value.contains("洗车")) {
            categories.add("洗车");
        }
        if (value.contains("家庭食品")) {
            categories.add("家庭食品");
        }
        if (value.contains("省区特色")) {
            categories.add("省区特色商品");
        }
        if (containsAny(value, "汽车清洁", "养护")) {
            categories.add("汽车清洁养护");
        }
        return List.copyOf(categories);
    }

    private List<String> inferExcludedCategories(String value) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        if (value.contains("除香烟") || value.contains("不含香烟")) {
            categories.add("香烟");
        }
        if (value.contains("除化肥") || value.contains("、化肥") || value.contains("不含化肥")) {
            categories.add("化肥");
        }
        return List.copyOf(categories);
    }

    private List<String> extractExplicitCouponIds(String value) {
        LinkedHashSet<String> couponIds = new LinkedHashSet<>();
        Matcher matcher = EXPLICIT_COUPON_ID_PATTERN.matcher(value);
        while (matcher.find()) {
            couponIds.add(normalizeIdentifier(matcher.group(1)));
        }
        return List.copyOf(couponIds);
    }

    private boolean containsAny(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int optionalInteger(String value, int defaultValue, String fieldName) {
        if (blank(value)) {
            return defaultValue;
        }
        BigDecimal decimal = decimal(value, fieldName);
        try {
            return decimal.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException exception) {
            throw new CouponImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT,
                    fieldName + "必须是整数", ImportErrorSeverity.ERROR);
        }
    }

    private BigDecimal decimal(String value, String fieldName) {
        if (blank(value)) {
            throw new CouponImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT,
                    fieldName + "不能为空", ImportErrorSeverity.ERROR);
        }
        String normalized = value.trim().replace(",", "").replace("%", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new CouponImportRowException(fieldName, value, ImportErrorCode.INVALID_AMOUNT,
                    fieldName + "不是可解析数值: " + value, ImportErrorSeverity.ERROR);
        }
    }

    private String required(String value, String fieldName) {
        if (blank(value)) {
            throw new CouponImportRowException(fieldName, value, ImportErrorCode.MISSING_REQUIRED_FIELD,
                    fieldName + "不能为空", ImportErrorSeverity.ERROR);
        }
        return value.trim();
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ImportErrorRow sheetWarning(
            ImportVersion version,
            String sheetName,
            String columnName,
            String rawValue,
            ImportErrorCode errorCode,
            String message
    ) {
        return new ImportErrorRow(version, sheetName, 0, columnName, rawValue,
                errorCode, List.of(), message, ImportErrorSeverity.WARNING);
    }

    record CouponMapping(
            ImportVersion importId,
            RawExcelRow sourceRow,
            CouponTemplate couponTemplate,
            List<Coupon> couponInstances,
            String mappingNote
    ) {
    }

    static final class CouponImportRowException extends RuntimeException {

        private final String columnName;
        private final String rawValue;
        private final ImportErrorCode errorCode;
        private final ImportErrorSeverity severity;

        private CouponImportRowException(
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

        ImportErrorRow toError(ImportVersion version, RawExcelRow row) {
            return new ImportErrorRow(version, row.sheetName(), row.rowNumber(), columnName,
                    rawValue, errorCode, row.cells(), getMessage(), severity);
        }

        ImportErrorSeverity severity() {
            return severity;
        }
    }
}
