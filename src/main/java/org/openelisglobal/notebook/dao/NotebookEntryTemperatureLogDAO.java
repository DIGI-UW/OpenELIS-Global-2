package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;

/**
 * DAO interface for NotebookEntryTemperatureLog operations.
 */
public interface NotebookEntryTemperatureLogDAO extends BaseDAO<NotebookEntryTemperatureLog, Integer> {

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
}
