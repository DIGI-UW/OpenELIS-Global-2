package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * Service interface for bulk operations on notebook page samples. Supports bulk
 * data entry, status updates, and batch processing for efficient handling of
 * 200+ samples per workflow.
 *
 * <p>
 * Per FR-033: Operations process in batches of 50 to prevent timeout.
 */
public interface NotebookBulkOperationService {

    /**
     * Apply common values to multiple samples on a page. Updates the JSONB data
     * field for each sample, merging with existing data.
     *
     * <p>
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
     * Update status for multiple samples on a page with control over T150
     * auto-routing.
     *
     * @param pageId          the notebook page ID
     * @param sampleIds       list of sample item IDs to update
     * @param status          the new status
     * @param userId          the user performing the update
     * @param skipAutoRouting if true, skip T150 auto-routing (frontend handles
     *                        routing)
     * @return number of samples updated
     */
    int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId,
            boolean skipAutoRouting);

    /**
     * Update status for multiple samples on a page with optional pathway-based routing.
     * Supports generic pathway routing where samples can be routed to different pages
     * based on their pathway selection stored in JSONB data.
     *
     * <p>
     * When pathwayRouting is enabled, each sample's pathway selection (analyticalPathwayId)
     * determines its target page:
     * - Samples with "path_a" route to the next page after sourcePage
     * - Samples with "path_b" route directly to targetPage
     *
     * @param pageId           the notebook page ID
     * @param sampleIds        list of sample item IDs to update
     * @param status           the new status
     * @param userId           the user performing the update
     * @param pathwayRouting   if true, apply pathway-based routing logic
     * @param sourcePageName   the source page name (used to find next page for path_a)
     * @param targetPageName   the target page name (destination for path_b samples)
     * @return number of samples updated
     */
    int bulkUpdateStatusWithPathwayRouting(Integer pageId, List<Integer> sampleIds, Status status,
            String userId, Boolean pathwayRouting, String sourcePageName, String targetPageName);

    /**
     * Update status for multiple samples on a page using String IDs. Supports
     * composite sample IDs (e.g., "123_cassette_0") used in pathology workflow
     * pages where samples are expanded from parent items.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs as Strings to update
     * @param status    the new status
     * @param userId    the user performing the update
     * @return number of samples updated
     */
    int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status, String userId);

    /**
     * Update status for multiple samples on a page using String IDs with optional
     * pathway-based routing. Supports composite sample IDs (e.g., "123_cassette_0")
     * used in pathology workflow pages where samples are expanded from parent items.
     *
     * @param pageId           the notebook page ID
     * @param sampleIds        list of sample item IDs as Strings to update
     * @param status           the new status
     * @param userId           the user performing the update
     * @param pathwayRouting   if true, apply pathway-based routing logic
     * @param sourcePageName   the source page name (used to find next page for path_a)
     * @param targetPageName   the target page name (destination for path_b samples)
     * @return number of samples updated
     */
    int bulkUpdateStatusStringWithPathwayRouting(Integer pageId, List<String> sampleIds, Status status,
            String userId, Boolean pathwayRouting, String sourcePageName, String targetPageName);

    /**
     * Apply common values to multiple samples on a page using String IDs. Updates
     * the JSONB data field for each sample, merging with existing data. Supports
     * composite sample IDs (e.g., "123_block_0") used in pathology workflow pages
     * where samples are expanded from parent items.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample item IDs as Strings to update
     * @param data      map of field names to values (stored in JSONB)
     * @param userId    the user performing the update
     * @return number of samples updated
     */
    int bulkApplyValuesString(Integer pageId, List<String> sampleIds, Map<String, Object> data, String userId);

    /**
     * Get progress information for a notebook page. Delegates to
     * NotebookPageSampleService.getPageProgress.
     *
     * <p>
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
     * <p>
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

    /**
     * Assign samples to storage location and persist to storage system. This
     * method: 1. Validates that target wells are not already occupied 2. Creates
     * SampleStorageAssignment records in the storage system 3. Updates notebook
     * page sample data with storage info
     *
     * @param pageId         the notebook page ID
     * @param sampleIds      list of sample IDs to assign
     * @param boxId          the storage box ID
     * @param wellCoordinate the well coordinate (for single assignment)
     * @param storageData    additional storage metadata (storageType, assignedBy,
     *                       etc.)
     * @param userId         the user performing the assignment
     * @return map containing assignedCount and any errors
     */
    java.util.Map<String, Object> assignSamplesToStorage(Integer pageId, List<Integer> sampleIds, Integer boxId,
            String wellCoordinate, Map<String, Object> storageData, String userId);

    /**
     * Auto-assign samples to next available wells in a storage box. This method: 1.
     * Finds next available wells in the box (not occupied) 2. Creates
     * SampleStorageAssignment records for each sample 3. Updates notebook page
     * sample data with storage info
     *
     * @param pageId        the notebook page ID
     * @param sampleIds     list of sample IDs to auto-assign
     * @param boxId         the storage box ID
     * @param rows          number of rows in the box (default 8)
     * @param columns       number of columns in the box (default 12)
     * @param occupiedWells list of already occupied well coordinates
     * @param storageData   additional storage metadata
     * @param userId        the user performing the assignment
     * @return map containing assignments array and any errors
     */
    java.util.Map<String, Object> autoAssignSamplesToStorage(Integer pageId, List<Integer> sampleIds, Integer boxId,
            Integer rows, Integer columns, List<String> occupiedWells, Map<String, Object> storageData, String userId);

    /**
     * Auto-assign samples to storage boxes using string sample IDs. This version
     * supports composite sample IDs (e.g., "4_cassette_0_block_0") used in
     * pathology workflows.
     *
     * @param pageId        the notebook page ID
     * @param sampleIds     list of sample IDs as strings
     * @param boxId         the storage box ID
     * @param rows          number of rows in the box (default 8)
     * @param columns       number of columns in the box (default 12)
     * @param occupiedWells list of already occupied well coordinates
     * @param storageData   additional storage metadata
     * @param userId        the user performing the assignment
     * @return map containing assignments array and any errors
     */
    java.util.Map<String, Object> autoAssignSamplesToStorageString(Integer pageId, List<String> sampleIds,
            Integer boxId, Integer rows, Integer columns, List<String> occupiedWells, Map<String, Object> storageData,
            String userId);

    /**
     * Assign samples to storage using a well assignments map where each sample has
     * its own well coordinate. This method: 1. Validates that target wells are not
     * already occupied 2. Creates SampleStorageAssignment records in the storage
     * system 3. Updates notebook page sample data with storage info
     *
     * @param pageId          the notebook page ID
     * @param sampleIds       list of sample IDs to assign
     * @param boxId           the storage box ID
     * @param wellAssignments map of sampleId (as string) to wellCoordinate
     * @param storageData     additional storage metadata
     * @param userId          the user performing the assignment
     * @return map containing assignedCount and any errors
     */
    java.util.Map<String, Object> assignSamplesToStorageWithWellMap(Integer pageId, List<Integer> sampleIds,
            Integer boxId, Map<String, String> wellAssignments, Map<String, Object> storageData, String userId);

    /**
     * Generate a report for samples on a page.
     *
     * @param pageId       the notebook page ID
     * @param sampleIds    list of sample IDs to include in report
     * @param reportType   type of report (SUMMARY, DETAILED, QC, STATISTICAL,
     *                     AUDIT, CUSTOM)
     * @param reportFormat output format (PDF, EXCEL, CSV, JSON)
     * @param userId       the user generating the report
     * @return byte array containing the report content
     */
    byte[] generateReport(Integer pageId, List<Integer> sampleIds, String reportType, String reportFormat,
            String userId);

    /**
     * Generate a REDCap-compatible CSV export for samples.
     *
     * @param pageId         the notebook page ID
     * @param sampleIds      list of sample IDs to export
     * @param recordIdField  the REDCap record ID field name
     * @param eventName      optional event name for longitudinal projects
     * @param instrumentName optional instrument name
     * @param userId         the user generating the export
     * @return byte array containing the CSV content
     */
    byte[] generateREDCapExport(Integer pageId, List<Integer> sampleIds, String recordIdField, String eventName,
            String instrumentName, String userId);

    /**
     * Generate a bioanalytical-specific REDCap export for bioequivalence studies.
     * Includes analytical method, sample type, bioequivalence statistics,
     * calibration data, Westgard QC status, and regulatory compliance data.
     *
     * @param pageId        the notebook page ID
     * @param sampleIds     list of NotebookPageSample IDs to export
     * @param recordIdField the REDCap record ID field name (defaults to
     *                      "record_id")
     * @param eventName     optional event name for longitudinal projects
     * @return byte array containing the CSV content in REDCap format
     */
    byte[] generateBioanalyticalREDCapExport(Integer pageId, List<Integer> sampleIds, String recordIdField,
            String eventName);

    /**
     * Parse CSV raw data file and apply results to samples by matching externalId
     * (primary) or accessionNumber (fallback). The CSV must contain a header row
     * with column names that map to sample data fields.
     *
     * Matching Priority: 1. externalId - primary matching key from
     * SampleItem.externalId 2. accessionNumber - fallback if externalId doesn't
     * match
     *
     * Supported columns: externalId, accessionNumber, testResult, ctValue,
     * concentration, absorbance, runId, kitLot, operator, machineType,
     * runCompleted, runIssues, notes
     *
     * @param pageId      the notebook page ID
     * @param csvContent  the CSV file content as bytes
     * @param machineType the machine type (from upload metadata)
     * @param runId       the run ID (from upload metadata)
     * @param userId      the user performing the upload
     * @return map containing matchedCount, updatedCount, unmatchedRows, and errors
     */
    Map<String, Object> parseAndApplyCsvResults(Integer pageId, byte[] csvContent, String machineType, String runId,
            String userId);
}
