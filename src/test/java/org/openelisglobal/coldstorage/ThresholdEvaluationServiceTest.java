package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ThresholdEvaluationService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.springframework.beans.factory.annotation.Autowired;

public class ThresholdEvaluationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ThresholdEvaluationService thresholdEvaluationService;

    @Autowired
    FreezerService freezerService;

    @Before
    public void setup() throws Exception {
        // Load user data first (required for created_by foreign key)
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/threshold_evaluation.xml");
    }

    @Test
    public void resolveActiveProfile_shouldReturnAssignedProfile() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime timestamp = OffsetDateTime.now();
        ThresholdProfile profile = thresholdEvaluationService.resolveActiveProfile(freezer, timestamp);

        assertNotNull("Profile should be resolved", profile);
        assertEquals("Profile name should be Ultra-Low Freezer Profile", "Ultra-Low Freezer Profile",
                profile.getName());
    }

    @Test
    public void evaluateStatus_shouldReturnNormalWhenTemperatureInNormalRange() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-80.0"); // Within normal range
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be NORMAL", FreezerReading.Status.NORMAL, status);
    }

    @Test
    public void evaluateStatus_shouldReturnWarningWhenTemperatureInWarningRange() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-77.5"); // In warning range (high)
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be WARNING", FreezerReading.Status.WARNING, status);
    }

    @Test
    public void evaluateStatus_shouldReturnWarningWhenTemperatureBelowWarningMin() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-82.5"); // In warning range (low)
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be WARNING", FreezerReading.Status.WARNING, status);
    }

    @Test
    public void evaluateStatus_shouldReturnCriticalWhenTemperatureAboveCriticalMax() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-74.0"); // Above critical max
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be CRITICAL", FreezerReading.Status.CRITICAL, status);
    }

    @Test
    public void evaluateStatus_shouldReturnCriticalWhenTemperatureBelowCriticalMin() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-86.0"); // Below critical min
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be CRITICAL", FreezerReading.Status.CRITICAL, status);
    }

    @Test
    public void evaluateStatus_shouldReturnNormalWhenProfileIsNull() {
        BigDecimal temperature = new BigDecimal("-80.0");
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, null);

        assertEquals("Status should be NORMAL when no profile exists", FreezerReading.Status.NORMAL, status);
    }

    @Test
    public void evaluateStatus_shouldHandleEdgeCaseAtWarningMax() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-78.0"); // Exactly at warning max
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be WARNING at boundary", FreezerReading.Status.WARNING, status);
    }

    @Test
    public void evaluateStatus_shouldHandleEdgeCaseAtWarningMin() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("-82.0"));
        profile.setWarningMax(new BigDecimal("-78.0"));
        profile.setCriticalMin(new BigDecimal("-85.0"));
        profile.setCriticalMax(new BigDecimal("-75.0"));

        BigDecimal temperature = new BigDecimal("-82.0"); // Exactly at warning min
        BigDecimal humidity = new BigDecimal("50.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);

        assertEquals("Status should be WARNING at boundary", FreezerReading.Status.WARNING, status);
    }
}
