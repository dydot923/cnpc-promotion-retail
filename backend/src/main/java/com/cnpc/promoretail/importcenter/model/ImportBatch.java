package com.cnpc.promoretail.importcenter.model;

import java.time.Instant;

public record ImportBatch(
        ImportVersion importId,
        ImportType importType,
        String sourceFile,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int invalidCount,
        int warningCount,
        Instant createdAt
) {

    public ImportBatch {
        if (importId == null) {
            throw new IllegalArgumentException("importId is required");
        }
        if (importType == null) {
            throw new IllegalArgumentException("importType is required");
        }
        sourceFile = sourceFile == null ? "" : sourceFile;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
