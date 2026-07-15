package com.cnpc.promoretail.member;

public record PointsChangeResponse(
        String memberCode,
        long change,
        long totalPoints,
        long availablePoints,
        String reason
) {
}
