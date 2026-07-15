package com.cnpc.promoretail.member;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MemberCreateRequest(
        String memberCode,
        @NotBlank String memberName,
        String phone,
        String levelCode,
        Long totalPoints,
        Long availablePoints,
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

    public MemberCreateRequest(
            String memberCode,
            @NotBlank String memberName,
            String phone,
            String levelCode,
            Long totalPoints,
            Long availablePoints,
            LocalDate birthday,
            String province,
            String status,
            List<String> memberTags
    ) {
        this(memberCode, memberName, phone, levelCode, totalPoints, availablePoints, birthday, province, status,
                memberTags, true);
    }

    public MemberCreateRequest(
            String memberCode,
            @NotBlank String memberName,
            String phone,
            String levelCode,
            Long totalPoints,
            Long availablePoints,
            LocalDate birthday,
            String province,
            String status,
            List<String> memberTags,
            Boolean openedCard
    ) {
        this(memberCode, memberName, phone, levelCode, totalPoints, availablePoints, birthday, province,
                null, null, null, null, status, memberTags, openedCard);
    }

    public MemberCreateRequest(
            String memberCode,
            @NotBlank String memberName,
            String phone,
            String levelCode,
            Long totalPoints,
            Long availablePoints,
            LocalDate birthday,
            String province,
            String status
    ) {
        this(memberCode, memberName, phone, levelCode, totalPoints, availablePoints, birthday, province, status,
                List.of(), true);
    }
}
