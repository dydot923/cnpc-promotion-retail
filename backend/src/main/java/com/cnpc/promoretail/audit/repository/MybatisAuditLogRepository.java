package com.cnpc.promoretail.audit.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.persistence.entity.AuditLogEntity;
import com.cnpc.promoretail.audit.persistence.mapper.AuditLogMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisAuditLogRepository implements AuditLogRepository {

    private final AuditLogMapper mapper;

    public MybatisAuditLogRepository(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        mapper.insert(toEntity(auditLog));
        return auditLog;
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, String entityId) {
        return mapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                        .eq(AuditLogEntity::getEntityType, entityType)
                        .eq(AuditLogEntity::getEntityId, entityId)
                        .orderByAsc(AuditLogEntity::getOperatedAt))
                .stream()
                .map(this::toLog)
                .toList();
    }

    @Override
    public List<AuditLog> search(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<AuditLogEntity>()
                .orderByDesc(AuditLogEntity::getOperatedAt)
                .last("limit " + query.limit());
        if (query.actionType() != null) {
            wrapper.eq(AuditLogEntity::getActionType, query.actionType());
        }
        if (query.entityType() != null) {
            wrapper.eq(AuditLogEntity::getEntityType, query.entityType());
        }
        if (query.entityId() != null) {
            wrapper.eq(AuditLogEntity::getEntityId, query.entityId());
        }
        if (query.operatorId() != null) {
            wrapper.eq(AuditLogEntity::getOperatorId, query.operatorId());
        }
        return mapper.selectList(wrapper).stream()
                .map(this::toLog)
                .toList();
    }

    private AuditLogEntity toEntity(AuditLog auditLog) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(auditLog.auditId());
        entity.setAction(auditLog.actionType());
        entity.setTargetType(auditLog.entityType());
        entity.setTargetId(auditLog.entityId());
        entity.setActionType(auditLog.actionType());
        entity.setEntityType(auditLog.entityType());
        entity.setEntityId(auditLog.entityId());
        entity.setBeforeSnapshot(auditLog.beforeSnapshot());
        entity.setAfterSnapshot(auditLog.afterSnapshot());
        entity.setDetailJson(auditLog.afterSnapshot());
        entity.setOperatorId(auditLog.operatorId());
        entity.setOperatorName(auditLog.operatorName());
        entity.setOperatedAt(auditLog.operatedAt());
        entity.setReason(auditLog.reason());
        entity.setCreatedAt(auditLog.createdAt());
        return entity;
    }

    private AuditLog toLog(AuditLogEntity entity) {
        return new AuditLog(
                entity.getAuditId(),
                entity.getActionType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getBeforeSnapshot(),
                entity.getAfterSnapshot(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getOperatedAt(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
