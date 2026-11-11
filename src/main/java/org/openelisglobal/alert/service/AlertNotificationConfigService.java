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
     * @param config    Map containing configuration values
     * @param sysUserId Acting user id, stamped on NotificationConfigOption /
     *                  SiteInformation entities so the audit-trail row emitted by
     *                  AuditableBaseObjectServiceImpl can resolve a valid
     *                  system_user. Must reference an existing row in
     *                  clinlims.system_user; otherwise the audit insert fails its
     *                  not-null check and rolls the transaction back.
     */
    void saveAlertNotificationConfig(Map<String, Object> config, String sysUserId);
}
