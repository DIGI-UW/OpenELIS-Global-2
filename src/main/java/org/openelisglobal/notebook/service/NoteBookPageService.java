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

    /**
     * Update the content of a notebook page. This method merges new content with
     * existing content JSON, allowing partial updates.
     *
     * @param pageId    the page ID
     * @param content   the new content JSON string to merge
     * @param sysUserId the user making the update
     * @return the updated page
     */
    NoteBookPage updatePageContent(Integer pageId, String content, String sysUserId);
}
