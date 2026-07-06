package com.cnpc.promoretail.importcenter.repository;

import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import java.util.List;

public interface ImportRecordRepository {

    ImportBatch saveImportBatch(ImportBatch batch);

    void saveErrorRows(List<ImportErrorRow> errorRows);

    List<ImportBatch> findAllBatches();

    List<ImportErrorRow> findErrorRowsByImportId(ImportVersion importId);
}
