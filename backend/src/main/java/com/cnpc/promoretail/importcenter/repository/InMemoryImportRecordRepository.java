package com.cnpc.promoretail.importcenter.repository;

import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryImportRecordRepository implements ImportRecordRepository {

    private final CopyOnWriteArrayList<ImportBatch> batches = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ImportErrorRow> errorRows = new CopyOnWriteArrayList<>();

    @Override
    public ImportBatch saveImportBatch(ImportBatch batch) {
        batches.add(batch);
        return batch;
    }

    @Override
    public void saveErrorRows(List<ImportErrorRow> rows) {
        if (rows != null) {
            errorRows.addAll(rows);
        }
    }

    @Override
    public List<ImportBatch> findAllBatches() {
        return batches.stream()
                .sorted(Comparator.comparing(ImportBatch::createdAt))
                .toList();
    }

    @Override
    public List<ImportErrorRow> findErrorRowsByImportId(ImportVersion importId) {
        return errorRows.stream()
                .filter(row -> row.importId().equals(importId))
                .sorted(Comparator.comparing(ImportErrorRow::rowNumber))
                .toList();
    }
}
