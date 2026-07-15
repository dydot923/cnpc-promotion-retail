package com.cnpc.promoretail.member;

import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MemberResponse(
        String memberCode,
        String memberName,
        String phone,
        String level,
        String levelName,
        long totalPoints,
        long availablePoints,
        LocalDate birthday,
        String province,
        String eEnjoyCardNo,
        String usualProvince,
        Instant registeredAt,
        Instant cardOpenedAt,
        String status,
        List<String> memberTags,
        BigDecimal discountRate,
        BigDecimal pointsMultiplier,
        List<String> benefits
) {

    public static MemberResponse from(Member member, MemberLevel level) {
        return new MemberResponse(
                member.memberCode(),
                member.memberName(),
                member.phone(),
                member.levelCode(),
                level == null ? member.levelCode() : level.levelName(),
                member.totalPoints(),
                member.availablePoints(),
                member.birthday(),
                member.province(),
                member.eEnjoyCardNo(),
                member.usualProvince(),
                member.registeredAt(),
                member.cardOpenedAt(),
                member.status(),
                member.memberTags(),
                level == null ? BigDecimal.ONE : level.discountRate(),
                level == null ? BigDecimal.ONE : level.pointsMultiplier(),
                level == null ? List.of() : level.benefits()
        );
    }
}
