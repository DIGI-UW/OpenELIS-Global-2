package org.openelisglobal.configuration.service;

import java.io.InputStream;

/**
 * Interface for domain-specific configuration handlers. Each domain (e.g.,
 * questionnaires, roles, etc.) should implement this interface.
 */
public interface DomainConfigurationHandler {

    /**
     * Returns the name of the domain this handler manages. This name is used for
     * directory paths and checksum files.
     *
     * @return domain name (e.g., "questionnaires", "roles")
     */
    String getDomainName();

    /**
     * Returns the file extension for this domain's configuration files.
     *
     * @return file extension without the dot (e.g., "json", "xml", "csv")
     */
    String getFileExtension();

    /**
     * Returns the loading order for this handler. Handlers with lower order values
     * are loaded first. This is used to ensure dependencies are loaded before
     * dependents (e.g., sample types before tests, test sections before tests).
     *
     * Default order values: - 100: Base entities (sample types, test sections,
     * units of measure) - 200: Dependent entities (tests, test-sample-type
     * mappings) - 300: Higher-level configurations (questionnaires, roles)
     *
     * @return the loading order (lower values load first)
     */
    default int getLoadOrder() {
        return 500; // Default to a middle value for handlers that don't specify
    }

    /**
     * Returns an Ant-style glob pattern that determines which files this handler
     * processes. When multiple handlers share the same domain, more-specific
     * patterns (from handlers with lower load order) claim files first; broader
     * patterns only see unclaimed files.
     *
     * @return Ant-style glob pattern (e.g., {@code "*.csv"},
     *         {@code "*-levels.csv"})
     */
    default String getFileMatcher() {
        return "*." + getFileExtension();
    }

    /**
     * Processes a configuration file from the given input stream.
     *
     * @param inputStream the input stream containing the configuration data
     * @param fileName    the name of the file being processed
     * @throws Exception if processing fails
     */
    void processConfiguration(InputStream inputStream, String fileName) throws Exception;

    /**
     * Outcome counters of the most recently processed file, for handlers that
     * record one row as created, updated or skipped; {@code null} otherwise.
     */
    default CsvLoadSummary getLastSummary() {
        return null;
    }

    /**
     * Whether {@link #processConfiguration(InputStream, String, boolean)} can run
     * without keeping anything, so an import preview can show the plan for this
     * domain. Handlers that write in one file-wide unit of work say no and are left
     * out of previews.
     */
    default boolean supportsDryRun() {
        return false;
    }

    /**
     * Processes the file, or, when {@code dryRun} is set and the handler supports
     * it, evaluates every row against the current database and rolls each one back,
     * leaving the outcomes in {@link #getLastSummary()}.
     */
    default void processConfiguration(InputStream inputStream, String fileName, boolean dryRun) throws Exception {
        if (dryRun && !supportsDryRun()) {
            throw new UnsupportedOperationException(getDomainName() + " does not support a dry run");
        }
        processConfiguration(inputStream, fileName);
    }
}
