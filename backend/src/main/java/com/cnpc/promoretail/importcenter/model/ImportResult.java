package com.cnpc.promoretail.importcenter.model;

import java.util.List;

public record ImportResult<T>(
        ImportVersion importVersion,
        ImportType importType,
        ImportBatch importBatch,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int invalidCount,
        int warningCount,
        List<T> records,
        List<ImportErrorRow> errors,
        List<String> warnings
) {

    public ImportResult {
        if (importBatch == null) {
            importBatch = new ImportBatch(importVersion, importType, "", insertedCount, updatedCount,
                    skippedCount, invalidCount, warningCount, null);
        }
        records = records == null ? List.of() : List.copyOf(records);
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
