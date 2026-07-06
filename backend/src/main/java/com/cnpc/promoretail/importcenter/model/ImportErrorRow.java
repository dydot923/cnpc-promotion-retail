package com.cnpc.promoretail.importcenter.model;

import java.util.List;

public record ImportErrorRow(
        ImportVersion importId,
        String sheetName,
        int rowNumber,
        String columnName,
        String rawValue,
        ImportErrorCode errorCode,
        List<String> rawValues,
        String errorMessage,
        ImportErrorSeverity severity
) {

    public ImportErrorRow {
        if (importId == null) {
            throw new IllegalArgumentException("importId is required");
        }
        columnName = columnName == null ? "" : columnName;
        rawValue = rawValue == null ? "" : rawValue;
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode is required");
        }
        rawValues = rawValues == null ? List.of() : List.copyOf(rawValues);
        if (severity == null) {
            throw new IllegalArgumentException("severity is required");
        }
    }
}
