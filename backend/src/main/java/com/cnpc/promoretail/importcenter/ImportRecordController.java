package com.cnpc.promoretail.importcenter;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImportRecordController {

    private final ImportRecordApplicationService importRecordApplicationService;

    public ImportRecordController(ImportRecordApplicationService importRecordApplicationService) {
        this.importRecordApplicationService = importRecordApplicationService;
    }

    @GetMapping("/api/import-batches")
    public ApiResponse<List<ImportBatch>> batches() {
        return ApiResponse.ok(importRecordApplicationService.batches());
    }

    @GetMapping("/api/import-batches/{importId}/errors")
    public ApiResponse<List<ImportErrorRow>> errors(
            @PathVariable String importId,
            @RequestParam(required = false) ImportErrorSeverity severity,
            @RequestParam(required = false) String sheetName,
            @RequestParam(required = false) ImportErrorCode errorCode
    ) {
        return ApiResponse.ok(importRecordApplicationService.errors(importId, severity, sheetName, errorCode));
    }

    @GetMapping("/api/import-batches/{importId}/errors/export")
    public ResponseEntity<byte[]> exportErrors(
            @PathVariable String importId,
            @RequestParam(required = false) ImportErrorSeverity severity,
            @RequestParam(required = false) String sheetName,
            @RequestParam(required = false) ImportErrorCode errorCode,
            @RequestParam(defaultValue = "system") String operatorId
    ) {
        byte[] csv = importRecordApplicationService.exportErrorsCsv(importId, severity, sheetName, errorCode, operatorId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(importId + "-errors.csv").build().toString())
                .body(csv);
    }
}
