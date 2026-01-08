package org.openelisglobal.alert.service;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notification.dao.NotificationConfigOptionDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class AlertNotificationConfigIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertNotificationConfigService alertNotificationConfigService;

    @Autowired
    private NotificationConfigOptionDAO notificationConfigOptionDAO;

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/alert_notification_config.xml");
        cleanRowsInCurrentConnection(new String[] { "notification_config_option", "site_information" });
    }

    @Test
    public void testSaveAlertNotificationConfig_CreatesConfigurationRecords() {
        Map<String, Object> config = new HashMap<>();

        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        Map<String, Boolean> temperatureConfig = new HashMap<>();
        temperatureConfig.put("email", true);
        temperatureConfig.put("sms", true);
        alertConfigs.put("FREEZER_TEMPERATURE_ALERT", temperatureConfig);

        Map<String, Boolean> equipmentConfig = new HashMap<>();
        equipmentConfig.put("email", true);
        equipmentConfig.put("sms", false);
        alertConfigs.put("EQUIPMENT_ALERT", equipmentConfig);

        Map<String, Boolean> inventoryConfig = new HashMap<>();
        inventoryConfig.put("email", false);
        inventoryConfig.put("sms", false);
        alertConfigs.put("INVENTORY_ALERT", inventoryConfig);

        config.put("alertConfigs", alertConfigs);
        config.put("escalationEnabled", true);
        config.put("escalationDelayMinutes", 30);
        config.put("supervisorEmail", "supervisor@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(config);
        List<NotificationConfigOption> temperatureAlerts = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);

        assertFalse("Temperature alerts config should exist", temperatureAlerts.isEmpty());

        NotificationConfigOption temperatureEmailConfig = temperatureAlerts.stream()
                .filter(c -> c.getNotificationMethod() == NotificationMethod.EMAIL).findFirst().orElse(null);

        NotificationConfigOption temperatureSmsConfig = temperatureAlerts.stream()
                .filter(c -> c.getNotificationMethod() == NotificationMethod.SMS).findFirst().orElse(null);

        assertNotNull("Temperature email config should exist", temperatureEmailConfig);
        assertNotNull("Temperature SMS config should exist", temperatureSmsConfig);
        assertTrue("Temperature email should be active", temperatureEmailConfig.getActive());
        assertTrue("Temperature SMS should be active", temperatureSmsConfig.getActive());
    }

    @Test
    public void testSaveAlertNotificationConfig_SavesEscalationSettings() {
        Map<String, Object> config = new HashMap<>();
        config.put("alertConfigs", new HashMap<>());
        config.put("escalationEnabled", true);
        config.put("escalationDelayMinutes", 45);
        config.put("supervisorEmail", "escalation@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(config);

        SiteInformation escalationEnabled = siteInformationService.getSiteInformationByName("alert.escalation.enabled");
        SiteInformation escalationDelay = siteInformationService
                .getSiteInformationByName("alert.escalation.delayMinutes");
        SiteInformation supervisorEmail = siteInformationService.getSiteInformationByName("alert.supervisor.email");

        assertNotNull("Escalation enabled setting should exist", escalationEnabled);
        assertNotNull("Escalation delay setting should exist", escalationDelay);
        assertNotNull("Supervisor email setting should exist", supervisorEmail);

        assertEquals("Escalation enabled should be true", "true", escalationEnabled.getValue());
        assertEquals("Escalation delay should be 45", "45", escalationDelay.getValue());
        assertEquals("Supervisor email should match", "escalation@lab.com", supervisorEmail.getValue());
    }

    @Test
    public void testGetAlertNotificationConfig_ReturnsExistingConfiguration() {
        Map<String, Object> savedConfig = getStringObjectMap();
        alertNotificationConfigService.saveAlertNotificationConfig(savedConfig);

        Map<String, Object> retrievedConfig = alertNotificationConfigService.getAlertNotificationConfig();

        assertNotNull("Retrieved config should not be null", retrievedConfig);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Boolean>> retrievedAlertConfigs = (Map<String, Map<String, Boolean>>) retrievedConfig
                .get("alertConfigs");

        assertNotNull("Alert configs should exist", retrievedAlertConfigs);
        assertTrue("Temperature alert config should exist",
                retrievedAlertConfigs.containsKey("FREEZER_TEMPERATURE_ALERT"));

        Map<String, Boolean> retrievedTempConfig = retrievedAlertConfigs.get("FREEZER_TEMPERATURE_ALERT");
        assertTrue("Email should be enabled", retrievedTempConfig.get("email"));
        assertFalse("SMS should be disabled", retrievedTempConfig.get("sms"));

        assertEquals("Escalation enabled should be false", false, retrievedConfig.get("escalationEnabled"));
        assertEquals("Escalation delay should be 20", 20, retrievedConfig.get("escalationDelayMinutes"));
        assertEquals("Supervisor email should match", "test@lab.com", retrievedConfig.get("supervisorEmail"));
    }

    private static Map<String, Object> getStringObjectMap() {
        Map<String, Object> savedConfig = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();

        Map<String, Boolean> temperatureConfig = new HashMap<>();
        temperatureConfig.put("email", true);
        temperatureConfig.put("sms", false);
        alertConfigs.put("FREEZER_TEMPERATURE_ALERT", temperatureConfig);

        savedConfig.put("alertConfigs", alertConfigs);
        savedConfig.put("escalationEnabled", false);
        savedConfig.put("escalationDelayMinutes", 20);
        savedConfig.put("supervisorEmail", "test@lab.com");
        return savedConfig;
    }

    @Test
    public void testSaveAlertNotificationConfig_UpdatesExistingRecords() {
        Map<String, Object> initialConfig = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs1 = new HashMap<>();
        Map<String, Boolean> config1 = new HashMap<>();
        config1.put("email", false);
        config1.put("sms", false);
        alertConfigs1.put("FREEZER_TEMPERATURE_ALERT", config1);
        initialConfig.put("alertConfigs", alertConfigs1);
        initialConfig.put("escalationEnabled", false);
        initialConfig.put("escalationDelayMinutes", 15);
        initialConfig.put("supervisorEmail", "");

        alertNotificationConfigService.saveAlertNotificationConfig(initialConfig);

        List<NotificationConfigOption> initialRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);
        int initialRecordCount = initialRecords.size();

        Map<String, Object> updatedConfig = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs2 = new HashMap<>();
        Map<String, Boolean> config2 = new HashMap<>();
        config2.put("email", true);
        config2.put("sms", true);
        alertConfigs2.put("FREEZER_TEMPERATURE_ALERT", config2);
        updatedConfig.put("alertConfigs", alertConfigs2);
        updatedConfig.put("escalationEnabled", true);
        updatedConfig.put("escalationDelayMinutes", 30);
        updatedConfig.put("supervisorEmail", "new-supervisor@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(updatedConfig);

        List<NotificationConfigOption> updatedRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);

        assertEquals("Record count should remain the same", initialRecordCount, updatedRecords.size());

        NotificationConfigOption emailConfig = updatedRecords.stream()
                .filter(c -> c.getNotificationMethod() == NotificationMethod.EMAIL).findFirst().orElse(null);

        NotificationConfigOption smsConfig = updatedRecords.stream()
                .filter(c -> c.getNotificationMethod() == NotificationMethod.SMS).findFirst().orElse(null);

        assertNotNull("Email config should exist", emailConfig);
        assertNotNull("SMS config should exist", smsConfig);
        assertTrue("Email should now be active", emailConfig.getActive());
        assertTrue("SMS should now be active", smsConfig.getActive());

        SiteInformation escalationEnabled = siteInformationService.getSiteInformationByName("alert.escalation.enabled");
        assertEquals("Escalation should be enabled", "true", escalationEnabled.getValue());
    }

    @Test
    public void testGetAlertNotificationConfig_ReturnsDefaultsWhenNoConfigExists() {
        Map<String, Object> config = alertNotificationConfigService.getAlertNotificationConfig();

        assertNotNull("Config should not be null", config);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Boolean>> alertConfigs = (Map<String, Map<String, Boolean>>) config.get("alertConfigs");

        assertNotNull("Alert configs should exist", alertConfigs);

        assertTrue("Temperature alert should exist", alertConfigs.containsKey("FREEZER_TEMPERATURE_ALERT"));
        assertTrue("Equipment alert should exist", alertConfigs.containsKey("EQUIPMENT_ALERT"));
        assertTrue("Inventory alert should exist", alertConfigs.containsKey("INVENTORY_ALERT"));

        Map<String, Boolean> temperatureConfig = alertConfigs.get("FREEZER_TEMPERATURE_ALERT");
        assertFalse("Email should be disabled by default", temperatureConfig.get("email"));
        assertFalse("SMS should be disabled by default", temperatureConfig.get("sms"));

        assertEquals("Escalation should be disabled by default", false, config.get("escalationEnabled"));
        assertEquals("Default escalation delay should be 15", 15, config.get("escalationDelayMinutes"));
        assertEquals("Default supervisor email should be empty", "", config.get("supervisorEmail"));
    }

    @Test
    public void testSaveAlertNotificationConfig_HandleMultipleAlertTypes() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();

        Map<String, Boolean> temp = new HashMap<>();
        temp.put("email", true);
        temp.put("sms", true);
        alertConfigs.put("FREEZER_TEMPERATURE_ALERT", temp);

        Map<String, Boolean> equip = new HashMap<>();
        equip.put("email", true);
        equip.put("sms", false);
        alertConfigs.put("EQUIPMENT_ALERT", equip);

        Map<String, Boolean> inv = new HashMap<>();
        inv.put("email", false);
        inv.put("sms", false);
        alertConfigs.put("INVENTORY_ALERT", inv);

        config.put("alertConfigs", alertConfigs);
        config.put("escalationEnabled", false);
        config.put("escalationDelayMinutes", 15);
        config.put("supervisorEmail", "");

        alertNotificationConfigService.saveAlertNotificationConfig(config);

        List<NotificationConfigOption> temperatureConfigs = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);
        List<NotificationConfigOption> equipmentConfigs = notificationConfigOptionDAO
                .getByNature(NotificationNature.EQUIPMENT_ALERT);
        List<NotificationConfigOption> inventoryConfigs = notificationConfigOptionDAO
                .getByNature(NotificationNature.INVENTORY_ALERT);

        // Temperature: email=true, sms=true
        assertEquals("Temperature should have 2 config records", 2, temperatureConfigs.size());
        assertTrue("Temperature email should be active", temperatureConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.EMAIL && c.getActive()));
        assertTrue("Temperature SMS should be active", temperatureConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.SMS && c.getActive()));

        // Equipment: email=true, sms=false
        assertEquals("Equipment should have 2 config records", 2, equipmentConfigs.size());
        assertTrue("Equipment email should be active", equipmentConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.EMAIL && c.getActive()));
        assertFalse("Equipment SMS should be inactive", equipmentConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.SMS && c.getActive()));

        // Inventory: email=false, sms=false
        assertEquals("Inventory should have 2 config records", 2, inventoryConfigs.size());
        assertFalse("Inventory email should be inactive", inventoryConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.EMAIL && c.getActive()));
        assertFalse("Inventory SMS should be inactive", inventoryConfigs.stream()
                .anyMatch(c -> c.getNotificationMethod() == NotificationMethod.SMS && c.getActive()));
    }
}
