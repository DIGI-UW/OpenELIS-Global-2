package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;

/**
 * Service interface for NotebookEntryTemperatureLog operations.
 */
public interface NotebookEntryTemperatureLogService extends BaseObjectService<NotebookEntryTemperatureLog, Integer> {

    /**
     * Find all temperature logs for a notebook entry, ordered by checked date/time
     * descending.
     *
     * @param entryId the notebook entry ID
     * @return list of temperature logs
     */
    List<NotebookEntryTemperatureLog> findByEntryId(Integer entryId);

    /**
     * Find temperature logs for a specific device/freezer.
     *
     * @param entryId   the notebook entry ID
     * @param freezerId the freezer/device ID
     * @return list of temperature logs for that device
     */
    List<NotebookEntryTemperatureLog> findByEntryIdAndFreezerId(Integer entryId, String freezerId);

    /**
     * Count temperature logs for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return count of logs
     */
    Long countByEntryId(Integer entryId);

    /**
     * Log a temperature reading for a notebook entry.
     *
     * @param entryId          the notebook entry ID
     * @param freezerId        the freezer/device ID
     * @param checkTime        AM or PM
     * @param temperatureValue the temperature value
     * @param temperatureUnit  C or F
     * @param checkedBy        name of person who checked
     * @param checkedDateTime  when the check was made
     * @param notes            optional notes
     * @param sysUserId        the user logging the reading
     * @return the created temperature log
     */
    NotebookEntryTemperatureLog logTemperature(Integer entryId, String freezerId, String checkTime,
            Double temperatureValue, String temperatureUnit, String checkedBy, Timestamp checkedDateTime, String notes,
            String sysUserId);
}
