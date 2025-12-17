package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.AnalyticsManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for Analytics Laboratory workflow operations.
 * Handles all five pages of the Analytics workflow:
 *
 * Page 1: Sample Creation & Full Metadata Capture
 * Page 2: Test Assignment & Preparation
 * Page 3: Analysis / Test Execution
 * Page 4: Result Review, Reporting & Release
 * Page 5: Post-Test Sample & Data Handling
 */
public interface AnalyticsWorkflowService {

    // ========================================
    // Common Records and Types
    // ========================================

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Analytics sample categories.
     */
    enum SampleCategory {
        BIOANALYTICAL, PHARMACEUTICAL
    }

    /**
     * Analytics test types.
     */
    enum AnalyticsTestType {
        ASSAY, DISSOLUTION, DISINTEGRATION, FRIABILITY, HARDNESS, IDENTITY_TEST, BIOANALYTICAL_QUANTIFICATION
    }

    /**
     * Analytical methodologies.
     */
    enum AnalyticalMethodology {
        HPLC, UV_VIS, LC_MS_MS, DISSOLUTION_USP_I, DISSOLUTION_USP_II, HARDNESS_TEST, FRIABILITY_TEST,
        DISINTEGRATION_TEST, IDENTITY_TEST
    }

    /**
     * Analyst roles.
     */
    enum AnalystRole {
        CHEMICAL_ANALYST, PHARMACIST, RESEARCHER
    }

    // ========================================
    // Page 1: Sample Creation & Full Metadata Capture
    // ========================================

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<AnalyticsManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the Analytics manifest CSV with all Analytics-specific
     * fields.
     */
    record AnalyticsManifestRow(int rowNumber, String sampleIdentifier, String barcode, String sampleCategory,
            String sampleType, String sampleSource, String requestingUnit, String requestedTests, String studyProjectId,
            String storageCondition, String receivedDateTime, String receivedBy) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record AnalyticsImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<String> createdAccessionNumbers, List<ParseError> errors) {
    }

    /**
     * Parse an Analytics manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the Analytics column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, AnalyticsManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Create SampleItem records from an Analytics manifest for a notebook entry.
     * Analytics-specific data is stored in sample metadata.
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples, accession numbers, and any errors
     */
    AnalyticsImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Create a single Analytics sample with full metadata.
     *
     * @param entryId          the notebook entry ID
     * @param sampleIdentifier the sample identifier
     * @param barcode          barcode/QR code (optional)
     * @param sampleCategory   BIOANALYTICAL or PHARMACEUTICAL
     * @param sampleType       sample type (API, Tablet, etc.)
     * @param sampleSource     sample source
     * @param requestingUnit   requesting unit/client
     * @param requestedTests   comma-separated test types
     * @param studyProjectId   study/project ID (optional)
     * @param storageCondition storage condition
     * @param receivedBy       person who received the sample
     * @param sysUserId        the user creating the sample
     * @return the created SampleItem
     */
    SampleItem createSample(Integer entryId, String sampleIdentifier, String barcode, String sampleCategory,
            String sampleType, String sampleSource, String requestingUnit, String requestedTests, String studyProjectId,
            String storageCondition, String receivedBy, String sysUserId);

    // ========================================
    // Page 2: Test Assignment & Preparation
    // ========================================

    /**
     * Assignment data for analyst/method assignment.
     */
    record TestAssignment(String analystRole, String analystName, String assignmentDate,
            List<String> analyticalMethodology) {
    }

    /**
     * Assign analyst and methods to samples.
     *
     * @param pageId     the notebook page ID
     * @param sampleIds  list of sample IDs to assign
     * @param assignment the assignment data
     * @param userId     user performing the assignment
     * @return number of samples assigned
     */
    int assignTestsToSamples(Integer pageId, List<Integer> sampleIds, TestAssignment assignment, String userId);

    /**
     * Get test assignment for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return assignment data or null if not assigned
     */
    TestAssignment getTestAssignment(Integer pageId, Integer sampleItemId);

    // ========================================
    // Page 3: Analysis / Test Execution
    // ========================================

    /**
     * Test execution data.
     */
    record TestExecutionData(String testType, String instrumentId, String analystName, String runDate,
            Map<String, Object> instrumentParameters, Map<String, Object> rawResults, String notes) {
    }

    /**
     * Record test execution results for a sample.
     *
     * @param pageId        the notebook page ID
     * @param sampleItemId  the sample item ID
     * @param executionData the test execution data
     * @param userId        user recording the results
     * @return true if successfully recorded
     */
    boolean recordTestExecution(Integer pageId, Integer sampleItemId, TestExecutionData executionData, String userId);

    /**
     * Bulk record test execution for multiple samples (same test parameters).
     *
     * @param pageId        the notebook page ID
     * @param sampleIds     list of sample IDs
     * @param executionData the test execution data
     * @param userId        user recording the results
     * @return number of samples updated
     */
    int bulkRecordTestExecution(Integer pageId, List<Integer> sampleIds, TestExecutionData executionData, String userId);

    /**
     * Get test execution data for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return execution data or null if not executed
     */
    TestExecutionData getTestExecution(Integer pageId, Integer sampleItemId);

    // ========================================
    // Page 4: Result Review, Reporting & Release
    // ========================================

    /**
     * Result review data.
     */
    record ResultReview(String complianceStatus, String reviewerName, String reviewDate, String reviewNotes,
            List<String> releaseRecipients, boolean resultsReleased) {
    }

    /**
     * Review and validate test results for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @param review       the review data
     * @param userId       user performing the review
     * @return true if successfully reviewed
     */
    boolean reviewResults(Integer pageId, Integer sampleItemId, ResultReview review, String userId);

    /**
     * Bulk review results for multiple samples.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs
     * @param review    the review data
     * @param userId    user performing the review
     * @return number of samples reviewed
     */
    int bulkReviewResults(Integer pageId, List<Integer> sampleIds, ResultReview review, String userId);

    /**
     * Release results to specified recipients.
     *
     * @param pageId     the notebook page ID
     * @param sampleIds  list of sample IDs to release
     * @param recipients list of recipients (e.g., "Requesting unit", "Researcher",
     *                   "Client")
     * @param userId     user releasing the results
     * @return number of samples released
     */
    int releaseResults(Integer pageId, List<Integer> sampleIds, List<String> recipients, String userId);

    /**
     * Get review status for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return review data or null if not reviewed
     */
    ResultReview getResultReview(Integer pageId, Integer sampleItemId);

    // ========================================
    // Page 5: Post-Test Sample & Data Handling
    // ========================================

    /**
     * Sample retention data.
     */
    record SampleRetention(String retentionReason, Integer retentionYears, String biorepositoryTransferStatus,
            String transferDate, String transferNotes) {
    }

    /**
     * Data archiving data.
     */
    record DataArchiving(boolean rawDataArchived, boolean processedResultsArchived, boolean metadataExported,
            String archiveLocation, String archiveDate) {
    }

    /**
     * Set sample retention for post-test handling.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @param retention    the retention data
     * @param userId       user setting the retention
     * @return true if successfully set
     */
    boolean setSampleRetention(Integer pageId, Integer sampleItemId, SampleRetention retention, String userId);

    /**
     * Bulk set retention for multiple samples.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs
     * @param retention the retention data
     * @param userId    user setting the retention
     * @return number of samples updated
     */
    int bulkSetSampleRetention(Integer pageId, List<Integer> sampleIds, SampleRetention retention, String userId);

    /**
     * Archive data for samples.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs
     * @param archiving the archiving data
     * @param userId    user performing the archiving
     * @return number of samples archived
     */
    int archiveData(Integer pageId, List<Integer> sampleIds, DataArchiving archiving, String userId);

    /**
     * Finalize sample lifecycle (mark as completed in workflow).
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to finalize
     * @param userId    user finalizing the samples
     * @return number of samples finalized
     */
    int finalizeSampleLifecycle(Integer pageId, List<Integer> sampleIds, String userId);

    /**
     * Get retention data for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return retention data or null if not set
     */
    SampleRetention getSampleRetention(Integer pageId, Integer sampleItemId);

    /**
     * Get archiving data for a sample.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @return archiving data or null if not archived
     */
    DataArchiving getDataArchiving(Integer pageId, Integer sampleItemId);
}
