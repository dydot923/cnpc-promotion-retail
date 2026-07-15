package com.cnpc.promoretail.promotion.productgroup.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("product_group")
public class ProductGroupEntity {

    @TableField("id")
    private String id;

    @TableField("name")
    private String name;

    @TableField("source")
    private String source;

    @TableField("description")
    private String description;

    @TableField("is_demo_data")
    private Boolean demoData;

    @TableField("created_at")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getDemoData() { return demoData; }
    public void setDemoData(Boolean demoData) { this.demoData = demoData; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
