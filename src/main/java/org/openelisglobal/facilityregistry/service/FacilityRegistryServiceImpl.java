package org.openelisglobal.facilityregistry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationService;
import org.openelisglobal.facilityregistry.form.FacilityLocationStatus;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.ThisLaboratoryService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FacilityRegistryServiceImpl implements FacilityRegistryService {

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final String HOSPITAL_LABORATORY = "HLAB";
    private static final int REGISTRY_TIMEOUT_MS = 20000;
    private static final int LOOKUP_TIMEOUT_MS = 5000;

    @Autowired
    private FhirFacilityOrganizationService facilityOrganizationService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ThisLaboratoryService thisLaboratoryService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private DaemonContextExecutor daemonContextExecutor;

    /**
     * The IP lookup is a public HTTPS service: it needs the JVM's certificate
     * authorities, which {@link #httpClient} (OpenELIS keystore and truststore
     * only) does not trust.
     */
    private final CloseableHttpClient lookupClient = HttpClients.createSystem();

    @Value("${org.openelisglobal.facility.facilityregistry:}")
    private String registryUrl;

    @Value("${org.openelisglobal.facility.auth:}")
    private String registryAuth;

    @Value("${org.openelisglobal.facility.authurl:}")
    private String registryAuthUrl;

    @Value("${org.openelisglobal.facility.username:}")
    private String registryUsername;

    @Value("${org.openelisglobal.facility.password:}")
    private String registryPassword;

    @Value("${org.openelisglobal.facility.latitude:}")
    private String configuredLatitude;

    @Value("${org.openelisglobal.facility.longitude:}")
    private String configuredLongitude;

    @Value("${org.openelisglobal.facility.autolocation:}")
    private String autolocation;

    @Value("${org.openelisglobal.facility.autolocation.url:https://get.geojs.io/v1/ip/geo.json}")
    private String autolocationUrl;

    @Value("${org.openelisglobal.facility.type:HLAB}")
    private String facilityType;

    private volatile boolean ipLookupDone;
    private volatile boolean ignoredManualCoordinatesLogged;
    private volatile String registeredFingerprint;
    private volatile Boolean lastRegistrationSuccess;
    private volatile Instant lastRegistrationAt;
    private volatile String lastRegistrationMessage;

    @Override
    public FacilityLocationStatus getStatus() {
        FacilityLocationStatus status = new FacilityLocationStatus();
        boolean auto = isAutomatic();
        status.setMode(auto ? MODE_AUTO : MODE_MANUAL);
        status.setAutolocation(auto);
        status.setManualCoordinatesIgnored(auto && manualCoordinates().isPresent());
        effectiveCoordinates(facilityRow()).ifPresent(coordinates -> {
            status.setLatitude(coordinates.latitude());
            status.setLongitude(coordinates.longitude());
            status.setSource(coordinates.source());
        });
        status.setRegistryConfigured(isRegistryConfigured());
        status.setRegistryUrl(isRegistryConfigured() ? registryUrl.trim() : null);
        status.setLastRegistrationSuccess(lastRegistrationSuccess);
        status.setLastRegistrationAt(lastRegistrationAt == null ? null : lastRegistrationAt.toString());
        status.setLastRegistrationMessage(lastRegistrationMessage);
        return status;
    }

    @Override
    public FacilityLocationStatus recordBrowserLocation(BigDecimal latitude, BigDecimal longitude, String sysUserId) {
        if (!isAutomatic()) {
            throw new FacilityLocationModeException("The facility location is in manual mode: its coordinates come"
                    + " from org.openelisglobal.facility.latitude and org.openelisglobal.facility.longitude."
                    + " Set org.openelisglobal.facility.autolocation=true to use this device's position.");
        }
        Coordinates coordinates = validated(latitude, longitude, SOURCE_BROWSER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Latitude must be between -90 and 90 and longitude between -180 and 180"));
        org.openelisglobal.organization.valueholder.Organization row = facilityRow();
        if (row == null) {
            throw new IllegalStateException("The facility is not initialized yet; try again in a minute");
        }
        store(row, coordinates, sysUserId);
        LogEvent.logInfo(getClass().getSimpleName(), "recordBrowserLocation",
                "Facility position set from an administrator's browser: " + coordinates);
        if (isRegistryConfigured()) {
            register();
        }
        return getStatus();
    }

    @Override
    @Scheduled(initialDelayString = "${org.openelisglobal.facility.facilityregistry.startdelay:20000}", fixedDelayString = "${org.openelisglobal.facility.facilityregistry.retry:300000}")
    public void synchronize() {
        try {
            facilityOrganizationService.refreshName();
            daemonContextExecutor.executeAsDaemon(this::alignCoordinatesWithMode);
            if (isRegistryConfigured() && (!Boolean.TRUE.equals(lastRegistrationSuccess)
                    || !Objects.equals(registeredFingerprint, fingerprint()))) {
                register();
            }
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "synchronize",
                    "Facility registry synchronization failed: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    @Override
    public boolean register() {
        if (!isRegistryConfigured()) {
            return false;
        }
        String base = StringUtils.removeEnd(registryUrl.trim(), "/");
        try {
            Organization organization = buildRegistryOrganization();
            Coordinates coordinates = effectiveCoordinates(facilityRow()).orElse(null);
            Location location = buildRegistryLocation(coordinates == null ? null : coordinates.latitude(),
                    coordinates == null ? null : coordinates.longitude());
            String authorization = authorizationHeader();
            put(base + "/" + ResourceType.Organization + "/" + organization.getIdPart(), organization, authorization);
            put(base + "/" + ResourceType.Location + "/" + location.getIdPart(), location, authorization);
            registeredFingerprint = fingerprint();
            recordOutcome(true, organization.getName() + " in " + base
                    + (coordinates == null ? " without a position" : " at " + coordinates));
            return true;
        } catch (IOException | RuntimeException e) {
            recordOutcome(false, base + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public Organization buildRegistryOrganization() {
        Organization facility = facilityOrganizationService.getFacilityOrganization()
                .orElseThrow(() -> new IllegalStateException("The facility Organization is not initialized"));
        Organization organization = new Organization();
        organization.setId(facility.getIdElement().getIdPart());
        organization.getMeta().addProfile(FACILITY_ORGANIZATION_PROFILE);
        organization.setActive(true);
        organization.setName(facility.getName());
        for (Identifier identifier : facility.getIdentifier()) {
            organization.addIdentifier(identifier.copy());
        }
        organization.setType(facilityTypes());
        if (facility.hasAddress()) {
            organization.addAddress(facility.getAddressFirstRep().copy());
        }
        return organization;
    }

    @Override
    public Location buildRegistryLocation(BigDecimal latitude, BigDecimal longitude) {
        Organization facility = facilityOrganizationService.getFacilityOrganization()
                .orElseThrow(() -> new IllegalStateException("The facility Organization is not initialized"));
        String facilityUuid = facility.getIdElement().getIdPart();
        Location location = new Location();
        location.setId(locationId(facilityUuid));
        location.getMeta().addProfile(FACILITY_LOCATION_PROFILE);
        location.setStatus(LocationStatus.ACTIVE);
        location.setName(facility.getName());
        for (Identifier identifier : facility.getIdentifier()) {
            location.addIdentifier(identifier.copy());
        }
        location.setType(facilityTypes());
        if (facility.hasAddress()) {
            location.setAddress(facility.getAddressFirstRep().copy());
        }
        location.setPhysicalType(new CodeableConcept()
                .addCoding(new Coding().setSystem(PHYSICAL_TYPE_SYSTEM).setCode(PHYSICAL_TYPE_BUILDING)));
        if (latitude != null && longitude != null) {
            location.setPosition(new LocationPositionComponent().setLatitude(latitude).setLongitude(longitude));
        }
        location.setManagingOrganization(new Reference(ResourceType.Organization + "/" + facilityUuid));
        return location;
    }

    static String locationId(String facilityUuid) {
        return UUID.nameUUIDFromBytes((facilityUuid + "/location").getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<CodeableConcept> facilityTypes() {
        String code = StringUtils.isBlank(facilityType) ? HOSPITAL_LABORATORY : facilityType.trim();
        String display = HOSPITAL_LABORATORY.equals(code) ? "hospital laboratory" : null;
        CodeableConcept kind = new CodeableConcept()
                .addCoding(new Coding().setSystem(ROLE_CODE_SYSTEM).setCode(code).setDisplay(display));
        kind.setText(display == null ? code : display);
        CodeableConcept facilityMarker = new CodeableConcept()
                .addCoding(new Coding().setSystem(MCSD_TYPE_SYSTEM).setCode(MCSD_FACILITY_CODE));
        return List.of(kind, facilityMarker);
    }

    private void alignCoordinatesWithMode() {
        org.openelisglobal.organization.valueholder.Organization row = facilityRow();
        if (row == null) {
            return;
        }
        if (isAutomatic()) {
            if (manualCoordinates().isPresent() && !ignoredManualCoordinatesLogged) {
                ignoredManualCoordinatesLogged = true;
                LogEvent.logWarn(getClass().getSimpleName(), "alignCoordinatesWithMode",
                        "org.openelisglobal.facility.autolocation=true: the latitude and longitude properties are"
                                + " ignored; the position comes from detection");
            }
            if (!SOURCE_BROWSER.equals(row.getGpsSource()) && !ipLookupDone) {
                lookUpByIp().ifPresent(coordinates -> {
                    store(row, coordinates, null);
                    ipLookupDone = true;
                });
            }
        } else {
            manualCoordinates().ifPresent(coordinates -> {
                if (!coordinates.sameAs(row)) {
                    store(row, coordinates, null);
                }
            });
        }
    }

    private Optional<Coordinates> lookUpByIp() {
        if (StringUtils.isBlank(autolocationUrl)) {
            return Optional.empty();
        }
        HttpGet get = new HttpGet(autolocationUrl.trim());
        get.setHeader("Accept", "application/json");
        get.setConfig(timeouts(LOOKUP_TIMEOUT_MS));
        try (CloseableHttpResponse response = lookupClient.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status != 200) {
                LogEvent.logWarn(getClass().getSimpleName(), "lookUpByIp",
                        "Location lookup at " + autolocationUrl + " answered HTTP " + status);
                return Optional.empty();
            }
            JsonNode json = new ObjectMapper().readTree(body);
            Optional<Coordinates> coordinates = validated(decimal(json, "latitude", "lat"),
                    decimal(json, "longitude", "lon"), SOURCE_IP);
            coordinates.ifPresentOrElse(
                    c -> LogEvent.logInfo(getClass().getSimpleName(), "lookUpByIp",
                            "Facility position from the IP lookup at " + autolocationUrl + ": " + c),
                    () -> LogEvent.logWarn(getClass().getSimpleName(), "lookUpByIp",
                            "Location lookup at " + autolocationUrl + " gave no usable coordinates"));
            return coordinates;
        } catch (IOException | RuntimeException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "lookUpByIp",
                    "Location lookup at " + autolocationUrl + " failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static BigDecimal decimal(JsonNode json, String name, String alternative) {
        JsonNode value = json.has(name) ? json.get(name) : json.get(alternative);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void store(org.openelisglobal.organization.valueholder.Organization row, Coordinates coordinates,
            String sysUserId) {
        row.setGpsLatitude(coordinates.latitude());
        row.setGpsLongitude(coordinates.longitude());
        row.setGpsSource(coordinates.source());
        if (sysUserId != null) {
            row.setSysUserId(sysUserId);
        }
        organizationService.update(row);
    }

    private Optional<Coordinates> effectiveCoordinates(org.openelisglobal.organization.valueholder.Organization row) {
        if (!isAutomatic()) {
            return manualCoordinates();
        }
        if (row == null || row.getGpsLatitude() == null || row.getGpsLongitude() == null) {
            return Optional.empty();
        }
        String source = row.getGpsSource();
        if (!SOURCE_IP.equals(source) && !SOURCE_BROWSER.equals(source)) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(row.getGpsLatitude(), row.getGpsLongitude(), source));
    }

    private Optional<Coordinates> manualCoordinates() {
        if (StringUtils.isBlank(configuredLatitude) || StringUtils.isBlank(configuredLongitude)) {
            return Optional.empty();
        }
        try {
            return validated(new BigDecimal(configuredLatitude.trim()), new BigDecimal(configuredLongitude.trim()),
                    SOURCE_PROPERTIES);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Coordinates> validated(BigDecimal latitude, BigDecimal longitude, String source) {
        if (latitude == null || longitude == null || latitude.abs().compareTo(MAX_LATITUDE) > 0
                || longitude.abs().compareTo(MAX_LONGITUDE) > 0) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(latitude, longitude, source));
    }

    private org.openelisglobal.organization.valueholder.Organization facilityRow() {
        return thisLaboratoryService.find();
    }

    private String fingerprint() {
        Optional<Organization> facility = facilityOrganizationService.getFacilityOrganization();
        String name = facility.map(Organization::getName).orElse("");
        String id = facility.map(o -> o.getIdElement().getIdPart()).orElse("");
        String position = effectiveCoordinates(facilityRow()).map(Coordinates::toString).orElse("");
        return id + "|" + name + "|" + position + "|" + facilityType;
    }

    private String authorizationHeader() throws IOException {
        String auth = StringUtils.trimToEmpty(registryAuth);
        if ("token".equalsIgnoreCase(auth)) {
            return "Bearer " + fhirUtil.getAccessToken(registryAuthUrl, registryUsername, registryPassword);
        }
        if ("basic".equalsIgnoreCase(auth)) {
            return "Basic " + Base64.getEncoder()
                    .encodeToString((registryUsername + ":" + registryPassword).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    private void put(String url, Resource resource, String authorization) throws IOException {
        HttpPut put = new HttpPut(url);
        put.setConfig(timeouts(REGISTRY_TIMEOUT_MS));
        put.setHeader("Content-Type", "application/json");
        put.setHeader("Accept", "application/json");
        if (authorization != null) {
            put.setHeader("Authorization", authorization);
        }
        put.setEntity(
                new StringEntity(fhirUtil.getFhirParser().encodeResourceToString(resource), StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = httpClient.execute(put)) {
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                String body = response.getEntity() == null ? ""
                        : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new IOException("PUT " + url + " answered HTTP " + status
                        + (body.isEmpty() ? "" : ": " + StringUtils.abbreviate(body, 300)));
            }
        }
    }

    private static RequestConfig timeouts(int milliseconds) {
        return RequestConfig.custom().setConnectTimeout(milliseconds).setConnectionRequestTimeout(milliseconds)
                .setSocketTimeout(milliseconds).build();
    }

    private void recordOutcome(boolean success, String message) {
        lastRegistrationSuccess = success;
        lastRegistrationAt = Instant.now();
        lastRegistrationMessage = message;
        if (success) {
            LogEvent.logInfo(getClass().getSimpleName(), "register", "Registered " + message);
        } else {
            LogEvent.logError(getClass().getSimpleName(), "register", "Registration failed in " + message);
        }
    }

    private boolean isAutomatic() {
        return "true".equalsIgnoreCase(StringUtils.trimToEmpty(autolocation));
    }

    private boolean isRegistryConfigured() {
        return StringUtils.isNotBlank(registryUrl);
    }

    record Coordinates(BigDecimal latitude, BigDecimal longitude, String source) {

        boolean sameAs(org.openelisglobal.organization.valueholder.Organization row) {
            return source.equals(row.getGpsSource()) && row.getGpsLatitude() != null && row.getGpsLongitude() != null
                    && latitude.compareTo(row.getGpsLatitude()) == 0 && longitude.compareTo(row.getGpsLongitude()) == 0;
        }

        @Override
        public String toString() {
            return latitude.stripTrailingZeros().toPlainString() + "," + longitude.stripTrailingZeros().toPlainString()
                    + " (" + source + ")";
        }
    }
}
