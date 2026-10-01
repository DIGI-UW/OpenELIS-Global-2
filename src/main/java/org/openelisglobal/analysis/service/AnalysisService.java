package org.openelisglobal.analysis.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalysisService extends BaseObjectService<Analysis, String> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getData(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Analysis getAnalysisById(String analysisId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByTestDescriptionAndCompletedDateRange(List<String> descriptions, Date sqlDayOne,
            Date sqlDayTwo);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(Sample sample);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionAnalysesReadyForReportPreviewBySample(List<String> accessionNumbers);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionPendingAnalysesReadyToBeReportedBySample(Sample sample);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleIdExcludedByStatusId(String id, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisStartedOrCompletedInDateRange(Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(Date lowDate, Date highDate, String testId,
            List<String> testScectionIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestSectionAndStatusExcludingQc(String testSectionId,
            List<String> analysisStatusList, List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<String> statusIdList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    /**
     * OGC-189 (M2): ids of the lab units (test sections) that still hold in-flight
     * work — at least one analysis not yet Finalized, Canceled or rejected.
     *
     * <p>
     * Drives the "isActive OR hasContent" rule for viewer controls, so a
     * deactivated unit stays on worklists until its pending analyses are finished
     * and then drops out by itself. Never used to gate order entry — choosers
     * filter on {@code isActive} alone.
     */
    Set<String> getTestSectionIdsWithPendingAnalyses();

    /**
     * OGC-189: ids of the lab units that hold <em>any</em> analysis, whatever its
     * status — including finalized, canceled and rejected.
     *
     * <p>
     * This is the "hasContent" half of the viewer rule. It deliberately counts
     * completed work: the guardrail requires that "ALL tests should be able to be
     * completed, <b>and the historical data viewed</b>, regardless of these
     * settings" (comment 37313 §2). Counting only pending analyses meant a
     * finalized result in a deactivated unit became invisible on the results pages
     * and unreachable for reporting the moment it was entered — the data was there,
     * and nobody could retrieve it.
     *
     * <p>
     * Consequence, accepted deliberately: a unit that has ever processed work stays
     * in viewer lists for good. Its results are permanent records, so that is the
     * correct trade against a shorter dropdown.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Set<String> getTestSectionIdsWithAnyAnalyses();

    /**
     * OGC-189 (M3): analysis counts for a lab unit's deactivation impact summary.
     * Index 0 = pending (still in flight), index 1 = historical.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    long[] countAnalysesForLabUnit(String testSectionId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionAnalysesBySampleIncludeCanceled(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByTestNamesAndCompletedDateRange(List<String> testNames, Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleIdTestIdAndStatusId(List<String> sampleIdList, List<String> testIdList,
            List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionParentTestAnalysesBySample(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleItemsExcludingByStatusIds(SampleItem sampleItem, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisStartedOnRangeByStatusId(Date lowDate, Date highDate, String statusID);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getRevisionHistoryOfAnalysesBySample(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisCollectedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Analysis getPreviousAnalysisForAmendedAnalysis(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestSectionAndExcludedStatus(String testSectionId, List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleStatusIdExcludingByStatusId(String statusId, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleItemIdAndStatusId(String sampleItemId, String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByTestSectionAndCompletedDateRange(String sectionID, Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionAnalysesReadyToBeReported();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getMaxRevisionAnalysisBySampleAndTest(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestAndExcludedStatus(String testId, List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesAlreadyReportedBySample(Sample sample);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getRevisionHistoryOfAnalysesBySampleAndTest(SampleItem sampleItem, Test test,
            boolean includeLatestRevision);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleStatusId(String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisEnteredAfterDate(Timestamp latestCollectionDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleIdAndStatusId(String id, Set<String> analysisStatusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesByPriorityAndStatusId(OrderPriority priority, List<String> analysisStatusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisStartedOn(Date collectionDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getMaxRevisionAnalysesBySample(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllChildAnalysesByResult(Result result);

    /**
     * The analyses on a sample, i.e. which tests were ordered on it, not their
     * results. Same rationale as getPendingAnalysesForWorkplan above: the order
     * path needs it. SamplePatientEntryServiceImpl#getTestNamesWithRangeNotApplied
     * calls it while assembling the save response, reading only each analysis's
     * test id and sample-item type to warn which ordered tests have no applicable
     * reference range. Under PRIV_RESULT_VIEW alone that denied Reception, and
     * because the read runs AFTER the order has already committed, the save
     * succeeded and the caller still got a 403.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_ORDER_VIEW')")
    List<Analysis> getAnalysesBySampleId(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesReadyToBeReported();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisBySampleAndTestIds(String sampleKey, List<String> testIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisCompleteInRange(Timestamp lowDate, Timestamp highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Object[]> getAffectedSampleItemIdsByAnalyzerAndTestCompletedInRange(String analyzerId, String testId,
            Timestamp lowDate, Timestamp highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean existsAnalysisCompletedBeforeByAnalyzerAndTest(String analyzerId, String testId, Timestamp before);

    /**
     * Lab-unit-keyed affected-analysis window for bench controls (OGC-1147).
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Object[]> getAffectedSampleItemIdsByTestSectionAndTestCompletedInRange(String testSectionId, String testId,
            Timestamp lowDate, Timestamp highDate);

    /** Lab-unit-keyed counterpart used for cap-reason accuracy (OGC-1147). */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean existsAnalysisCompletedBeforeByTestSectionAndTest(String testSectionId, String testId, Timestamp before);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesForStatusId(String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesForStatusIdExcludingQc(String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getCollectedAnalysesForStatusIdExcludingQc(String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysesForStatusIds(List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysesForStatusIdsExcludingQc(List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfCollectedAnalysesForStatusIdsExcludingQc(List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllMaxRevisionAnalysesPerTest(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByAccessionAndTestId(String accessionNumber, String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisCollectedOn(Date collectionDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestAndStatus(String testId, List<String> statusIdList);

    /**
     * The analyses on a specimen, i.e. which tests were ordered on it, not their
     * results. Order-facing screens need this to show what an order contains: the
     * order dashboard reads it for each row's progress, and vector fan-out re-keys
     * these rows onto the pool. Gating it on PRIV_RESULT_VIEW denied the dashboard
     * to Reception outright, so it also accepts PRIV_ORDER_VIEW. Reading the actual
     * RESULT values stays on PRIV_RESULT_VIEW throughout the rest of this
     * interface.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_ORDER_VIEW')")
    List<Analysis> getPendingAnalysesForWorkplan(List<String> statusIdList, List<String> testIdList,
            Collection<String> excludedAnalysisIds, int maxResults);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesByIdsWithDetails(List<String> analysisIds);

    /**
     * The analyses on one sample item, i.e. which tests were ordered on it. Same
     * rationale as getAnalysesBySampleId: the order dashboard calls this for every
     * item of every row to work out each order's step state (it only asks whether
     * the list is empty), to list the ordered tests and their panels, and to see
     * whether any analysis has a referral. None of those reads a result value, so
     * it also accepts PRIV_ORDER_VIEW; on PRIV_RESULT_VIEW alone the whole clinical
     * dashboard denied for Reception.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_ORDER_VIEW')")
    List<Analysis> getAnalysesBySampleItem(SampleItem sampleItem);

    /**
     * The analyses on a vector pool, read by the order dashboard to size the pool
     * and list its members' tests. Same order-facing read as
     * getAnalysesBySampleItem above, so it takes the same pair of authorities.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_ORDER_VIEW')")
    List<Analysis> getAnalysesByVectorPoolId(String vectorPoolId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIdList, List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    Analysis buildAnalysis(Test test, SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    void updateAnalysises(List<Analysis> cancelAnalysis, List<Analysis> newAnalysis, String sysUserId);

    /**
     * Clears the "corrected since patient report" flag once a report has printed
     * the correction. Report bookkeeping, not result entry: {@code PatientReport}
     * is its only caller in the codebase, it writes no result value, and it
     * deliberately skips the audit trail because nothing clinically meaningful
     * changed. Gating it on PRIV_RESULT_ENTER denied the Reports role - whose job
     * is running that very report - at the end of generating it, after the PDF had
     * been assembled.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_ENTER','PRIV_REPORT_RUN')")
    void updateAllNoAuditTrail(List<Analysis> updatedAnalysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    void updateNoAuditTrail(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getTestDisplayName(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getCompletedDateForDisplay(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getAnalysisType(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getJSONMultiSelectResults(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getCSVMultiselectResults(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Result getQuantifiedResult(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getStatusId(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Boolean getTriggeredReflex(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean resultIsConclusion(Result currentResult, Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean isParentNonConforming(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTest(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Result> getResults(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean hasBeenCorrectedSinceLastPatientReport(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean patientReportHasBeenDone(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getNotesAsString(Analysis analysis, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            boolean excludeExternPrefix);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getOrderAccessionNumber(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TypeOfSample getTypeOfSample(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Panel getPanel(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    /**
     * The analysis's own section when one is assigned, else the test's home
     * section. Null only when neither is known.
     */
    TestSection getTestSection(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> list, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> get(List<String> value);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestsAndStatusAndCompletedDateRange(List<String> nfsTestIdList,
            List<String> analysisStatusList, List<String> sampleStatusList, Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByTestSectionAndStatusExcludingQc(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisByTestSectionAndStatus(String sectionId, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisByTestSectionAndStatusExcludingQc(String sectionId, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisAtAccessionNumberAndStatus(String accessionNumber, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisAtAccessionNumberAndStatusExcludingQc(String accessionNumber, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByTestSectionAndStatus(String sectionId, List<String> statusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByTestSectionAndStatusExcludingQc(String sectionId, List<String> statusList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber, String upperRangeAccessionNumber, boolean doRange, boolean finished);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getStudyAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesCompletedOnByStatusId(Date completedDate, String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesResultEnteredOnExcludedByStatusId(Date completedDate, Set<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisCompletedOnByStatusId(Date completedDate, List<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisStartedOnByStatusId(Date startedDate, List<String> statusIds);

    /**
     * Test-section-scoped counterpart of
     * {@link #getCountOfAnalysesForStatusIdsExcludingQc(List)}. Returns 0 for an
     * empty section list.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysesForStatusIdsAndTestSectionsExcludingQc(List<String> statusIdList,
            List<String> testSectionIds);

    /**
     * Test-section-scoped counterpart of
     * {@link #getCountOfCollectedAnalysesForStatusIdsExcludingQc(List)}. Returns 0
     * for an empty section list.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfCollectedAnalysesForStatusIdsAndTestSectionsExcludingQc(List<String> statusIdList,
            List<String> testSectionIds);

    /**
     * Test-section-scoped counterpart of
     * {@link #getCountOfAnalysisCompletedOnByStatusId(Date, List)}. Returns 0 for
     * an empty section list.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisCompletedOnByStatusIdAndTestSections(Date completedDate, List<String> statusIds,
            List<String> testSectionIds);

    /**
     * Test-section-scoped counterpart of
     * {@link #getCountOfAnalysisStartedOnExcludedByStatusId(Date, Set)}. Returns 0
     * for an empty section list.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisStartedOnExcludedByStatusIdAndTestSections(Date startedDate, Set<String> statusIds,
            List<String> testSectionIds);

    /**
     * Test-section-scoped counterpart of
     * {@link #getCountOfAnalysisStartedOnByStatusId(Date, List)}. Returns 0 for an
     * empty section list.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysisStartedOnByStatusIdAndTestSections(Date startedDate, List<String> statusIds,
            List<String> testSectionIds);

    /**
     * Analyses started on the given date with any of the statuses (same predicate
     * as the count).
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisStartedOnByStatusId(Date startedDate, List<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getMethodId(Analysis analysis);

    /**
     * Find an analysis by sample item ID and test ID.
     *
     * <p>
     * Used for duplicate detection when adding tests to sample items. Returns the
     * analysis if a matching test already exists for the sample item, or null if no
     * such analysis exists.
     *
     * <p>
     * Related: Feature 001-sample-management, User Story 2 (Add Tests)
     *
     * @param sampleItemId the sample item ID
     * @param testId       the test ID
     * @return the existing Analysis or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Analysis getAnalysisBySampleItemAndTest(String sampleItemId, String testId);
}
