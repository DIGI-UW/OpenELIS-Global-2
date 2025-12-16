package org.openelisglobal.medlab.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for MedLab patient order operations. Handles patient-order
 * creation and management for the MedLab workflow.
 */
public interface MedLabPatientOrderService {

    /**
     * Creates a new patient order for the MedLab workflow.
     *
     * @param patientId       the patient ID
     * @param labNo           the lab accession number
     * @param requestDate     the order request date
     * @param receivedDate    the sample received date
     * @param priority        the order priority
     * @param testIds         list of test IDs to include
     * @param notebookEntryId notebook entry ID (optional)
     * @param notebookPageId  notebook page ID (optional)
     * @param sysUserId       the system user ID
     * @return the created order information
     */
    Map<String, Object> createPatientOrder(String patientId, String labNo, String requestDate, String receivedDate,
            String priority, List<String> testIds, Integer notebookEntryId, Integer notebookPageId, String sysUserId);

    /**
     * Gets orders for a notebook page.
     *
     * @param pageId the notebook page ID
     * @return list of orders for the page
     */
    List<Map<String, Object>> getOrdersForPage(Integer pageId);

    /**
     * Gets order details by accession number.
     *
     * @param labNo the lab accession number
     * @return order details
     */
    Map<String, Object> getOrderByLabNo(String labNo);

    /**
     * Gets orders for a notebook entry (across all pages).
     *
     * @param entryId the notebook entry ID
     * @return list of orders for the entry
     */
    List<Map<String, Object>> getOrdersForEntry(Integer entryId);

    /**
     * Records sample collection details for an order.
     *
     * @param labNo          the lab accession number
     * @param sampleTypeId   the sample type ID
     * @param containerType  the container type
     * @param collectionTime the time of collection
     * @param collectionDate the date of collection
     * @param collectorId    the ID of the person who collected the sample
     * @param volume         the sample volume
     * @param notes          collection notes
     * @param notebookPageId the notebook page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordSampleCollection(String labNo, String sampleTypeId, String containerType,
            String collectionTime, String collectionDate, String collectorId, String volume, String notes,
            Integer notebookPageId, String sysUserId);

    /**
     * Gets samples ready for quality check (collected samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for QC
     */
    List<Map<String, Object>> getSamplesForQC(Integer entryId);

    /**
     * Gets collected samples ready for transport packaging verification. Only
     * returns samples that have been collected (completed Sample Collection page).
     *
     * @param entryId the notebook entry ID
     * @return list of collected samples ready for transport packaging
     */
    List<Map<String, Object>> getSamplesForTransport(Integer entryId);

    /**
     * Records a QC decision (accept or reject) for a sample.
     *
     * @param labNo           the lab accession number
     * @param accepted        true if sample accepted, false if rejected
     * @param rejectionReason reason for rejection (if rejected)
     * @param notebookPageId  the QC page ID
     * @param sysUserId       the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordQCDecision(String labNo, boolean accepted, String rejectionReason, Integer notebookPageId,
            String sysUserId);

    /**
     * Records bulk QC decisions (accept or reject) for multiple samples.
     *
     * @param labNumbers      list of lab accession numbers
     * @param accepted        true if samples accepted, false if rejected
     * @param rejectionReason reason for rejection (if rejected)
     * @param notebookPageId  the QC page ID
     * @param sysUserId       the system user ID
     * @return result of the operation with count of processed samples
     */
    Map<String, Object> recordBulkQCDecision(List<String> labNumbers, boolean accepted, String rejectionReason,
            Integer notebookPageId, String sysUserId);

    // ==================== Result Entry Methods ====================

    /**
     * Gets samples ready for result entry (QC-accepted samples with pending tests).
     * Includes units, reference ranges, and flags for each test.
     *
     * @param entryId the notebook entry ID
     * @return list of samples with their tests ready for result entry
     */
    List<Map<String, Object>> getSamplesForResultEntry(Integer entryId);

    /**
     * Saves a test result for a sample with enhanced data.
     *
     * @param labNo          the lab accession number
     * @param testId         the test ID
     * @param resultValue    the result value
     * @param unit           the unit of measure
     * @param entryType      entry type (MANUAL, AUTO_IMPORT, MICROSCOPY, RDT)
     * @param notes          optional notes
     * @param notebookPageId the result entry page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> saveTestResult(String labNo, String testId, String resultValue, String unit, String entryType,
            String notes, Integer notebookPageId, String sysUserId);

    /**
     * Saves multiple test results in bulk (for analyzer import).
     *
     * @param results        list of result data (labNo, testId, resultValue, unit)
     * @param entryType      entry type (AUTO_IMPORT)
     * @param analyzerName   the analyzer name
     * @param runId          the analyzer run ID
     * @param notebookPageId the result entry page ID
     * @param sysUserId      the system user ID
     * @return result of the operation with counts
     */
    Map<String, Object> saveBulkResults(List<Map<String, Object>> results, String entryType, String analyzerName,
            String runId, Integer notebookPageId, String sysUserId);

    /**
     * Marks samples as complete on the result entry page (all results entered).
     *
     * @param sampleIds      list of sample IDs
     * @param notebookPageId the result entry page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> markResultEntryComplete(List<Integer> sampleIds, Integer notebookPageId, String sysUserId);

    // ==================== Result Verification Methods ====================

    /**
     * Gets results pending verification.
     *
     * @param entryId the notebook entry ID
     * @return list of results pending verification
     */
    List<Map<String, Object>> getResultsForVerification(Integer entryId);

    /**
     * Verifies (approves or rejects) a test result.
     *
     * @param labNo          the lab accession number
     * @param testId         the test ID
     * @param approved       true if approved, false if rejected
     * @param comments       verification comments
     * @param notebookPageId the verification page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> verifyResult(String labNo, String testId, boolean approved, String comments,
            Integer notebookPageId, String sysUserId);

    // ==================== Reporting Methods ====================

    /**
     * Gets results ready for reporting (verified results).
     *
     * @param entryId the notebook entry ID
     * @return list of results ready for reporting
     */
    List<Map<String, Object>> getResultsForReporting(Integer entryId);

    /**
     * Marks a result as reported/delivered.
     *
     * @param labNo          the lab accession number
     * @param deliveryMethod the delivery method (email, print, etc.)
     * @param recipient      the recipient information
     * @param notebookPageId the reporting page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> markResultReported(String labNo, String deliveryMethod, String recipient,
            Integer notebookPageId, String sysUserId);

    // ==================== Storage Methods ====================

    /**
     * Gets samples ready for storage assignment (QC-accepted samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for storage
     */
    List<Map<String, Object>> getSamplesForStorage(Integer entryId);

    /**
     * Assigns samples to storage locations.
     *
     * @param sampleIds       list of sample item IDs
     * @param boxId           the storage box ID
     * @param boxLabel        the storage box label
     * @param wellAssignments map of sampleId -> well coordinate
     * @param condition       the storage condition
     * @param retentionYears  retention period in years
     * @param cryovialId      optional cryovial ID
     * @param notebookPageId  the storage page ID
     * @param sysUserId       the system user ID
     * @return result of the operation
     */
    Map<String, Object> assignSamplesToStorage(List<Integer> sampleIds, Integer boxId, String boxLabel,
            Map<Integer, String> wellAssignments, String condition, Integer retentionYears, String cryovialId,
            Integer notebookPageId, String sysUserId);

    /**
     * Records an environmental temperature reading.
     *
     * @param entryId            the notebook entry ID
     * @param deviceId           the storage device ID
     * @param temperatureReading the temperature reading
     * @param readingTime        the time of reading
     * @param readingPeriod      AM or PM
     * @param recordedBy         who recorded the reading
     * @param alarmTriggered     whether an alarm was triggered
     * @param notes              optional notes
     * @param sysUserId          the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordEnvironmentalReading(Integer entryId, Integer deviceId, Double temperatureReading,
            String readingTime, String readingPeriod, String recordedBy, Boolean alarmTriggered, String notes,
            String sysUserId);

    /**
     * Gets environmental readings for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of environmental readings
     */
    List<Map<String, Object>> getEnvironmentalReadings(Integer entryId);

    // ==================== Sample Processing Methods ====================

    /**
     * Gets samples ready for processing (QC-accepted samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for processing
     */
    List<Map<String, Object>> getSamplesForProcessing(Integer entryId);

    /**
     * Records sample processing details.
     *
     * @param sampleIds               list of sample item IDs
     * @param processingType          the processing type (discipline-specific)
     * @param derivedMaterial         the derived material type
     * @param notes                   processing notes
     * @param isBioequivalence        whether this is a bioequivalence study sample
     * @param transferToBioanalytical whether sample should be transferred to
     *                                bioanalytical lab
     * @param notebookPageId          the processing page ID
     * @param sysUserId               the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordProcessing(List<Integer> sampleIds, String processingType, String derivedMaterial,
            String notes, Boolean isBioequivalence, Boolean transferToBioanalytical, Integer notebookPageId,
            String sysUserId);

    /**
     * Creates aliquots (child samples) from parent samples for MedLab workflow.
     *
     * @param parentSampleIds     list of parent sample item IDs
     * @param childCountPerParent number of aliquots per parent
     * @param externalIdPrefix    prefix for external IDs
     * @param containerType       the container type for aliquots
     * @param notebookPageId      the processing page ID
     * @param sysUserId           the system user ID
     * @return result of the operation with created count
     */
    Map<String, Object> createAliquots(List<Integer> parentSampleIds, int childCountPerParent, String externalIdPrefix,
            String containerType, Integer notebookPageId, String sysUserId);

    // ==================== Testing & Analyzer Methods ====================

    /**
     * Gets samples ready for testing (QC-accepted samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for testing with their tests
     */
    List<Map<String, Object>> getSamplesForTesting(Integer entryId);

    /**
     * Records test execution details for samples.
     *
     * @param sampleIds         list of sample item IDs
     * @param analyzerId        the analyzer ID (optional for manual)
     * @param analyzerName      the analyzer name
     * @param worklistGenerated whether a worklist was generated
     * @param isManualTest      whether this is manual testing
     * @param technologyUsed    the technology used for testing
     * @param notebookPageId    the testing page ID
     * @param sysUserId         the system user ID
     * @return result of the operation
     */
    Map<String, Object> executeTests(List<Integer> sampleIds, String analyzerId, String analyzerName,
            Boolean worklistGenerated, Boolean isManualTest, String technologyUsed, Integer notebookPageId,
            String sysUserId);

    /**
     * Assigns tests to samples that don't have Analysis records. Creates Analysis
     * records for each sample × test combination.
     *
     * @param sampleItemIds  list of sample item IDs to assign tests to
     * @param testIds        list of test IDs to assign
     * @param notebookPageId the testing page ID (optional)
     * @param sysUserId      the system user ID
     * @return result of the operation with count of Analysis records created
     */
    Map<String, Object> assignTestsToSamples(List<Integer> sampleItemIds, List<String> testIds, Integer notebookPageId,
            String sysUserId);

    /**
     * Records a QC record for an analyzer.
     *
     * @param analyzerId        the analyzer ID (optional)
     * @param analyzerName      the analyzer name
     * @param qcType            the QC type (daily, calibration, eqa)
     * @param qcLevel           the control level (normal, pathologic-low,
     *                          pathologic-high)
     * @param qcResult          pass or fail
     * @param calibrationStatus the calibration status
     * @param notes             QC notes
     * @param notebookPageId    the testing page ID
     * @param sysUserId         the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordQc(String analyzerId, String analyzerName, String qcType, String qcLevel, String qcResult,
            String calibrationStatus, String notes, Integer notebookPageId, String sysUserId);

    /**
     * Gets QC records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of QC records
     */
    List<Map<String, Object>> getQcRecords(Integer entryId);

    /**
     * Records a deviation/error for samples.
     *
     * @param sampleIds         list of sample item IDs
     * @param deviationType     the type of deviation
     * @param actionTaken       the action taken
     * @param rootCauseAnalysis root cause analysis description
     * @param notes             additional notes
     * @param notebookPageId    the testing page ID
     * @param sysUserId         the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordDeviation(List<Integer> sampleIds, String deviationType, String actionTaken,
            String rootCauseAnalysis, String notes, Integer notebookPageId, String sysUserId);

    /**
     * Gets deviations for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of deviations
     */
    List<Map<String, Object>> getDeviations(Integer entryId);

    // ==================== Performance Monitoring Methods ====================

    /**
     * Gets performance dashboard data for a notebook entry. Includes QC trends,
     * sample acceptance rates, TAT, and equipment usage.
     *
     * @param entryId the notebook entry ID
     * @return performance metrics and dashboard data
     */
    Map<String, Object> getPerformanceDashboard(Integer entryId);

    /**
     * Gets turnaround time (TAT) statistics for samples.
     *
     * @param entryId the notebook entry ID
     * @return TAT statistics by phase
     */
    Map<String, Object> getTurnaroundTimeStats(Integer entryId);

    /**
     * Gets sample acceptance/rejection statistics.
     *
     * @param entryId the notebook entry ID
     * @return acceptance rate statistics
     */
    Map<String, Object> getSampleAcceptanceStats(Integer entryId);

    /**
     * Gets QC performance trends over time.
     *
     * @param entryId the notebook entry ID
     * @return QC trend data for charting
     */
    List<Map<String, Object>> getQcPerformanceTrends(Integer entryId);

    /**
     * Gets equipment usage statistics.
     *
     * @param entryId the notebook entry ID
     * @return equipment usage by analyzer
     */
    List<Map<String, Object>> getEquipmentUsageStats(Integer entryId);

    /**
     * Gets sample utilization report data.
     *
     * @param entryId the notebook entry ID
     * @return sample utilization data
     */
    Map<String, Object> getSampleUtilizationReport(Integer entryId);

    /**
     * Modifies reference range for a test (supervisor action).
     *
     * @param testId         the test ID
     * @param lowNormal      new low normal value
     * @param highNormal     new high normal value
     * @param lowCritical    new low critical value (optional)
     * @param highCritical   new high critical value (optional)
     * @param notebookPageId the page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> modifyReferenceRange(String testId, Double lowNormal, Double highNormal, Double lowCritical,
            Double highCritical, Integer notebookPageId, String sysUserId);

    /**
     * Gets corrective actions log for deviations.
     *
     * @param entryId the notebook entry ID
     * @return list of corrective actions
     */
    List<Map<String, Object>> getCorrectiveActionsLog(Integer entryId);

    /**
     * Marks verification page as complete for all samples.
     *
     * @param sampleIds      list of sample IDs
     * @param notebookPageId the verification page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> markVerificationComplete(List<Integer> sampleIds, Integer notebookPageId, String sysUserId);

    // ==================== Disposal & Archiving Methods ====================

    /**
     * Gets samples ready for disposal (reported samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for disposal with their status
     */
    List<Map<String, Object>> getSamplesForDisposal(Integer entryId);

    /**
     * Records sample disposal.
     *
     * @param sampleIds         list of sample item IDs
     * @param disposalReason    the reason for disposal (expiry, exhaustion,
     *                          qc_fail)
     * @param disposalMethod    the disposal method (incineration, autoclaving,
     *                          chemical_treatment)
     * @param disposalDate      the disposal date
     * @param responsiblePerson the person responsible for disposal
     * @param facilityDetails   licensed disposal facility details
     * @param notes             additional notes
     * @param notebookPageId    the disposal page ID
     * @param sysUserId         the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordDisposal(List<Integer> sampleIds, String disposalReason, String disposalMethod,
            String disposalDate, String responsiblePerson, String facilityDetails, String notes, Integer notebookPageId,
            String sysUserId);

    /**
     * Gets disposal records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of disposal records
     */
    List<Map<String, Object>> getDisposalRecords(Integer entryId);

    /**
     * Records sample archiving for biobank transfer.
     *
     * @param sampleIds         list of sample item IDs
     * @param retentionYears    retention period in years
     * @param storageCondition  storage condition (e.g., -80°C)
     * @param transferToBiobank whether to transfer to biobank
     * @param biobankDetails    biobank details if transferring
     * @param notes             additional notes
     * @param notebookPageId    the archiving page ID
     * @param sysUserId         the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordArchiving(List<Integer> sampleIds, Integer retentionYears, String storageCondition,
            Boolean transferToBiobank, String biobankDetails, String notes, Integer notebookPageId, String sysUserId);

    /**
     * Gets archiving records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of archiving records
     */
    List<Map<String, Object>> getArchivingRecords(Integer entryId);

    /**
     * Gets disposal/archiving summary for dashboard.
     *
     * @param entryId the notebook entry ID
     * @return summary statistics
     */
    Map<String, Object> getDisposalArchivingSummary(Integer entryId);

    // ==================== Accreditation Support Methods ====================

    /**
     * Gets audit trail for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of audit events
     */
    List<Map<String, Object>> getAuditTrail(Integer entryId);

    /**
     * Gets SOP compliance status for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return SOP compliance data with linked procedures
     */
    Map<String, Object> getSopComplianceStatus(Integer entryId);

    /**
     * Records SOP completion for a page.
     *
     * @param sopId          the SOP ID
     * @param completedBy    who completed the SOP check
     * @param completionDate completion date
     * @param notes          any notes
     * @param notebookPageId the page ID
     * @param sysUserId      the system user ID
     * @return result of the operation
     */
    Map<String, Object> recordSopCompletion(String sopId, String completedBy, String completionDate, String notes,
            Integer notebookPageId, String sysUserId);

    /**
     * Finalizes a notebook entry (prevents further modification).
     *
     * @param entryId   the notebook entry ID
     * @param sysUserId the system user ID
     * @return result of the operation
     */
    Map<String, Object> finalizeNotebookEntry(Integer entryId, String sysUserId);
}
