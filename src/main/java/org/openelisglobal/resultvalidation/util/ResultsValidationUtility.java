/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved. I-TECH,
 * University of Washington, Seattle WA.
 */
package org.openelisglobal.resultvalidation.util;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.TestIdentityService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.form.PatientInfoForm;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.QcEvaluation;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.action.util.ResultValidationItem;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.bean.QcFailureItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultsValidationUtility {

    @Autowired
    protected DictionaryService dictionaryService;
    @Autowired
    protected PatientService patientService;
    @Autowired
    protected TestSectionService testSectionService;
    @Autowired
    protected ResultService resultService;
    @Autowired
    protected TestResultService testResultService;
    @Autowired
    protected TestService testService;
    @Autowired
    protected SampleService sampleService;
    @Autowired
    protected ObservationHistoryService observationHistoryService;
    @Autowired
    protected AnalyteService analyteService;
    @Autowired
    protected ObservationHistoryTypeService ohTypeService;
    @Autowired
    protected AnalysisService analysisService;
    @Autowired
    protected ResultLimitService resultLimitService;
    @Autowired
    protected org.openelisglobal.qc.dao.SampleItemQcProfileDAO sampleItemQcProfileDAO;
    @Autowired
    protected org.openelisglobal.vector.service.VectorPoolService vectorPoolService;
    @Autowired
    protected org.openelisglobal.analysis.service.AnalysisAnchorService analysisAnchorService;
    @Autowired
    protected org.openelisglobal.testresultcomponent.service.TestResultComponentService testResultComponentService;

    private Patient currentPatient;
    protected String SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID;
    protected String CD4_COUNT_SORT_NUMBER;

    protected String ANALYTE_CD4_CT_GENERATED_ID;
    protected String CONCLUSION_ID;

    protected List<String> notValidStatus = new ArrayList<>();
    protected Map<String, String> testIdToUnits = new HashMap<>();
    protected Map<String, Boolean> accessionToValidMap;
    protected String totalTestName = "";
    private static boolean depersonalize = FormFields.getInstance().useField(Field.DepersonalizedResults);

    @PostConstruct
    private void initilaizeGlobalVariables() {
        notValidStatus.add(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
        notValidStatus.add(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled));
        notValidStatus.add(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected));
        notValidStatus.add(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        notValidStatus
                .add(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated));
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        analyte = analyteService.getAnalyteByName(analyte, false);
        CONCLUSION_ID = analyte.getId();
        analyte = new Analyte();
        analyte.setAnalyteName("generated CD4 Count");
        analyte = analyteService.getAnalyteByName(analyte, false);
        ANALYTE_CD4_CT_GENERATED_ID = analyte == null ? "" : analyte.getId();

        Test test = testService.getTestByLocalizedName("CD4 absolute count", Locale.ENGLISH);
        if (test != null) {
            CD4_COUNT_SORT_NUMBER = test.getSortOrder();
        }

        ObservationHistoryType oht = ohTypeService.getByName("SampleRecordStatus");
        if (oht != null) {
            SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID = oht.getId();
        }
    }

    public List<AnalysisItem> getResultValidationList(List<String> statusList, String testSectionId,
            String accessionNumber, String date) {

        List<AnalysisItem> resultList = new ArrayList<>();

        if (!GenericValidator.isBlankOrNull(testSectionId)) {
            List<ResultValidationItem> testList = getPageUnValidatedTestResultItemsInTestSection(testSectionId,
                    statusList);
            resultList = testResultListToAnalysisItemList(testList);
            sortByAccessionNumberAndOrder(resultList);
            setGroupingNumbers(resultList);
        } else if (!GenericValidator.isBlankOrNull(accessionNumber)) {
            List<ResultValidationItem> testList = getPageUnValidatedTestResultItemsAtAccessionNumber(accessionNumber,
                    statusList);
            resultList = testResultListToAnalysisItemList(testList);
            sortByAccessionNumberAndOrder(resultList);
            setGroupingNumbers(resultList);
        } else if (!GenericValidator.isBlankOrNull(date)) {
            List<ResultValidationItem> testList = getPageUnValidatedTestResultItemsByTestDate(date, statusList);
            resultList = testResultListToAnalysisItemList(testList);
            sortByAccessionNumberAndOrder(resultList);
            setGroupingNumbers(resultList);
        }

        return resultList;
    }

    public int getCountResultValidationList(List<String> statusList, String testSectionId) {

        // List<AnalysisItem> resultList = new ArrayList<>();
        int count = 0;
        if (!GenericValidator.isBlankOrNull(testSectionId)) {
            count = getCountUnValidatedTestResultItemsInTestSection(testSectionId, statusList);
            // resultList = testResultListToAnalysisItemList(testList);
            // sortByAccessionNumberAndOrder(resultList);
            // setGroupingNumbers(resultList);
        }

        return count;
    }

    @SuppressWarnings("unchecked")
    public final List<ResultValidationItem> getPageUnValidatedTestResultItemsInTestSection(String sectionId,
            List<String> statusList) {

        // QC samples are evaluated automatically by the QC engine and don't require a
        // validator sign-off, so they're hidden from the validation workbench. Failed
        // QC is surfaced separately on the validation screen via the QC acknowledgment
        // banner.
        List<Analysis> analysisList = analysisService.getPageAnalysisByTestSectionAndStatusExcludingQc(sectionId,
                statusList, false);
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
    }

    @SuppressWarnings("unchecked")
    public final List<ResultValidationItem> getPageUnValidatedTestResultItemsAtAccessionNumber(String accessionNumber,
            List<String> statusList) {

        // The DAO query uses LEFT JOIN + EXISTS so it returns both sampleItem-anchored
        // (member-level) and vectorPoolId-anchored (pool-level) analyses in one call.
        List<Analysis> analysisList = analysisService
                .getPageAnalysisAtAccessionNumberAndStatusExcludingQc(accessionNumber, statusList, false);
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
    }

    @SuppressWarnings("unchecked")
    public final List<ResultValidationItem> getPageUnValidatedTestResultItemsByTestDate(String date,
            List<String> statusList) {

        List<Analysis> analysisList = analysisService.getAnalysisStartedOn(DateUtil.convertStringDateToSqlDate(date))
                .stream().filter(analysis -> statusList.contains(analysis.getStatusId())).collect(Collectors.toList());
        return getGroupedTestsForAnalysisList(excludeQcAnalyses(analysisList),
                !StatusRules.useRecordStatusForValidation());
    }

    /**
     * Drops any analysis whose sample item has a QC profile (BLANK / DUPLICATE /
     * CONTROL). QC outcomes are evaluated automatically by the QC engine and
     * surfaced via the validation-screen acknowledgment banner — they don't need an
     * individual sign-off.
     */
    private List<Analysis> excludeQcAnalyses(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return analyses;
        }
        java.util.Set<Integer> sampleItemIds = new java.util.HashSet<>();
        for (Analysis a : analyses) {
            if (a.getSampleItem() != null && a.getSampleItem().getId() != null) {
                try {
                    sampleItemIds.add(Integer.valueOf(a.getSampleItem().getId()));
                } catch (NumberFormatException ignored) {
                    // SampleItem.id should always be numeric; skip rather than fail.
                }
            }
        }
        if (sampleItemIds.isEmpty()) {
            return analyses;
        }
        java.util.Set<Integer> qcSampleItemIds = new java.util.HashSet<>();
        for (org.openelisglobal.qc.valueholder.SampleItemQcProfile profile : sampleItemQcProfileDAO
                .findBySampleItemIds(new java.util.ArrayList<>(sampleItemIds))) {
            qcSampleItemIds.add(profile.getSampleItemId());
        }
        if (qcSampleItemIds.isEmpty()) {
            return analyses;
        }
        return analyses.stream().filter(a -> {
            try {
                return !qcSampleItemIds.contains(Integer.valueOf(a.getSampleItem().getId()));
            } catch (NumberFormatException e) {
                return true;
            }
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public final int getCountUnValidatedTestResultItemsInTestSection(String sectionId, List<String> statusList) {
        return analysisService.getCountAnalysisByTestSectionAndStatusExcludingQc(sectionId, statusList);
    }

    protected final void sortByAccessionNumberAndOrder(List<AnalysisItem> resultItemList) {
        Collections.sort(resultItemList, new Comparator<AnalysisItem>() {
            @Override
            public final int compare(AnalysisItem a, AnalysisItem b) {
                int accessionComp = a.getAccessionNumber().compareTo(b.getAccessionNumber());
                return ((accessionComp == 0)
                        ? Integer.parseInt(a.getTestSortNumber()) - Integer.parseInt(b.getTestSortNumber())
                        : accessionComp);
            }
        });
    }

    protected final void setGroupingNumbers(List<AnalysisItem> resultList) {
        String currentAccessionNumber = null;
        AnalysisItem headItem = null;
        int groupingCount = 1;

        for (AnalysisItem analysisResultItem : resultList) {
            if (!analysisResultItem.getAccessionNumber().equals(currentAccessionNumber)) {
                currentAccessionNumber = analysisResultItem.getAccessionNumber();
                headItem = analysisResultItem;
                groupingCount++;
            } else {
                if (headItem == null) {
                    throw new IllegalStateException("headItem should not be null here");
                }
                headItem.setMultipleResultForSample(true);
                analysisResultItem.setMultipleResultForSample(true);
            }

            analysisResultItem.setSampleGroupingNumber(groupingCount);
        }
    }

    /*
     * N.B. The ignoreRecordStatus is an abomination and should be removed. It is a
     * quick and dirty fix for workplan and validation using the same code but
     * having different rules
     */
    public final List<ResultValidationItem> getGroupedTestsForAnalysisList(Collection<Analysis> filteredAnalysisList,
            boolean ignoreRecordStatus) throws LIMSRuntimeException {

        List<ResultValidationItem> selectedTestList = new ArrayList<>();
        Dictionary dictionary;

        for (Analysis analysis : filteredAnalysisList) {
            // Use AnalysisAnchorService — same pattern as ResultsLoadUtility — so both
            // sampleItem-anchored and vectorPoolId-anchored analyses resolve correctly.
            org.openelisglobal.analysis.service.AnalysisAnchor anchor = analysisAnchorService.resolveAnchor(analysis);
            if (anchor == null || anchor.getSample() == null) {
                continue;
            }

            boolean ready = ignoreRecordStatus || sampleReadyForValidation(anchor.getSample());
            if (ready) {
                List<ResultValidationItem> testResultItemList = getResultItemFromAnalysis(analysis, anchor);
                // NB. The resultValue is filled in during getResultItemFromAnalysis as a side
                // effect of setResult
                for (ResultValidationItem validationItem : testResultItemList) {
                    if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(validationItem.getResultType())) {
                        dictionary = new Dictionary();
                        String resultValue = null;
                        try {
                            dictionary.setId(validationItem.getResultValue());
                            dictionaryService.getData(dictionary);
                            resultValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                                    ? dictionary.getDictEntry()
                                    : dictionary.getLocalAbbreviation();
                        } catch (RuntimeException e) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getGroupedTestsForAnalysisList",
                                    e.getMessage());
                            // no-op
                        }

                        validationItem.setResultValue(resultValue);
                    }

                    validationItem.setAnalysis(analysis);
                    validationItem.setNonconforming(QAService.isAnalysisParentNonConforming(analysis) || StatusService
                            .getInstance().matches(analysis.getStatusId(), AnalysisStatus.TechnicalRejected));
                    selectedTestList.add(validationItem);
                }
            }
        }

        return selectedTestList;
    }

    public final int getCountGroupedTestsForAnalysisList(Collection<Analysis> filteredAnalysisList,
            boolean ignoreRecordStatus) throws LIMSRuntimeException {

        List<ResultValidationItem> selectedTestList = new ArrayList<>();
        Dictionary dictionary;

        for (Analysis analysis : filteredAnalysisList) {
            org.openelisglobal.analysis.service.AnalysisAnchor anchor = analysisAnchorService.resolveAnchor(analysis);
            if (anchor == null || anchor.getSample() == null) {
                continue;
            }

            boolean countReady = ignoreRecordStatus || sampleReadyForValidation(anchor.getSample());
            if (countReady) {
                List<ResultValidationItem> testResultItemList = getResultItemFromAnalysis(analysis, anchor);
                // NB. The resultValue is filled in during getResultItemFromAnalysis as a side
                // effect of setResult
                for (ResultValidationItem validationItem : testResultItemList) {
                    if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(validationItem.getResultType())) {
                        dictionary = new Dictionary();
                        String resultValue = null;
                        try {
                            dictionary.setId(validationItem.getResultValue());
                            dictionaryService.getData(dictionary);
                            resultValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                                    ? dictionary.getDictEntry()
                                    : dictionary.getLocalAbbreviation();
                        } catch (RuntimeException e) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getGroupedTestsForAnalysisList",
                                    e.getMessage());
                            // no-op
                        }

                        validationItem.setResultValue(resultValue);
                    }

                    validationItem.setAnalysis(analysis);
                    validationItem.setNonconforming(QAService.isAnalysisParentNonConforming(analysis) || StatusService
                            .getInstance().matches(analysis.getStatusId(), AnalysisStatus.TechnicalRejected));
                    selectedTestList.add(validationItem);
                }
            }
        }

        return selectedTestList.size();
    }

    protected final boolean sampleReadyForValidation(Sample sample) {

        Boolean valid = accessionToValidMap.get(sample.getAccessionNumber());

        if (valid == null) {
            valid = getSampleRecordStatus(sample) != RecordStatus.NotRegistered;
            accessionToValidMap.put(sample.getAccessionNumber(), valid);
        }

        return valid;
    }

    /**
     * For a multi-component test, label the row with its component so validators
     * can tell the values apart (the component is resolved via the result's
     * test_result row).
     */
    protected String appendComponentLabel(String displayTestName, Result result, Test test) {
        if (result == null || result.getTestResult() == null || result.getTestResult().getComponentId() == null) {
            return displayTestName;
        }
        List<org.openelisglobal.testresultcomponent.valueholder.TestResultComponent> components = testResultComponentService
                .getActiveComponentsByTestId(test.getId());
        if (components.size() < 2) {
            return displayTestName;
        }
        for (org.openelisglobal.testresultcomponent.valueholder.TestResultComponent component : components) {
            if (component.getId().equals(result.getTestResult().getComponentId())
                    && !GenericValidator.isBlankOrNull(component.getLabel())) {
                return displayTestName + " — " + component.getLabel();
            }
        }
        return displayTestName;
    }

    public final List<ResultValidationItem> getResultItemFromAnalysis(Analysis analysis) throws LIMSRuntimeException {
        org.openelisglobal.analysis.service.AnalysisAnchor anchor = analysisAnchorService.resolveAnchor(analysis);
        return getResultItemFromAnalysis(analysis, anchor);
    }

    public final List<ResultValidationItem> getResultItemFromAnalysis(Analysis analysis,
            org.openelisglobal.analysis.service.AnalysisAnchor anchor) throws LIMSRuntimeException {
        List<ResultValidationItem> testResultList = new ArrayList<>();

        if (anchor == null || anchor.getSample() == null) {
            return testResultList;
        }

        List<Result> resultList = resultService.getResultsByAnalysis(analysis);
        NoteType[] noteTypes = { NoteType.EXTERNAL, NoteType.INTERNAL, NoteType.REJECTION_REASON,
                NoteType.NON_CONFORMITY };
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String notes = noteService.getNotesAsString(analysis, true, true, "<br/>", noteTypes, false);

        if (resultList == null) {
            return testResultList;
        }

        // For historical reasons we add a null member to the collection if it
        // is empty
        // this should be refactored.
        // The result list are results associated with the analysis, if there is
        // none we want
        // to present the user with a blank one
        if (resultList.isEmpty()) {
            resultList.add(null);
        }

        // Resolve accession number and sort order via anchor — works for both
        // sampleItem-anchored (member-level) and vectorPoolId-anchored (pool-level).
        String accessionNumber = anchor.getSample().getAccessionNumber();
        String sortOrder = anchor.getSampleItem() != null ? anchor.getSampleItem().getSortOrder() : "1";

        ResultValidationItem parentItem = null;
        for (Result result : resultList) {
            if (parentItem != null && result.getParentResult() != null
                    && parentItem.getResultId().equals(result.getParentResult().getId())) {
                parentItem.setQualifiedResultValue(result.getValue());
                parentItem.setHasQualifiedResult(true);
                parentItem.setQualificationResultId(result.getId());
                continue;
            }

            ResultValidationItem resultItem = createTestResultItem(analysis, analysis.getTest(), sortOrder, result,
                    accessionNumber, notes);

            notes = null; // we only want it once
            if (resultItem.getQualifiedDictionaryId() != null) {
                parentItem = resultItem;
            }

            testResultList.add(resultItem);
        }

        return testResultList;
    }

    protected final ResultValidationItem createTestResultItem(Analysis analysis, Test test, String sequenceNumber,
            Result result, String accessionNumber, String notes) {

        List<TestResult> testResults = getPossibleResultsForTest(test);
        // Results Entry narrows these to the row's own component so the significant
        // digits (and hence the rendered range) come from that component; do the same
        // here or the two screens print the same range to different precision.
        String rowComponentId = result == null || result.getTestResult() == null ? null
                : result.getTestResult().getComponentId();
        if (rowComponentId != null) {
            List<TestResult> componentRows = new ArrayList<>();
            for (TestResult testResult : testResults) {
                if (rowComponentId.equals(testResult.getComponentId())) {
                    componentRows.add(testResult);
                }
            }
            if (!componentRows.isEmpty()) {
                testResults = componentRows;
            }
        }

        // The same display name Results Entry shows, so a row reads identically on
        // both screens — including naming the specimen the row is actually for.
        String displayTestName = analysisService.getTestDisplayName(analysis);
        displayTestName = appendComponentLabel(displayTestName, result, test);

        // Results Entry chooses the range for the sample's patient; this screen used to
        // leave the patient null, so an age- or sex-specific band never matched and the
        // row showed a different range from the one the technician entered against.
        currentPatient = analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null ? null
                : sampleService.getPatient(analysis.getSampleItem().getSample());

        // The same range selection Results Entry uses: the component's own range on a
        // multi-component test, else the test-level one, both chosen for the patient
        // and scoped to this specimen.
        ResultLimit resultLimit = SpringContext.getBean(ResultLimitService.class).getResultLimitForResult(analysis,
                result, currentPatient);
        ResultValidationItem testItem = new ResultValidationItem();

        testItem.setAccessionNumber(accessionNumber);
        testItem.setAnalysis(analysis);
        testItem.setSequenceNumber(sequenceNumber);
        testItem.setTestName(displayTestName);
        testItem.setTestId(test.getId());
        setResultLimitDependencies(resultLimit, testItem, testResults);
        testItem.setCritical(ValidationSignals.isCritical(resultLimit, result));
        testItem.setAnalysisMethod(analysis.getAnalysisType());
        testItem.setResult(result);
        testItem.setDictionaryResults(getAnyDictonaryValues(testResults));
        // The test-level type is the first test_result row's, which for a
        // multi-component test is the primary's; an entered result knows its
        // own component's type, so prefer the stored one.
        if (result != null && !GenericValidator.isBlankOrNull(result.getResultType())) {
            testItem.setResultType(result.getResultType());
        } else {
            testItem.setResultType(getTestResultType(testResults));
        }
        testItem.setTestSortNumber(test.getSortOrder());
        testItem.setReflexGroup(analysis.getTriggeredReflex());
        testItem.setChildReflex(analysis.getTriggeredReflex() && isConclusion(result, analysis));
        testItem.setQualifiedDictionaryId(getQualifiedDictionaryId(testResults));
        testItem.setPastNotes(notes);

        testItem.setNormalResult(isNormalResult(analysis, result));

        return testItem;
    }

    private void setResultLimitDependencies(ResultLimit resultLimit, ResultValidationItem testItem,
            List<TestResult> testResults) {
        if (resultLimit != null) {
            testItem.setResultLimitId(resultLimit.getId());
            testItem.setLowerCritical(
                    resultLimit.getLowCritical() == Double.NEGATIVE_INFINITY ? 0 : resultLimit.getLowCritical());
            testItem.setHigherCritical(
                    resultLimit.getHighCritical() == Double.POSITIVE_INFINITY ? 0 : resultLimit.getHighCritical());

            testItem.setNormalRange(SpringContext.getBean(ResultLimitService.class).getDisplayReferenceRange(
                    resultLimit, testResults.isEmpty() ? "0" : testResults.get(0).getSignificantDigits(), " - "));
        }
    }

    private boolean isNormalResult(Analysis analysis, Result result) {
        boolean normalResult = false;
        ResultLimit resultLimit = resultLimitService.getResultLimitForAnalysis(analysis);
        if (resultLimit != null && result != null) {
            if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(result.getResultType())
                    && result.getValue().equals(resultLimit.getDictionaryNormalId())) {
                normalResult = true;
            } else if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(result.getResultType())
                    && !GenericValidator.isBlankOrNull(result.getValue())
                    && (resultLimit.getHighNormal() >= Double.parseDouble(result.getValue(true))
                            && resultLimit.getLowNormal() <= Double.parseDouble(result.getValue(true)))) {
                normalResult = true;
            } else if (!TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(result.getResultType())
                    && !GenericValidator.isBlankOrNull(result.getValue())
                    && GenericValidator.isDouble(result.getValue(true))
                    && (resultLimit.getHighNormal() >= Double.parseDouble(result.getValue(true))
                            && resultLimit.getLowNormal() <= Double.parseDouble(result.getValue(true)))) {
                normalResult = true;
            }
        }
        return normalResult;
    }

    protected final String getQualifiedDictionaryId(List<TestResult> testResults) {
        String qualDictionaryIds = "";
        for (TestResult testResult : testResults) {
            if (testResult.getIsQuantifiable()) {
                if (!"".equals(qualDictionaryIds)) {
                    qualDictionaryIds += ",";
                }
                qualDictionaryIds += testResult.getValue();
            }
        }
        return "".equals(qualDictionaryIds) ? null : "[" + qualDictionaryIds + "]";
    }

    protected final String augmentUOMWithRange(String uom, Result result) {
        if (result == null) {
            return uom;
        }
        ResultService resultResultService = SpringContext.getBean(ResultService.class);
        String range = resultResultService.getDisplayReferenceRange(result, true);
        uom = StringUtil.blankIfNull(uom);
        return GenericValidator.isBlankOrNull(range) ? uom : (uom + " ( " + range + " )");
    }

    protected final boolean isConclusion(Result testResult, Analysis analysis) {
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        if (results.size() == 1) {
            return false;
        }

        Long testResultId = Long.parseLong(testResult.getId());
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

    protected final List<TestResult> getPossibleResultsForTest(Test test) {
        return testResultService.getAllActiveTestResultsPerTest(test);
    }

    protected final List<IdValuePair> getAnyDictonaryValues(List<TestResult> testResults) {
        List<IdValuePair> values = null;
        Dictionary dictionary;

        if (testResults != null) {
            for (TestResult testResult : testResults) {
                // Note: result group use to be a criteria but was removed, if
                // results are not as expected investigate
                // A multi-component test mixes row types, so dictionary options
                // are collected from any dictionary-variant row rather than
                // gating on the first row's type.
                if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
                    if (values == null) {
                        values = new ArrayList<>();
                        values.add(new IdValuePair("0", ""));
                    }
                    dictionary = dictionaryService.getDataForId(testResult.getValue());
                    String displayValue = dictionary.getLocalizedName();

                    if ("unknown".equals(displayValue)) {
                        displayValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                                ? dictionary.getDictEntry()
                                : dictionary.getLocalAbbreviation();
                    }
                    values.add(new IdValuePair(testResult.getValue(), displayValue));
                }
            }
        }

        return values;
    }

    protected final String getTestResultType(List<TestResult> testResults) {
        String testResultType = TypeOfTestResultServiceImpl.ResultType.NUMERIC.getCharacterValue();

        if (testResults != null && testResults.size() > 0) {
            testResultType = testResults.get(0).getTestResultType();
        }

        return testResultType;
    }

    public final List<AnalysisItem> testResultListToAnalysisItemList(List<ResultValidationItem> testResultList) {
        List<AnalysisItem> analysisResultList = new ArrayList<>();

        /*
         * The issue with multiselect results is that each selection is one
         * ResultValidationItem but they all need to be condensed into one AnalysisItem
         * (whose multiSelectResultValues carries every selection as JSON). The
         * condensing is scoped per analysis: other results of the same accession —
         * other analyses, or other components of a multi-component analysis — must
         * still get their own rows. Qualified results found among an analysis's
         * multiselect selections are captured onto its condensed item.
         */
        Map<String, AnalysisItem> condensedMultiSelectByAnalysis = new HashMap<>();
        for (ResultValidationItem testResultItem : testResultList) {
            String analysisId = testResultItem.getAnalysis().getId();
            boolean multiSelect = TypeOfTestResultServiceImpl.ResultType
                    .isMultiSelectVariant(testResultItem.getResultType());

            if (!multiSelect || !condensedMultiSelectByAnalysis.containsKey(analysisId)) {
                AnalysisItem convertedItem = testResultItemToAnalysisItem(testResultItem);
                analysisResultList.add(convertedItem);
                if (multiSelect) {
                    condensedMultiSelectByAnalysis.put(analysisId, convertedItem);
                }
            }

            AnalysisItem condensedItem = condensedMultiSelectByAnalysis.get(analysisId);
            if (condensedItem != null && testResultItem.isHasQualifiedResult()) {
                condensedItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
                condensedItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
                condensedItem.setHasQualifiedResult(true);
                condensedItem.setNormalRange(testResultItem.getNormalRange());
                condensedItem.setPatientName(testResultItem.getPatientName());
            }
        }

        return analysisResultList;
    }

    protected final RecordStatus getSampleRecordStatus(Sample sample) {

        List<ObservationHistory> ohList = observationHistoryService.getAll(null, sample,
                SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID);

        if (ohList.isEmpty()) {
            return null;
        }

        return SpringContext.getBean(IStatusService.class).getRecordStatusForID(ohList.get(0).getValue());
    }

    /**
     * OGC-1027 — the "Check before release" inputs for one queue row, loaded once
     * per rendered row. The rules live in {@link ValidationSignals}; this only
     * fetches what they need. Services are resolved lazily so no new
     * construction-time edge is added to this bean.
     */
    private void populateReleaseSignals(AnalysisItem analysisResultItem, ResultValidationItem testResultItem) {
        analysisResultItem.setCritical(testResultItem.isCritical());
        Analysis analysis = testResultItem.getAnalysis();
        if (analysis == null) {
            analysisResultItem.setQcStatus(ValidationSignals.QC_UNKNOWN);
            return;
        }
        analysisResultItem.setModified(ValidationSignals.isModified(analysis.getRevision()));
        analysisResultItem.setNceOpen(hasOpenNonConformity(analysis));
        analysisResultItem.setAckPending(hasOpenCriticalAlert(analysis));
        analysisResultItem.setQcStatus(qcStatusFor(analysis));
    }

    private boolean hasOpenNonConformity(Analysis analysis) {
        if (analysis.getSampleItem() == null || GenericValidator.isBlankOrNull(analysis.getSampleItem().getId())) {
            return false;
        }
        Integer sampleItemId;
        try {
            sampleItemId = Integer.valueOf(analysis.getSampleItem().getId());
        } catch (NumberFormatException e) {
            return false;
        }
        List<NceSpecimen> specimens = SpringContext.getBean(NceSpecimenService.class)
                .getSpecimenBySampleItemId(sampleItemId);
        if (specimens == null || specimens.isEmpty()) {
            return false;
        }
        NCEventService ncEventService = SpringContext.getBean(NCEventService.class);
        for (NceSpecimen specimen : specimens) {
            if (specimen == null || specimen.getNceId() == null) {
                continue;
            }
            NcEvent event = ncEventService.get(specimen.getNceId());
            if (event != null && ValidationSignals.isNceOpen(event.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOpenCriticalAlert(Analysis analysis) {
        Long analysisId;
        try {
            analysisId = Long.valueOf(analysis.getId());
        } catch (NumberFormatException e) {
            return false;
        }
        List<Alert> alerts = SpringContext.getBean(AlertService.class).getAlertsByEntity("ANALYSIS", analysisId);
        return ValidationSignals.hasOpenCriticalAlert(alerts);
    }

    /**
     * PASS only when every result of the analysis was evaluated and passed; FAIL as
     * soon as any failed; otherwise UNKNOWN — never read as passed (FR-A2).
     */
    private String qcStatusFor(Analysis analysis) {
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        if (results == null || results.isEmpty()) {
            return ValidationSignals.QC_UNKNOWN;
        }
        boolean allPass = true;
        for (Result result : results) {
            QcEvaluation evaluation = result == null ? null : result.getQcEvaluation();
            if (evaluation == QcEvaluation.FAIL) {
                return ValidationSignals.QC_FAIL;
            }
            if (evaluation != QcEvaluation.PASS) {
                allPass = false;
            }
        }
        return allPass ? ValidationSignals.QC_PASS : ValidationSignals.QC_UNKNOWN;
    }

    public final AnalysisItem testResultItemToAnalysisItem(ResultValidationItem testResultItem) {
        AnalysisItem analysisResultItem = new AnalysisItem();
        String testUnits = getUnitsByTestId(testResultItem.getTestId());
        String testName = testResultItem.getTestName();
        String sortOrder = testResultItem.getTestSortNumber();
        Result result = testResultItem.getResult();

        if (result != null && result.getAnalyte() != null
                && ANALYTE_CD4_CT_GENERATED_ID.equals(testResultItem.getResult().getAnalyte().getId())) {
            testUnits = "";
            testName = MessageUtil.getMessage("result.conclusion.cd4");
            analysisResultItem.setShowAcceptReject(false);
            sortOrder = CD4_COUNT_SORT_NUMBER;
        } else if (testResultItem.getTestName().equals(totalTestName)) {
            analysisResultItem.setShowAcceptReject(false);
            analysisResultItem.setReadOnly(true);
            testUnits = testResultItem.getUnitsOfMeasure();
            analysisResultItem.setIsHighlighted(!"100.0".equals(testResultItem.getResult().getValue()));
        }

        testUnits = augmentUOMWithRange(testUnits, testResultItem.getResult());

        analysisResultItem.setAccessionNumber(testResultItem.getAccessionNumber());
        analysisResultItem.setLowerCritical(
                testResultItem.getLowerCritical() == Double.NEGATIVE_INFINITY ? 0 : testResultItem.getLowerCritical());
        analysisResultItem.setHigherCritical(testResultItem.getHigherCritical() == Double.POSITIVE_INFINITY ? 0
                : testResultItem.getHigherCritical());
        analysisResultItem.setNormalRange(testResultItem.getNormalRange());
        analysisResultItem.setPatientName(testResultItem.getPatientName());
        analysisResultItem.setTestName(testName);
        analysisResultItem.setUnits(testUnits);
        analysisResultItem.setAnalysisId(testResultItem.getAnalysis().getId());
        analysisResultItem.setPastNotes(testResultItem.getPastNotes());
        analysisResultItem.setResultId(testResultItem.getResultId());
        if (result != null && result.getTestResult() != null) {
            analysisResultItem.setTestResultComponentId(result.getTestResult().getComponentId());
        }
        analysisResultItem.setResultType(testResultItem.getResultType());
        analysisResultItem.setTestId(testResultItem.getTestId());
        analysisResultItem.setTestSortNumber(sortOrder);
        analysisResultItem.setDictionaryResults(testResultItem.getDictionaryResults());
        analysisResultItem.setDisplayResultAsLog(
                TestIdentityService.getInstance().isTestNumericViralLoad(testResultItem.getTestId()));
        analysisResultItem.setNormal(testResultItem.isNormalResult());
        if (result != null) {
            if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
                Analysis analysis = testResultItem.getAnalysis();
                analysisResultItem.setMultiSelectResultValues(analysisService.getJSONMultiSelectResults(analysis));
            } else {
                analysisResultItem.setResult(getFormattedResult(testResultItem));
            }

            if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(testResultItem.getResultType())) {
                // analysisResultItem.setSignificantDigits( result.getMinNormal().equals(
                // result.getMaxNormal())? -1 : result.getSignificantDigits());
                analysisResultItem.setSignificantDigits(result.getSignificantDigits());
            }
        }
        analysisResultItem.setReflexGroup(testResultItem.isReflexGroup());
        analysisResultItem.setChildReflex(testResultItem.isChildReflex());
        analysisResultItem
                .setNonconforming(testResultItem.isNonconforming() || SpringContext.getBean(IStatusService.class)
                        .matches(testResultItem.getAnalysis().getStatusId(), AnalysisStatus.TechnicalRejected));
        populateReleaseSignals(analysisResultItem, testResultItem);
        analysisResultItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
        analysisResultItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
        analysisResultItem.setQualifiedResultId(testResultItem.getQualificationResultId());
        analysisResultItem.setHasQualifiedResult(testResultItem.isHasQualifiedResult());

        Analysis itemAnalysis = testResultItem.getAnalysis();
        if (itemAnalysis != null && itemAnalysis.getSampleItem() != null) {
            Timestamp holdingStart = itemAnalysis.getSampleItem().getCollectionDate() != null
                    ? itemAnalysis.getSampleItem().getCollectionDate()
                    : itemAnalysis.getSampleItem().getReceivedDate();
            if (holdingStart != null) {
                analysisResultItem.setCollectionDate(DateUtil.convertTimestampToStringDate(holdingStart) + " "
                        + DateUtil.convertTimestampToStringTime(holdingStart));
            }
        }
        if (itemAnalysis != null && itemAnalysis.getTest() != null) {
            analysisResultItem.setTimeHolding(itemAnalysis.getTest().getTimeHolding());
        }
        if (itemAnalysis != null && itemAnalysis.getCompletedDate() != null) {
            analysisResultItem.setResultDate(DateUtil.convertTimestampToStringDate(itemAnalysis.getCompletedDate())
                    + " " + DateUtil.convertTimestampToStringTime(itemAnalysis.getCompletedDate()));
        }
        if (itemAnalysis != null && itemAnalysis.getVectorPoolId() != null
                && !itemAnalysis.getVectorPoolId().isBlank()) {
            analysisResultItem.setVectorPoolId(itemAnalysis.getVectorPoolId());
            try {
                analysisResultItem.setVectorPoolMemberCount(
                        vectorPoolService.countMembersByPoolId(Integer.parseInt(itemAnalysis.getVectorPoolId())));
            } catch (NumberFormatException ignored) {
            }
            if (itemAnalysis.getSampleTypeName() != null) {
                analysisResultItem.setSampleType(itemAnalysis.getSampleTypeName());
            }
        }

        analysisResultItem.setExpandedUncertainty(testResultItem.getExpandedUncertainty());

        return analysisResultItem;
    }

    protected final String getFormattedResult(ResultValidationItem testResultItem) {
        String result = testResultItem.getResult().getValue();
        if (TestIdentityService.getInstance().isTestNumericViralLoad(testResultItem.getTestId())
                && !GenericValidator.isBlankOrNull(result)) {
            return result.split("\\(")[0].trim();
        } else {
            ResultService resultResultService = SpringContext.getBean(ResultService.class);
            return resultResultService.getResultValue(testResultItem.getResult(), false);
        }
    }

    public final String getUnitsByTestId(String testId) {

        String uomName = null;

        if (testId != null) {
            uomName = testIdToUnits.get(testId);
            if (uomName == null) {
                Test test = new Test();
                test.setId(testId);
                test = testService.getTestById(test);

                if (test.getUnitOfMeasure() != null) {
                    uomName = test.getUnitOfMeasure().getName();
                    testIdToUnits.put(testId, uomName);
                } else {
                    testIdToUnits.put(testId, "");
                }
            }
        }
        return uomName;
    }

    public List<AnalysisItem> getValidationAnalysisBySample(Sample sample) {
        List<AnalysisItem> resultList = new ArrayList<>();

        List<ResultValidationItem> testList = getGroupedTestsForSample(sample);
        resultList = testResultListToAnalysisItemList(testList);
        sortByAccessionNumberAndOrder(resultList);
        setGroupingNumbers(resultList);

        return resultList;
    }

    public List<ResultValidationItem> getGroupedTestsForSample(Sample sample) {
        Set<String> excludedAnalysisStatus = new HashSet<>();
        excludedAnalysisStatus.addAll(this.notValidStatus);
        List<Analysis> analysisList = new ArrayList<>(
                analysisService.getAnalysesBySampleIdExcludedByStatusId(sample.getId(), excludedAnalysisStatus));
        // For vector-domain samples also include pool-level analyses (vectorPoolId set,
        // sampleItem null) — the base query joins through sampleItem and misses them.
        if ("V".equals(sample.getDomain())) {
            List<org.openelisglobal.vector.valueholder.VectorPool> pools = vectorPoolService
                    .getBySampleId(sample.getId());
            for (org.openelisglobal.vector.valueholder.VectorPool pool : pools) {
                List<Analysis> poolAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId()));
                if (poolAnalyses != null) {
                    for (Analysis a : poolAnalyses) {
                        if (!excludedAnalysisStatus.contains(a.getStatusId())) {
                            analysisList.add(a);
                        }
                    }
                }
            }
        }
        // QC analyses don't require validator sign-off (the QC engine evaluates them
        // automatically; failures surface via the validation screen's QC banner).
        return getGroupedTestsForAnalysisList(excludeQcAnalyses(analysisList),
                !StatusRules.useRecordStatusForValidation());
    }

    /**
     * Returns the failed-QC samples in the batch identified by the given accession.
     * Drives the validation screen's QC acknowledgment panel (S-08 FR-04). A sample
     * item qualifies only if it has a {@code SampleItemQcProfile} and at least one
     * of its results carries {@code qcEvaluation = FAIL}.
     */
    public List<QcFailureItem> findFailedQcForAccession(String accessionNumber) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            return Collections.emptyList();
        }
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return Collections.emptyList();
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        if (analyses == null || analyses.isEmpty()) {
            return Collections.emptyList();
        }

        // One DAO call to fetch the QC profile for every sample item in the batch.
        Set<Integer> sampleItemIds = new HashSet<>();
        for (Analysis a : analyses) {
            if (a.getSampleItem() != null && a.getSampleItem().getId() != null) {
                try {
                    sampleItemIds.add(Integer.valueOf(a.getSampleItem().getId()));
                } catch (NumberFormatException ignored) {
                    // SampleItem.id should always be numeric; skip rather than fail.
                }
            }
        }
        if (sampleItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, org.openelisglobal.qc.valueholder.SampleItemQcProfile> profilesById = new HashMap<>();
        for (org.openelisglobal.qc.valueholder.SampleItemQcProfile profile : sampleItemQcProfileDAO
                .findBySampleItemIds(new ArrayList<>(sampleItemIds))) {
            profilesById.put(profile.getSampleItemId(), profile);
        }
        if (profilesById.isEmpty()) {
            return Collections.emptyList();
        }

        List<QcFailureItem> failures = new ArrayList<>();
        for (Analysis a : analyses) {
            Integer sid;
            try {
                sid = Integer.valueOf(a.getSampleItem().getId());
            } catch (NumberFormatException e) {
                continue;
            }
            org.openelisglobal.qc.valueholder.SampleItemQcProfile profile = profilesById.get(sid);
            if (profile == null) {
                continue;
            }
            List<Result> results = resultService.getResultsByAnalysis(a);
            if (results == null) {
                continue;
            }
            for (Result r : results) {
                if (r.getQcEvaluation() == QcEvaluation.FAIL) {
                    QcFailureItem item = new QcFailureItem();
                    item.setAnalysisId(a.getId());
                    item.setAccessionNumber(accessionNumber);
                    item.setQcType(profile.getQcType());
                    item.setTestName(a.getTest() != null ? a.getTest().getName() : null);
                    item.setResultValue(r.getValue());
                    item.setQcEvaluationDetail(r.getQcEvaluationDetail());
                    failures.add(item);
                    break;
                }
            }
        }
        return failures;
    }

    public void addIdentifingPatientInfo(Patient patient, PatientInfoForm form) {

        if (patient == null) {
            return;
        }

        PatientIdentityTypeMap identityMap = PatientIdentityTypeMap.getInstance();
        List<PatientIdentity> identityList = PatientUtil.getIdentityListForPatient(patient);

        if (!depersonalize) {
            form.setFirstName(patient.getPerson().getFirstName());
            form.setLastName(patient.getPerson().getLastName());
            form.setDob(patient.getBirthDateForDisplay());
            form.setGender(patient.getGender());
        }

        form.setSt(identityMap.getIdentityValue(identityList, "ST"));
        form.setNationalId(GenericValidator.isBlankOrNull(patient.getNationalId()) ? patient.getExternalId()
                : patient.getNationalId());
        form.setSubjectNumber(patientService.getSubjectNumber(patient));
    }
}
