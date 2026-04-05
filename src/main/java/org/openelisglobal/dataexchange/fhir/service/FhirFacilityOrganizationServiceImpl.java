package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
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
import org.openelisglobal.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Implementation of FhirFacilityOrganizationService that creates and manages
 * the FHIR Organization resource representing this OpenELIS facility.
 *
 * <p>
 * The local OpenELIS DB is the source of truth for whether a facility
 * Organization already exists. The DB record is identified by the constant
 * short name {@code FACILITY_ORG}, which is stable across facility ID changes.
 * The FHIR UUID stored on that DB record is used for all FHIR PUT operations,
 * ensuring no duplicate FHIR resources are ever created.
 */
@Service
public class FhirFacilityOrganizationServiceImpl implements FhirFacilityOrganizationService {

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private OrganizationService organizationService;

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

    @Value("${org.openelisglobal.facility.id:}")
    private String configuredFacilityId;

    /** Constant short name used to identify the facility org row in the DB. */
    private static final String SHORT_NAME = "FACILITY_ORG";

    private Organization facilityOrganization;
    private String facilityUuid;
    private String facilityId;
    private Reference facilityReference;

    @Override
    public String getFacilityIdentifierSystem() {
        return fhirConfig.getOeFhirSystem() + "/facility_id";
    }

    @Override
    public String getFacilityId() {
        if (facilityOrganization == null) {
            initialize();
        }
        return facilityId;
    }

    @Override
    public String getFacilityUuid() {
        if (facilityOrganization == null) {
            initialize();
        }
        return facilityUuid;
    }

    @Override
    public Reference getFacilityOrganizationReference() {
        if (facilityOrganization == null) {
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
    public Optional<Organization> getFacilityOrganization() {
        if (facilityOrganization == null) {
            initialize();
        }
        return Optional.ofNullable(facilityOrganization);
    }

    /**
     * Resolves the facility display name from the BANNER_TEXT configuration
     * property, falling back to "OpenELIS Global" if not set.
     */
    private String resolveFacilityName() {
        String bannerTextId = ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT);
        if (StringUtils.isNotBlank(bannerTextId)) {
            String localizedName = localizationService.getLocalizedValueById(bannerTextId);
            if (StringUtils.isNotBlank(localizedName)) {
                return localizedName;
            }
        }
        return "OpenELIS Global";
    }

    /**
     * Builds the facility Address from configured properties.
     *
     * @return the Address, or null if no address fields are populated
     */
    private Address buildFacilityAddress() {
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

        if (!address.hasCity() && !address.hasDistrict() && !address.hasState() && !address.hasPostalCode()
                && !address.hasCountry()) {
            return null;
        }

        return address;
    }

    /**
     * Builds a FHIR Organization resource for the facility using the given UUID.
     *
     * @param uuid         the FHIR resource ID to assign
     * @param identifierId the value for the facility identifier
     * @param name         the facility display name
     * @return the constructed Organization
     */
    private Organization buildFhirOrganization(String uuid, String identifierId, String name) {
        Organization organization = new Organization();
        organization.setId(uuid);
        organization.setName(name);
        organization.setActive(true);

        Identifier identifier = new Identifier();
        identifier.setSystem(getFacilityIdentifierSystem());
        identifier.setValue(identifierId);
        identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        organization.addIdentifier(identifier);

        Address address = buildFacilityAddress();
        if (address != null) {
            organization.addAddress(address);
        }

        return organization;
    }

    /**
     * Public method that generates a fresh facility Organization resource with a
     * new random UUID. This does NOT consult the DB; callers who need the
     * persisted/canonical resource should use {@link #getFacilityOrganization()}.
     */
    @Override
    public Organization generateFacilityOrganization() {
        String facilityName = resolveFacilityName();
        facilityId = StringUtils.isNotBlank(configuredFacilityId) ? configuredFacilityId : facilityName;
        facilityUuid = UUID.randomUUID().toString();
        facilityOrganization = buildFhirOrganization(facilityUuid, facilityId, facilityName);
        return facilityOrganization;
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

    /**
     * Applies the configured facility address fields to an OpenELIS Organization.
     * Only non-blank values are set so existing data is not cleared by empty
     * config.
     */
    private void applyAddressToOeOrg(org.openelisglobal.organization.valueholder.Organization oeOrg) {
        if (StringUtils.isNotBlank(facilityCity)) {
            oeOrg.setCity(facilityCity);
        }
        if (StringUtils.isNotBlank(facilityState)) {
            oeOrg.setState(facilityState);
        }
        if (StringUtils.isNotBlank(facilityPostalCode)) {
            oeOrg.setZipCode(facilityPostalCode);
        }
        if (StringUtils.isNotBlank(facilityDistrict)) {
            oeOrg.setStreetAddress(facilityDistrict);
        }
    }

    /**
     * Creates or updates the facility row in the OpenELIS DB using the constant
     * short name {@code FACILITY_ORG} as the stable identifier.
     */
    private void syncToOpenElisDb() {
        if (facilityOrganization == null || facilityUuid == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncToOpenElisDb",
                    "Facility Organization not initialized, skipping OpenELIS DB sync");
            return;
        }

        try {
            org.openelisglobal.organization.valueholder.Organization oeOrg = organizationService
                    .getOrganizationByShortName(SHORT_NAME, false);

            if (oeOrg == null) {
                oeOrg = new org.openelisglobal.organization.valueholder.Organization();
                oeOrg.setShortName(SHORT_NAME);
                oeOrg.setMlsLabFlag("N");
                oeOrg.setMlsSentinelLabFlag("N");
            }

            oeOrg.setOrganizationName(facilityOrganization.getName());
            oeOrg.setIsActive("Y");
            oeOrg.setFhirUuid(UUID.fromString(facilityUuid));
            applyAddressToOeOrg(oeOrg);

            organizationService.save(oeOrg);
            LogEvent.logInfo(this.getClass().getSimpleName(), "syncToOpenElisDb",
                    "Synced OpenELIS Organization for facility: " + oeOrg.getId());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToOpenElisDb",
                    "Failed to sync facility Organization to OpenELIS database: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    @Override
    @EventListener(ContextRefreshedEvent.class)
    @Order(100)
    public void initialize() {
        LogEvent.logInfo(this.getClass().getSimpleName(), "initialize", "Initializing facility Organization resource");

        try {
            // Resolve current facility name and ID from configuration
            String facilityName = resolveFacilityName();
            facilityId = StringUtils.isNotBlank(configuredFacilityId) ? configuredFacilityId : facilityName;

            // Look up the facility org in the local DB by the constant short name.
            // This is the source of truth and is stable even when the facility ID changes.
            org.openelisglobal.organization.valueholder.Organization dbOrg = organizationService
                    .getOrganizationByShortName(SHORT_NAME, false);

            if (dbOrg != null && dbOrg.getFhirUuid() != null) {
                // Reuse the FHIR UUID from the DB record so we always PUT to the same resource
                facilityUuid = dbOrg.getFhirUuid().toString();
                LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                        "Found existing facility org in DB, reusing FHIR UUID: " + facilityUuid);
            } else {
                // First time: generate a new UUID and it will be persisted to DB below
                facilityUuid = UUID.randomUUID().toString();
                LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                        "No existing facility org in DB, creating new one with UUID: " + facilityUuid);
            }

            // Build the FHIR Organization from current configuration, using the resolved
            // UUID
            facilityOrganization = buildFhirOrganization(facilityUuid, facilityId, facilityName);

            // Rebuild the reference
            facilityReference = new Reference();
            facilityReference.setReference(ResourceType.Organization + "/" + facilityUuid);
            facilityReference.setDisplay(facilityName);

            // PUT to FHIR servers — idempotent upsert, no duplicates since UUID is stable
            syncToLocalFhirServer();
            syncToRemoteFhirServers();

            // Persist / update the facility row in the OpenELIS DB
            syncToOpenElisDb();

            LogEvent.logInfo(this.getClass().getSimpleName(), "initialize",
                    "Facility Organization initialized successfully with UUID: " + facilityUuid + ", Name: "
                            + facilityName);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "initialize",
                    "Failed to initialize facility Organization: " + e.getMessage());
            LogEvent.logError(e);
        }
    }
}
