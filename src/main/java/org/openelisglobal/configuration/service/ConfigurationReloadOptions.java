package org.openelisglobal.configuration.service;

import java.util.Collections;
import java.util.Set;

public record ConfigurationReloadOptions(Set<String> domains, boolean force) {

    public ConfigurationReloadOptions {
        domains = domains == null ? Collections.emptySet() : Set.copyOf(domains);
    }

    public static ConfigurationReloadOptions all() {
        return new ConfigurationReloadOptions(Collections.emptySet(), false);
    }

    public boolean includesDomain(String domain) {
        return domains.isEmpty() || domains.contains(domain);
    }
}
