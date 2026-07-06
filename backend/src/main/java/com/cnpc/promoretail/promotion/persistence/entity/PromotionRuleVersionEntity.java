package com.cnpc.promoretail.promotion.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.time.Instant;

@TableName(value = "promotion_rule_version", autoResultMap = true)
public class PromotionRuleVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("version_id")
    private String versionId;

    @TableField("rule_id")
    private String ruleId;

    @TableField("source_import_id")
    private String sourceImportId;

    @TableField("source_sheet_name")
    private String sourceSheetName;

    @TableField("source_row_number")
    private Integer sourceRowNumber;

    @TableField("rule_type")
    private String ruleType;

    @TableField("status")
    private String status;

    @TableField(value = "rule_json", typeHandler = JsonbTypeHandler.class)
    private PromotionRule ruleJson;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("confirmed_at")
    private Instant confirmedAt;

    @TableField("confirmed_by")
    private String confirmedBy;

    @TableField("change_reason")
    private String changeReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getSourceImportId() {
        return sourceImportId;
    }

    public void setSourceImportId(String sourceImportId) {
        this.sourceImportId = sourceImportId;
    }

    public String getSourceSheetName() {
        return sourceSheetName;
    }

    public void setSourceSheetName(String sourceSheetName) {
        this.sourceSheetName = sourceSheetName;
    }

    public Integer getSourceRowNumber() {
        return sourceRowNumber;
    }

    public void setSourceRowNumber(Integer sourceRowNumber) {
        this.sourceRowNumber = sourceRowNumber;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PromotionRule getRuleJson() {
        return ruleJson;
    }

    public void setRuleJson(PromotionRule ruleJson) {
        this.ruleJson = ruleJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
