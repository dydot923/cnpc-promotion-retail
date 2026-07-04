package com.cnpc.promoretail.ruleengine.explanation;

import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.util.ArrayList;
import java.util.List;

public class DefaultExplanationBuilder implements ExplanationBuilder {

    @Override
    public List<String> summarize(PromotionCandidate recommended, List<BlockedPromotion> blockedPromotions) {
        List<String> explanations = new ArrayList<>();
        if (recommended != null) {
            explanations.add("推荐方案：" + recommended.title() + "，应付 "
                    + recommended.payableAmount() + " 元，优惠 " + recommended.discountAmount() + " 元。");
            explanations.add(recommended.explanation());
        }
        if (!blockedPromotions.isEmpty()) {
            explanations.add("有 " + blockedPromotions.size() + " 个促销当前不可用，已返回不可用原因。");
        }
        return explanations;
    }
}
