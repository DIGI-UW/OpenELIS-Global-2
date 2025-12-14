package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * Service interface for bulk operations on notebook page samples. Supports bulk
 * data entry, status updates, and batch processing for efficient handling of
 * 200+ samples per workflow.
 *
 * Per FR-033: Operations process in batches of 50 to prevent timeout.
 */
public interface NotebookBulkOperationService {

    /**
     * Apply common values to multiple samples on a page. Updates the JSONB data
     * field for each sample, merging with existing data.
     *
     * Per FR-031: System MUST support "Apply to Selected" to update common values
     * for multiple samples in single transaction.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs to update
     * @param data      map of field names to values (stored in JSONB)
     * @param userId    the user performing the update
     * @return number of samples updated
     */
    int bulkApplyValues(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId);

    /**
     * Update status for multiple samples on a page. Delegates to
     * NotebookPageSampleService.bulkUpdateStatus.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs to update
     * @param status    the new status
     * @param userId    the user performing the update
     * @return number of samples updated
     */
    int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId);

    /**
     * Get progress information for a notebook page. Delegates to
     * NotebookPageSampleService.getPageProgress.
     *
     * Per FR-004: System MUST display progress indicators showing samples
     * completed/total per page.
     *
     * @param pageId the notebook page ID
     * @return PageProgress containing status counts
     */
    NotebookPageSampleService.PageProgress getPageProgress(Integer pageId);

    /**
     * Get paginated samples for a page with optional status filter.
     *
     * Per FR-040: System MUST display paginated sample grid with configurable page
     * sizes (10, 25, 50, 100).
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @param page   page number (0-indexed)
     * @param size   page size
     * @return list of NotebookPageSample entities
     */
    List<org.openelisglobal.notebook.valueholder.NotebookPageSample> getSamplesPaginated(Integer pageId, Status status,
            int page, int size);

    /**
     * Get total count of samples for a page with optional status filter.
     *
     * @param pageId the notebook page ID
     * @param status optional status filter (null for all)
     * @return total count
     */
    long getSamplesCount(Integer pageId, Status status);

    /**
     * Mark a page as complete after validation. Validates that all required samples
     * have been processed before allowing page completion.
     *
     * @param pageId          the notebook page ID
     * @param userId          the user marking completion
     * @param requireComplete if true, all samples must be COMPLETED or SKIPPED
     * @return true if page was marked complete, false if validation failed
     */
    boolean markPageComplete(Integer pageId, String userId, boolean requireComplete);

    /**
     * Batch size for bulk operations. Per FR-033: Operations process in batches of
     * 50 to prevent timeout.
     */
    int BATCH_SIZE = 50;
}
