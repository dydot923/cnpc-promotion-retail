package com.cnpc.promoretail.importcenter.model;

import com.cnpc.promoretail.promotion.coupon.CouponTemplate;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.util.List;

public record CouponImportRecord(
        ImportVersion importId,
        String sourceSheetName,
        int sourceRowNumber,
        CouponTemplate couponTemplate,
        List<Coupon> couponInstances,
        String mappingNote
) {

    public CouponImportRecord {
        if (importId == null) {
            throw new IllegalArgumentException("importId is required");
        }
        if (sourceSheetName == null || sourceSheetName.isBlank()) {
            throw new IllegalArgumentException("sourceSheetName is required");
        }
        if (sourceRowNumber <= 0) {
            throw new IllegalArgumentException("sourceRowNumber must be positive");
        }
        if (couponTemplate == null) {
            throw new IllegalArgumentException("couponTemplate is required");
        }
        couponInstances = couponInstances == null ? List.of() : List.copyOf(couponInstances);
        mappingNote = mappingNote == null ? "" : mappingNote;
    }
}
