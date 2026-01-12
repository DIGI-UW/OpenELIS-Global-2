package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * Service interface for NotebookPageSample operations. Provides
 * per-sample-per-page tracking for immunology workflow.
 */
public interface NotebookPageSampleService extends BaseObjectService<NotebookPageSample, Integer> {

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
     * Get progress information for a notebook page.
     *
     * @param pageId the notebook page ID
     * @return PageProgress containing status counts
     */
    PageProgress getPageProgress(Integer pageId);

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
     * @param userId    the user performing the update
     * @return number of records updated
     */
    int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId);

    /**
     * Bulk update status for multiple samples on a page using String IDs. Supports
     * composite sample IDs (e.g., "123_cassette_0") used in pathology workflow
     * pages where samples are expanded from parent items.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs as Strings
     * @param status    the new status
     * @param userId    the user performing the update
     * @return number of records updated
     */
    int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status, String userId);

    /**
     * Bulk apply data values to multiple samples on a page.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs
     * @param data      the data to apply to all samples
     * @param userId    the user performing the update
     * @return number of records updated
     */
    int bulkApplyData(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId);

    /**
     * Get paginated samples for a page with optional status filter.
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @param page   page number (0-indexed)
     * @param size   page size
     * @return list of NotebookPageSample entities
     */
    List<NotebookPageSample> getByPageIdPaginated(Integer pageId, Status status, int page, int size);

    /**
     * Get total count of samples for a page with optional status filter.
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @return total count
     */
    long getCountByPageId(Integer pageId, Status status);

    /**
     * Create page sample records for all pages when linking a sample to a notebook.
     * Note: This now only creates a record for Page 1 (first page by order).
     * Samples progress to subsequent pages via bulkUpdateStatus when marked
     * COMPLETED.
     *
     * @param notebookId   the notebook ID
     * @param sampleItemId the sample item ID
     */
    void createPageSamplesForNotebook(Integer notebookId, Integer sampleItemId);

    /**
     * Create a page sample record for a specific page. Used when child samples are
     * created and should appear on a specific page.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @param status       the initial status (e.g., PENDING)
     */
    void createPageSampleForPage(Integer pageId, Integer sampleItemId, Status status);

    /**
     * Create a page sample record for a specific page using String sample ID.
     * Supports composite sample IDs (e.g., "4_cassette_0_block_0") used in
     * pathology workflows.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID as String
     * @param status       the initial status (e.g., PENDING)
     */
    void createPageSampleForPageString(Integer pageId, String sampleItemId, Status status);

    /**
     * Create a page sample with initial data (used for page advancement to preserve
     * data from previous page).
     *
     * @param pageId       the page ID
     * @param sampleItemId the sample item ID (as String)
     * @param status       the initial status (e.g., PENDING)
     * @param data         the initial JSONB data to copy from source page
     */
    void createPageSampleForPageString(Integer pageId, String sampleItemId, Status status, Map<String, Object> data);

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

    /** Progress information for a notebook page. */
    record PageProgress(int total, int pending, int inProgress, int completed, int skipped, double percentage) {
    }

    /**
     * Create PageProgress from status count map.
     */
    static PageProgress createPageProgress(Map<Status, Long> counts) {
        int pending = counts.getOrDefault(Status.PENDING, 0L).intValue();
        int inProgress = counts.getOrDefault(Status.IN_PROGRESS, 0L).intValue();
        int completed = counts.getOrDefault(Status.COMPLETED, 0L).intValue();
        int skipped = counts.getOrDefault(Status.SKIPPED, 0L).intValue();
        int total = pending + inProgress + completed + skipped;
        double percentage = total > 0 ? (completed * 100.0 / total) : 0.0;
        return new PageProgress(total, pending, inProgress, completed, skipped, percentage);
    }
}
