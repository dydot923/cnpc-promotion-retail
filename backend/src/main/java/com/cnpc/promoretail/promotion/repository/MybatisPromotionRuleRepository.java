package com.cnpc.promoretail.promotion.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditAction;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.persistence.entity.PromotionRuleAuditLogEntity;
import com.cnpc.promoretail.promotion.persistence.entity.PromotionRuleDraftEntity;
import com.cnpc.promoretail.promotion.persistence.entity.PromotionRuleVersionEntity;
import com.cnpc.promoretail.promotion.persistence.mapper.PromotionRuleAuditLogMapper;
import com.cnpc.promoretail.promotion.persistence.mapper.PromotionRuleDraftMapper;
import com.cnpc.promoretail.promotion.persistence.mapper.PromotionRuleVersionMapper;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPromotionRuleRepository implements PromotionRuleRepository {

    private final PromotionRuleDraftMapper draftMapper;
    private final PromotionRuleVersionMapper versionMapper;
    private final PromotionRuleAuditLogMapper auditLogMapper;

    public MybatisPromotionRuleRepository(
            PromotionRuleDraftMapper draftMapper,
            PromotionRuleVersionMapper versionMapper,
            PromotionRuleAuditLogMapper auditLogMapper
    ) {
        this.draftMapper = draftMapper;
        this.versionMapper = versionMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public PromotionRuleDraft saveDraft(PromotionRuleDraft draft) {
        return saveDraft(draft, false);
    }

    @Override
    public PromotionRuleDraft saveDraft(PromotionRuleDraft draft, boolean overwriteManualLocked) {
        PromotionRuleDraftEntity existing = findDraftEntityByRuleId(draft.rule().ruleId()).orElse(null);
        if (!overwriteManualLocked && existing != null && Boolean.TRUE.equals(existing.getManualLocked())) {
            return toDraft(existing);
        }

        PromotionRuleDraftEntity entity = toEntity(draft);
        if (existing == null) {
            draftMapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            draftMapper.updateById(entity);
        }
        return toDraft(entity);
    }

    @Override
    public Optional<PromotionRuleDraft> findDraftById(String draftId) {
        return Optional.ofNullable(draftMapper.selectOne(new LambdaQueryWrapper<PromotionRuleDraftEntity>()
                        .eq(PromotionRuleDraftEntity::getDraftId, draftId)))
                .map(this::toDraft);
    }

    @Override
    public Optional<PromotionRuleDraft> findDraftByRuleId(String ruleId) {
        return findDraftEntityByRuleId(ruleId).map(this::toDraft);
    }

    @Override
    public PromotionRuleVersion saveVersion(PromotionRuleVersion version) {
        versionMapper.insert(toEntity(version));
        return version;
    }

    @Override
    public List<PromotionRule> findConfirmedRules() {
        return draftMapper.selectList(new LambdaQueryWrapper<PromotionRuleDraftEntity>()
                        .eq(PromotionRuleDraftEntity::getStatus, PromotionRuleStatus.CONFIRMED.name())
                        .orderByAsc(PromotionRuleDraftEntity::getRuleId))
                .stream()
                .map(PromotionRuleDraftEntity::getRuleJson)
                .filter(PromotionRule::active)
                .toList();
    }

    @Override
    public void appendAuditLog(PromotionRuleAuditLog auditLog) {
        auditLogMapper.insert(toEntity(auditLog));
    }

    @Override
    public List<PromotionRuleAuditLog> findAuditLogsByRuleId(String ruleId) {
        return auditLogMapper.selectList(new LambdaQueryWrapper<PromotionRuleAuditLogEntity>()
                        .eq(PromotionRuleAuditLogEntity::getRuleId, ruleId)
                        .orderByAsc(PromotionRuleAuditLogEntity::getCreatedAt))
                .stream()
                .map(this::toAuditLog)
                .toList();
    }

    private Optional<PromotionRuleDraftEntity> findDraftEntityByRuleId(String ruleId) {
        return Optional.ofNullable(draftMapper.selectOne(new LambdaQueryWrapper<PromotionRuleDraftEntity>()
                .eq(PromotionRuleDraftEntity::getRuleId, ruleId)));
    }

    private PromotionRuleDraftEntity toEntity(PromotionRuleDraft draft) {
        PromotionRuleDraftEntity entity = new PromotionRuleDraftEntity();
        entity.setDraftId(draft.draftId());
        entity.setRuleId(draft.rule().ruleId());
        entity.setSourceImportId(draft.sourceImportId());
        entity.setSourceSheetName(draft.sourceSheetName());
        entity.setSourceRowNumber(draft.sourceRowNumber());
        entity.setRuleType(draft.rule().ruleType().name());
        entity.setStatus(draft.status().name());
        entity.setConditionJson(draft.rule().condition());
        entity.setBenefitJson(draft.rule().benefit());
        entity.setRuleJson(draft.rule());
        entity.setManualLocked(draft.manualLocked());
        entity.setCreatedBy(draft.createdBy());
        entity.setCreatedAt(draft.createdAt());
        entity.setUpdatedAt(draft.updatedAt());
        return entity;
    }

    private PromotionRuleDraft toDraft(PromotionRuleDraftEntity entity) {
        return new PromotionRuleDraft(
                entity.getDraftId(),
                entity.getRuleJson(),
                entity.getSourceImportId(),
                entity.getSourceSheetName(),
                entity.getSourceRowNumber(),
                PromotionRuleStatus.valueOf(entity.getStatus()),
                Boolean.TRUE.equals(entity.getManualLocked()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }

    private PromotionRuleVersionEntity toEntity(PromotionRuleVersion version) {
        PromotionRuleVersionEntity entity = new PromotionRuleVersionEntity();
        entity.setVersionId(version.versionId());
        entity.setRuleId(version.ruleId());
        entity.setSourceImportId(version.sourceImportId());
        entity.setSourceSheetName(version.sourceSheetName());
        entity.setSourceRowNumber(version.sourceRowNumber());
        entity.setRuleType(version.ruleType().name());
        entity.setStatus(version.status().name());
        entity.setRuleJson(version.rule());
        entity.setCreatedAt(version.createdAt());
        entity.setCreatedBy(version.createdBy());
        entity.setConfirmedAt(version.confirmedAt());
        entity.setConfirmedBy(version.confirmedBy());
        entity.setChangeReason(version.changeReason());
        return entity;
    }

    private PromotionRuleAuditLogEntity toEntity(PromotionRuleAuditLog auditLog) {
        PromotionRuleAuditLogEntity entity = new PromotionRuleAuditLogEntity();
        entity.setAuditId(auditLog.auditId());
        entity.setRuleId(auditLog.ruleId());
        entity.setAction(auditLog.action().name());
        entity.setStatusBefore(auditLog.statusBefore() == null ? null : auditLog.statusBefore().name());
        entity.setStatusAfter(auditLog.statusAfter() == null ? null : auditLog.statusAfter().name());
        entity.setOperatorId(auditLog.operatorId());
        entity.setChangeReason(auditLog.changeReason());
        entity.setCreatedAt(auditLog.createdAt());
        return entity;
    }

    private PromotionRuleAuditLog toAuditLog(PromotionRuleAuditLogEntity entity) {
        return new PromotionRuleAuditLog(
                entity.getAuditId(),
                entity.getRuleId(),
                PromotionRuleAuditAction.valueOf(entity.getAction()),
                entity.getStatusBefore() == null ? null : PromotionRuleStatus.valueOf(entity.getStatusBefore()),
                entity.getStatusAfter() == null ? null : PromotionRuleStatus.valueOf(entity.getStatusAfter()),
                entity.getOperatorId(),
                entity.getChangeReason(),
                entity.getCreatedAt()
        );
    }
}
