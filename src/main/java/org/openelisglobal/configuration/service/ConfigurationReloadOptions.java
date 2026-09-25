package org.openelisglobal.configuration.service;

import java.util.Collections;
import java.util.Set;

/**
 * Which catalog files a reload should read. {@code domains} narrows it to those
 * areas, empty meaning all of them. A file whose content has not changed since
 * the last load is skipped unless it is forced: {@code force} forces every file
 * in scope. {@code forcedFiles} narrows the reload further to exactly the named
 * files, read whatever their checksum says and with every other file in scope
 * left alone, which is how an upload re-applies what was uploaded and nothing
 * else.
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

    /**
     * Reads the named files, and only those, again whatever their checksum says.
     */
    public static ConfigurationReloadOptions forFiles(Set<String> domains, Set<String> fileNames) {
        return new ConfigurationReloadOptions(domains, false, fileNames);
    }

    public boolean includesDomain(String domain) {
        return domains.isEmpty() || domains.contains(domain);
    }

    /** Whether the reload reads this file at all. */
    public boolean includesFile(String fileName) {
        return forcedFiles.isEmpty() || forcedFiles.contains(fileName);
    }

    public boolean forces(String fileName) {
        return force || forcedFiles.contains(fileName);
    }
}
