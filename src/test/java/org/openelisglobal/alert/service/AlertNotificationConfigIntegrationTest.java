package org.openelisglobal.alert.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notification.dao.NotificationConfigOptionDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testSaveAlertNotificationConfig_CreatesConfigurationRecords() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        Map<String, Boolean> temperatureMethods = new HashMap<>();
        temperatureMethods.put("email", true);
        temperatureMethods.put("sms", false);
        alertConfigs.put(NotificationNature.FREEZER_TEMPERATURE_ALERT.toString(), temperatureMethods);

        config.put("alertConfigs", alertConfigs);
        config.put("escalationEnabled", true);
        config.put("escalationDelayMinutes", 30);
        config.put("supervisorEmail", "supervisor@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(config);
        List<NotificationConfigOption> temperatureAlerts = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);

        assertNotNull(temperatureAlerts);
        assertTrue(temperatureAlerts.stream()
                .anyMatch(opt -> opt.getActive() && "EMAIL".equals(opt.getNotificationMethod().toString())));
    }

    @Test
    public void testSaveAlertNotificationConfig_SavesEscalationSettings() {
        Map<String, Object> config = new HashMap<>();
        config.put("escalationEnabled", true);
        config.put("escalationDelayMinutes", 45);
        config.put("supervisorEmail", "escalation@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(config);

        SiteInformation escalationEnabled = siteInformationService.getSiteInformationByName("alert.escalation.enabled");
        SiteInformation escalationDelay = siteInformationService
                .getSiteInformationByName("alert.escalation.delayMinutes");
        SiteInformation supervisorEmail = siteInformationService.getSiteInformationByName("alert.supervisor.email");

        assertNotNull(escalationEnabled);
        assertEquals("true", escalationEnabled.getValue());
        assertNotNull(escalationDelay);
        assertEquals("45", escalationDelay.getValue());
        assertNotNull(supervisorEmail);
        assertEquals("escalation@lab.com", supervisorEmail.getValue());
    }

    @Test
    public void testGetAlertNotificationConfig_ReturnsExistingConfiguration() {
        Map<String, Object> savedConfig = getStringObjectMap();
        alertNotificationConfigService.saveAlertNotificationConfig(savedConfig);

        Map<String, Object> retrievedConfig = alertNotificationConfigService.getAlertNotificationConfig();

        assertNotNull(retrievedConfig);
        assertTrue((Boolean) retrievedConfig.get("escalationEnabled"));
        assertEquals(30, retrievedConfig.get("escalationDelayMinutes"));
        assertEquals("supervisor@lab.com", retrievedConfig.get("supervisorEmail"));
    }

    private Map<String, Object> getStringObjectMap() {
        Map<String, Object> savedConfig = new HashMap<>();
        savedConfig.put("escalationEnabled", true);
        savedConfig.put("escalationDelayMinutes", 30);
        savedConfig.put("supervisorEmail", "supervisor@lab.com");

        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        Map<String, Boolean> freezerMethods = new HashMap<>();
        freezerMethods.put("email", true);
        alertConfigs.put(NotificationNature.FREEZER_TEMPERATURE_ALERT.toString(), freezerMethods);
        savedConfig.put("alertConfigs", alertConfigs);
        return savedConfig;
    }

    @Test
    public void testSaveAlertNotificationConfig_UpdatesExistingRecords() {
        Map<String, Object> initialConfig = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        Map<String, Boolean> freezerMethods = new HashMap<>();
        freezerMethods.put("email", true);
        alertConfigs.put(NotificationNature.FREEZER_TEMPERATURE_ALERT.toString(), freezerMethods);
        initialConfig.put("alertConfigs", alertConfigs);
        initialConfig.put("escalationEnabled", false);
        initialConfig.put("escalationDelayMinutes", 15);
        initialConfig.put("supervisorEmail", "");

        alertNotificationConfigService.saveAlertNotificationConfig(initialConfig);

        List<NotificationConfigOption> initialRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);
        assertEquals(1, initialRecords.size());
        assertTrue(initialRecords.get(0).getActive());

        Map<String, Object> updatedConfig = new HashMap<>();
        Map<String, Boolean> updatedMethods = new HashMap<>();
        updatedMethods.put("email", false);
        Map<String, Map<String, Boolean>> updatedAlertConfigs = new HashMap<>();
        updatedAlertConfigs.put(NotificationNature.FREEZER_TEMPERATURE_ALERT.toString(), updatedMethods);
        updatedConfig.put("alertConfigs", updatedAlertConfigs);
        updatedConfig.put("escalationEnabled", true);
        updatedConfig.put("escalationDelayMinutes", 30);
        updatedConfig.put("supervisorEmail", "new-supervisor@lab.com");

        alertNotificationConfigService.saveAlertNotificationConfig(updatedConfig);

        List<NotificationConfigOption> updatedRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);
        assertEquals(1, updatedRecords.size());
        assertNotNull(updatedRecords.get(0).getActive());
        assertTrue(!updatedRecords.get(0).getActive());
    }

    @Test
    public void testSaveAlertNotificationConfig_HandleMultipleAlertTypes() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();

        Map<String, Boolean> freezerMethods = new HashMap<>();
        freezerMethods.put("email", true);
        alertConfigs.put(NotificationNature.FREEZER_TEMPERATURE_ALERT.toString(), freezerMethods);

        Map<String, Boolean> inventoryMethods = new HashMap<>();
        inventoryMethods.put("sms", true);
        alertConfigs.put(NotificationNature.INVENTORY_ALERT.toString(), inventoryMethods);

        config.put("alertConfigs", alertConfigs);

        alertNotificationConfigService.saveAlertNotificationConfig(config);

        List<NotificationConfigOption> freezerRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.FREEZER_TEMPERATURE_ALERT);
        List<NotificationConfigOption> inventoryRecords = notificationConfigOptionDAO
                .getByNature(NotificationNature.INVENTORY_ALERT);

        assertTrue(freezerRecords.stream()
                .anyMatch(opt -> opt.getActive() && "EMAIL".equals(opt.getNotificationMethod().toString())));
        assertTrue(inventoryRecords.stream()
                .anyMatch(opt -> opt.getActive() && "SMS".equals(opt.getNotificationMethod().toString())));
    }
}
