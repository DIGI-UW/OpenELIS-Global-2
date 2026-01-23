package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

/**
 * DAO interface for NoteBookPage entity. Provides data access operations for
 * notebook pages.
 */
public interface NoteBookPageDAO extends BaseDAO<NoteBookPage, Integer> {

    /**
     * Get all pages for a notebook, ordered by page order.
     *
     * @param notebookId the notebook ID
     * @return list of pages for the notebook
     */
    List<NoteBookPage> getByNotebookId(Integer notebookId);

    /**
     * Get page ID by notebook ID and page order without loading the entity. This
     * avoids loading the entity into the Hibernate session, preventing stale entity
     * issues in subsequent operations.
     *
     * @param notebookId the notebook ID
     * @param pageOrder  the page order
     * @return the page ID, or null if not found
     */
    Integer getPageIdByNotebookIdAndOrder(Integer notebookId, Integer pageOrder);

    /**
     * Get page ID by notebook ID and title pattern without loading the entity. This
     * avoids loading the entity into the Hibernate session, preventing stale entity
     * issues in subsequent operations.
     *
     * @param notebookId   the notebook ID
     * @param titlePattern the title pattern to match (using LIKE)
     * @return the page ID, or null if not found
     */
    Integer getPageIdByNotebookIdAndTitlePattern(Integer notebookId, String titlePattern);
}
