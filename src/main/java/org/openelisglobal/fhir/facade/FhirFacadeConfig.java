package org.openelisglobal.fhir.facade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the embedded FHIR Facade layer.
 *
 * <p>
 * The FHIR Facade provides a native FHIR R4 REST API that serves FHIR resources
 * directly from the OpenELIS database without requiring an external HAPI JPA
 * Server.
 *
 * <p>
 * When enabled (fhir.facade.enabled=true), the facade runs in parallel with the
 * existing HAPI JPA server. This allows gradual migration of FHIR traffic from
 * the external server to the embedded facade.
 *
 * <p>
 * Configuration properties:
 * <ul>
 * <li>fhir.facade.enabled - Enable/disable the FHIR facade (default: true)</li>
 * <li>fhir.facade.path - URL path for the FHIR endpoint (default: /fhir)</li>
 * <li>fhir.facade.server.name - Server name in CapabilityStatement (default:
 * OpenELIS FHIR Facade)</li>
 * <li>fhir.facade.server.version - Server version (default: 1.0.0)</li>
 * </ul>
 *
 * @author OpenELIS Global Team
 * @since 3.0
 */
@Configuration
public class FhirFacadeConfig {

    @Value("${fhir.facade.enabled:true}")
    private boolean facadeEnabled;

    @Value("${fhir.facade.path:/fhir}")
    private String fhirPath;

    @Value("${fhir.facade.server.name:OpenELIS FHIR Facade}")
    private String serverName;

    @Value("${fhir.facade.server.version:1.0.0}")
    private String serverVersion;

    @Value("${fhir.facade.default.page.size:20}")
    private int defaultPageSize;

    @Value("${fhir.facade.max.page.size:100}")
    private int maxPageSize;

    /**
     * Check if the FHIR facade is enabled.
     *
     * @return true if facade is enabled, false otherwise
     */
    public boolean isFacadeEnabled() {
        return facadeEnabled;
    }

    /**
     * Get the FHIR endpoint path.
     *
     * @return the configured FHIR path (e.g., "/fhir")
     */
    public String getFhirPath() {
        return fhirPath;
    }

    /**
     * Get the server name for CapabilityStatement.
     *
     * @return the configured server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the server version for CapabilityStatement.
     *
     * @return the configured server version
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Get the default page size for search results.
     *
     * @return the default page size
     */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /**
     * Get the maximum allowed page size for search results.
     *
     * @return the maximum page size
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }
}
