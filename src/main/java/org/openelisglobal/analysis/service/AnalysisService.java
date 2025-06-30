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

public interface AnalysisService extends BaseObjectService<Analysis, String> {
    void getData(Analysis analysis);

    Analysis getAnalysisById(String analysisId);

    List<Analysis> getAnalysisByTestDescriptionAndCompletedDateRange(List<String> descriptions, Date sqlDayOne,
            Date sqlDayTwo);

    List<Analysis> getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(Sample sample);

    List<Analysis> getMaxRevisionAnalysesReadyForReportPreviewBySample(List<String> accessionNumbers);

    List<Analysis> getMaxRevisionPendingAnalysesReadyToBeReportedBySample(Sample sample);

    List<Analysis> getAnalysesBySampleIdExcludedByStatusId(String id, Set<Integer> statusIds);

    List<Analysis> getAnalysisStartedOrCompletedInDateRange(Date lowDate, Date highDate);

    List<Analysis> getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(Date lowDate, Date highDate, String testId,
            List<Integer> testScectionIds);

    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList);

    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> statusIdList,
            boolean sortedByDateAndAccession);

    List<Analysis> getMaxRevisionAnalysesBySampleIncludeCanceled(SampleItem sampleItem);

    List<Analysis> getAnalysisByTestNamesAndCompletedDateRange(List<String> testNames, Date lowDate, Date highDate);

    List<Analysis> getAnalysesBySampleIdTestIdAndStatusId(List<Integer> sampleIdList, List<Integer> testIdList,
            List<Integer> statusIdList);

    List<Analysis> getMaxRevisionParentTestAnalysesBySample(SampleItem sampleItem);

    List<Analysis> getAnalysesBySampleItemsExcludingByStatusIds(SampleItem sampleItem, Set<Integer> statusIds);

    List<Analysis> getAnalysisStartedOnRangeByStatusId(Date lowDate, Date highDate, String statusID);

    List<Analysis> getRevisionHistoryOfAnalysesBySample(SampleItem sampleItem);

    List<Analysis> getAnalysisCollectedOnExcludedByStatusId(Date collectionDate, Set<Integer> statusIds);

    Analysis getPreviousAnalysisForAmendedAnalysis(Analysis analysis);

    List<Analysis> getAllAnalysisByTestSectionAndExcludedStatus(String testSectionId, List<Integer> statusIdList);

    List<Analysis> getAnalysesBySampleStatusIdExcludingByStatusId(String statusId, Set<Integer> statusIds);

    List<Analysis> getAnalysesBySampleItemIdAndStatusId(String sampleItemId, String statusId);

    List<Analysis> getAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<Integer> statusIds);

    int getCountOfAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<Integer> statusIds);

    List<Analysis> getAnalysisByTestSectionAndCompletedDateRange(String sectionID, Date lowDate, Date highDate);

    List<Analysis> getMaxRevisionAnalysesReadyToBeReported();

    void getMaxRevisionAnalysisBySampleAndTest(Analysis analysis);

    List<Analysis> getAllAnalysisByTestAndExcludedStatus(String testId, List<Integer> statusIdList);

    List<Analysis> getAnalysesAlreadyReportedBySample(Sample sample);

    List<Analysis> getRevisionHistoryOfAnalysesBySampleAndTest(SampleItem sampleItem, Test test,
            boolean includeLatestRevision);

    List<Analysis> getAnalysesBySampleStatusId(String statusId);

    List<Analysis> getAnalysisEnteredAfterDate(Timestamp latestCollectionDate);

    List<Analysis> getAnalysesBySampleIdAndStatusId(String id, Set<Integer> analysisStatusIds);

    List<Analysis> getAnalysesByPriorityAndStatusId(OrderPriority priority, List<Integer> analysisStatusIds);

    List<Analysis> getAnalysisStartedOn(Date collectionDate);

    List<Analysis> getMaxRevisionAnalysesBySample(SampleItem sampleItem);

    List<Analysis> getAllChildAnalysesByResult(Result result);

    List<Analysis> getAnalysesBySampleId(String id);

    List<Analysis> getAnalysesReadyToBeReported();

    List<Analysis> getAnalysisBySampleAndTestIds(String sampleKey, List<Integer> testIds);

    List<Analysis> getAnalysisCompleteInRange(Timestamp lowDate, Timestamp highDate);

    List<Analysis> getAnalysesForStatusId(String statusId);

    int getCountOfAnalysesForStatusIds(List<Integer> statusIdList);

    List<Analysis> getAllMaxRevisionAnalysesPerTest(Test test);

    List<Analysis> getAnalysisByAccessionAndTestId(String accessionNumber, String testId);

    List<Analysis> getAnalysisCollectedOn(Date collectionDate);

    List<Analysis> getAllAnalysisByTestAndStatus(String testId, List<Integer> statusIdList);

    List<Analysis> getAnalysesBySampleItem(SampleItem sampleItem);

    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIdList, List<Integer> statusIdList);

    Analysis buildAnalysis(Test test, SampleItem sampleItem);

    void updateAnalysises(List<Analysis> cancelAnalysis, List<Analysis> newAnalysis, String sysUserId);

    void updateAllNoAuditTrail(List<Analysis> updatedAnalysis);

    void updateNoAuditTrail(Analysis analysis);

    String getTestDisplayName(Analysis analysis);

    String getCompletedDateForDisplay(Analysis analysis);

    String getAnalysisType(Analysis analysis);

    String getJSONMultiSelectResults(Analysis analysis);

    String getCSVMultiselectResults(Analysis analysis);

    Result getQuantifiedResult(Analysis analysis);

    String getStatusId(Analysis analysis);

    Boolean getTriggeredReflex(Analysis analysis);

    boolean resultIsConclusion(Result currentResult, Analysis analysis);

    boolean isParentNonConforming(Analysis analysis);

    Test getTest(Analysis analysis);

    List<Result> getResults(Analysis analysis);

    boolean hasBeenCorrectedSinceLastPatientReport(Analysis analysis);

    boolean patientReportHasBeenDone(Analysis analysis);

    String getNotesAsString(Analysis analysis, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            boolean excludeExternPrefix);

    String getOrderAccessionNumber(Analysis analysis);

    TypeOfSample getTypeOfSample(Analysis analysis);

    Panel getPanel(Analysis analysis);

    TestSection getTestSection(Analysis analysis);

    List<Analysis> getAllAnalysisByTestsAndStatus(List<Integer> list, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList);

    List<Analysis> get(List<String> value);

    List<Analysis> getAllAnalysisByTestsAndStatusAndCompletedDateRange(List<Integer> nfsTestIdList,
            List<Integer> analysisStatusList, List<Integer> sampleStatusList, Date lowDate, Date highDate);

    List<Analysis> getPageAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList);

    int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList);

    List<Analysis> getPageAnalysisByTestSectionAndStatus(String sectionId, List<Integer> statusList,
            boolean sortedByDateAndAccession);

    List<Analysis> getPageAnalysisAtAccessionNumberAndStatus(String accessionNumber, List<Integer> statusList,
            boolean sortedByDateAndAccession);

    int getCountAnalysisByTestSectionAndStatus(String sectionId, List<Integer> statusList);

    int getCountAnalysisByStatusFromAccession(List<Integer> analysisStatusList, List<Integer> sampleStatusList,
            String accessionNumber);

    List<Analysis> getPageAnalysisByStatusFromAccession(List<Integer> analysisStatusList,
            List<Integer> sampleStatusList, String accessionNumber);

    List<Analysis> getPageAnalysisByStatusFromAccession(List<Integer> analysisStatusList,
            List<Integer> sampleStatusList, String accessionNumber, String upperRangeAccessionNumber, boolean doRange,
            boolean finished);

    List<Analysis> getAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Analysis> getStudyAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Analysis> getAnalysesCompletedOnByStatusId(Date completedDate, String statusId);

    List<Analysis> getAnalysesResultEnteredOnExcludedByStatusId(Date completedDate, Set<Integer> statusIds);

    int getCountOfAnalysisCompletedOnByStatusId(Date completedDate, List<Integer> statusIds);

    int getCountOfAnalysisStartedOnByStatusId(Date startedDate, List<Integer> statusIds);

    String getMethodId(Analysis analysis);
}
