package com.cnpc.promoretail.promotion.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.time.Instant;

@TableName(value = "promotion_rule_draft", autoResultMap = true)
public class PromotionRuleDraftEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("draft_id")
    private String draftId;

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

    @TableField(value = "condition_json", typeHandler = JsonbTypeHandler.class)
    private PromotionCondition conditionJson;

    @TableField(value = "benefit_json", typeHandler = JsonbTypeHandler.class)
    private PromotionBenefit benefitJson;

    @TableField(value = "rule_json", typeHandler = JsonbTypeHandler.class)
    private PromotionRule ruleJson;

    @TableField("manual_locked")
    private Boolean manualLocked;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
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

    public PromotionCondition getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(PromotionCondition conditionJson) {
        this.conditionJson = conditionJson;
    }

    public PromotionBenefit getBenefitJson() {
        return benefitJson;
    }

    public void setBenefitJson(PromotionBenefit benefitJson) {
        this.benefitJson = benefitJson;
    }

    public PromotionRule getRuleJson() {
        return ruleJson;
    }

    public void setRuleJson(PromotionRule ruleJson) {
        this.ruleJson = ruleJson;
    }

    public Boolean getManualLocked() {
        return manualLocked;
    }

    public void setManualLocked(Boolean manualLocked) {
        this.manualLocked = manualLocked;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
