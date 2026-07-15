package com.cnpc.promoretail.promotion.coupon.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.promotion.coupon.CouponTemplate;
import java.math.BigDecimal;
import java.util.List;

@TableName(value = "coupon_template", autoResultMap = true)
public class CouponTemplateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableField("valid_days")
    private Integer validDays;

    @TableField("issue_quantity")
    private Integer issueQuantity;

    @TableField("per_customer_limit")
    private Integer perCustomerLimit;

    @TableField(value = "redeem_channels", typeHandler = JsonbTypeHandler.class)
    private List<String> redeemChannels;

    @TableField("member_only")
    private Boolean memberOnly;

    @TableField("stackable")
    private Boolean stackable;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    public static CouponTemplateEntity from(CouponTemplate template) {
        CouponTemplateEntity entity = new CouponTemplateEntity();
        entity.setCouponTemplateId(template.couponTemplateId());
        entity.setCouponName(template.couponName());
        entity.setFaceValue(template.faceValue());
        entity.setMinSpendAmount(template.minSpendAmount());
        entity.setApplicableCategories(template.applicableCategories());
        entity.setExcludedCategories(template.excludedCategories());
        entity.setApplicableProductCodes(template.applicableProductCodes());
        entity.setExcludedProductCodes(template.excludedProductCodes());
        entity.setValidDays(template.validDays());
        entity.setIssueQuantity(template.issueQuantity());
        entity.setPerCustomerLimit(template.perCustomerLimit());
        entity.setRedeemChannels(template.redeemChannels());
        entity.setMemberOnly(template.memberOnly());
        entity.setStackable(template.stackable());
        entity.setDiscountRate(template.discountRate());
        return entity;
    }

    public CouponTemplate toCouponTemplate() {
        return new CouponTemplate(couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validDays == null ? 0 : validDays, issueQuantity == null ? 0 : issueQuantity,
                perCustomerLimit == null ? 0 : perCustomerLimit, redeemChannels,
                Boolean.TRUE.equals(memberOnly), Boolean.TRUE.equals(stackable), discountRate);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public Integer getIssueQuantity() { return issueQuantity; }
    public void setIssueQuantity(Integer issueQuantity) { this.issueQuantity = issueQuantity; }
    public Integer getPerCustomerLimit() { return perCustomerLimit; }
    public void setPerCustomerLimit(Integer perCustomerLimit) { this.perCustomerLimit = perCustomerLimit; }
    public List<String> getRedeemChannels() { return redeemChannels; }
    public void setRedeemChannels(List<String> redeemChannels) { this.redeemChannels = redeemChannels; }
    public Boolean getMemberOnly() { return memberOnly; }
    public void setMemberOnly(Boolean memberOnly) { this.memberOnly = memberOnly; }
    public Boolean getStackable() { return stackable; }
    public void setStackable(Boolean stackable) { this.stackable = stackable; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
}
