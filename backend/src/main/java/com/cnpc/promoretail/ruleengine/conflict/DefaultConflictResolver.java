package com.cnpc.promoretail.ruleengine.conflict;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultConflictResolver implements ConflictResolver {

    @Override
    public List<PromotionCandidate> resolve(List<PromotionCandidate> candidates) {
        List<PromotionCandidate> resolved = new ArrayList<>();
        Map<String, PromotionCandidate> exclusiveWinners = new LinkedHashMap<>();

        for (PromotionCandidate candidate : candidates) {
            if (candidate.stackable() || candidate.exclusiveGroup() == null || candidate.exclusiveGroup().isBlank()) {
                resolved.add(candidate);
                continue;
            }
            exclusiveWinners.merge(candidate.exclusiveGroup(), candidate, this::better);
        }

        resolved.addAll(exclusiveWinners.values());
        return resolved.stream()
                .sorted(Comparator.comparing(PromotionCandidate::candidateId))
                .toList();
    }

    private PromotionCandidate better(PromotionCandidate left, PromotionCandidate right) {
        int payableCompare = left.payableAmount().compareTo(right.payableAmount());
        if (payableCompare != 0) {
            return payableCompare <= 0 ? left : right;
        }
        int discountCompare = left.discountAmount().compareTo(right.discountAmount());
        if (discountCompare != 0) {
            return discountCompare >= 0 ? left : right;
        }
        return left.priority() >= right.priority() ? left : right;
    }
}
