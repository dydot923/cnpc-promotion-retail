package com.cnpc.promoretail.ruleengine.ranking;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DefaultCandidateRanker implements CandidateRanker {

    @Override
    public Optional<PromotionCandidate> recommend(List<PromotionCandidate> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(PromotionCandidate::payableAmount)
                        .thenComparing(Comparator.comparing(PromotionCandidate::discountAmount).reversed())
                        .thenComparing(Comparator.comparing(PromotionCandidate::priority).reversed())
                        .thenComparing(PromotionCandidate::candidateId));
    }
}
