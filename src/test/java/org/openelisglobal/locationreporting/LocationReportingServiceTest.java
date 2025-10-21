package org.openelisglobal.locationreporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.locationreporting.service.LocationReportingService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationReportingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LocationReportingService locationReportingService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/location-reporting.xml");
    }

    @Test
    public void isOptedIn_whenOptInIsTrue_shouldReturnTrue() {
        boolean result = locationReportingService.isOptedIn();
        assertTrue("Should be opted in when value is 'true'", result);
    }

    @Test
    public void isOptedIn_whenOptInIsFalse_shouldReturnFalse() {
        SiteInformation optInInfo = siteInformationService.getSiteInformationByName("location_reporting_opt_in");
        optInInfo.setValue("false");
        siteInformationService.persistData(optInInfo, false);

        boolean result = locationReportingService.isOptedIn();
        assertFalse("Should not be opted in when value is 'false'", result);
    }

    @Test
    public void setOptIn_whenNewRecord_shouldCreateNewSiteInformation() {
        siteInformationService.delete(siteInformationService.getSiteInformationByName("location_reporting_opt_in"));

        locationReportingService.setOptIn(true);

        SiteInformation optInInfo = siteInformationService.getSiteInformationByName("location_reporting_opt_in");
        assertNotNull("Opt-in site information should be created", optInInfo);
        assertEquals("Opt-in value should be 'true'", "true", optInInfo.getValue());
    }

    @Test
    public void setOptIn_whenExistingRecord_shouldUpdateSiteInformation() {
        SiteInformation originalOptIn = siteInformationService.getSiteInformationByName("location_reporting_opt_in");
        assertEquals("Initial value should be 'true'", "true", originalOptIn.getValue());

        locationReportingService.setOptIn(false);

        SiteInformation updatedOptIn = siteInformationService.getSiteInformationByName("location_reporting_opt_in");
        assertEquals("Opt-in value should be updated to 'false'", "false", updatedOptIn.getValue());
    }

    @Test
    public void setOptIn_toTrue_shouldOptIn() {
        locationReportingService.setOptIn(true);

        assertTrue("Should be opted in after setting to true", locationReportingService.isOptedIn());
    }

    @Test
    public void setOptIn_toFalse_shouldOptOut() {
        locationReportingService.setOptIn(false);

        assertFalse("Should be opted out after setting to false", locationReportingService.isOptedIn());
    }

    @Test
    public void sendLocationReport_whenOptedOut_shouldNotThrowException() {
        locationReportingService.setOptIn(false);

        try {
            locationReportingService.sendLocationReport();
        } catch (Exception e) {
            org.junit.Assert.fail("Should not throw exception when opted out: " + e.getMessage());
        }
    }

    @Test
    public void getLastReportTimestamp_shouldReturnStoredTimestamp() {
        SiteInformation lastReportInfo = siteInformationService
                .getSiteInformationByName("location_reporting_last_report");
        assertNotNull("Last report timestamp should exist", lastReportInfo);
        assertEquals("Should return stored timestamp", "1735732800000", lastReportInfo.getValue());
    }
}
