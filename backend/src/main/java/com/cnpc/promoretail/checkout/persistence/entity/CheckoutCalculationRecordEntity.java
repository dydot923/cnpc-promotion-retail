package com.cnpc.promoretail.checkout.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import java.time.Instant;
import java.util.List;

@TableName(value = "checkout_calculation_record", autoResultMap = true)
public class CheckoutCalculationRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("calculation_id")
    private String calculationId;

    @TableField(value = "request_snapshot", typeHandler = JsonbTypeHandler.class)
    private OrderContext requestSnapshot;

    @TableField(value = "result_snapshot", typeHandler = JsonbTypeHandler.class)
    private CalculationResult resultSnapshot;

    @TableField(value = "rule_version_ids", typeHandler = JsonbTypeHandler.class)
    private List<String> ruleVersionIds;

    @TableField("created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCalculationId() {
        return calculationId;
    }

    public void setCalculationId(String calculationId) {
        this.calculationId = calculationId;
    }

    public OrderContext getRequestSnapshot() {
        return requestSnapshot;
    }

    public void setRequestSnapshot(OrderContext requestSnapshot) {
        this.requestSnapshot = requestSnapshot;
    }

    public CalculationResult getResultSnapshot() {
        return resultSnapshot;
    }

    public void setResultSnapshot(CalculationResult resultSnapshot) {
        this.resultSnapshot = resultSnapshot;
    }

    public List<String> getRuleVersionIds() {
        return ruleVersionIds;
    }

    public void setRuleVersionIds(List<String> ruleVersionIds) {
        this.ruleVersionIds = ruleVersionIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
