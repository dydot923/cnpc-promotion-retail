package com.cnpc.promoretail.promotion.benefitpackage.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@TableName(value = "benefit_package_purchase", autoResultMap = true)
public class BenefitPackagePurchaseEntity {

    @TableId("purchase_id")
    private String purchaseId;

    @TableField("member_code")
    private String memberCode;

    @TableField("package_code")
    private String packageCode;

    @TableField("package_name")
    private String packageName;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    @TableField("station_code")
    private String stationCode;

    @TableField("checkout_transaction_no")
    private String checkoutTransactionNo;

    @TableField("status")
    private String status;

    @TableField(value = "entitlement_snapshot", typeHandler = JsonbTypeHandler.class)
    private List<BenefitPackageItem> entitlementSnapshot;

    @TableField("purchased_at")
    private Instant purchasedAt;

    @TableField("activated_at")
    private Instant activatedAt;

    @TableField("expired_at")
    private Instant expiredAt;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public static BenefitPackagePurchaseEntity from(BenefitPackagePurchase purchase) {
        BenefitPackagePurchaseEntity entity = new BenefitPackagePurchaseEntity();
        entity.setPurchaseId(purchase.purchaseId());
        entity.setMemberCode(purchase.memberCode());
        entity.setPackageCode(purchase.packageCode());
        entity.setPackageName(purchase.packageName());
        entity.setSalePrice(purchase.salePrice());
        entity.setPaymentAmount(purchase.paymentAmount());
        entity.setStationCode(purchase.stationCode());
        entity.setCheckoutTransactionNo(purchase.checkoutTransactionNo());
        entity.setStatus(purchase.status());
        entity.setEntitlementSnapshot(purchase.entitlementSnapshot());
        entity.setPurchasedAt(purchase.purchasedAt());
        entity.setActivatedAt(purchase.activatedAt());
        entity.setExpiredAt(purchase.expiredAt());
        entity.setOperatorId(purchase.operatorId());
        entity.setOperatorName(purchase.operatorName());
        return entity;
    }

    public BenefitPackagePurchase toPurchase() {
        return new BenefitPackagePurchase(
                purchaseId,
                memberCode,
                packageCode,
                packageName,
                salePrice,
                paymentAmount,
                stationCode,
                checkoutTransactionNo,
                status,
                entitlementSnapshot,
                purchasedAt,
                activatedAt,
                expiredAt,
                operatorId,
                operatorName
        );
    }

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getCheckoutTransactionNo() { return checkoutTransactionNo; }
    public void setCheckoutTransactionNo(String checkoutTransactionNo) { this.checkoutTransactionNo = checkoutTransactionNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<BenefitPackageItem> getEntitlementSnapshot() { return entitlementSnapshot; }
    public void setEntitlementSnapshot(List<BenefitPackageItem> entitlementSnapshot) { this.entitlementSnapshot = entitlementSnapshot; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant purchasedAt) { this.purchasedAt = purchasedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getExpiredAt() { return expiredAt; }
    public void setExpiredAt(Instant expiredAt) { this.expiredAt = expiredAt; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
