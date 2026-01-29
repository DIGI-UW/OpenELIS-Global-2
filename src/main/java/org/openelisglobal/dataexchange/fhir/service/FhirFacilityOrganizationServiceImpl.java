package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Implementation of FhirFacilityOrganizationService that creates and manages
 * the FHIR Organization resource representing this OpenELIS facility.
 */
@Service
public class FhirFacilityOrganizationServiceImpl implements FhirFacilityOrganizationService {

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private LocalizationService localizationService;

    @Value("${org.openelisglobal.facility.country:}")
    private String facilityCountry;

    @Value("${org.openelisglobal.facility.state:}")
    private String facilityState;

    @Value("${org.openelisglobal.facility.district:}")
    private String facilityDistrict;

    @Value("${org.openelisglobal.facility.city:}")
    private String facilityCity;

    @Value("${org.openelisglobal.facility.postalcode:}")
    private String facilityPostalCode;

    private Organization facilityOrganization;
    private String facilityUuid;
    private Reference facilityReference;
    private boolean initialized = false;

    /**
     * Gets the identifier system used for facility Organization resources.
     */
    private String getFacilityIdentifierSystem() {
        return fhirConfig.getOeFhirSystem() + "/facility_uuid";
    }

    /**
     * Searches for an existing facility Organization in the local FHIR server by
     * identifier system.
     *
     * @return Optional containing the existing Organization if found
     */
    private Optional<Organization> findExistingFacilityOrganization() {
        String localFhirPath = fhirConfig.getLocalFhirStorePath();
        if (StringUtils.isBlank(localFhirPath)) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findExistingFacilityOrganization",
                    "Local FHIR server not configured, cannot search for existing facility Organization");
            return Optional.empty();
        }

        try {
            IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirPath);
            Bundle bundle = localFhirClient.search().forResource(Organization.class).returnBundle(Bundle.class)
                    .where(Organization.IDENTIFIER.hasSystemWithAnyCode(getFacilityIdentifierSystem())).execute();

            if (bundle.hasEntry() && !bundle.getEntry().isEmpty()) {
                Organization existingOrg = (Organization) bundle.getEntryFirstRep().getResource();
                LogEvent.logInfo(this.getClass().getSimpleName(), "findExistingFacilityOrganization",
                        "Found existing facility Organization with ID: " + existingOrg.getIdElement().getIdPart());
                return Optional.of(existingOrg);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findExistingFacilityOrganization",
                    "Error searching for existing facility Organization: " + e.getMessage());
            LogEvent.logError(e);
        }

        return Optional.empty();
    }

    @Override
    public Organization generateFacilityOrganization() {
        LogEvent.logTrace(this.getClass().getSimpleName(), "generateFacilityOrganization",
                "Generating facility Organization resource");

        Organization organization = new Organization();

        // Use existing UUID if already set, otherwise generate new one
        if (facilityUuid == null) {
            facilityUuid = UUID.randomUUID().toString();
        }
        organization.setId(facilityUuid);

        // Set the name from BANNER_TEXT configuration
        String bannerTextId = ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT);
        String facilityName = "OpenELIS Global";
        if (StringUtils.isNotBlank(bannerTextId)) {
            String localizedName = localizationService.getLocalizedValueById(bannerTextId);
            if (StringUtils.isNotBlank(localizedName)) {
                facilityName = localizedName;
            }
        }
        organization.setName(facilityName);

        // Add identifier with system and value
        Identifier identifier = new Identifier();
        identifier.setSystem(getFacilityIdentifierSystem());
        identifier.setValue(facilityUuid);
        identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        organization.addIdentifier(identifier);

        // Build the address from facility properties
        Address address = new Address();
        address.setUse(Address.AddressUse.WORK);

        if (StringUtils.isNotBlank(facilityCity)) {
            address.setCity(facilityCity);
        }
        if (StringUtils.isNotBlank(facilityDistrict)) {
            address.setDistrict(facilityDistrict);
        }
        if (StringUtils.isNotBlank(facilityState)) {
            address.setState(facilityState);
        }
        if (StringUtils.isNotBlank(facilityPostalCode)) {
            address.setPostalCode(facilityPostalCode);
        }
        if (StringUtils.isNotBlank(facilityCountry)) {
            address.setCountry(facilityCountry);
        }

        // Only add address if at least one field is populated
        if (address.hasCity() || address.hasDistrict() || address.hasState() || address.hasPostalCode()
                || address.hasCountry()) {
            organization.addAddress(address);
        }

        // Mark as active
        organization.setActive(true);

        return organization;
    }

    /**
     * Updates an existing Organization with current facility configuration.
     * Preserves the original ID and identifier value.
     *
     * @param existingOrg the existing Organization to update
     * @return the updated Organization
     */
    private Organization updateExistingOrganization(Organization existingOrg) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "updateExistingOrganization",
                "Updating existing facility Organization");

        // Preserve the existing ID
        String existingId = existingOrg.getIdElement().getIdPart();

        // Get existing facility UUID from identifier
        String existingFacilityUuid = null;
        for (Identifier id : existingOrg.getIdentifier()) {
            if (getFacilityIdentifierSystem().equals(id.getSystem())) {
                existingFacilityUuid = id.getValue();
                break;
            }
        }

        // Use existing UUID
        facilityUuid = existingFacilityUuid != null ? existingFacilityUuid : existingId;

        // Update the name from current BANNER_TEXT configuration
        String bannerTextId = ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT);
        String facilityName = "OpenELIS Global";
        if (StringUtils.isNotBlank(bannerTextId)) {
            String localizedName = localizationService.getLocalizedValueById(bannerTextId);
            if (StringUtils.isNotBlank(localizedName)) {
                facilityName = localizedName;
            }
        }
        existingOrg.setName(facilityName);

        // Clear and rebuild address from current facility properties
        existingOrg.getAddress().clear();
        Address address = new Address();
        address.setUse(Address.AddressUse.WORK);

        if (StringUtils.isNotBlank(facilityCity)) {
            address.setCity(facilityCity);
        }
        if (StringUtils.isNotBlank(facilityDistrict)) {
            address.setDistrict(facilityDistrict);
        }
        if (StringUtils.isNotBlank(facilityState)) {
            address.setState(facilityState);
        }
        if (StringUtils.isNotBlank(facilityPostalCode)) {
            address.setPostalCode(facilityPostalCode);
        }
        if (StringUtils.isNotBlank(facilityCountry)) {
            address.setCountry(facilityCountry);
        }

        // Only add address if at least one field is populated
        if (address.hasCity() || address.hasDistrict() || address.hasState() || address.hasPostalCode()
                || address.hasCountry()) {
            existingOrg.addAddress(address);
        }

        existingOrg.setActive(true);

        return existingOrg;
    }

    @Override
    public String getFacilityUuid() {
        if (!initialized) {
            initialize();
        }
        return facilityUuid;
    }

    @Override
    public Reference getFacilityOrganizationReference() {
        if (!initialized) {
            initialize();
        }
        if (facilityReference == null && facilityUuid != null) {
            facilityReference = new Reference();
            facilityReference.setReference(ResourceType.Organization + "/" + facilityUuid);
            if (facilityOrganization != null) {
                facilityReference.setDisplay(facilityOrganization.getName());
            }
        }
        return facilityReference;
    }

    @Override
    public void syncToLocalFhirServer() {
        if (facilityOrganization == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncToLocalFhirServer",
                    "Facility Organization not initialized, cannot sync to local FHIR server");
            return;
        }

        String localFhirPath = fhirConfig.getLocalFhirStorePath();
        if (StringUtils.isBlank(localFhirPath)) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "syncToLocalFhirServer",
                    "Local FHIR server not configured, skipping sync");
            return;
        }

        try {
            IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirPath);
            localFhirClient.update().resource(facilityOrganization).execute();
            LogEvent.logInfo(this.getClass().getSimpleName(), "syncToLocalFhirServer",
                    "Successfully synced facility Organization to local FHIR server: " + facilityUuid);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToLocalFhirServer",
                    "Failed to sync facility Organization to local FHIR server: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    @Override
    public void syncToRemoteFhirServers() {
        if (facilityOrganization == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncToRemoteFhirServers",
                    "Facility Organization not initialized, cannot sync to remote FHIR servers");
            return;
        }

        String[] remotePaths = fhirConfig.getRemoteStorePaths();
        if (remotePaths == null || remotePaths.length == 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "syncToRemoteFhirServers",
                    "No remote FHIR servers configured, skipping sync");
            return;
        }

        for (String remotePath : remotePaths) {
            if (StringUtils.isBlank(remotePath)) {
                continue;
            }

            try {
                IGenericClient remoteFhirClient = fhirUtil.getFhirClient(remotePath);

                // Add authentication if configured and not the local server
                String localPath = fhirConfig.getLocalFhirStorePath();
                if (StringUtils.isNotBlank(fhirConfig.getUsername()) && !remotePath.equals(localPath)) {
                    IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                            fhirConfig.getPassword());
                    remoteFhirClient.registerInterceptor(authInterceptor);
                }

                remoteFhirClient.update().resource(facilityOrganization).execute();
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncToRemoteFhirServers",
                        "Successfully synced facility Organization to remote FHIR server: " + remotePath);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "syncToRemoteFhirServers",
                        "Failed to sync facility Organization to remote FHIR server " + remotePath + ": "
                                + e.getMessage());
                LogEvent.logError(e);
            }
        }
    }

    @Override
    public Optional<Organization> getFacilityOrganization() {
        if (!initialized) {
            initialize();
        }
        return Optional.ofNullable(facilityOrganization);
    }

    @Override
    @EventListener(ContextRefreshedEvent.class)
    @Order(100) // Run after other initialization services
    public void initialize() {
        if (initialized) {
            return;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "initialize", "Initializing facility Organization resource");

        try {
            // First, check if a facility Organization already exists in the local FHIR
            // server
            Optional<Organization> existingOrg = findExistingFacilityOrganization();

            if (existingOrg.isPresent()) {
                // Update existing Organization with current configuration
                LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                        "Found existing facility Organization, updating with current configuration");
                facilityOrganization = updateExistingOrganization(existingOrg.get());
            } else {
                // Generate new Organization resource
                LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                        "No existing facility Organization found, creating new one");
                facilityOrganization = generateFacilityOrganization();
            }

            // Create the reference
            facilityReference = new Reference();
            facilityReference.setReference(ResourceType.Organization + "/" + facilityUuid);
            facilityReference.setDisplay(facilityOrganization.getName());

            // Sync to FHIR servers (update/create)
            syncToLocalFhirServer();
            syncToRemoteFhirServers();

            initialized = true;
            LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                    "Facility Organization initialized successfully with UUID: " + facilityUuid + ", Name: "
                            + facilityOrganization.getName());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "initialize",
                    "Failed to initialize facility Organization: " + e.getMessage());
            LogEvent.logError(e);
        }
    }
}
