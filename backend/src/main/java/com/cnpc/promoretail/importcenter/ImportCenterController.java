package com.cnpc.promoretail.importcenter;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/import")
public class ImportCenterController {

    private static final Logger log = LoggerFactory.getLogger(ImportCenterController.class);

    private final ImportCenterService importCenterService;

    public ImportCenterController(ImportCenterService importCenterService) {
        this.importCenterService = importCenterService;
    }

    @PostMapping(value = "/prices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult<PriceImportRow>> importPrices(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(withTemporaryFile(file, importCenterService::importPrices));
    }

    @PostMapping(value = "/inventory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult<InventoryImportRow>> importInventory(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(withTemporaryFile(file, importCenterService::importInventory));
    }

    @PostMapping(value = "/promotions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult<ImportedPromotionRule>> importPromotions(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(withTemporaryFile(file, importCenterService::importNinePointNineFixedPricePromotions));
    }

    private <T> T withTemporaryFile(MultipartFile file, FileImporter<T> importer) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("promotion-retail-import-", ".xlsx");
            file.transferTo(tempFile);
            return importer.importFile(tempFile);
        } catch (IOException exception) {
            throw new IllegalStateException("导入文件暂存失败", exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException exception) {
                    log.warn("导入临时文件清理失败: {}", tempFile, exception);
                }
            }
        }
    }

    @FunctionalInterface
    private interface FileImporter<T> {
        T importFile(Path file);
    }
}
