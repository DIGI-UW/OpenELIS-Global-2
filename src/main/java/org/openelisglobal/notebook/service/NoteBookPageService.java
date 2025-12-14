package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

/**
 * Service interface for NoteBookPage entity. Provides business operations for
 * notebook pages.
 */
public interface NoteBookPageService extends BaseObjectService<NoteBookPage, Integer> {

    /**
     * Get all pages for a notebook, ordered by page order.
     *
     * @param notebookId the notebook ID
     * @return list of pages for the notebook
     */
    List<NoteBookPage> getByNotebookId(Integer notebookId);
}
