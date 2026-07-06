package com.cnpc.promoretail.importcenter.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record ImportVersion(String value) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public ImportVersion {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("import version is required");
        }
    }

    public static ImportVersion newVersion(ImportType importType) {
        String prefix = importType.name().toLowerCase(Locale.ROOT);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new ImportVersion(prefix + "-" + LocalDateTime.now().format(FORMATTER) + "-" + suffix);
    }
}
