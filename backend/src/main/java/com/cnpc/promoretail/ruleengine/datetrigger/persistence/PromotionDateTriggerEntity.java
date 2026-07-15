package com.cnpc.promoretail.ruleengine.datetrigger.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTrigger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@TableName(value = "promotion_date_trigger", autoResultMap = true)
public class PromotionDateTriggerEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("activity_code")
    private String activityCode;

    @TableField("rule_id")
    private String ruleId;

    @TableField("trigger_type")
    private String triggerType;

    @TableField(value = "days_of_month", typeHandler = JsonbTypeHandler.class)
    private List<Integer> daysOfMonth;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("time_from")
    private LocalTime timeFrom;

    @TableField("time_to")
    private LocalTime timeTo;

    @TableField("description")
    private String description;

    @TableField("source_sheet_name")
    private String sourceSheetName;

    @TableField("source_row_number")
    private Integer sourceRowNumber;

    @TableField("created_at")
    private Instant createdAt;

    public PromotionDateTrigger toTrigger() {
        return new PromotionDateTrigger(
                id,
                activityCode,
                ruleId,
                triggerType,
                daysOfMonth == null ? Set.of() : Set.copyOf(daysOfMonth),
                startDate,
                endDate,
                timeFrom,
                timeTo,
                description,
                sourceSheetName,
                sourceRowNumber,
                true
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String activityCode) { this.activityCode = activityCode; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public List<Integer> getDaysOfMonth() { return daysOfMonth; }
    public void setDaysOfMonth(List<Integer> daysOfMonth) { this.daysOfMonth = daysOfMonth; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getTimeFrom() { return timeFrom; }
    public void setTimeFrom(LocalTime timeFrom) { this.timeFrom = timeFrom; }
    public LocalTime getTimeTo() { return timeTo; }
    public void setTimeTo(LocalTime timeTo) { this.timeTo = timeTo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String sourceSheetName) { this.sourceSheetName = sourceSheetName; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
