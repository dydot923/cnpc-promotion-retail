package com.cnpc.promoretail.promotion.operation;

import java.util.List;

public record OperationCampaignDefinition(
        String campaignCode,
        String campaignName,
        String endpoint,
        String benefitSummary,
        List<String> requiredFields,
        List<String> optionalFields
) {
}
