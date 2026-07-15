package com.cnpc.promoretail.ruleengine.conflict;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class DefaultConflictResolver implements ConflictResolver {

    @Override
    public List<PromotionCandidate> resolve(List<PromotionCandidate> candidates) {
        List<PromotionCandidate> groupedWinners = new ArrayList<>();
        Map<String, PromotionCandidate> exclusiveWinners = new LinkedHashMap<>();

        for (PromotionCandidate candidate : candidates) {
            if (candidate.ruleType() == PromotionRuleType.COUPON_REDEEM) {
                groupedWinners.add(candidate);
                continue;
            }
            if (candidate.stackable() || candidate.exclusiveGroup() == null || candidate.exclusiveGroup().isBlank()) {
                groupedWinners.add(candidate);
                continue;
            }
            exclusiveWinners.merge(candidate.exclusiveGroup(), candidate, this::better);
        }

        groupedWinners.addAll(exclusiveWinners.values());
        return removeOverlappingNonStackableCandidates(groupedWinners);
    }

    private List<PromotionCandidate> removeOverlappingNonStackableCandidates(List<PromotionCandidate> candidates) {
        List<PromotionCandidate> selected = new ArrayList<>();
        Set<String> occupiedProductCodes = new HashSet<>();

        List<PromotionCandidate> sorted = candidates.stream()
                .sorted(this::compareBetter)
                .toList();

        for (PromotionCandidate candidate : sorted) {
            if (canSelect(candidate, selected, occupiedProductCodes)) {
                selected.add(candidate);
                if (!candidate.stackable() && candidate.ruleType() != PromotionRuleType.COUPON_REDEEM) {
                    occupiedProductCodes.addAll(candidate.consumedProductCodes());
                }
            }
        }

        return selected.stream()
                .sorted(Comparator.comparing(PromotionCandidate::candidateId))
                .toList();
    }

    private boolean canSelect(
            PromotionCandidate candidate,
            List<PromotionCandidate> selected,
            Set<String> occupiedProductCodes
    ) {
        if (candidate.stackable() || candidate.consumedProductCodes().isEmpty()) {
            return true;
        }
        if (candidate.ruleType() == PromotionRuleType.COUPON_REDEEM) {
            return selected.stream()
                    .filter(selectedCandidate -> !selectedCandidate.stackable())
                    .filter(selectedCandidate -> selectedCandidate.ruleType() != PromotionRuleType.COUPON_REDEEM)
                    .noneMatch(selectedCandidate -> sameExclusiveGroup(candidate, selectedCandidate)
                            || !disjoint(candidate.consumedProductCodes(), selectedCandidate.consumedProductCodes()));
        }
        boolean conflictsWithSelectedCoupon = selected.stream()
                .filter(selectedCandidate -> !selectedCandidate.stackable())
                .filter(selectedCandidate -> selectedCandidate.ruleType() == PromotionRuleType.COUPON_REDEEM)
                .anyMatch(selectedCandidate -> sameExclusiveGroup(candidate, selectedCandidate)
                        || !disjoint(candidate.consumedProductCodes(), selectedCandidate.consumedProductCodes()));
        if (conflictsWithSelectedCoupon) {
            return false;
        }
        return disjoint(candidate.consumedProductCodes(), occupiedProductCodes);
    }

    private boolean sameExclusiveGroup(PromotionCandidate left, PromotionCandidate right) {
        return left.exclusiveGroup() != null
                && !left.exclusiveGroup().isBlank()
                && left.exclusiveGroup().equals(right.exclusiveGroup());
    }

    private boolean disjoint(Set<String> left, Set<String> right) {
        return left.stream().noneMatch(right::contains);
    }

    private PromotionCandidate better(PromotionCandidate left, PromotionCandidate right) {
        return compareBetter(left, right) <= 0 ? left : right;
    }

    private int compareBetter(PromotionCandidate left, PromotionCandidate right) {
        int payableCompare = left.payableAmount().compareTo(right.payableAmount());
        if (payableCompare != 0) {
            return payableCompare;
        }
        int discountCompare = left.discountAmount().compareTo(right.discountAmount());
        if (discountCompare != 0) {
            return -discountCompare;
        }
        int priorityCompare = Integer.compare(left.priority(), right.priority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return left.candidateId().compareTo(right.candidateId());
    }
}
