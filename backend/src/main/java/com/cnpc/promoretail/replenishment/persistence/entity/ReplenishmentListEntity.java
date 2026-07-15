package com.cnpc.promoretail.replenishment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.replenishment.model.ReplenishmentItem;
import java.time.Instant;
import java.util.List;

@TableName(value = "replenishment_list", autoResultMap = true)
public class ReplenishmentListEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("list_id")
    private String listId;

    @TableField("list_name")
    private String listName;

    @TableField("status")
    private String status;

    @TableField(value = "items", typeHandler = JsonbTypeHandler.class)
    private List<ReplenishmentItem> items;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ReplenishmentItem> getItems() {
        return items;
    }

    public void setItems(List<ReplenishmentItem> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
