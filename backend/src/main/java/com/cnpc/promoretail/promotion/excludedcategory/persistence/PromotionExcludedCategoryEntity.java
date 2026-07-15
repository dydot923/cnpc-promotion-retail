package com.cnpc.promoretail.promotion.excludedcategory.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.promotion.excludedcategory.PromotionExcludedCategory;
import java.time.Instant;

@TableName("promotion_excluded_category")
public class PromotionExcludedCategoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_id")
    private String ruleId;

    @TableField("category_name")
    private String categoryName;

    @TableField("reason")
    private String reason;

    @TableField("source_sheet_name")
    private String sourceSheetName;

    @TableField("created_at")
    private Instant createdAt;

    public PromotionExcludedCategory toExcludedCategory() {
        return new PromotionExcludedCategory(ruleId, categoryName, reason);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String sourceSheetName) { this.sourceSheetName = sourceSheetName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
