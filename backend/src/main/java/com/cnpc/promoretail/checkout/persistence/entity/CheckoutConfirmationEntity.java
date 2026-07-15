package com.cnpc.promoretail.checkout.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.time.Instant;

@TableName(value = "checkout_confirmation", autoResultMap = true)
public class CheckoutConfirmationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("confirmation_id")
    private String confirmationId;

    @TableField("calculation_id")
    private String calculationId;

    @TableField("selected_candidate_id")
    private String selectedCandidateId;

    @TableField(value = "selected_candidate_snapshot", typeHandler = JsonbTypeHandler.class)
    private PromotionCandidate selectedCandidateSnapshot;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("skipped")
    private Boolean skipped;

    @TableField("confirmed_at")
    private Instant confirmedAt;

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

    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getCalculationId() {
        return calculationId;
    }

    public void setCalculationId(String calculationId) {
        this.calculationId = calculationId;
    }

    public String getSelectedCandidateId() {
        return selectedCandidateId;
    }

    public void setSelectedCandidateId(String selectedCandidateId) {
        this.selectedCandidateId = selectedCandidateId;
    }

    public PromotionCandidate getSelectedCandidateSnapshot() {
        return selectedCandidateSnapshot;
    }

    public void setSelectedCandidateSnapshot(PromotionCandidate selectedCandidateSnapshot) {
        this.selectedCandidateSnapshot = selectedCandidateSnapshot;
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

    public Boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
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
