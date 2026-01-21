package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;

/**
 * Service interface for NotebookEntryRoomEnvironmentLog operations.
 */
public interface NotebookEntryRoomEnvironmentLogService
        extends BaseObjectService<NotebookEntryRoomEnvironmentLog, Integer> {

    /**
     * Find all room environment logs for a notebook entry, ordered by checked
     * date/time descending.
     *
     * @param entryId the notebook entry ID
     * @return list of room environment logs
     */
    List<NotebookEntryRoomEnvironmentLog> findByEntryId(Integer entryId);

    /**
     * Find room environment logs for a specific room.
     *
     * @param entryId the notebook entry ID
     * @param roomId  the room ID
     * @return list of room environment logs for that room
     */
    List<NotebookEntryRoomEnvironmentLog> findByEntryIdAndRoomId(Integer entryId, String roomId);

    /**
     * Count room environment logs for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return count of logs
     */
    Long countByEntryId(Integer entryId);

    /**
     * Log a room environment reading for a notebook entry.
     *
     * @param entryId         the notebook entry ID
     * @param roomId          the room ID
     * @param roomName        the room name
     * @param oxygenLevel     the O2 level percentage (normal ~21%, alert <19.5%)
     * @param humidity        the humidity percentage (optimal 30-60%)
     * @param checkedBy       name of person who checked
     * @param checkedDateTime when the check was made
     * @param notes           optional notes
     * @param sysUserId       the user logging the reading
     * @return the created room environment log
     */
    NotebookEntryRoomEnvironmentLog logRoomEnvironment(Integer entryId, String roomId, String roomName,
            Double oxygenLevel, Double humidity, String checkedBy, Timestamp checkedDateTime, String notes,
            String sysUserId);
}
