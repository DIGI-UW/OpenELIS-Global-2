package org.openelisglobal.analysis.service;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.ReportTrackingService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultServiceImpl;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class AnalysisServiceImpl extends AuditableBaseObjectServiceImpl<Analysis, String> implements AnalysisService {

    @Autowired
    protected AnalysisDAO baseObjectDAO;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private ReferenceTablesService referenceTablesService;
    @Autowired
    private NoteService noteService;

    private static String TABLE_REFERENCE_ID;
    private final String DEFAULT_ANALYSIS_TYPE = "MANUAL";

    @PostConstruct
    public void initializeGlobalVariables() {
        if (TABLE_REFERENCE_ID == null) {
            TABLE_REFERENCE_ID = referenceTablesService.getReferenceTableByName("ANALYSIS").getId();
        }
    }

    public AnalysisServiceImpl() {
        super(Analysis.class);
        this.auditTrailLog = true;
    }

    public static String getTableReferenceId() {
        return TABLE_REFERENCE_ID;
    }

    @Override
    public String insert(Analysis analysis) {
        if (analysis.getFhirUuid() == null) {
            analysis.setFhirUuid(UUID.randomUUID());
        }
        return super.insert(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTestDisplayName(Analysis analysis) {
        if (analysis == null) {
            return "";
        }
        Test test = getTest(analysis);
        String name = TestServiceImpl.getLocalizedTestNameWithType(test);
        if (analysis.getSampleItem().getTypeOfSampleId().equals(
                SpringContext.getBean(TypeOfSampleService.class).getTypeOfSampleIdForLocalAbbreviation("Variable"))) {
            name += "(" + analysis.getSampleTypeName() + ")";
        }

        // TypeOfSample typeOfSample = SpringContext.getBean(TypeOfSampleService.class)
        // .getTypeOfSampleForTest(test.getId());
        // if (typeOfSample != null && typeOfSample.getId().equals(
        //
        // SpringContext.getBean(TypeOfSampleService.class).getTypeOfSampleIdForLocalAbbreviation("Variable")))
        // {
        // name += "(" + analysis.getSampleTypeName() + ")";
        // }

        String parentResultType = analysis.getParentResult() != null ? analysis.getParentResult().getResultType() : "";
        if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(parentResultType)) {
            Dictionary dictionary = dictionaryService.getDictionaryById(analysis.getParentResult().getValue());
            if (dictionary != null) {
                String parentResult = dictionary.getLocalAbbreviation();
                if (GenericValidator.isBlankOrNull(parentResult)) {
                    parentResult = dictionary.getDictEntry();
                }
                name = parentResult + " &rarr; " + name;
            }
        }

        return name;
    }

    @Override
    @Transactional(readOnly = true)
    public String getCSVMultiselectResults(Analysis analysis) {
        if (analysis == null) {
            return "";
        }
        List<Result> existingResults = resultService.getResultsByAnalysis(analysis);
        StringBuilder multiSelectBuffer = new StringBuilder();
        for (Result existingResult : existingResults) {
            if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(existingResult.getResultType())) {
                multiSelectBuffer.append(existingResult.getValue());
                multiSelectBuffer.append(',');
            }
        }

        // remove last ','
        multiSelectBuffer.setLength(multiSelectBuffer.length() - 1);

        return multiSelectBuffer.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String getJSONMultiSelectResults(Analysis analysis) {
        return analysis == null ? ""
                : ResultServiceImpl.getJSONStringForMultiSelect(resultService.getResultsByAnalysis(analysis));
    }

    @Override
    @Transactional(readOnly = true)
    public Result getQuantifiedResult(Analysis analysis) {
        if (analysis == null) {
            return null;
        }
        List<Result> existingResults = resultService.getResultsByAnalysis(analysis);
        List<String> quantifiableResultsIds = new ArrayList<>();
        for (Result existingResult : existingResults) {
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(existingResult.getResultType())) {
                quantifiableResultsIds.add(existingResult.getId());
            }
        }

        for (Result existingResult : existingResults) {
            if (!TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(existingResult.getResultType())
                    && existingResult.getParentResult() != null
                    && quantifiableResultsIds.contains(existingResult.getParentResult().getId())
                    && !GenericValidator.isBlankOrNull(existingResult.getValue())) {
                return existingResult;
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public String getCompletedDateForDisplay(Analysis analysis) {
        return analysis == null ? "" : analysis.getCompletedDateForDisplay();
    }

    @Override
    @Transactional(readOnly = true)
    public String getAnalysisType(Analysis analysis) {
        return analysis == null ? "" : analysis.getAnalysisType();
    }

    @Override
    @Transactional(readOnly = true)
    public String getStatusId(Analysis analysis) {
        return analysis == null ? "" : analysis.getStatusId();
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean getTriggeredReflex(Analysis analysis) {
        return analysis == null ? false : analysis.getTriggeredReflex();
    }

    @Override
    public boolean resultIsConclusion(Result currentResult, Analysis analysis) {
        if (analysis == null || currentResult == null) {
            return false;
        }
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        if (results.size() == 1) {
            return false;
        }

        Long testResultId = Long.parseLong(currentResult.getId());
        // This based on the fact that the conclusion is always added
        // after the shared result so if there is a result with a larger id
        // then this is not a conclusion
        for (Result result : results) {
            if (Long.parseLong(result.getId()) > testResultId) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isParentNonConforming(Analysis analysis) {
        return analysis == null ? false : QAService.isAnalysisParentNonConforming(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTest(Analysis analysis) {
        return analysis == null ? null : analysis.getTest();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisStartedOrCompletedInDateRange(Date lowDate, Date highDate) {
        return baseObjectDAO.getAnalysisStartedOrCompletedInDateRange(lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(Date lowDate, Date highDate,
            String testId, List<Integer> testSectionIds) {
        return baseObjectDAO.getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(lowDate, highDate, testId,
                testSectionIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResults(Analysis analysis) {
        return analysis == null ? new ArrayList<>() : resultService.getResultsByAnalysis(analysis);
    }

    @Override
    public boolean hasBeenCorrectedSinceLastPatientReport(Analysis analysis) {
        return analysis == null ? false : analysis.isCorrectedSincePatientReport();
    }

    @Override
    public boolean patientReportHasBeenDone(Analysis analysis) {
        return analysis == null ? false
                : SpringContext.getBean(IReportTrackingService.class).getLastReportForSample(
                        analysis.getSampleItem().getSample(), ReportTrackingService.ReportType.PATIENT) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public String getNotesAsString(Analysis analysis, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            boolean excludeExternPrefix) {
        if (analysis == null) {
            return "";
        } else {
            return noteService.getNotesAsString(analysis, prefixType, prefixTimestamp, noteSeparator,
                    excludeExternPrefix);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getOrderAccessionNumber(Analysis analysis) {
        return analysis == null ? "" : analysis.getSampleItem().getSample().getAccessionNumber();
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTypeOfSample(Analysis analysis) {
        return analysis == null ? null
                : typeOfSampleService.getTypeOfSampleById(analysis.getSampleItem().getTypeOfSampleId());
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanel(Analysis analysis) {
        return analysis == null ? null : analysis.getPanel();
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSection(Analysis analysis) {
        return analysis == null ? null : analysis.getTestSection();
    }

    @Override
    public Analysis buildAnalysis(Test test, SampleItem sampleItem) {

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setRevision("0");
        analysis.setStartedDate(DateUtil.getNowAsSqlDate());
        analysis.setStatusId(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.NotStarted));
        analysis.setSampleItem(sampleItem);
        analysis.setTestSection(test.getTestSection());
        analysis.setSampleTypeName(sampleItem.getTypeOfSample().getLocalizedName());

        return analysis;
    }

    @Override
    protected AnalysisDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleId(String id) {
        return baseObjectDAO.getAnalysesBySampleId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisByAccessionAndTestId(String accessionNumber, String testId) {
        if (accessionNumber != null && accessionNumber.contains(".")) {
            accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf('.'));
        }
        return baseObjectDAO.getAnalysisByAccessionAndTestId(accessionNumber, testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisCollectedOnExcludedByStatusId(Date date, Set<Integer> excludedStatusIds) {
        return baseObjectDAO.getAnalysisCollectedOnExcludedByStatusId(date, excludedStatusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleItemsExcludingByStatusIds(SampleItem sampleItem,
            Set<Integer> excludedStatusIds) {
        return baseObjectDAO.getAnalysesBySampleItemsExcludingByStatusIds(sampleItem, excludedStatusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesForStatusId(String status) {
        return baseObjectDAO.getAllMatching("statusId", status);
    }

    @Override
    public int getCountOfAnalysesForStatusIds(List<Integer> statusIdList) {
        return baseObjectDAO.getCountOfAnalysesForStatusIds(statusIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleStatusIdExcludingByStatusId(String sampleStatus,
            Set<Integer> excludedStatusIds) {
        return baseObjectDAO.getAnalysesBySampleStatusIdExcludingByStatusId(sampleStatus, excludedStatusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestAndExcludedStatus(String testId, List<Integer> excludedStatusIntList) {
        return baseObjectDAO.getAllAnalysisByTestAndExcludedStatus(testId, excludedStatusIntList);
    }

    @Override
    @Transactional
    public void updateAnalysises(List<Analysis> cancelAnalysis, List<Analysis> newAnalysis, String sysUserId) {
        String cancelStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(StatusService.AnalysisStatus.Canceled);
        for (Analysis analysis : cancelAnalysis) {
            analysis.setStatusId(cancelStatus);
            analysis.setSysUserId(sysUserId);
            update(analysis);
        }

        for (Analysis analysis : newAnalysis) {
            analysis.setSysUserId(sysUserId);
            insert(analysis);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestAndStatus(String id, List<Integer> statusList) {
        return baseObjectDAO.getAllAnalysisByTestAndStatus(id, statusList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestsAndStatusAndCompletedDateRange(List<Integer> testIdList,
            List<Integer> analysisStatusList, List<Integer> sampleStatusList, Date lowDate, Date highDate) {
        return baseObjectDAO.getAllAnalysisByTestsAndStatusAndCompletedDateRange(testIdList, analysisStatusList,
                sampleStatusList, lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestSectionAndStatus(String sectionId, List<Integer> statusList,
            boolean sortedByDateAndAccession) {
        return baseObjectDAO.getAllAnalysisByTestSectionAndStatus(sectionId, statusList, sortedByDateAndAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getPageAnalysisByTestSectionAndStatus(String sectionId, List<Integer> statusList,
            boolean sortedByDateAndAccession) {
        return baseObjectDAO.getPageAnalysisByTestSectionAndStatus(sectionId, statusList, sortedByDateAndAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getPageAnalysisAtAccessionNumberAndStatus(String accessionNumber, List<Integer> statusList,
            boolean sortedByDateAndAccession) {
        if (accessionNumber != null && accessionNumber.contains(".")) {
            accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf('.'));
        }
        return baseObjectDAO.getPageAnalysisAtAccessionNumberAndStatus(accessionNumber, statusList,
                sortedByDateAndAccession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleItemIdAndStatusId(String sampleItemId, String canceledTestStatusId) {
        return baseObjectDAO.getAnalysesBySampleItemIdAndStatusId(sampleItemId, canceledTestStatusId);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Analysis analysis) {
        getBaseObjectDAO().getData(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public Analysis getAnalysisById(String analysisId) {
        return getBaseObjectDAO().getAnalysisById(analysisId);
    }

    @Override
    public void updateNoAuditTrail(Analysis analysis) {
        getBaseObjectDAO().update(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisByTestDescriptionAndCompletedDateRange(List<String> descriptions, Date sqlDayOne,
            Date sqlDayTwo) {
        return getBaseObjectDAO().getAnalysisByTestDescriptionAndCompletedDateRange(descriptions, sqlDayOne, sqlDayTwo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(Sample sample) {
        return getBaseObjectDAO().getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionAnalysesReadyForReportPreviewBySample(List<String> accessionNumbers) {
        return getBaseObjectDAO().getMaxRevisionAnalysesReadyForReportPreviewBySample(accessionNumbers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionPendingAnalysesReadyToBeReportedBySample(Sample sample) {
        return getBaseObjectDAO().getMaxRevisionPendingAnalysesReadyToBeReportedBySample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleIdExcludedByStatusId(String id, Set<Integer> statusIds) {
        return getBaseObjectDAO().getAnalysesBySampleIdExcludedByStatusId(id, statusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestsAndStatus(List<Integer> testIds, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList) {
        return getBaseObjectDAO().getAllAnalysisByTestsAndStatus(testIds, analysisStatusList, sampleStatusList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList) {
        return getBaseObjectDAO().getAllAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList,
                sampleStatusList);
    }

    @Override
    public List<Analysis> getPageAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList) {
        return getBaseObjectDAO().getPageAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList,
                sampleStatusList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionAnalysesBySampleIncludeCanceled(SampleItem sampleItem) {
        return getBaseObjectDAO().getMaxRevisionAnalysesBySampleIncludeCanceled(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisByTestNamesAndCompletedDateRange(List<String> testNames, Date lowDate,
            Date highDate) {
        return getBaseObjectDAO().getAnalysisByTestNamesAndCompletedDateRange(testNames, lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleIdTestIdAndStatusId(List<Integer> sampleIdList, List<Integer> testIdList,
            List<Integer> statusIdList) {
        return getBaseObjectDAO().getAnalysesBySampleIdTestIdAndStatusId(sampleIdList, testIdList, statusIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionParentTestAnalysesBySample(SampleItem sampleItem) {
        return getBaseObjectDAO().getMaxRevisionParentTestAnalysesBySample(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisStartedOnRangeByStatusId(Date lowDate, Date highDate, String statusID) {
        return getBaseObjectDAO().getAnalysisStartedOnRangeByStatusId(lowDate, highDate, statusID);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getRevisionHistoryOfAnalysesBySample(SampleItem sampleItem) {
        return getBaseObjectDAO().getRevisionHistoryOfAnalysesBySample(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Analysis getPreviousAnalysisForAmendedAnalysis(Analysis analysis) {
        return getBaseObjectDAO().getPreviousAnalysisForAmendedAnalysis(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllAnalysisByTestSectionAndExcludedStatus(String testSectionId,
            List<Integer> statusIdList) {
        return getBaseObjectDAO().getAllAnalysisByTestSectionAndExcludedStatus(testSectionId, statusIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<Integer> statusIds) {
        return getBaseObjectDAO().getAnalysisStartedOnExcludedByStatusId(collectionDate, statusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisByTestSectionAndCompletedDateRange(String sectionID, Date lowDate, Date highDate) {
        return getBaseObjectDAO().getAnalysisByTestSectionAndCompletedDateRange(sectionID, lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionAnalysesReadyToBeReported() {
        return getBaseObjectDAO().getMaxRevisionAnalysesReadyToBeReported();
    }

    @Override
    @Transactional(readOnly = true)
    public void getMaxRevisionAnalysisBySampleAndTest(Analysis analysis) {
        getBaseObjectDAO().getMaxRevisionAnalysisBySampleAndTest(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesAlreadyReportedBySample(Sample sample) {
        return getBaseObjectDAO().getAnalysesAlreadyReportedBySample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getRevisionHistoryOfAnalysesBySampleAndTest(SampleItem sampleItem, Test test,
            boolean includeLatestRevision) {
        return getBaseObjectDAO().getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem, test, includeLatestRevision);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleStatusId(String statusId) {
        return getBaseObjectDAO().getAnalysesBySampleStatusId(statusId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisEnteredAfterDate(Timestamp latestCollectionDate) {
        return getBaseObjectDAO().getAnalysisEnteredAfterDate(latestCollectionDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleIdAndStatusId(String id, Set<Integer> analysisStatusIds) {
        return getBaseObjectDAO().getAnalysesBySampleIdAndStatusId(id, analysisStatusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisStartedOn(Date collectionDate) {
        return getBaseObjectDAO().getAnalysisStartedOn(collectionDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getMaxRevisionAnalysesBySample(SampleItem sampleItem) {
        return getBaseObjectDAO().getMaxRevisionAnalysesBySample(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllChildAnalysesByResult(Result result) {
        return getBaseObjectDAO().getAllChildAnalysesByResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesReadyToBeReported() {
        return getBaseObjectDAO().getAnalysesReadyToBeReported();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisBySampleAndTestIds(String sampleKey, List<Integer> testIds) {
        return getBaseObjectDAO().getAnalysisBySampleAndTestIds(sampleKey, testIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisCompleteInRange(Timestamp lowDate, Timestamp highDate) {
        return getBaseObjectDAO().getAnalysisCompleteInRange(lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAllMaxRevisionAnalysesPerTest(Test test) {
        return getBaseObjectDAO().getAllMaxRevisionAnalysesPerTest(test);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysisCollectedOn(Date collectionDate) {
        return getBaseObjectDAO().getAnalysisCollectedOn(collectionDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> getAnalysesBySampleItem(SampleItem sampleItem) {
        return getBaseObjectDAO().getAnalysesBySampleItem(sampleItem);
    }

    @Override
    @Transactional
    public void updateAllNoAuditTrail(List<Analysis> updatedAnalysis) {
        for (Analysis analysis : updatedAnalysis) {
            updateNoAuditTrail(analysis);
        }
    }

    @Override
    public List<Analysis> get(List<String> value) {
        return baseObjectDAO.get(value);
    }

    @Override
    public List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIdList, List<Integer> statusIdList) {
        return baseObjectDAO.getAllAnalysisByTestsAndStatus(testIdList, statusIdList);
    }

    @Override
    public int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList,
            List<Integer> sampleStatusList) {
        return baseObjectDAO.getCountAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList,
                sampleStatusList);
    }

    @Override
    public int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<Integer> analysisStatusList) {
        return baseObjectDAO.getCountAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList);
    }

    @Override
    public int getCountAnalysisByStatusFromAccession(List<Integer> analysisStatusList, List<Integer> sampleStatusList,
            String accessionNumber) {
        if (accessionNumber != null && accessionNumber.contains(".")) {
            accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf('.'));
        }
        return baseObjectDAO.getCountAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                accessionNumber);
    }

    @Override
    public List<Analysis> getPageAnalysisByStatusFromAccession(List<Integer> analysisStatusList,
            List<Integer> sampleStatusList, String accessionNumber) {
        if (accessionNumber != null && accessionNumber.contains(".")) {
            accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf('.'));
        }
        return baseObjectDAO.getPageAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                accessionNumber);
    }

    @Override
    public List<Analysis> getPageAnalysisByStatusFromAccession(List<Integer> analysisStatusList,
            List<Integer> sampleStatusList, String accessionNumber, String upperRangeAccessionNumber, boolean doRange,
            boolean finished) {
        if (accessionNumber != null && accessionNumber.contains(".")) {
            accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf('.'));
        }
        return baseObjectDAO.getPageAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList, accessionNumber,
                upperRangeAccessionNumber, doRange, finished);
    }

    @Override
    public List<Analysis> getAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        return baseObjectDAO.getAnalysisForSiteBetweenResultDates(referringSiteId, lowerDate, upperDate);
    }

    @Override
    public List<Analysis> getAnalysesByPriorityAndStatusId(OrderPriority priority, List<Integer> analysisStatusIds) {
        return baseObjectDAO.getAnalysesByPriorityAndStatusId(priority, analysisStatusIds);
    }

    @Override
    public List<Analysis> getStudyAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        return baseObjectDAO.getStudyAnalysisForSiteBetweenResultDates(referringSiteId, lowerDate, upperDate);
    }

    @Override
    public List<Analysis> getAnalysesCompletedOnByStatusId(Date completedDate, String statusId) {
        return baseObjectDAO.getAnalysesCompletedOnByStatusId(completedDate, statusId);
    }

    @Override
    public int getCountOfAnalysisCompletedOnByStatusId(Date completedDate, List<Integer> statusIds) {
        return baseObjectDAO.getCountOfAnalysisCompletedOnByStatusId(completedDate, statusIds);
    }

    @Override
    public int getCountOfAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<Integer> statusIds) {
        return baseObjectDAO.getCountOfAnalysisStartedOnExcludedByStatusId(collectionDate, statusIds);
    }

    @Override
    public int getCountOfAnalysisStartedOnByStatusId(Date startedDate, List<Integer> statusIds) {
        return baseObjectDAO.getCountOfAnalysisStartedOnByStatusId(startedDate, statusIds);
    }

    @Override
    public List<Analysis> getAnalysesResultEnteredOnExcludedByStatusId(Date completedDate, Set<Integer> statusIds) {
        return baseObjectDAO.getAnalysesResultEnteredOnExcludedByStatusId(completedDate, statusIds);
    }

    @Override
    public String getMethodId(Analysis analysis) {
        return analysis == null ? "" : analysis.getMethod() == null ? "" : analysis.getMethod().getId();
    }
}
