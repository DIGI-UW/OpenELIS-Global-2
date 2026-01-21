package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;

/**
 * DAO interface for NotebookEntryRoomEnvironmentLog operations.
 */
public interface NotebookEntryRoomEnvironmentLogDAO extends BaseDAO<NotebookEntryRoomEnvironmentLog, Integer> {

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
}
