package com.cnpc.promoretail.ruleengine.ranking;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.List;
import java.util.Optional;

public interface CandidateRanker {

    Optional<PromotionCandidate> recommend(List<PromotionCandidate> candidates);
}

