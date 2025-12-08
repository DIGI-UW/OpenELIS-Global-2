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
     * Processes a configuration file from the given input stream.
     *
     * @param inputStream the input stream containing the configuration data
     * @param fileName    the name of the file being processed
     * @throws Exception if processing fails
     */
    void processConfiguration(InputStream inputStream, String fileName) throws Exception;
}
