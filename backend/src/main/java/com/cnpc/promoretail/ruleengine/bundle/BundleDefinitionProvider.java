package com.cnpc.promoretail.ruleengine.bundle;

import com.cnpc.promoretail.ruleengine.model.BundleDefinition;
import java.util.Optional;

public interface BundleDefinitionProvider {

    Optional<BundleDefinition> findActiveBundle(String bundleId);

    static BundleDefinitionProvider empty() {
        return bundleId -> Optional.empty();
    }
}
