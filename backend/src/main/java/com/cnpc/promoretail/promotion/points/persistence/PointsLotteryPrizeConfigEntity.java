package com.cnpc.promoretail.promotion.points.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.promotion.points.PointsLotteryPrizeConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@TableName(value = "points_lottery_prize_config", autoResultMap = true)
public class PointsLotteryPrizeConfigEntity {

    @TableId("prize_id")
    private String prizeId;

    @TableField("activity_code")
    private String activityCode;

    @TableField("prize_name")
    private String prizeName;

    @TableField("prize_type")
    private String prizeType;

    @TableField("coupon_template_id")
    private String couponTemplateId;

    @TableField("coupon_name")
    private String couponName;

    @TableField("face_value")
    private BigDecimal faceValue;

    @TableField("min_spend_amount")
    private BigDecimal minSpendAmount;

    @TableField(value = "applicable_categories", typeHandler = JsonbTypeHandler.class)
    private List<String> applicableCategories;

    @TableField(value = "excluded_categories", typeHandler = JsonbTypeHandler.class)
    private List<String> excludedCategories;

    @TableField("valid_days")
    private Integer validDays;

    @TableField("weight")
    private Integer weight;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public static PointsLotteryPrizeConfigEntity from(PointsLotteryPrizeConfig config) {
        PointsLotteryPrizeConfigEntity entity = new PointsLotteryPrizeConfigEntity();
        entity.setPrizeId(config.prizeId());
        entity.setActivityCode(config.activityCode());
        entity.setPrizeName(config.prizeName());
        entity.setPrizeType(config.prizeType());
        entity.setCouponTemplateId(config.couponTemplateId());
        entity.setCouponName(config.couponName());
        entity.setFaceValue(config.faceValue());
        entity.setMinSpendAmount(config.minSpendAmount());
        entity.setApplicableCategories(config.applicableCategories());
        entity.setExcludedCategories(config.excludedCategories());
        entity.setValidDays(config.validDays());
        entity.setWeight(config.weight());
        entity.setStatus(config.status());
        entity.setCreatedAt(config.createdAt());
        entity.setUpdatedAt(config.updatedAt());
        return entity;
    }

    public PointsLotteryPrizeConfig toConfig() {
        return new PointsLotteryPrizeConfig(
                prizeId,
                activityCode,
                prizeName,
                prizeType,
                couponTemplateId,
                couponName,
                faceValue,
                minSpendAmount,
                applicableCategories,
                excludedCategories,
                validDays == null ? 30 : validDays,
                weight == null ? 0 : weight,
                status,
                createdAt,
                updatedAt
        );
    }

    public String getPrizeId() { return prizeId; }
    public void setPrizeId(String prizeId) { this.prizeId = prizeId; }
    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String activityCode) { this.activityCode = activityCode; }
    public String getPrizeName() { return prizeName; }
    public void setPrizeName(String prizeName) { this.prizeName = prizeName; }
    public String getPrizeType() { return prizeType; }
    public void setPrizeType(String prizeType) { this.prizeType = prizeType; }
    public String getCouponTemplateId() { return couponTemplateId; }
    public void setCouponTemplateId(String couponTemplateId) { this.couponTemplateId = couponTemplateId; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
    public BigDecimal getMinSpendAmount() { return minSpendAmount; }
    public void setMinSpendAmount(BigDecimal minSpendAmount) { this.minSpendAmount = minSpendAmount; }
    public List<String> getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(List<String> applicableCategories) { this.applicableCategories = applicableCategories; }
    public List<String> getExcludedCategories() { return excludedCategories; }
    public void setExcludedCategories(List<String> excludedCategories) { this.excludedCategories = excludedCategories; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
