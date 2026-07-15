package com.cnpc.promoretail.promotion.productgroup;

import java.util.List;

public record ProductGroupMapping(
        String groupId,
        String groupName,
        String source,
        String description,
        List<String> productCodes,
        boolean demoData
) {

    public ProductGroupMapping {
        productCodes = productCodes == null ? List.of() : List.copyOf(productCodes);
    }
}
