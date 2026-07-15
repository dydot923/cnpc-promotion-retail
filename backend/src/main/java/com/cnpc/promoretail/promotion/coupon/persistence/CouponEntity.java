package com.cnpc.promoretail.promotion.coupon.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName(value = "coupon", autoResultMap = true)
public class CouponEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("coupon_id")
    private String couponId;

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

    @TableField(value = "applicable_product_codes", typeHandler = JsonbTypeHandler.class)
    private List<String> applicableProductCodes;

    @TableField(value = "excluded_product_codes", typeHandler = JsonbTypeHandler.class)
    private List<String> excludedProductCodes;

    @TableField("valid_from")
    private LocalDate validFrom;

    @TableField("valid_until")
    private LocalDate validUntil;

    @TableField("member_only")
    private Boolean memberOnly;

    @TableField("stackable")
    private Boolean stackable;

    @TableField("status")
    private String status;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("operator_id")
    private String operatorId;

    @TableField("holder_member_id")
    private String holderMemberId;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("sequence_group")
    private String sequenceGroup;

    @TableField("sequence_order")
    private Integer sequenceOrder;

    public static CouponEntity from(Coupon coupon) {
        CouponEntity entity = new CouponEntity();
        entity.setCouponId(coupon.couponId());
        entity.setCouponTemplateId(coupon.couponTemplateId());
        entity.setCouponName(coupon.couponName());
        entity.setFaceValue(coupon.faceValue());
        entity.setMinSpendAmount(coupon.minSpendAmount());
        entity.setApplicableCategories(coupon.applicableCategories());
        entity.setExcludedCategories(coupon.excludedCategories());
        entity.setApplicableProductCodes(coupon.applicableProductCodes());
        entity.setExcludedProductCodes(coupon.excludedProductCodes());
        entity.setValidFrom(coupon.validFrom());
        entity.setValidUntil(coupon.validUntil());
        entity.setMemberOnly(coupon.memberOnly());
        entity.setStackable(coupon.stackable());
        entity.setStatus(coupon.status().name());
        entity.setIssuedAt(coupon.issuedAt());
        entity.setUsedAt(coupon.usedAt());
        entity.setOperatorId(coupon.operatorId());
        entity.setHolderMemberId(coupon.holderMemberId());
        entity.setDiscountRate(coupon.discountRate());
        entity.setSequenceGroup(coupon.sequenceGroup());
        entity.setSequenceOrder(coupon.sequenceOrder());
        return entity;
    }

    public Coupon toCoupon() {
        return new Coupon(couponId, couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, Boolean.TRUE.equals(memberOnly), Boolean.TRUE.equals(stackable),
                status == null ? CouponStatus.AVAILABLE : CouponStatus.valueOf(status),
                issuedAt, usedAt, operatorId, discountRate, sequenceGroup, sequenceOrder, holderMemberId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }
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
    public List<String> getApplicableProductCodes() { return applicableProductCodes; }
    public void setApplicableProductCodes(List<String> applicableProductCodes) { this.applicableProductCodes = applicableProductCodes; }
    public List<String> getExcludedProductCodes() { return excludedProductCodes; }
    public void setExcludedProductCodes(List<String> excludedProductCodes) { this.excludedProductCodes = excludedProductCodes; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public Boolean getMemberOnly() { return memberOnly; }
    public void setMemberOnly(Boolean memberOnly) { this.memberOnly = memberOnly; }
    public Boolean getStackable() { return stackable; }
    public void setStackable(Boolean stackable) { this.stackable = stackable; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getHolderMemberId() { return holderMemberId; }
    public void setHolderMemberId(String holderMemberId) { this.holderMemberId = holderMemberId; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public String getSequenceGroup() { return sequenceGroup; }
    public void setSequenceGroup(String sequenceGroup) { this.sequenceGroup = sequenceGroup; }
    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
}
