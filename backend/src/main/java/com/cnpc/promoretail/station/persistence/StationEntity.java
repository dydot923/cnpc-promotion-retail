package com.cnpc.promoretail.station.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.station.model.Station;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@TableName(value = "station", autoResultMap = true)
public class StationEntity {

    @TableId("station_code")
    private String stationCode;

    @TableField("hos_code")
    private String hosCode;

    @TableField("station_name")
    private String stationName;

    @TableField("branch_company")
    private String branchCompany;

    @TableField("prefecture")
    private String prefecture;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("address")
    private String address;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("station_type")
    private String stationType;

    @TableField(value = "sales_scope", typeHandler = JsonbTypeHandler.class)
    private List<String> salesScope;

    @TableField("remark")
    private String remark;

    @TableField("source_sheet_name")
    private String sourceSheetName;

    @TableField("source_row_number")
    private Integer sourceRowNumber;

    @TableField("is_demo_data")
    private Boolean demoData;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public Station toStation() {
        return new Station(
                stationCode,
                hosCode,
                stationName,
                branchCompany,
                prefecture,
                province,
                city,
                district,
                address,
                longitude,
                latitude,
                contactName,
                contactPhone,
                stationType,
                salesScope,
                remark,
                sourceSheetName,
                sourceRowNumber,
                Boolean.TRUE.equals(demoData)
        );
    }

    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getHosCode() { return hosCode; }
    public void setHosCode(String hosCode) { this.hosCode = hosCode; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getBranchCompany() { return branchCompany; }
    public void setBranchCompany(String branchCompany) { this.branchCompany = branchCompany; }
    public String getPrefecture() { return prefecture; }
    public void setPrefecture(String prefecture) { this.prefecture = prefecture; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getStationType() { return stationType; }
    public void setStationType(String stationType) { this.stationType = stationType; }
    public List<String> getSalesScope() { return salesScope; }
    public void setSalesScope(List<String> salesScope) { this.salesScope = salesScope; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String sourceSheetName) { this.sourceSheetName = sourceSheetName; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
    public Boolean getDemoData() { return demoData; }
    public void setDemoData(Boolean demoData) { this.demoData = demoData; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
