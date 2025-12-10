package org.openelisglobal.notebook.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * DAO interface for NotebookPageSample entity operations.
 */
public interface NotebookPageSampleDAO extends BaseDAO<NotebookPageSample, Integer> {

    /**
     * Get all page samples for a specific notebook page.
     *
     * @param pageId the notebook page ID
     * @return list of NotebookPageSample entities
     */
    List<NotebookPageSample> getByPageId(Integer pageId);

    /**
     * Get page samples for a specific page filtered by status.
     *
     * @param pageId the notebook page ID
     * @param status the status to filter by
     * @return list of NotebookPageSample entities matching the status
     */
    List<NotebookPageSample> getByPageIdAndStatus(Integer pageId, Status status);

    /**
     * Get the page sample for a specific page and sample item combination.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return the NotebookPageSample or null if not found
     */
    NotebookPageSample getByPageIdAndSampleItemId(Integer pageId, Integer sampleItemId);

    /**
     * Get status counts by page ID for progress tracking.
     *
     * @param pageId the notebook page ID
     * @return map of Status to count
     */
    Map<Status, Long> getStatusCountsByPageId(Integer pageId);

    /**
     * Get all page samples for a specific sample item across all pages.
     *
     * @param sampleItemId the sample item ID
     * @return list of NotebookPageSample entities
     */
    List<NotebookPageSample> getBySampleItemId(Integer sampleItemId);

    /**
     * Bulk update status for multiple samples on a page.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs
     * @param status    the new status
     * @return number of records updated
     */
    int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status);

    /**
     * Get paginated samples for a page with optional status filter.
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @param offset offset for pagination
     * @param limit  limit for pagination
     * @return list of NotebookPageSample entities
     */
    List<NotebookPageSample> getByPageIdPaginated(Integer pageId, Status status, int offset, int limit);

    /**
     * Get total count of samples for a page with optional status filter.
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @return total count
     */
    long getCountByPageId(Integer pageId, Status status);

    /**
     * Get all page samples for a notebook (across all pages).
     *
     * @param notebookId the notebook ID
     * @return list of NotebookPageSample entities
     */
    List<NotebookPageSample> getByNotebookId(Integer notebookId);

    /**
     * Get page sample for a specific sample on a specific page.
     *
     * @param sampleItemId the sample item ID (as String)
     * @param pageId       the notebook page ID
     * @return the NotebookPageSample or null if not found
     */
    NotebookPageSample getBySampleItemIdAndPageId(String sampleItemId, Integer pageId);
}
