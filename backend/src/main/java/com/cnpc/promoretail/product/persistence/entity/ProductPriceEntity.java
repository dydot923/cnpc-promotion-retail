package com.cnpc.promoretail.product.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;

@TableName("product_price")
public class ProductPriceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("product_code")
    private String productCode;

    @TableField("execution_price")
    private BigDecimal executionPrice;

    @TableField("import_version")
    private String importVersion;

    @TableField("effective_at")
    private Instant effectiveAt;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("is_demo_data")
    private Boolean demoData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice;
    }

    public void setExecutionPrice(BigDecimal executionPrice) {
        this.executionPrice = executionPrice;
    }

    public String getImportVersion() {
        return importVersion;
    }

    public void setImportVersion(String importVersion) {
        this.importVersion = importVersion;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDemoData() {
        return demoData;
    }

    public void setDemoData(Boolean demoData) {
        this.demoData = demoData;
    }
}
