package org.openelisglobal.alert.service;

import java.util.Map;

public interface AlertNotificationConfigService {

    /**
     * Get current alert notification configuration.
     *
     * @return Map containing: - emailNotificationsEnabled (Boolean) -
     *         smsNotificationsEnabled (Boolean) - escalationEnabled (Boolean) -
     *         escalationDelayMinutes (Integer) - supervisorEmail (String)
     */
    Map<String, Object> getAlertNotificationConfig();

    /**
     * Save/update alert notification configuration.
     *
     * @param config Map containing configuration values
     */
    void saveAlertNotificationConfig(Map<String, Object> config);
}
