package com.cnpc.promoretail.promotion.points.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.promotion.points.PointsLotteryDraw;
import java.time.Instant;
import java.time.LocalDate;

@TableName("points_lottery_draw")
public class PointsLotteryDrawEntity {

    @TableId("draw_id")
    private String drawId;

    @TableField("member_code")
    private String memberCode;

    @TableField("activity_code")
    private String activityCode;

    @TableField("points_cost")
    private Integer pointsCost;

    @TableField("prize_type")
    private String prizeType;

    @TableField("prize_coupon_id")
    private String prizeCouponId;

    @TableField("result_label")
    private String resultLabel;

    @TableField("business_date")
    private LocalDate businessDate;

    @TableField("station_code")
    private String stationCode;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("created_at")
    private Instant createdAt;

    public static PointsLotteryDrawEntity from(PointsLotteryDraw draw) {
        PointsLotteryDrawEntity entity = new PointsLotteryDrawEntity();
        entity.setDrawId(draw.drawId());
        entity.setMemberCode(draw.memberCode());
        entity.setActivityCode(draw.activityCode());
        entity.setPointsCost(draw.pointsCost());
        entity.setPrizeType(draw.prizeType());
        entity.setPrizeCouponId(draw.prizeCouponId());
        entity.setResultLabel(draw.resultLabel());
        entity.setBusinessDate(draw.businessDate());
        entity.setStationCode(draw.stationCode());
        entity.setOperatorId(draw.operatorId());
        entity.setOperatorName(draw.operatorName());
        entity.setCreatedAt(draw.createdAt());
        return entity;
    }

    public PointsLotteryDraw toDraw() {
        return new PointsLotteryDraw(drawId, memberCode, activityCode,
                pointsCost == null ? 500 : pointsCost,
                prizeType, prizeCouponId, resultLabel, businessDate, stationCode,
                operatorId, operatorName, createdAt);
    }

    public String getDrawId() { return drawId; }
    public void setDrawId(String drawId) { this.drawId = drawId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String activityCode) { this.activityCode = activityCode; }
    public Integer getPointsCost() { return pointsCost; }
    public void setPointsCost(Integer pointsCost) { this.pointsCost = pointsCost; }
    public String getPrizeType() { return prizeType; }
    public void setPrizeType(String prizeType) { this.prizeType = prizeType; }
    public String getPrizeCouponId() { return prizeCouponId; }
    public void setPrizeCouponId(String prizeCouponId) { this.prizeCouponId = prizeCouponId; }
    public String getResultLabel() { return resultLabel; }
    public void setResultLabel(String resultLabel) { this.resultLabel = resultLabel; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
