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

    /**
     * Evict a page entity from the Hibernate session cache. This is useful to
     * prevent stale entity issues when the same entity needs to be fetched fresh
     * later in the same transaction.
     *
     * @param page the page entity to evict
     */
    void evict(NoteBookPage page);

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
