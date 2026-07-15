package com.cnpc.promoretail.promotion.benefitpackage.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@TableName("benefit_package")
public class BenefitPackageEntity {

    @TableId("package_code")
    private String packageCode;

    @TableField("package_name")
    private String packageName;

    @TableField("sales_channel")
    private String salesChannel;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("status")
    private String status;

    @TableField("source_sheet_name")
    private String sourceSheetName;

    @TableField("source_row_number")
    private Integer sourceRowNumber;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public BenefitPackage toPackage(List<BenefitPackageItem> items) {
        return new BenefitPackage(
                packageCode,
                packageName,
                salesChannel,
                salePrice,
                status,
                sourceSheetName,
                sourceRowNumber,
                items
        );
    }

    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String salesChannel) { this.salesChannel = salesChannel; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String sourceSheetName) { this.sourceSheetName = sourceSheetName; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
