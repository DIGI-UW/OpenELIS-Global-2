package org.openelisglobal.locationreporting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.locationreporting.service.LocationReportingService;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationReportingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LocationReportingService locationReportingService;

    // Tests should exercise public API of the service only

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/location-reporting.xml");
    }

    @Test
    public void isOptedIn_whenOptInIsTrue_shouldReturnTrue() {
        locationReportingService.setOptIn(true);
        boolean result = locationReportingService.isOptedIn();
        assertTrue("Should be opted in when value is 'true'", result);
    }

    @Test
    public void isOptedIn_whenOptInIsFalse_shouldReturnFalse() {
        locationReportingService.setOptIn(false);
        boolean result = locationReportingService.isOptedIn();
        assertFalse("Should not be opted in when value is 'false'", result);
    }

    @Test
    public void setOptIn_whenNewRecord_shouldReflectInServiceState() {
        // Ensure opt-out, then opt-in and verify via service API
        locationReportingService.setOptIn(false);
        locationReportingService.setOptIn(true);
        assertTrue("Opt-in value should be 'true'", locationReportingService.isOptedIn());
    }

    @Test
    public void setOptIn_whenExistingRecord_shouldUpdateServiceState() {
        locationReportingService.setOptIn(true);
        locationReportingService.setOptIn(false);
        assertFalse("Opt-in value should be updated to 'false'", locationReportingService.isOptedIn());
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

    // Removed direct assertions on internal storage; tests focus on public API
}
