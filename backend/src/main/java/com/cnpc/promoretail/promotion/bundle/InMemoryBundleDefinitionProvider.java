package com.cnpc.promoretail.promotion.bundle;

import com.cnpc.promoretail.ruleengine.bundle.BundleDefinitionProvider;
import com.cnpc.promoretail.ruleengine.model.BundleDefinition;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryBundleDefinitionProvider implements BundleDefinitionProvider {

    private final ConcurrentMap<String, BundleDefinition> bundles = new ConcurrentHashMap<>();

    public BundleDefinition save(BundleDefinition definition) {
        bundles.put(definition.bundleId(), definition);
        return definition;
    }

    @Override
    public Optional<BundleDefinition> findActiveBundle(String bundleId) {
        return Optional.ofNullable(bundles.get(bundleId));
    }
}
