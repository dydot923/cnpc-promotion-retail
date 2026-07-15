package com.cnpc.promoretail.member.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import java.time.Instant;

@TableName("member_points_change")
public class MemberPointsChangeEntity {

    @TableId("change_id")
    private String changeId;

    @TableField("member_code")
    private String memberCode;

    @TableField("change_type")
    private String changeType;

    @TableField("points_change")
    private Long pointsChange;

    @TableField("total_points_after")
    private Long totalPointsAfter;

    @TableField("available_points_after")
    private Long availablePointsAfter;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("rule_id")
    private String ruleId;

    @TableField("station_code")
    private String stationCode;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("reason")
    private String reason;

    @TableField("occurred_at")
    private Instant occurredAt;

    public static MemberPointsChangeEntity from(MemberPointsChange change) {
        MemberPointsChangeEntity entity = new MemberPointsChangeEntity();
        entity.setChangeId(change.changeId());
        entity.setMemberCode(change.memberCode());
        entity.setChangeType(change.changeType());
        entity.setPointsChange(change.pointsChange());
        entity.setTotalPointsAfter(change.totalPointsAfter());
        entity.setAvailablePointsAfter(change.availablePointsAfter());
        entity.setSourceType(change.sourceType());
        entity.setSourceId(change.sourceId());
        entity.setRuleId(change.ruleId());
        entity.setStationCode(change.stationCode());
        entity.setOperatorId(change.operatorId());
        entity.setOperatorName(change.operatorName());
        entity.setReason(change.reason());
        entity.setOccurredAt(change.occurredAt());
        return entity;
    }

    public MemberPointsChange toChange() {
        return new MemberPointsChange(changeId, memberCode, changeType,
                pointsChange == null ? 0 : pointsChange,
                totalPointsAfter == null ? 0 : totalPointsAfter,
                availablePointsAfter == null ? 0 : availablePointsAfter,
                sourceType, sourceId, ruleId, stationCode, operatorId, operatorName, reason, occurredAt);
    }

    public String getChangeId() { return changeId; }
    public void setChangeId(String changeId) { this.changeId = changeId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Long getPointsChange() { return pointsChange; }
    public void setPointsChange(Long pointsChange) { this.pointsChange = pointsChange; }
    public Long getTotalPointsAfter() { return totalPointsAfter; }
    public void setTotalPointsAfter(Long totalPointsAfter) { this.totalPointsAfter = totalPointsAfter; }
    public Long getAvailablePointsAfter() { return availablePointsAfter; }
    public void setAvailablePointsAfter(Long availablePointsAfter) { this.availablePointsAfter = availablePointsAfter; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
