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
}
