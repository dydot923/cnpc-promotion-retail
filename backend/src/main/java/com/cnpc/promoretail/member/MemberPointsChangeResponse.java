package com.cnpc.promoretail.member;

import com.cnpc.promoretail.member.model.MemberPointsChange;
import java.time.Instant;

public record MemberPointsChangeResponse(
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

    public static MemberPointsChangeResponse from(MemberPointsChange change) {
        return new MemberPointsChangeResponse(
                change.changeId(),
                change.memberCode(),
                change.changeType(),
                change.pointsChange(),
                change.totalPointsAfter(),
                change.availablePointsAfter(),
                change.sourceType(),
                change.sourceId(),
                change.ruleId(),
                change.stationCode(),
                change.operatorId(),
                change.operatorName(),
                change.reason(),
                change.occurredAt()
        );
    }
}
