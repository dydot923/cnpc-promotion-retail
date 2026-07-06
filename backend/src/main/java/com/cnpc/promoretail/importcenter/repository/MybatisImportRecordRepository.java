package com.cnpc.promoretail.importcenter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.importcenter.model.ImportBatch;
import com.cnpc.promoretail.importcenter.model.ImportErrorCode;
import com.cnpc.promoretail.importcenter.model.ImportErrorRow;
import com.cnpc.promoretail.importcenter.model.ImportErrorSeverity;
import com.cnpc.promoretail.importcenter.model.ImportType;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.persistence.entity.ImportBatchEntity;
import com.cnpc.promoretail.importcenter.persistence.entity.ImportErrorRowEntity;
import com.cnpc.promoretail.importcenter.persistence.mapper.ImportBatchMapper;
import com.cnpc.promoretail.importcenter.persistence.mapper.ImportErrorRowMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisImportRecordRepository implements ImportRecordRepository {

    private final ImportBatchMapper batchMapper;
    private final ImportErrorRowMapper errorRowMapper;

    public MybatisImportRecordRepository(ImportBatchMapper batchMapper, ImportErrorRowMapper errorRowMapper) {
        this.batchMapper = batchMapper;
        this.errorRowMapper = errorRowMapper;
    }

    @Override
    public ImportBatch saveImportBatch(ImportBatch batch) {
        batchMapper.insert(toEntity(batch));
        return batch;
    }

    @Override
    public void saveErrorRows(List<ImportErrorRow> errorRows) {
        if (errorRows == null) {
            return;
        }
        errorRows.forEach(row -> errorRowMapper.insert(toEntity(row)));
    }

    @Override
    public List<ImportBatch> findAllBatches() {
        return batchMapper.selectList(new LambdaQueryWrapper<ImportBatchEntity>()
                        .orderByAsc(ImportBatchEntity::getCreatedAt))
                .stream()
                .map(this::toBatch)
                .toList();
    }

    @Override
    public List<ImportErrorRow> findErrorRowsByImportId(ImportVersion importId) {
        return errorRowMapper.selectList(new LambdaQueryWrapper<ImportErrorRowEntity>()
                        .eq(ImportErrorRowEntity::getImportId, importId.value())
                        .orderByAsc(ImportErrorRowEntity::getRowNumber))
                .stream()
                .map(this::toErrorRow)
                .toList();
    }

    private ImportBatchEntity toEntity(ImportBatch batch) {
        ImportBatchEntity entity = new ImportBatchEntity();
        entity.setImportVersion(batch.importId().value());
        entity.setImportType(batch.importType().name());
        entity.setSourceFile(batch.sourceFile());
        entity.setInsertedCount(batch.insertedCount());
        entity.setUpdatedCount(batch.updatedCount());
        entity.setSkippedCount(batch.skippedCount());
        entity.setInvalidCount(batch.invalidCount());
        entity.setWarningCount(batch.warningCount());
        entity.setCreatedAt(batch.createdAt());
        return entity;
    }

    private ImportErrorRowEntity toEntity(ImportErrorRow row) {
        ImportErrorRowEntity entity = new ImportErrorRowEntity();
        entity.setImportVersion(row.importId().value());
        entity.setImportId(row.importId().value());
        entity.setSheetName(row.sheetName());
        entity.setRowNumber(row.rowNumber());
        entity.setRawJson(row.rawValues());
        entity.setColumnName(row.columnName());
        entity.setRawValue(row.rawValue());
        entity.setErrorCode(row.errorCode().name());
        entity.setErrorMessage(row.errorMessage());
        entity.setSeverity(row.severity().name());
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private ImportBatch toBatch(ImportBatchEntity entity) {
        return new ImportBatch(
                new ImportVersion(entity.getImportVersion()),
                ImportType.valueOf(entity.getImportType()),
                entity.getSourceFile(),
                zero(entity.getInsertedCount()),
                zero(entity.getUpdatedCount()),
                zero(entity.getSkippedCount()),
                zero(entity.getInvalidCount()),
                zero(entity.getWarningCount()),
                entity.getCreatedAt()
        );
    }

    private ImportErrorRow toErrorRow(ImportErrorRowEntity entity) {
        return new ImportErrorRow(
                new ImportVersion(entity.getImportId() == null ? entity.getImportVersion() : entity.getImportId()),
                entity.getSheetName(),
                entity.getRowNumber() == null ? 0 : entity.getRowNumber(),
                entity.getColumnName(),
                entity.getRawValue(),
                ImportErrorCode.valueOf(entity.getErrorCode()),
                entity.getRawJson(),
                entity.getErrorMessage(),
                ImportErrorSeverity.valueOf(entity.getSeverity())
        );
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
