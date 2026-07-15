package com.cnpc.promoretail.promotion.points.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.promotion.points.PointsActivity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@TableName(value = "points_activity", autoResultMap = true)
public class PointsActivityEntity {

    @TableId("activity_id")
    private String activityId;

    @TableField("rule_id")
    private String ruleId;

    @TableField("activity_name")
    private String activityName;

    @TableField("points_multiplier")
    private BigDecimal pointsMultiplier;

    @TableField("member_required")
    private Boolean memberRequired;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField(value = "days_of_month", typeHandler = JsonbTypeHandler.class)
    private List<Integer> daysOfMonth;

    @TableField(value = "station_types", typeHandler = JsonbTypeHandler.class)
    private List<String> stationTypes;

    @TableField(value = "station_provinces", typeHandler = JsonbTypeHandler.class)
    private List<String> stationProvinces;

    @TableField(value = "fuel_types", typeHandler = JsonbTypeHandler.class)
    private List<String> fuelTypes;

    @TableField(value = "included_categories", typeHandler = JsonbTypeHandler.class)
    private List<String> includedCategories;

    @TableField(value = "excluded_categories", typeHandler = JsonbTypeHandler.class)
    private List<String> excludedCategories;

    @TableField("status")
    private String status;

    public PointsActivity toActivity() {
        return new PointsActivity(
                activityId,
                ruleId,
                activityName,
                pointsMultiplier,
                Boolean.TRUE.equals(memberRequired),
                startDate,
                endDate,
                daysOfMonth == null ? Set.of() : Set.copyOf(daysOfMonth),
                stationTypes == null ? Set.of() : Set.copyOf(stationTypes),
                stationProvinces == null ? Set.of() : Set.copyOf(stationProvinces),
                fuelTypes == null ? Set.of() : Set.copyOf(fuelTypes),
                includedCategories == null ? Set.of() : Set.copyOf(includedCategories),
                excludedCategories == null ? Set.of() : Set.copyOf(excludedCategories),
                status
        );
    }

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public BigDecimal getPointsMultiplier() { return pointsMultiplier; }
    public void setPointsMultiplier(BigDecimal pointsMultiplier) { this.pointsMultiplier = pointsMultiplier; }
    public Boolean getMemberRequired() { return memberRequired; }
    public void setMemberRequired(Boolean memberRequired) { this.memberRequired = memberRequired; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public List<Integer> getDaysOfMonth() { return daysOfMonth; }
    public void setDaysOfMonth(List<Integer> daysOfMonth) { this.daysOfMonth = daysOfMonth; }
    public List<String> getStationTypes() { return stationTypes; }
    public void setStationTypes(List<String> stationTypes) { this.stationTypes = stationTypes; }
    public List<String> getStationProvinces() { return stationProvinces; }
    public void setStationProvinces(List<String> stationProvinces) { this.stationProvinces = stationProvinces; }
    public List<String> getFuelTypes() { return fuelTypes; }
    public void setFuelTypes(List<String> fuelTypes) { this.fuelTypes = fuelTypes; }
    public List<String> getIncludedCategories() { return includedCategories; }
    public void setIncludedCategories(List<String> includedCategories) { this.includedCategories = includedCategories; }
    public List<String> getExcludedCategories() { return excludedCategories; }
    public void setExcludedCategories(List<String> excludedCategories) { this.excludedCategories = excludedCategories; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
