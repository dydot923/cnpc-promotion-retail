package com.cnpc.promoretail.member.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cnpc.promoretail.common.persistence.JsonbTypeHandler;
import com.cnpc.promoretail.member.model.Member;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@TableName(value = "member", autoResultMap = true)
public class MemberEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("member_code")
    private String memberCode;

    @TableField("member_name")
    private String memberName;

    @TableField("phone")
    private String phone;

    @TableField("level_code")
    private String levelCode;

    @TableField("total_points")
    private Long totalPoints;

    @TableField("available_points")
    private Long availablePoints;

    @TableField("birthday")
    private LocalDate birthday;

    @TableField("province")
    private String province;

    @TableField("e_enjoy_card_no")
    private String eEnjoyCardNo;

    @TableField("usual_province")
    private String usualProvince;

    @TableField("registered_at")
    private Instant registeredAt;

    @TableField("card_opened_at")
    private Instant cardOpenedAt;

    @TableField(value = "member_tags", typeHandler = JsonbTypeHandler.class)
    private List<String> memberTags;

    @TableField("status")
    private String status;

    @TableField("is_demo_data")
    private Boolean demoData;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public Member toMember() {
        return new Member(memberCode, memberName, phone, levelCode,
                totalPoints == null ? 0 : totalPoints,
                availablePoints == null ? 0 : availablePoints,
                birthday, province, eEnjoyCardNo, usualProvince, registeredAt, cardOpenedAt,
                status, memberTags);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
    public Long getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Long totalPoints) { this.totalPoints = totalPoints; }
    public Long getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Long availablePoints) { this.availablePoints = availablePoints; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getEEnjoyCardNo() { return eEnjoyCardNo; }
    public void setEEnjoyCardNo(String eEnjoyCardNo) { this.eEnjoyCardNo = eEnjoyCardNo; }
    public String getUsualProvince() { return usualProvince; }
    public void setUsualProvince(String usualProvince) { this.usualProvince = usualProvince; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    public Instant getCardOpenedAt() { return cardOpenedAt; }
    public void setCardOpenedAt(Instant cardOpenedAt) { this.cardOpenedAt = cardOpenedAt; }
    public List<String> getMemberTags() { return memberTags; }
    public void setMemberTags(List<String> memberTags) { this.memberTags = memberTags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getDemoData() { return demoData; }
    public void setDemoData(Boolean demoData) { this.demoData = demoData; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
