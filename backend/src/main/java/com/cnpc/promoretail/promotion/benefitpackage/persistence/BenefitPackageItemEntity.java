package com.cnpc.promoretail.promotion.benefitpackage.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import java.math.BigDecimal;

@TableName("benefit_package_item")
public class BenefitPackageItemEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("package_code")
    private String packageCode;

    @TableField("item_name")
    private String itemName;

    @TableField("quantity")
    private BigDecimal quantity;

    @TableField("remark")
    private String remark;

    @TableField("source_row_number")
    private Integer sourceRowNumber;

    public BenefitPackageItem toItem() {
        return new BenefitPackageItem(itemName, quantity, remark, sourceRowNumber);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
}
