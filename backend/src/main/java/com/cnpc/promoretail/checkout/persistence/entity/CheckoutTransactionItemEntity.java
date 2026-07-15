package com.cnpc.promoretail.checkout.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("checkout_transaction_item")
public class CheckoutTransactionItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("transaction_id")
    private Long transactionId;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("barcode")
    private String barcode;

    @TableField("category")
    private String category;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("actual_price")
    private BigDecimal actualPrice;

    @TableField("quantity")
    private Integer quantity;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("applied_promo_id")
    private String appliedPromoId;

    @TableField("applied_coupon_code")
    private String appliedCouponCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getActualPrice() { return actualPrice; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getAppliedPromoId() { return appliedPromoId; }
    public void setAppliedPromoId(String appliedPromoId) { this.appliedPromoId = appliedPromoId; }
    public String getAppliedCouponCode() { return appliedCouponCode; }
    public void setAppliedCouponCode(String appliedCouponCode) { this.appliedCouponCode = appliedCouponCode; }
}
