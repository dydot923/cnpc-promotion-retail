package com.cnpc.promoretail.member.model;

import java.time.Instant;

public record MemberPointsChange(
        String changeId,
        String memberCode,
        String changeType,
        long pointsChange,
        long totalPointsAfter,
        long availablePointsAfter,
        String sourceType,
        String sourceId,
        String ruleId,
        String stationCode,
        String operatorId,
        String operatorName,
        String reason,
        Instant occurredAt
) {

    public MemberPointsChange {
        if (changeId == null || changeId.isBlank()) {
            throw new IllegalArgumentException("changeId is required");
        }
        if (memberCode == null || memberCode.isBlank()) {
            throw new IllegalArgumentException("memberCode is required");
        }
        changeType = changeType == null || changeType.isBlank() ? "ADJUST" : changeType;
        sourceType = sourceType == null || sourceType.isBlank() ? "MANUAL" : sourceType;
        sourceId = sourceId == null ? "" : sourceId;
        ruleId = ruleId == null ? "" : ruleId;
        stationCode = stationCode == null ? "" : stationCode;
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
        reason = reason == null ? "" : reason;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
