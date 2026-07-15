package com.cnpc.promoretail.importcenter;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.repository.ImportRecordRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImportRecordApplicationService {

    private final ImportRecordRepository importRecordRepository;
    private final AuditLogService auditLogService;

    public ImportRecordApplicationService(
            ImportRecordRepository importRecordRepository,
            AuditLogService auditLogService
    ) {
        this.importRecordRepository = importRecordRepository;
        this.auditLogService = auditLogService;
    }

    public List<ImportBatch> batches() {
        return importRecordRepository.findAllBatches();
    }

    public List<ImportErrorRow> errors(
            String importId,
            ImportErrorSeverity severity,
            String sheetName,
            ImportErrorCode errorCode
    ) {
        return importRecordRepository.findErrorRowsByImportId(new ImportVersion(importId)).stream()
                .filter(row -> severity == null || row.severity() == severity)
                .filter(row -> sheetName == null || sheetName.isBlank() || row.sheetName().equals(sheetName))
                .filter(row -> errorCode == null || row.errorCode() == errorCode)
                .toList();
    }

    public byte[] exportErrorsCsv(
            String importId,
            ImportErrorSeverity severity,
            String sheetName,
            ImportErrorCode errorCode,
            String operatorId
    ) {
        List<ImportErrorRow> rows = errors(importId, severity, sheetName, errorCode);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("importId,sheetName,rowNumber,columnName,rawValue,errorCode,errorMessage,severity\n");
        for (ImportErrorRow row : rows) {
            csv.append(escape(row.importId().value())).append(',')
                    .append(escape(row.sheetName())).append(',')
                    .append(row.rowNumber()).append(',')
                    .append(escape(row.columnName())).append(',')
                    .append(escape(row.rawValue())).append(',')
                    .append(row.errorCode()).append(',')
                    .append(escape(row.errorMessage())).append(',')
                    .append(row.severity()).append('\n');
        }
        auditLogService.record("IMPORT_ERRORS_EXPORT", "IMPORT_BATCH", importId,
                null, rows, operatorId, "", "Export import error rows as CSV");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
