package com.cnpc.promoretail.checkout.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;

@TableName("checkout_transaction")
public class CheckoutTransactionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("txn_no")
    private String txnNo;

    @TableField("confirmation_id")
    private String confirmationId;

    @TableField("calculation_id")
    private String calculationId;

    @TableField("selected_candidate_id")
    private String selectedCandidateId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("member_code")
    private String memberCode;

    @TableField("station_code")
    private String stationCode;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTxnNo() { return txnNo; }
    public void setTxnNo(String txnNo) { this.txnNo = txnNo; }
    public String getConfirmationId() { return confirmationId; }
    public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }
    public String getCalculationId() { return calculationId; }
    public void setCalculationId(String calculationId) { this.calculationId = calculationId; }
    public String getSelectedCandidateId() { return selectedCandidateId; }
    public void setSelectedCandidateId(String selectedCandidateId) { this.selectedCandidateId = selectedCandidateId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
