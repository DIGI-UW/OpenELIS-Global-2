package org.openelisglobal.analyzerresults.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class AnalyzerResultsAcceptServiceImpl implements AnalyzerResultsAcceptService {

    private static final String REJECT_VALUE = "XXXX";
    private static final String RESULT_SUBJECT = "Analyzer Result Note";

    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ResultLimitService resultLimitService;

    // ---------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------

    @Override
    public void acceptAndPersist(List<AnalyzerResultItem> allResults, String sysUserId) {
        List<AnalyzerResultItem> actionableResults = extractActionableResult(allResults);

        if (actionableResults.isEmpty()) {
            return;
        }

        // Remove actionable items from the remaining list so we can detect
        // childless controls among the leftovers.
        List<AnalyzerResultItem> remaining = new ArrayList<>(allResults);
        remaining.removeAll(actionableResults);

        List<AnalyzerResultItem> childlessControls = extractChildlessControls(remaining);
        List<AnalyzerResults> deletableAnalyzerResults = getRemovableAnalyzerResults(actionableResults,
                childlessControls);

        List<SampleGrouping> sampleGroupList = new ArrayList<>();
        buildSampleGroupings(actionableResults, sampleGroupList, sysUserId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "acceptAndPersist",
                "Accept: " + actionableResults.size() + " actionable, " + sampleGroupList.size() + " sample groupings, "
                        + deletableAnalyzerResults.size() + " to delete from staging");

        if (sampleGroupList.isEmpty() && !actionableResults.isEmpty()) {
            LogEvent.logError(this.getClass().getSimpleName(), "acceptAndPersist",
                    "BUG: actionable results exist but no sample groupings were built — staging will be deleted without creating accepted records!");
        }

        analyzerResultsService.persistAnalyzerResults(deletableAnalyzerResults, sampleGroupList, sysUserId);
    }

    // ---------------------------------------------------------------
    // Extraction helpers
    // ---------------------------------------------------------------

    List<AnalyzerResultItem> extractActionableResult(List<AnalyzerResultItem> resultItemList) {
        List<AnalyzerResultItem> actionableResultList = new ArrayList<>();

        int currentSampleGrouping = 0;
        boolean acceptResult = false;
        boolean rejectResult = false;
        boolean deleteResult = false;
        String accessionNumber = null;

        for (AnalyzerResultItem resultItem : resultItemList) {

            if (currentSampleGrouping != resultItem.getSampleGroupingNumber()) {
                currentSampleGrouping = resultItem.getSampleGroupingNumber();
                acceptResult = resultItem.getIsAccepted();
                rejectResult = resultItem.getIsRejected();
                deleteResult = resultItem.getIsDeleted();
                accessionNumber = resultItem.getAccessionNumber();
            } else {
                resultItem.setAccessionNumber(accessionNumber);
                resultItem.setIsAccepted(acceptResult);
                resultItem.setIsRejected(rejectResult);
                resultItem.setIsDeleted(deleteResult);
            }

            if (acceptResult || rejectResult || deleteResult) {
                actionableResultList.add(resultItem);
            }
        }

        return actionableResultList;
    }

    List<AnalyzerResultItem> extractChildlessControls(List<AnalyzerResultItem> resultItemList) {
        /*
         * A childless control is a control which is adjacent to another control. It is
         * the first set of controls which will be removed. For that reason we're going
         * through the list backwards.
         */
        List<AnalyzerResultItem> childLessControlList = new ArrayList<>();
        int sampleGroupingNumber = 0;
        boolean lastGroupIsControl = false;
        boolean inControlGroup = true; // covers the bottom control has no children

        for (int i = resultItemList.size() - 1; i >= 0; i--) {
            AnalyzerResultItem resultItem = resultItemList.get(i);

            if (sampleGroupingNumber != resultItem.getSampleGroupingNumber()) {
                lastGroupIsControl = inControlGroup;
                inControlGroup = resultItem.getIsControl();
                sampleGroupingNumber = resultItem.getSampleGroupingNumber();
            }

            if (lastGroupIsControl && resultItem.getIsControl()) {
                childLessControlList.add(resultItem);
            }
        }

        return childLessControlList;
    }

    List<AnalyzerResults> getRemovableAnalyzerResults(List<AnalyzerResultItem> actionableResults,
            List<AnalyzerResultItem> childlessControls) {

        Set<AnalyzerResults> deletableAnalyzerResults = new HashSet<>();

        for (AnalyzerResultItem resultItem : actionableResults) {
            AnalyzerResults result = new AnalyzerResults();
            result.setId(resultItem.getId());
            deletableAnalyzerResults.add(result);
        }

        for (AnalyzerResultItem resultItem : childlessControls) {
            AnalyzerResults result = new AnalyzerResults();
            result.setId(resultItem.getId());
            deletableAnalyzerResults.add(result);
        }

        List<AnalyzerResults> resultList = new ArrayList<>();
        resultList.addAll(deletableAnalyzerResults);
        return resultList;
    }

    // ---------------------------------------------------------------
    // SampleGrouping construction (was createResultsFromItems)
    // ---------------------------------------------------------------

    void buildSampleGroupings(List<AnalyzerResultItem> actionableResults, List<SampleGrouping> sampleGroupList,
            String sysUserId) {
        int groupingNumber = -1;
        List<AnalyzerResultItem> groupedResultList = new ArrayList<>();

        for (AnalyzerResultItem analyzerResultItem : actionableResults) {
            if (analyzerResultItem.getIsDeleted()) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "buildSampleGroupings",
                        "Skipping deleted item: " + analyzerResultItem.getAccessionNumber());
                continue;
            }

            if (analyzerResultItem.getSampleGroupingNumber() != groupingNumber) {
                groupingNumber = analyzerResultItem.getSampleGroupingNumber();

                SampleGrouping sampleGrouping = createRecordsForNewResult(groupedResultList, sysUserId);

                if (sampleGrouping != null) {
                    sampleGrouping.triggersToSelectedReflexesMap = new HashMap<>();
                    sampleGroupList.add(sampleGrouping);
                }

                groupedResultList = new ArrayList<>();
            }

            if (!analyzerResultItem.isReadOnly()) {
                groupedResultList.add(analyzerResultItem);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildSampleGroupings",
                        "Skipping read-only item: accession=" + analyzerResultItem.getAccessionNumber() + ", test="
                                + analyzerResultItem.getTestName() + ", testId=" + analyzerResultItem.getTestId());
            }
        }

        // for the last set of results the grouping number will not change
        SampleGrouping sampleGrouping = createRecordsForNewResult(groupedResultList, sysUserId);
        if (sampleGrouping != null) {
            sampleGrouping.triggersToSelectedReflexesMap = new HashMap<>();
            sampleGroupList.add(sampleGrouping);
        }
    }

    private SampleGrouping createRecordsForNewResult(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            String sysUserId) {

        if (groupedAnalyzerResultItems != null && !groupedAnalyzerResultItems.isEmpty()) {
            String accessionNumber = groupedAnalyzerResultItems.get(0).getAccessionNumber();
            StatusSet statusSet = statusService.getStatusSetForAccessionNumber(accessionNumber);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                    "Accession: " + accessionNumber + ", sampleStatus="
                            + (statusSet != null ? statusSet.getSampleRecordStatus() : "null") + ", patientStatus="
                            + (statusSet != null ? statusSet.getPatientRecordStatus() : "null") + ", noEntryDone="
                            + noEntryDone(statusSet, accessionNumber));

            if (noEntryDone(statusSet, accessionNumber)) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForNoSampleEntryDone for " + accessionNumber);
                return createGroupForNoSampleEntryDone(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else if (statusSet
                    .getSampleRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered
                    && statusSet
                            .getPatientRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForPreviousAnalyzerDone for " + accessionNumber);
                return createGroupForPreviousAnalyzerDone(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else if (statusSet
                    .getSampleRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForDemographicsEntered for " + accessionNumber);
                return createGroupForDemographicsEntered(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForSampleAndDemographicsEntered for " + accessionNumber);
                return createGroupForSampleAndDemographicsEntered(groupedAnalyzerResultItems, statusSet, sysUserId);
            }
        }

        return null;
    }

    private boolean noEntryDone(StatusSet statusSet, String accessionNumber) {
        boolean sampleOrPatientEntryDone = statusSet.getPatientRecordStatus() != null
                || statusSet.getSampleRecordStatus() != null;

        if (sampleOrPatientEntryDone) {
            return false;
        }

        return sampleService.getSampleByAccessionNumber(accessionNumber) == null;
    }

    // ---------------------------------------------------------------
    // Group-creation methods (one per status scenario)
    // ---------------------------------------------------------------

    private SampleGrouping createGroupForPreviousAnalyzerDone(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);

        Patient patient = sampleHumanService.getPatientForSample(sample);
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, patient, sysUserId);

        SampleItem sampleItem = getOrCreateSampleItem(groupedAnalyzerResultItems, sample, sysUserId);

        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.addSampleItem = sampleItem.getId() == null;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    SampleItem getOrCreateSampleItem(List<AnalyzerResultItem> groupedAnalyzerResultItems, Sample sample,
            String sysUserId) {
        List<Analysis> dBAnalysisList = analysisService.getAnalysesBySampleId(sample.getId());

        List<TypeOfSampleTest> typeOfSampleForNewTest = typeOfSampleTestService
                .getTypeOfSampleTestsForTest(groupedAnalyzerResultItems.get(0).getTestId());
        List<String> typeOfSampleIds = typeOfSampleForNewTest.stream().map(e -> e.getTypeOfSampleId())
                .collect(Collectors.toList());

        SampleItem sampleItem = null;
        int maxSampleItemSortOrder = 0;

        for (Analysis dbAnalysis : dBAnalysisList) {
            if (!GenericValidator.isBlankOrNull(dbAnalysis.getSampleItem().getSortOrder())) {
                maxSampleItemSortOrder = Math.max(maxSampleItemSortOrder,
                        Integer.parseInt(dbAnalysis.getSampleItem().getSortOrder()));
            }
            if (typeOfSampleIds.contains(dbAnalysis.getSampleItem().getTypeOfSampleId())) {
                sampleItem = dbAnalysis.getSampleItem();
                break;
            }
        }

        boolean newSampleItem = sampleItem == null;

        if (newSampleItem) {
            sampleItem = new SampleItem();
            sampleItem.setSysUserId(sysUserId);
            sampleItem.setSortOrder(Integer.toString(maxSampleItemSortOrder + 1));
            sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
            TypeOfSample typeOfSample = typeOfSampleService.get(typeOfSampleIds.get(0));
            sampleItem.setTypeOfSample(typeOfSample);
        }
        return sampleItem;
    }

    private SampleGrouping createGroupForDemographicsEntered(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        SampleItem sampleItem = getOrCreateSampleItem(groupedAnalyzerResultItems, sample, sysUserId);

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        if (statusService.getStatusID(OrderStatus.Entered).equals(sample.getStatusId())) {
            sample.setStatusId(statusService.getStatusID(OrderStatus.Started));
        }
        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);

        Patient patient = sampleHumanService.getPatientForSample(sample);
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, patient, sysUserId);

        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.updateSample = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.addSampleItem = sampleItem.getId() == null;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    private SampleGrouping createGroupForSampleAndDemographicsEntered(
            List<AnalyzerResultItem> groupedAnalyzerResultItems, StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        if (statusService.getStatusID(OrderStatus.Entered).equals(sample.getStatusId())) {
            sample.setStatusId(statusService.getStatusID(OrderStatus.Started));
        }
        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);

        SampleItem sampleItem = null;
        List<Analysis> dBAnalysisList = analysisService.getAnalysesBySampleId(sample.getId());
        Patient patient = sampleHumanService.getPatientForSample(sample);

        for (AnalyzerResultItem resultItem : groupedAnalyzerResultItems) {
            Analysis analysis = null;

            for (Analysis dbAnalysis : dBAnalysisList) {
                if (dbAnalysis.getTest().getId().equals(resultItem.getTestId())) {
                    analysis = dbAnalysis;
                    break;
                }
            }

            if (analysis == null) {
                analysis = new Analysis();
                Test test = testService.get(resultItem.getTestId());
                analysis.setTest(test);
                List<TypeOfSample> typeOfSamples = typeOfSampleService.getTypeOfSampleForTest(test.getId());
                if (typeOfSamples == null) {
                    typeOfSamples = new ArrayList<>();
                }
                List<SampleItem> sampleItemsForSample = sampleItemService.getSampleItemsBySampleId(sample.getId());
                List<String> allowedTypeIds = typeOfSamples.stream().map(TypeOfSample::getId)
                        .collect(Collectors.toList());

                for (SampleItem item : sampleItemsForSample) {
                    if (!allowedTypeIds.isEmpty() && item.getTypeOfSample() != null
                            && allowedTypeIds.contains(item.getTypeOfSample().getId())) {
                        sampleItem = item;
                        analysis.setSampleItem(sampleItem);
                    }
                }
                if (sampleItem == null && allowedTypeIds.isEmpty() && !sampleItemsForSample.isEmpty()) {
                    sampleItem = sampleItemsForSample.get(0);
                    analysis.setSampleItem(sampleItem);
                }
                if (sampleItem == null) {
                    sampleItem = new SampleItem();
                    sampleItem.setSysUserId(sysUserId);
                    sampleItem.setSortOrder("1");
                    sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
                    sampleItem.setCollectionDate(DateUtil.getNowAsTimestamp());
                    if (!typeOfSamples.isEmpty()) {
                        sampleItem.setTypeOfSample(typeOfSamples.get(0));
                    }
                    analysis.setSampleItem(sampleItem);
                }
            } else {
                dBAnalysisList.remove(analysis);
            }
            if (sampleItem == null) {
                sampleItem = analysis.getSampleItem();
                sampleItem.setSysUserId(sysUserId);
            }

            populateAnalysis(resultItem, analysis, analysis.getTest());
            analysis.setSysUserId(sysUserId);
            analysisList.add(analysis);

            Result result = getResult(analysis, patient, resultItem, sysUserId);
            resultToUserSelectionMap.put(result, resultItem.getReflexSelectionId());

            resultList.add(result);

            if (GenericValidator.isBlankOrNull(resultItem.getNote())) {
                noteList.add(null);
            } else {
                Note note = noteService.createSavableNote(analysis, NoteServiceImpl.NoteType.INTERNAL,
                        resultItem.getNote(), RESULT_SUBJECT, sysUserId);
                noteList.add(note);
            }
        }

        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.updateSample = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.addSampleItem = (sampleItem == null || sampleItem.getId() == null);
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    private SampleGrouping createGroupForNoSampleEntryDone(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = new Sample();
        SampleHuman sampleHuman = new SampleHuman();
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSysUserId(sysUserId);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        sample.setAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());
        sample.setDomain("H");
        sample.setStatusId(statusService.getStatusID(OrderStatus.Started));
        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setReceivedDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);

        sampleHuman.setPatientId(PatientUtil.getUnknownPatient().getId());
        sampleHuman.setSysUserId(sysUserId);

        Patient patient = PatientUtil.getUnknownPatient();
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, patient, sysUserId);

        addSampleTypeToSampleItem(sampleItem, analysisList, sample.getAccessionNumber());

        sampleGrouping.sample = sample;
        sampleGrouping.sampleHuman = sampleHuman;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.patient = patient;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = true;
        sampleGrouping.addSampleItem = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    // ---------------------------------------------------------------
    // Analysis / Result / Note construction
    // ---------------------------------------------------------------

    private void createAndAddItems_Analysis_Results(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            List<Analysis> analysisList, List<Result> resultList, Map<Result, String> resultToUserSelectionMap,
            List<Note> noteList, Patient patient, String sysUserId) {

        for (AnalyzerResultItem resultItem : groupedAnalyzerResultItems) {
            Analysis analysis = getExistingAnalysis(resultItem);

            if (analysis == null) {
                analysis = new Analysis();
                Test test = testService.get(resultItem.getTestId());
                populateAnalysis(resultItem, analysis, test);
            } else {
                String statusId = statusService
                        .getStatusID(resultItem.getIsAccepted() ? AnalysisStatus.TechnicalAcceptance
                                : AnalysisStatus.TechnicalRejected);
                analysis.setStatusId(statusId);
                analysis.setAnalyzerId(resultItem.getAnalyzerId());
            }

            analysis.setSysUserId(sysUserId);
            analysisList.add(analysis);

            Result result = getResult(analysis, patient, resultItem, sysUserId);
            resultList.add(result);
            resultToUserSelectionMap.put(result, resultItem.getReflexSelectionId());
            if (GenericValidator.isBlankOrNull(resultItem.getNote())) {
                noteList.add(null);
            } else {
                Note note = noteService.createSavableNote(analysis, NoteServiceImpl.NoteType.INTERNAL,
                        resultItem.getNote(), RESULT_SUBJECT, sysUserId);
                noteList.add(note);
            }
        }
    }

    private Analysis getExistingAnalysis(AnalyzerResultItem resultItem) {
        List<Analysis> analysisList = analysisService.getAnalysisByAccessionAndTestId(resultItem.getAccessionNumber(),
                resultItem.getTestId());

        return analysisList.isEmpty() ? null : analysisList.get(0);
    }

    private Result getResult(Analysis analysis, Patient patient, AnalyzerResultItem resultItem, String sysUserId) {
        Result result = null;

        if (analysis.getId() != null) {
            List<Result> resultList = resultService.getResultsByAnalysis(analysis);

            if (!resultList.isEmpty()) {
                result = resultList.get(resultList.size() - 1);
                String resultValue = resultItem.getIsRejected() ? REJECT_VALUE : resultItem.getResult();
                TestResult resolvedTestResult = getTestResultForResult(resultItem);
                result.setTestResult(resolvedTestResult);
                if ("D".equals(resultItem.getTestResultType()) && resolvedTestResult != null
                        && !resultItem.getIsRejected()) {
                    result.setValue(resolvedTestResult.getValue());
                } else {
                    result.setValue(resultValue);
                }
                result.setSysUserId(sysUserId);

                setAnalyte(result);
            }
        }

        if (result == null) {
            result = createNewResult(resultItem, patient, sysUserId);
        }

        return result;
    }

    private Result createNewResult(AnalyzerResultItem resultItem, Patient patient, String sysUserId) {
        Result result = new Result();
        String rawValue = resultItem.getIsRejected() ? REJECT_VALUE : resultItem.getResult();
        TestResult resolvedTestResult = getTestResultForResult(resultItem);
        result.setTestResult(resolvedTestResult);
        if ("D".equals(resultItem.getTestResultType()) && resolvedTestResult != null && !resultItem.getIsRejected()) {
            result.setValue(resolvedTestResult.getValue());
        } else {
            result.setValue(rawValue);
        }
        result.setResultType(resultItem.getTestResultType());
        if (!GenericValidator.isBlankOrNull(resultItem.getSignificantDigits())) {
            if (StringUtil.isInteger(resultItem.getSignificantDigits())) {
                result.setSignificantDigits(Integer.parseInt(resultItem.getSignificantDigits()));
            } else {
                LogEvent.logWarn(AnalyzerResultsAcceptServiceImpl.class.getSimpleName(), "createNewResult",
                        "Invalid significantDigits value for testId '" + resultItem.getTestId() + "'");
            }
        }

        addMinMaxNormal(result, resultItem, patient);
        result.setSysUserId(sysUserId);

        return result;
    }

    private void populateAnalysis(AnalyzerResultItem resultItem, Analysis analysis, Test test) {
        if (!statusService.getStatusID(AnalysisStatus.Canceled).equals(analysis.getStatusId())) {
            String statusId = statusService.getStatusID(
                    resultItem.getIsAccepted() ? AnalysisStatus.TechnicalAcceptance : AnalysisStatus.TechnicalRejected);
            analysis.setStatusId(statusId);
            analysis.setAnalysisType(resultItem.getManual() ? IActionConstants.ANALYSIS_TYPE_MANUAL
                    : IActionConstants.ANALYSIS_TYPE_AUTO);
            analysis.setCompletedDateForDisplay(resultItem.getCompleteDate());
            analysis.setTest(test);
            analysis.setTestSection(test.getTestSection());
            analysis.setIsReportable(test.getIsReportable());
            analysis.setRevision("0");
            analysis.setAnalyzerId(resultItem.getAnalyzerId());
        }
    }

    private void setAnalyte(Result result) {
        TestAnalyte testAnalyte = ResultUtil.getTestAnalyteForResult(result);

        if (testAnalyte != null) {
            result.setAnalyte(testAnalyte.getAnalyte());
        }
    }

    private TestResult getTestResultForResult(AnalyzerResultItem resultItem) {
        if ("D".equals(resultItem.getTestResultType())) {
            TestResult testResult = testResultService.getTestResultsByTestAndDictonaryResult(resultItem.getTestId(),
                    resultItem.getResult());
            if (testResult == null && !StringUtil.isInteger(resultItem.getResult())) {
                String desired = resultItem.getResult().trim();
                List<TestResult> candidates = testResultService.getActiveTestResultsByTest(resultItem.getTestId());
                if (candidates != null) {
                    for (TestResult candidate : candidates) {
                        if (!"D".equals(candidate.getTestResultType())) {
                            continue;
                        }
                        Dictionary dict = dictionaryService.get(candidate.getValue());
                        if (dict != null && dict.getDictEntry() != null
                                && desired.equalsIgnoreCase(dict.getDictEntry().trim())) {
                            testResult = candidate;
                            break;
                        }
                    }
                }
            }
            return testResult;
        } else {
            List<TestResult> testResultList = testResultService.getActiveTestResultsByTest(resultItem.getTestId());
            if (!testResultList.isEmpty()) {
                return testResultList.get(0);
            }
        }

        return null;
    }

    private void addMinMaxNormal(Result result, AnalyzerResultItem resultItem, Patient patient) {
        boolean limitsFound = false;

        if (resultItem != null) {
            ResultLimit resultLimit = resultLimitService.getResultLimitForTestAndPatient(resultItem.getTestId(),
                    patient);
            if (resultLimit != null) {
                result.setMinNormal(resultLimit.getLowNormal());
                result.setMaxNormal(resultLimit.getHighNormal());
                limitsFound = true;
            }
        }

        if (!limitsFound) {
            result.setMinNormal(Double.NEGATIVE_INFINITY);
            result.setMaxNormal(Double.POSITIVE_INFINITY);
        }
    }

    // ---------------------------------------------------------------
    // Sample type helpers
    // ---------------------------------------------------------------

    private void addSampleTypeToSampleItem(SampleItem sampleItem, List<Analysis> analysisList, String accessionNumber) {
        if (analysisList.size() > 0) {
            String typeOfSampleId = getTypeOfSampleId(analysisList, accessionNumber);
            sampleItem.setTypeOfSample(typeOfSampleService.get(typeOfSampleId));
        }
    }

    private String getTypeOfSampleId(List<Analysis> analysisList, String accessionNumber) {
        if (IS_RETROCI && accessionNumber.startsWith("LDBS")) {
            List<TypeOfSampleTest> typeOfSmapleTestList = typeOfSampleTestService
                    .getTypeOfSampleTestsForTest(analysisList.get(0).getTest().getId());

            for (TypeOfSampleTest typeOfSampleTest : typeOfSmapleTestList) {
                if (DBS_SAMPLE_TYPE_ID.equals(typeOfSampleTest.getTypeOfSampleId())) {
                    return DBS_SAMPLE_TYPE_ID;
                }
            }
        }

        List<TypeOfSampleTest> sampleTests = typeOfSampleTestService
                .getTypeOfSampleTestsForTest(analysisList.get(0).getTest().getId());
        return sampleTests.get(0).getTypeOfSampleId();
    }

    // ---------------------------------------------------------------
    // QA / validation helpers
    // ---------------------------------------------------------------

    boolean getQaEventByTestSection(Analysis analysis) {
        if (analysis == null) {
            return false;
        }
        if (analysis.getTestSection() != null && analysis.getSampleItem().getSample() != null) {
            Sample sample = analysis.getSampleItem().getSample();
            List<SampleQaEvent> sampleQaEventsList = getSampleQaEvents(sample);
            for (SampleQaEvent event : sampleQaEventsList) {
                QAService qa = new QAService(event);
                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION)
                                .equals(analysis.getTestSection().getNameKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    List<SampleQaEvent> getSampleQaEvents(Sample sample) {
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    public Errors validateSavableItems(List<AnalyzerResultItem> savableResults, Errors errors) {
        for (AnalyzerResultItem item : savableResults) {
            if (item.getIsAccepted() && item.isUserChoicePending()) {
                StringBuilder augmentedAccession = new StringBuilder(item.getAccessionNumber());
                augmentedAccession.append(" : ");
                augmentedAccession.append(item.getTestName());
                augmentedAccession.append(" - ");
                augmentedAccession.append(MessageUtil.getMessage("error.reflexStep.notChosen"));
                String errorMsg = "errors.followingAccession";
                errors.reject(errorMsg, new String[] { augmentedAccession.toString() }, errorMsg);
            }
        }

        return errors;
    }

    // ---------------------------------------------------------------
    // Configuration constants (copied from controller)
    // ---------------------------------------------------------------

    private static final boolean IS_RETROCI = org.openelisglobal.common.util.ConfigurationProperties.getInstance()
            .isPropertyValueEqual(org.openelisglobal.common.util.ConfigurationProperties.Property.configurationName,
                    "CI_GENERAL");

    private final String DBS_SAMPLE_TYPE_ID;

    /**
     * Constructor — resolves the DBS sample type ID when running in RetroCI mode.
     */
    public AnalyzerResultsAcceptServiceImpl(TypeOfSampleService typeOfSampleService) {
        if (IS_RETROCI) {
            TypeOfSample typeOfSample = new TypeOfSample();
            typeOfSample.setDescription("DBS");
            typeOfSample.setDomain("H");
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(typeOfSample, false);
            DBS_SAMPLE_TYPE_ID = typeOfSample.getId();
        } else {
            DBS_SAMPLE_TYPE_ID = null;
        }
    }
}
