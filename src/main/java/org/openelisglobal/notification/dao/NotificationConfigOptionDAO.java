package org.openelisglobal.notification.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;

public interface NotificationConfigOptionDAO extends BaseDAO<NotificationConfigOption, Integer> {

    /**
     * Get notification config options by nature (e.g., FREEZER_TEMPERATURE_ALERT).
     *
     * @param nature The notification nature
     * @return List of notification config options for that nature
     */
    List<NotificationConfigOption> getByNature(NotificationNature nature);

    /**
     * Get all alert-related notification config options.
     *
     * @return List of all alert notification configs (FREEZER_TEMPERATURE_ALERT,
     *         EQUIPMENT_ALERT, INVENTORY_ALERT)
     */
    List<NotificationConfigOption> getAllAlertConfigs();
}
