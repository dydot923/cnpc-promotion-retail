package com.cnpc.promoretail.member.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Member(
        String memberCode,
        String memberName,
        String phone,
        String levelCode,
        long totalPoints,
        long availablePoints,
        LocalDate birthday,
        String province,
        String eEnjoyCardNo,
        String usualProvince,
        Instant registeredAt,
        Instant cardOpenedAt,
        String status,
        List<String> memberTags
) {

    public Member {
        if (memberCode == null || memberCode.isBlank()) {
            throw new IllegalArgumentException("memberCode is required");
        }
        memberName = memberName == null || memberName.isBlank() ? memberCode : memberName;
        phone = phone == null ? "" : phone;
        levelCode = levelCode == null || levelCode.isBlank() ? "normal" : levelCode;
        totalPoints = Math.max(0, totalPoints);
        availablePoints = Math.max(0, availablePoints);
        province = province == null ? "" : province;
        eEnjoyCardNo = eEnjoyCardNo == null ? "" : eEnjoyCardNo;
        usualProvince = usualProvince == null || usualProvince.isBlank() ? province : usualProvince;
        status = status == null || status.isBlank() ? "ACTIVE" : status;
        memberTags = memberTags == null ? List.of() : List.copyOf(memberTags);
    }

    public Member(
            String memberCode,
            String memberName,
            String phone,
            String levelCode,
            long totalPoints,
            long availablePoints,
            LocalDate birthday,
            String province,
            String status,
            List<String> memberTags
    ) {
        this(memberCode, memberName, phone, levelCode, totalPoints, availablePoints,
                birthday, province, "", province, null, null, status, memberTags);
    }

    public Member(
            String memberCode,
            String memberName,
            String phone,
            String levelCode,
            long totalPoints,
            long availablePoints,
            LocalDate birthday,
            String province,
            String status
    ) {
        this(memberCode, memberName, phone, levelCode, totalPoints, availablePoints,
                birthday, province, status, List.of());
    }

    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public Integer birthMonth() {
        return birthday == null ? null : birthday.getMonthValue();
    }

    public Member withProfile(String memberName, String phone, String levelCode, LocalDate birthday, String province, String status) {
        return withProfile(memberName, phone, levelCode, birthday, province,
                null, null, null, null, status);
    }

    public Member withProfile(
            String memberName,
            String phone,
            String levelCode,
            LocalDate birthday,
            String province,
            String eEnjoyCardNo,
            String usualProvince,
            Instant registeredAt,
            Instant cardOpenedAt,
            String status
    ) {
        return new Member(
                memberCode,
                memberName == null || memberName.isBlank() ? this.memberName : memberName,
                phone == null ? this.phone : phone,
                levelCode == null || levelCode.isBlank() ? this.levelCode : levelCode,
                totalPoints,
                availablePoints,
                birthday == null ? this.birthday : birthday,
                province == null ? this.province : province,
                eEnjoyCardNo == null ? this.eEnjoyCardNo : eEnjoyCardNo,
                usualProvince == null ? this.usualProvince : usualProvince,
                registeredAt == null ? this.registeredAt : registeredAt,
                cardOpenedAt == null ? this.cardOpenedAt : cardOpenedAt,
                status == null || status.isBlank() ? this.status : status,
                memberTags
        );
    }

    public Member withTags(List<String> memberTags) {
        return new Member(memberCode, memberName, phone, levelCode, totalPoints, availablePoints,
                birthday, province, eEnjoyCardNo, usualProvince, registeredAt, cardOpenedAt, status, memberTags);
    }

    public Member withPoints(long totalPoints, long availablePoints) {
        return new Member(memberCode, memberName, phone, levelCode, totalPoints, availablePoints,
                birthday, province, eEnjoyCardNo, usualProvince, registeredAt, cardOpenedAt, status, memberTags);
    }
}
