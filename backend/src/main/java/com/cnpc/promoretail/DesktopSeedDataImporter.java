package com.cnpc.promoretail;

import com.cnpc.promoretail.importcenter.ImportCenterService;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportResult;
import com.cnpc.promoretail.importcenter.repository.ImportRecordRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
final class DesktopSeedDataImporter implements ApplicationRunner {

    private static final String DESKTOP_PROPERTY = "cnpc.desktop";
    private static final List<SeedWorkbook> WORKBOOKS = List.of(
            new SeedWorkbook("seed-data/价格.xlsx", "prices", ImportCenterService::importPrices),
            new SeedWorkbook("seed-data/库存.xlsx", "inventory", ImportCenterService::importInventory)
    );

    private final ImportCenterService importCenterService;
    private final ImportRecordRepository importRecordRepository;

    DesktopSeedDataImporter(
            ImportCenterService importCenterService,
            ImportRecordRepository importRecordRepository
    ) {
        this.importCenterService = importCenterService;
        this.importRecordRepository = importRecordRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.getBoolean(DESKTOP_PROPERTY)) {
            return;
        }
        WORKBOOKS.forEach(this::importIfRequired);
    }

    private void importIfRequired(SeedWorkbook workbook) {
        byte[] content = readWorkbook(workbook.classpathLocation());
        String fingerprint = sha256(content).substring(0, 16);
        Path seedDirectory = DesktopEmbeddedPostgres.applicationDataDirectory().resolve("seed-data");
        Path targetFile = seedDirectory.resolve(workbook.filePrefix() + "-" + fingerprint + ".xlsx");
        if (alreadyImported(targetFile)) {
            return;
        }

        try {
            Files.createDirectories(seedDirectory);
            Files.write(targetFile, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare bundled seed workbook " + workbook.classpathLocation(),
                    exception);
        }

        ImportResult<?> result = workbook.importer().apply(importCenterService, targetFile);
        if (result.invalidCount() > 0) {
            throw new IllegalStateException("Bundled seed workbook contains " + result.invalidCount()
                    + " invalid rows: " + workbook.classpathLocation());
        }
    }

    private boolean alreadyImported(Path targetFile) {
        String normalizedTarget = targetFile.toString();
        return importRecordRepository.findAllBatches().stream()
                .map(ImportBatch::sourceFile)
                .anyMatch(normalizedTarget::equalsIgnoreCase);
    }

    private byte[] readWorkbook(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream input = resource.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled seed workbook is missing: " + classpathLocation, exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record SeedWorkbook(
            String classpathLocation,
            String filePrefix,
            WorkbookImporter importer
    ) {
    }

    @FunctionalInterface
    private interface WorkbookImporter {

        ImportResult<?> apply(ImportCenterService service, Path file);
    }
}
