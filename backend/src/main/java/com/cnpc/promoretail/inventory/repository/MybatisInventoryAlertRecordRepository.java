package com.cnpc.promoretail.inventory.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.inventory.model.InventoryAlertRecord;
import com.cnpc.promoretail.inventory.persistence.entity.InventoryAlertRecordEntity;
import com.cnpc.promoretail.inventory.persistence.mapper.InventoryAlertRecordMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisInventoryAlertRecordRepository implements InventoryAlertRecordRepository {

    private final InventoryAlertRecordMapper mapper;

    public MybatisInventoryAlertRecordRepository(InventoryAlertRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InventoryAlertRecord save(InventoryAlertRecord record) {
        InventoryAlertRecordEntity existing = mapper.selectOne(new LambdaQueryWrapper<InventoryAlertRecordEntity>()
                .eq(InventoryAlertRecordEntity::getAlertId, record.alertId()));
        InventoryAlertRecordEntity entity = toEntity(record);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            mapper.updateById(entity);
        }
        return record;
    }

    @Override
    public Optional<InventoryAlertRecord> findByAlertId(String alertId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<InventoryAlertRecordEntity>()
                        .eq(InventoryAlertRecordEntity::getAlertId, alertId)))
                .map(this::toRecord);
    }

    @Override
    public List<InventoryAlertRecord> findByAlertIds(List<String> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<InventoryAlertRecordEntity>()
                        .in(InventoryAlertRecordEntity::getAlertId, alertIds))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private InventoryAlertRecordEntity toEntity(InventoryAlertRecord record) {
        InventoryAlertRecordEntity entity = new InventoryAlertRecordEntity();
        entity.setAlertId(record.alertId());
        entity.setStatus(record.status());
        entity.setHandledBy(record.handledBy());
        entity.setHandledAt(record.handledAt());
        entity.setHandleNote(record.handleNote());
        entity.setReplenishmentListId(record.replenishmentListId());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    private InventoryAlertRecord toRecord(InventoryAlertRecordEntity entity) {
        return new InventoryAlertRecord(
                entity.getAlertId(),
                entity.getStatus(),
                entity.getHandledBy(),
                entity.getHandledAt(),
                entity.getHandleNote(),
                entity.getReplenishmentListId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
