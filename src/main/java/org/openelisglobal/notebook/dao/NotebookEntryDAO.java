package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;

/**
 * DAO interface for NotebookEntry operations.
 */
public interface NotebookEntryDAO extends BaseDAO<NotebookEntry, Integer> {

    /**
     * Find all entries for a specific notebook (template).
     */
    List<NotebookEntry> findByNotebookId(Integer notebookId);

    /**
     * Find entries by status.
     */
    List<NotebookEntry> findByStatus(EntryStatus status);

    /**
     * Find entries by technician.
     */
    List<NotebookEntry> findByTechnicianId(Integer technicianId);

    /**
     * Find entries by notebook and status.
     */
    List<NotebookEntry> findByNotebookIdAndStatus(Integer notebookId, EntryStatus status);

    /**
     * Count entries for a notebook.
     */
    Long countByNotebookId(Integer notebookId);

    /**
     * Count entries by status.
     */
    Long countByStatus(EntryStatus status);
}
