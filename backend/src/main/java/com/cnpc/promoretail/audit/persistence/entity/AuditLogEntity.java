package com.cnpc.promoretail.audit.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import java.time.Instant;

@TableName(value = "audit_log", autoResultMap = true)
public class AuditLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("audit_id")
    private String auditId;

    @TableField("action")
    private String action;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("action_type")
    private String actionType;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private String entityId;

    @TableField(value = "before_snapshot", typeHandler = JsonbTypeHandler.class)
    private Object beforeSnapshot;

    @TableField(value = "after_snapshot", typeHandler = JsonbTypeHandler.class)
    private Object afterSnapshot;

    @TableField(value = "detail_json", typeHandler = JsonbTypeHandler.class)
    private Object detailJson;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operated_at")
    private Instant operatedAt;

    @TableField("reason")
    private String reason;

    @TableField("created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Object getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(Object beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    public Object getAfterSnapshot() {
        return afterSnapshot;
    }

    public void setAfterSnapshot(Object afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }

    public Object getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(Object detailJson) {
        this.detailJson = detailJson;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Instant getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(Instant operatedAt) {
        this.operatedAt = operatedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
