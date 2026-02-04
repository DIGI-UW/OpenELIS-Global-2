package org.openelisglobal.notebook.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/** DAO interface for NotebookPageSample entity operations. */
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
     * Bulk update status for multiple samples on a page using String IDs. Supports
     * composite sample IDs (e.g., "123_cassette_0") used in pathology workflow
     * pages where samples are expanded from parent items.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs as Strings
     * @param status    the new status
     * @return number of records updated
     */
    int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status);

    /**
     * Clear destinationType from the JSONB data field for multiple samples on a
     * page. This makes samples available for re-routing.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs
     * @return number of records updated
     */
    int clearDestinationType(Integer pageId, List<Integer> sampleIds);

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

    /**
     * Get occupied well coordinates and sample info for a specific box from
     * archived pathology samples. Queries the JSONB data field for boxId and
     * wellCoordinate.
     *
     * @param boxId the storage box ID
     * @return map of well coordinate to sample info (sampleItemId, externalId)
     */
    java.util.Map<String, java.util.Map<String, String>> getOccupiedWellsByBoxId(Integer boxId);

    /**
     * Check if a patient encounter ID already exists within a notebook. Queries the
     * JSONB data field for patientEncounterId across all pages in the notebook.
     *
     * @param notebookId         the notebook ID to check within
     * @param patientEncounterId the patient encounter ID to check
     * @return true if the ID exists in any sample's data within the notebook, false
     *         otherwise
     */
    boolean existsByPatientEncounterIdInNotebook(Integer notebookId, String patientEncounterId);
}
