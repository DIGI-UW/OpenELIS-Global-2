package org.openelisglobal.dataexchange.fhir.service;

import java.util.Optional;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;

/**
 * Service for managing the FHIR Organization resource representing this
 * OpenELIS facility. The facility Organization is created at startup and synced
 * to local and remote FHIR servers.
 */
public interface FhirFacilityOrganizationService {

    /**
     * Generates the FHIR Organization resource for this facility. Uses facility
     * properties for address and BANNER_TEXT for name.
     *
     * @return the generated Organization resource
     */
    Organization generateFacilityOrganization();

    /**
     * Gets the facility Organization UUID. Creates and syncs if not already done.
     *
     * @return the facility UUID
     */
    String getFacilityUuid();

    /**
     * Gets a Reference to the facility Organization for use as an identifier
     * assigner.
     *
     * @return Reference to the facility Organization
     */
    Reference getFacilityOrganizationReference();

    /**
     * Syncs the facility Organization to the local FHIR server if available.
     */
    void syncToLocalFhirServer();

    /**
     * Syncs the facility Organization to remote FHIR servers if configured.
     */
    void syncToRemoteFhirServers();

    /**
     * Gets the cached facility Organization.
     *
     * @return Optional containing the facility Organization if initialized
     */
    Optional<Organization> getFacilityOrganization();

    /**
     * Initializes the facility Organization and syncs to FHIR servers. Called at
     * application startup.
     */
    void initialize();
}
