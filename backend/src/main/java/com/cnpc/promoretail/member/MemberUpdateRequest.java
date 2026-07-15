package com.cnpc.promoretail.member;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MemberUpdateRequest(
        String memberName,
        String phone,
        String levelCode,
        LocalDate birthday,
        String province,
        String eEnjoyCardNo,
        String usualProvince,
        Instant registeredAt,
        Instant cardOpenedAt,
        String status,
        List<String> memberTags,
        Boolean openedCard
) {

    public MemberUpdateRequest(
            String memberName,
            String phone,
            String levelCode,
            LocalDate birthday,
            String province,
            String status,
            List<String> memberTags
    ) {
        this(memberName, phone, levelCode, birthday, province, status, memberTags, null);
    }

    public MemberUpdateRequest(
            String memberName,
            String phone,
            String levelCode,
            LocalDate birthday,
            String province,
            String status,
            List<String> memberTags,
            Boolean openedCard
    ) {
        this(memberName, phone, levelCode, birthday, province, null, null, null, null,
                status, memberTags, openedCard);
    }

    public MemberUpdateRequest(
            String memberName,
            String phone,
            String levelCode,
            LocalDate birthday,
            String province,
            String status
    ) {
        this(memberName, phone, levelCode, birthday, province, status, null, null);
    }
}
