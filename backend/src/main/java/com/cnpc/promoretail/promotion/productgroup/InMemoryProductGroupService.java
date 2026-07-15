package com.cnpc.promoretail.promotion.productgroup;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev-db & !postgres")
public class InMemoryProductGroupService implements ProductGroupService {

    @Override
    public List<ProductGroupMapping> findAll() {
        return List.of();
    }

    @Override
    public Optional<ProductGroupMapping> findByGroupId(String groupId) {
        return Optional.empty();
    }
}
