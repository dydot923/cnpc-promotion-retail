package com.cnpc.promoretail.inventory.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("inventory_alert_record")
public class InventoryAlertRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alert_id")
    private String alertId;

    @TableField("status")
    private String status;

    @TableField("handled_by")
    private String handledBy;

    @TableField("handled_at")
    private Instant handledAt;

    @TableField("handle_note")
    private String handleNote;

    @TableField("replenishment_list_id")
    private String replenishmentListId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }
    public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(Instant handledAt) { this.handledAt = handledAt; }
    public String getHandleNote() { return handleNote; }
    public void setHandleNote(String handleNote) { this.handleNote = handleNote; }
    public String getReplenishmentListId() { return replenishmentListId; }
    public void setReplenishmentListId(String replenishmentListId) { this.replenishmentListId = replenishmentListId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
