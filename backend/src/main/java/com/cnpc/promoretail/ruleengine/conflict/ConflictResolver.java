package com.cnpc.promoretail.ruleengine.conflict;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.List;

public interface ConflictResolver {

    List<PromotionCandidate> resolve(List<PromotionCandidate> candidates);
}

