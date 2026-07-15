package com.cnpc.promoretail.member;

import jakarta.validation.constraints.NotBlank;

public record MemberIdentifyRequest(
        @NotBlank String identifier,
        String identifyType
) {
}
