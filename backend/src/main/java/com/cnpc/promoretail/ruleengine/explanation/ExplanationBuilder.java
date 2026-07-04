package com.cnpc.promoretail.ruleengine.explanation;

import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.List;

public interface ExplanationBuilder {

    List<String> summarize(PromotionCandidate recommended, List<BlockedPromotion> blockedPromotions);
}

