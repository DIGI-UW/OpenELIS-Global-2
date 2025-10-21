package org.openelisglobal.locationreporting.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.locationreporting.form.LocationReportingForm;
import org.openelisglobal.locationreporting.service.LocationReportingService;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationReportingControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LocationReportingController locationReportingController;

    @Autowired
    private LocationReportingService locationReportingService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/location-reporting.xml");
    }

    @Test
    public void getSettings_whenOptedIn_shouldReturnFormWithOptInTrue() {
        locationReportingService.setOptIn(true);

        LocationReportingForm result = locationReportingController.getSettings();

        assertNotNull("Form should not be null", result);
        assertTrue("Opt-in should be true", result.getOptIn());
    }

    @Test
    public void getSettings_whenOptedOut_shouldReturnFormWithOptInFalse() {
        locationReportingService.setOptIn(false);

        LocationReportingForm result = locationReportingController.getSettings();

        assertNotNull("Form should not be null", result);
        assertFalse("Opt-in should be false", result.getOptIn());
    }

    @Test
    public void updateSettings_withOptInTrue_shouldEnableReporting() {
        LocationReportingForm form = new LocationReportingForm();
        form.setOptIn(true);

        LocationReportingForm result = locationReportingController.updateSettings(form);

        assertNotNull("Result should not be null", result);
        assertTrue("Opt-in should be enabled", result.getOptIn());
        assertTrue("Service should reflect opt-in status", locationReportingService.isOptedIn());
    }

    @Test
    public void updateSettings_withOptInFalse_shouldDisableReporting() {
        LocationReportingForm form = new LocationReportingForm();
        form.setOptIn(false);

        LocationReportingForm result = locationReportingController.updateSettings(form);

        assertNotNull("Result should not be null", result);
        assertFalse("Opt-in should be disabled", result.getOptIn());
        assertFalse("Service should reflect opt-out status", locationReportingService.isOptedIn());
    }

    @Test
    public void sendReportNow_shouldNotThrowException() {
        try {
            locationReportingController.sendReportNow();
        } catch (Exception e) {
            org.junit.Assert.fail("Should not throw exception when manually triggering report: " + e.getMessage());
        }
    }

    @Test
    public void updateSettings_shouldPersistChanges() {
        LocationReportingForm form = new LocationReportingForm();
        form.setOptIn(true);

        locationReportingController.updateSettings(form);

        LocationReportingForm retrievedForm = locationReportingController.getSettings();
        assertEquals("Persisted value should match", true, retrievedForm.getOptIn());
    }
}
