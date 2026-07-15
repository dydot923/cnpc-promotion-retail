package com.cnpc.promoretail.promotion.productgroup;

import java.util.List;
import java.util.Optional;

public interface ProductGroupService {

    List<ProductGroupMapping> findAll();

    Optional<ProductGroupMapping> findByGroupId(String groupId);
}
