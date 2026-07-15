package com.cnpc.promoretail.product.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("product")
public class ProductEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("barcode")
    private String barcode;

    @TableField("category")
    private String category;

    @TableField("is_cigarette")
    private Boolean cigarette;

    @TableField("is_fertilizer")
    private Boolean fertilizer;

    @TableField("is_demo_data")
    private Boolean demoData;

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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getCigarette() {
        return cigarette;
    }

    public void setCigarette(Boolean cigarette) {
        this.cigarette = cigarette;
    }

    public Boolean getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(Boolean fertilizer) {
        this.fertilizer = fertilizer;
    }

    public Boolean getDemoData() {
        return demoData;
    }

    public void setDemoData(Boolean demoData) {
        this.demoData = demoData;
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
