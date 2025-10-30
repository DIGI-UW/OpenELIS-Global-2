package org.openelisglobal.locationreporting.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.io.IOException;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LocationReportingServiceImpl implements LocationReportingService {

    @Value("${org.openelisglobal.locationreporting.fhirstore:}")
    private String locationReportingFhirStore;

    @Value("${org.openelisglobal.locationreporting.authurl:}")
    private String locationReportingAuthUrl;

    @Value("${org.openelisglobal.locationreporting.username:}")
    private String locationReportingUserName;

    @Value("${org.openelisglobal.locationreporting.password:}")
    private String locationReportingPassword;

    @Value("${org.openelisglobal.locationreporting.auth:basic}")
    private String locationReportingAuth;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private SiteInformationService siteInformationService;

    private static final String OPT_IN_SITE_INFO_NAME = "locationReportingOptIn";
    private static final String LAST_REPORT_SITE_INFO_NAME = "lastLocationReportDate";

    private static final String SITE_ID_SYSTEM = "http://openelis-global.org/site-id";
    private static final String SITE_LOCATION_ID_SYSTEM = "http://openelis-global.org/site-location-id";
    private static final String DEFAULT_SITE_NAME = "OpenELIS Installation";
    private static final String ORGANIZATION_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/organization-type";
    private static final String ORGANIZATION_TYPE_CODE = "prov";
    private static final String ORGANIZATION_TYPE_DISPLAY = "Healthcare Provider";
    private static final String VERSION_SYSTEM = "http://openelis-global.org/version";
    private static final String ADDRESS_FIELD_CITY = "facilityCity";
    private static final String ADDRESS_FIELD_DISTRICT = "facilityDistrict";
    private static final String ADDRESS_FIELD_COUNTRY = "facilityCountry";
    private static final String SITE_PHONE = "facilityPhone";
    private static final String SITE_NUMBER = "siteNumber";
    private static final String SITE_NAME = "SiteName";
    private static final String RELEASE_NAME_PREFIX = "OpenELIS Global ";

    @Override
    public boolean isOptedIn() {
        SiteInformation optInInfo = siteInformationService.getSiteInformationByName(OPT_IN_SITE_INFO_NAME);
        if (optInInfo != null && !GenericValidator.isBlankOrNull(optInInfo.getValue())) {
            return "true".equalsIgnoreCase(optInInfo.getValue().trim());
        }
        return false;
    }

    @Override
    public void setOptIn(boolean optIn) {
        SiteInformation optInInfo = siteInformationService.getSiteInformationByName(OPT_IN_SITE_INFO_NAME);
        boolean isNew = (optInInfo == null);

        if (isNew) {
            optInInfo = new SiteInformation();
            optInInfo.setName(OPT_IN_SITE_INFO_NAME);
            optInInfo.setDescription("Anonymous location reporting opt-in status");
        }
        optInInfo.setValue(String.valueOf(optIn));
        siteInformationService.persistData(optInInfo, isNew);

        if (optIn) {
            sendLocationReport();
        }
    }

    @Override
    @Async
    public void sendInitialLocationReport() {
        if (isOptedIn()) {
            SiteInformation lastReportInfo = siteInformationService
                    .getSiteInformationByName(LAST_REPORT_SITE_INFO_NAME);
            if (lastReportInfo == null || GenericValidator.isBlankOrNull(lastReportInfo.getValue())) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "sendInitialLocationReport",
                        "Sending initial location report");
                sendLocationReport();
            }
        }
    }

    @Override
    @Async
    @Scheduled(cron = "${org.openelisglobal.locationreporting.schedule:0 0 2 1 * ?}")
    public void sendLocationReport() {
        if (!isOptedIn()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendLocationReport", "Location reporting is disabled");
            return;
        }

        if (GenericValidator.isBlankOrNull(locationReportingFhirStore)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "sendLocationReport",
                    "Location reporting FHIR store not configured");
            return;
        }

        try {
            IGenericClient client = getAuthenticatedFhirClient();

            Organization organization = createOrganizationResource();
            Location location = createLocationResource(organization);

            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);

            Bundle.BundleEntryComponent orgEntry = bundle.addEntry();
            orgEntry.setResource(organization);
            orgEntry.getRequest().setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("Organization?identifier=" + organization.getIdentifierFirstRep().getValue());

            Bundle.BundleEntryComponent locEntry = bundle.addEntry();
            locEntry.setResource(location);
            locEntry.getRequest().setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("Location?identifier=" + location.getIdentifierFirstRep().getValue());

            client.transaction().withBundle(bundle).execute();

            updateLastReportDate();

            LogEvent.logInfo(this.getClass().getSimpleName(), "sendLocationReport",
                    "Successfully sent location report to " + locationReportingFhirStore);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendLocationReport",
                    "Failed to send location report: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    private IGenericClient getAuthenticatedFhirClient() throws IOException {
        if ("token".equals(locationReportingAuth)) {
            String token = fhirUtil.getAccessToken(locationReportingAuthUrl, locationReportingUserName,
                    locationReportingPassword);
            return fhirUtil.getFhirClient(locationReportingFhirStore, token);
        } else {
            return fhirUtil.getFhirClient(locationReportingFhirStore);
        }
    }

    private Organization createOrganizationResource() {
        Organization org = new Organization();

        String siteId = getSiteInformationValue(SITE_NUMBER);
        org.addIdentifier(new Identifier().setSystem(SITE_ID_SYSTEM).setValue(siteId != null ? siteId : "unknown"));

        String siteName = getSiteInformationValue(SITE_NAME);
        org.setName(siteName != null ? siteName : DEFAULT_SITE_NAME);

        org.setActive(true);

        org.addType(new CodeableConcept().addCoding(new Coding().setSystem(ORGANIZATION_TYPE_SYSTEM)
                .setCode(ORGANIZATION_TYPE_CODE).setDisplay(ORGANIZATION_TYPE_DISPLAY)));

        String description = RELEASE_NAME_PREFIX
                + ConfigurationProperties.getInstance().getPropertyValue(Property.releaseNumber);
        org.getMeta()
                .addTag(new Coding().setSystem(VERSION_SYSTEM)
                        .setCode(ConfigurationProperties.getInstance().getPropertyValue(Property.releaseNumber))
                        .setDisplay(description));

        return org;
    }

    private Location createLocationResource(Organization organization) {
        Location location = new Location();

        String siteId = getSiteInformationValue(SITE_NUMBER);
        location.addIdentifier(
                new Identifier().setSystem(SITE_LOCATION_ID_SYSTEM).setValue(siteId != null ? siteId : "unknown"));

        String siteName = getSiteInformationValue(SITE_NAME);
        location.setName(siteName != null ? siteName : DEFAULT_SITE_NAME);

        location.setStatus(LocationStatus.ACTIVE);

        location.setManagingOrganization(new Reference().setIdentifier(organization.getIdentifierFirstRep()));

        String city = getSiteInformationValue(ADDRESS_FIELD_CITY);
        String district = getSiteInformationValue(ADDRESS_FIELD_DISTRICT);
        String country = getSiteInformationValue(ADDRESS_FIELD_COUNTRY);

        if (!GenericValidator.isBlankOrNull(city) || !GenericValidator.isBlankOrNull(district)
                || !GenericValidator.isBlankOrNull(country)) {

            location.getAddress().setCity(city).setDistrict(district).setCountry(country);
        }

        String phone = getSiteInformationValue(SITE_PHONE);
        if (!GenericValidator.isBlankOrNull(phone)) {
            location.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(phone));
        }

        return location;
    }

    private String getSiteInformationValue(String name) {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName(name);
        return siteInfo != null ? siteInfo.getValue() : null;
    }

    private void updateLastReportDate() {
        SiteInformation lastReportInfo = siteInformationService.getSiteInformationByName(LAST_REPORT_SITE_INFO_NAME);
        boolean isNew = (lastReportInfo == null);

        if (isNew) {
            lastReportInfo = new SiteInformation();
            lastReportInfo.setName(LAST_REPORT_SITE_INFO_NAME);
            lastReportInfo.setDescription("Last location report date");
        }
        lastReportInfo.setValue(String.valueOf(System.currentTimeMillis()));
        siteInformationService.persistData(lastReportInfo, isNew);
    }
}
