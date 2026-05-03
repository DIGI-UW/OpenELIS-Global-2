package org.openelisglobal.analysis.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
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
    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<String> statusIdList,
            boolean sortedByDateAndAccession);

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

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleId(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesReadyToBeReported();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisBySampleAndTestIds(String sampleKey, List<String> testIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisCompleteInRange(Timestamp lowDate, Timestamp highDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesForStatusId(String statusId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountOfAnalysesForStatusIds(List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllMaxRevisionAnalysesPerTest(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisByAccessionAndTestId(String accessionNumber, String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysisCollectedOn(Date collectionDate);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestAndStatus(String testId, List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAnalysesBySampleItem(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIdList, List<String> statusIdList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    Analysis buildAnalysis(Test test, SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    void updateAnalysises(List<Analysis> cancelAnalysis, List<Analysis> newAnalysis, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
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
    List<Analysis> getPageAnalysisByTestSectionAndStatus(String sectionId, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> getPageAnalysisAtAccessionNumberAndStatus(String accessionNumber, List<String> statusList,
            boolean sortedByDateAndAccession);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    int getCountAnalysisByTestSectionAndStatus(String sectionId, List<String> statusList);

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
