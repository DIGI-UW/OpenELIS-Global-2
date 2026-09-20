package org.openelisglobal.configuration.service;

import java.util.Collections;
import java.util.Set;

/**
 * Which catalog files a reload should read. {@code domains} narrows it to those
 * areas, empty meaning all of them. A file whose content has not changed since
 * the last load is skipped unless it is forced: {@code force} forces every file
 * in scope, {@code forcedFiles} only the named ones, which is how an upload
 * re-applies exactly what was uploaded without re-reading the rest of the
 * shipped catalog.
 */
public record ConfigurationReloadOptions(Set<String> domains, boolean force, Set<String> forcedFiles) {

    public ConfigurationReloadOptions {
        domains = domains == null ? Collections.emptySet() : Set.copyOf(domains);
        forcedFiles = forcedFiles == null ? Collections.emptySet() : Set.copyOf(forcedFiles);
    }

    public ConfigurationReloadOptions(Set<String> domains, boolean force) {
        this(domains, force, Collections.emptySet());
    }

    public static ConfigurationReloadOptions all() {
        return new ConfigurationReloadOptions(Collections.emptySet(), false);
    }

    /** Reads the named files again whatever their checksum says. */
    public static ConfigurationReloadOptions forFiles(Set<String> domains, Set<String> fileNames) {
        return new ConfigurationReloadOptions(domains, false, fileNames);
    }

    public boolean includesDomain(String domain) {
        return domains.isEmpty() || domains.contains(domain);
    }

    public boolean forces(String fileName) {
        return force || forcedFiles.contains(fileName);
    }
}
