package com.cnpc.promoretail.member.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.member.model.MemberLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@TableName(value = "member_level", autoResultMap = true)
public class MemberLevelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("level_code")
    private String levelCode;

    @TableField("level_name")
    private String levelName;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("points_multiplier")
    private BigDecimal pointsMultiplier;

    @TableField("min_consumption")
    private BigDecimal minConsumption;

    @TableField(value = "benefits", typeHandler = JsonbTypeHandler.class)
    private List<String> benefits;

    @TableField("priority")
    private Integer priority;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public MemberLevel toMemberLevel() {
        return new MemberLevel(levelCode, levelName, discountRate, pointsMultiplier,
                minConsumption, benefits, priority == null ? 0 : priority);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public BigDecimal getPointsMultiplier() { return pointsMultiplier; }
    public void setPointsMultiplier(BigDecimal pointsMultiplier) { this.pointsMultiplier = pointsMultiplier; }
    public BigDecimal getMinConsumption() { return minConsumption; }
    public void setMinConsumption(BigDecimal minConsumption) { this.minConsumption = minConsumption; }
    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
