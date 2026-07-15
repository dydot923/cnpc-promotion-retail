package com.cnpc.promoretail.inventory.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;

@TableName("inventory_snapshot")
public class InventorySnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("station_code")
    private String stationCode;

    @TableField("product_code")
    private String productCode;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("import_version")
    private String importVersion;

    @TableField("snapshot_at")
    private Instant snapshotAt;

    @TableField("is_demo_data")
    private Boolean demoData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getImportVersion() {
        return importVersion;
    }

    public void setImportVersion(String importVersion) {
        this.importVersion = importVersion;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(Instant snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public Boolean getDemoData() {
        return demoData;
    }

    public void setDemoData(Boolean demoData) {
        this.demoData = demoData;
    }
}
