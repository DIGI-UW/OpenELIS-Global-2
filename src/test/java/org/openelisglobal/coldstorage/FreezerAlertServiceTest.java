package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.service.FreezerAlertService;
import org.springframework.beans.factory.annotation.Autowired;

public class FreezerAlertServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    FreezerAlertService freezerAlertService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer_alert.xml");
    }

    @Test
    public void createFreezerTemperatureAlert_shouldCreateAlertWithCriticalHighThreshold() {
        Long freezerId = 1L;
        BigDecimal temperature = new BigDecimal("-15.5");
        BigDecimal thresholdValue = new BigDecimal("-20.0");
        String thresholdType = "CRITICAL_HIGH";

        Alert alert = freezerAlertService.createFreezerTemperatureAlert(freezerId, temperature, thresholdValue,
                thresholdType);

        assertNotNull("Alert should not be null", alert);
        assertNotNull("Alert ID should not be null", alert.getId());
        assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE, alert.getAlertType());
        assertEquals("Alert entity type should be Freezer", "Freezer", alert.getAlertEntityType());
        assertEquals("Alert entity ID should match freezer ID", freezerId, alert.getAlertEntityId());
        assertEquals("Alert severity should be CRITICAL", AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
        assertNotNull("Alert context data should not be null", alert.getContextData());
    }

    @Test
    public void createFreezerTemperatureAlert_shouldCreateAlertWithWarningHighThreshold() {
        Long freezerId = 1L;
        BigDecimal temperature = new BigDecimal("-18.0");
        BigDecimal thresholdValue = new BigDecimal("-20.0");
        String thresholdType = "WARNING_HIGH";

        Alert alert = freezerAlertService.createFreezerTemperatureAlert(freezerId, temperature, thresholdValue,
                thresholdType);

        assertNotNull("Alert should not be null", alert);
        assertEquals("Alert severity should be WARNING", AlertSeverity.WARNING, alert.getSeverity());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
    }

    @Test
    public void createFreezerTemperatureAlert_shouldCreateAlertWithCriticalLowThreshold() {
        Long freezerId = 2L;
        BigDecimal temperature = new BigDecimal("-85.0");
        BigDecimal thresholdValue = new BigDecimal("-80.0");
        String thresholdType = "CRITICAL_LOW";

        Alert alert = freezerAlertService.createFreezerTemperatureAlert(freezerId, temperature, thresholdValue,
                thresholdType);

        assertNotNull("Alert should not be null", alert);
        assertEquals("Alert severity should be CRITICAL", AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals("Alert entity ID should match freezer ID", freezerId, alert.getAlertEntityId());
    }

    @Test
    public void createFreezerTemperatureAlert_shouldCreateAlertWithWarningLowThreshold() {
        Long freezerId = 2L;
        BigDecimal temperature = new BigDecimal("-82.0");
        BigDecimal thresholdValue = new BigDecimal("-80.0");
        String thresholdType = "WARNING_LOW";

        Alert alert = freezerAlertService.createFreezerTemperatureAlert(freezerId, temperature, thresholdValue,
                thresholdType);

        assertNotNull("Alert should not be null", alert);
        assertEquals("Alert severity should be WARNING", AlertSeverity.WARNING, alert.getSeverity());
    }

    @Test
    public void createFreezerTemperatureAlert_shouldIncludeTemperatureDataInContext() {
        Long freezerId = 1L;
        BigDecimal temperature = new BigDecimal("-15.5");
        BigDecimal thresholdValue = new BigDecimal("-20.0");
        String thresholdType = "CRITICAL_HIGH";

        Alert alert = freezerAlertService.createFreezerTemperatureAlert(freezerId, temperature, thresholdValue,
                thresholdType);

        assertNotNull("Alert context data should not be null", alert.getContextData());
        String contextData = alert.getContextData();
        assertEquals("Context data should contain temperature information", true,
                contextData.contains("temperature") || contextData.contains("threshold"));
    }

    @Test
    public void createFreezerTemperatureAlert_shouldCreateMultipleAlertsForDifferentFreezers() {
        Long freezerId1 = 1L;
        Long freezerId2 = 2L;
        BigDecimal temperature = new BigDecimal("-15.5");
        BigDecimal thresholdValue = new BigDecimal("-20.0");
        String thresholdType = "CRITICAL_HIGH";

        Alert alert1 = freezerAlertService.createFreezerTemperatureAlert(freezerId1, temperature, thresholdValue,
                thresholdType);
        Alert alert2 = freezerAlertService.createFreezerTemperatureAlert(freezerId2, temperature, thresholdValue,
                thresholdType);

        assertNotNull("First alert should not be null", alert1);
        assertNotNull("Second alert should not be null", alert2);
        assertEquals("First alert should be for freezer 1", freezerId1, alert1.getAlertEntityId());
        assertEquals("Second alert should be for freezer 2", freezerId2, alert2.getAlertEntityId());
    }
}
