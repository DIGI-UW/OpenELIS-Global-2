package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
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

    @Autowired
    FreezerReadingService freezerReadingService;

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

    @Test
    public void evaluateStatus_shouldNotFallThroughToNormalExactlyAtCriticalMax() {
        // Regression test: isWarningTemperature previously required temp strictly
        // < criticalMax while isCriticalTemperature required strictly > criticalMax,
        // leaving temp == criticalMax classified as neither - silently NORMAL.
        ThresholdProfile profile = new ThresholdProfile();
        profile.setWarningMin(new BigDecimal("1.0"));
        profile.setWarningMax(new BigDecimal("9.0"));
        profile.setCriticalMin(new BigDecimal("0.0"));
        profile.setCriticalMax(new BigDecimal("10.0"));

        BigDecimal temperature = new BigDecimal("10.0"); // Exactly at critical max

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, null, profile);

        assertEquals("Status must not silently fall through to NORMAL at the critical boundary",
                FreezerReading.Status.WARNING, status);
    }

    @Test
    public void evaluateStatusWithHysteresis_shouldSuppressEscalationOnFirstBreach() {
        // Ultra-Low Freezer Profile (id=100) has min_excursion_minutes=5. With no
        // prior reading history at all, a breach cannot yet have persisted for the
        // full window, so it must not escalate on the very first reading.
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);
        OffsetDateTime now = OffsetDateTime.now();
        ThresholdProfile profile = thresholdEvaluationService.resolveActiveProfile(freezer, now);
        assertNotNull("Profile should be resolved", profile);

        BigDecimal criticalTemperature = new BigDecimal("-74.0"); // above critical max (-75.0)

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(criticalTemperature, null, profile,
                freezer, now);

        assertEquals("First breach with no history should not yet escalate", FreezerReading.Status.NORMAL, status);
    }

    @Test
    public void evaluateStatusWithHysteresis_shouldEscalateAfterSustainedBreach() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);
        OffsetDateTime now = OffsetDateTime.now();
        ThresholdProfile profile = thresholdEvaluationService.resolveActiveProfile(freezer, now);
        assertNotNull("Profile should be resolved", profile);

        BigDecimal criticalTemperature = new BigDecimal("-74.0"); // above critical max (-75.0)

        // Simulate a breach that has already been present for the full 5-minute
        // minExcursionMinutes window: prior readings at -6, -4, -2 minutes, all
        // already critical.
        freezerReadingService.saveReading(freezer, now.minusMinutes(6), criticalTemperature, null, null,
                FreezerReading.Status.CRITICAL, true, null);
        freezerReadingService.saveReading(freezer, now.minusMinutes(4), criticalTemperature, null, null,
                FreezerReading.Status.CRITICAL, true, null);
        freezerReadingService.saveReading(freezer, now.minusMinutes(2), criticalTemperature, null, null,
                FreezerReading.Status.CRITICAL, true, null);

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(criticalTemperature, null, profile,
                freezer, now);

        assertEquals("Sustained breach spanning the full window should escalate", FreezerReading.Status.CRITICAL,
                status);
    }

    @Test
    public void evaluateStatusWithHysteresis_shouldFallBackToInstantaneousWithoutFreezerContext() {
        ThresholdProfile profile = new ThresholdProfile();
        profile.setCriticalMax(new BigDecimal("-75.0"));
        profile.setMinExcursionMinutes(5);

        BigDecimal temperature = new BigDecimal("-74.0");

        FreezerReading.Status status = thresholdEvaluationService.evaluateStatus(temperature, null, profile, null,
                null);

        assertEquals("Without freezer/timestamp context, hysteresis cannot be applied", FreezerReading.Status.CRITICAL,
                status);
    }
}
