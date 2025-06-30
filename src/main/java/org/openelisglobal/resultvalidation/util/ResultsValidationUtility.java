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
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.action.util.ResultValidationItem;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
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

    private Patient currentPatient;
    protected String SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID;
    protected String CD4_COUNT_SORT_NUMBER;

    protected String ANALYTE_CD4_CT_GENERATED_ID;
    protected String CONCLUSION_ID;

    protected List<Integer> notValidStatus = new ArrayList<>();
    protected Map<String, String> testIdToUnits = new HashMap<>();
    protected Map<String, Boolean> accessionToValidMap;
    protected String totalTestName = "";
    private static boolean depersonalize = FormFields.getInstance().useField(Field.DepersonalizedResults);

    @PostConstruct
    private void initilaizeGlobalVariables() {
        notValidStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        notValidStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        notValidStatus.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
        notValidStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
        notValidStatus.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
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

    public List<AnalysisItem> getResultValidationList(List<Integer> statusList, String testSectionId,
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

    public int getCountResultValidationList(List<Integer> statusList, String testSectionId) {

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
            List<Integer> statusList) {

        // List<Analysis> analysisList =
        // analysisService.getAllAnalysisByTestSectionAndStatus(sectionId, statusList,
        // false);
        // getPage for validation
        List<Analysis> analysisList = analysisService.getPageAnalysisByTestSectionAndStatus(sectionId, statusList,
                false);
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
    }

    @SuppressWarnings("unchecked")
    public final List<ResultValidationItem> getPageUnValidatedTestResultItemsAtAccessionNumber(String accessionNumber,
            List<Integer> statusList) {

        // List<Analysis> analysisList =
        // analysisService.getAllAnalysisByTestSectionAndStatus(sectionId, statusList,
        // false);
        // getPage for validation
        List<Analysis> analysisList = analysisService.getPageAnalysisAtAccessionNumberAndStatus(accessionNumber,
                statusList, false);
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
    }

    @SuppressWarnings("unchecked")
    public final List<ResultValidationItem> getPageUnValidatedTestResultItemsByTestDate(String date,
            List<Integer> statusList) {

        List<Analysis> analysisList = analysisService.getAnalysisStartedOn(DateUtil.convertStringDateToSqlDate(date))
                .stream().filter(analysis -> statusList.contains(Integer.valueOf(analysis.getStatusId())))
                .collect(Collectors.toList());
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
    }

    @SuppressWarnings("unchecked")
    public final int getCountUnValidatedTestResultItemsInTestSection(String sectionId, List<Integer> statusList) {
        return analysisService.getCountAnalysisByTestSectionAndStatus(sectionId, statusList);
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

            if (ignoreRecordStatus || sampleReadyForValidation(analysis.getSampleItem().getSample())) {
                List<ResultValidationItem> testResultItemList = getResultItemFromAnalysis(analysis);
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

            if (ignoreRecordStatus || sampleReadyForValidation(analysis.getSampleItem().getSample())) {
                List<ResultValidationItem> testResultItemList = getResultItemFromAnalysis(analysis);
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

    public final List<ResultValidationItem> getResultItemFromAnalysis(Analysis analysis) throws LIMSRuntimeException {
        List<ResultValidationItem> testResultList = new ArrayList<>();

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

        ResultValidationItem parentItem = null;
        for (Result result : resultList) {
            if (parentItem != null && result.getParentResult() != null
                    && parentItem.getResultId().equals(result.getParentResult().getId())) {
                parentItem.setQualifiedResultValue(result.getValue());
                parentItem.setHasQualifiedResult(true);
                parentItem.setQualificationResultId(result.getId());
                continue;
            }

            ResultValidationItem resultItem = createTestResultItem(analysis, analysis.getTest(),
                    analysis.getSampleItem().getSortOrder(), result,
                    analysis.getSampleItem().getSample().getAccessionNumber(), notes);

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

        String displayTestName = TestServiceImpl.getLocalizedTestNameWithType(test);
        // displayTestName = augmentTestNameWithRange(displayTestName, result);

        ResultLimit resultLimit = SpringContext.getBean(ResultLimitService.class).getResultLimitForTestAndPatient(test,
                currentPatient);
        ResultValidationItem testItem = new ResultValidationItem();

        testItem.setAccessionNumber(accessionNumber);
        testItem.setAnalysis(analysis);
        testItem.setSequenceNumber(sequenceNumber);
        testItem.setTestName(displayTestName);
        testItem.setTestId(test.getId());
        setResultLimitDependencies(resultLimit, testItem, testResults);
        testItem.setAnalysisMethod(analysis.getAnalysisType());
        testItem.setResult(result);
        testItem.setDictionaryResults(getAnyDictonaryValues(testResults));
        testItem.setResultType(getTestResultType(testResults));
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

        if (testResults != null && testResults.size() > 0
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResults.get(0).getTestResultType())) {
            values = new ArrayList<>();
            values.add(new IdValuePair("0", ""));

            for (TestResult testResult : testResults) {
                // Note: result group use to be a criteria but was removed, if
                // results are not as expected investigate
                if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
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
         * ResultValidationItem but they all need to be condensed into one AnalysisItem.
         * There is a many to one mapping. The first multiselect result we have gets
         * rolled into one AnalysisItem and the rest are skipped but we want to capture
         * any qualified results
         */
        boolean multiResultEntered = false;
        String currentAccession = null;
        AnalysisItem currentMultiSelectAnalysisItem = null;
        for (ResultValidationItem testResultItem : testResultList) {
            if (!testResultItem.getAccessionNumber().equals(currentAccession)) {
                currentAccession = testResultItem.getAccessionNumber();
                currentMultiSelectAnalysisItem = null;
                multiResultEntered = false;
            }
            if (!multiResultEntered) {
                AnalysisItem convertedItem = testResultItemToAnalysisItem(testResultItem);
                analysisResultList.add(convertedItem);
                if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
                    multiResultEntered = true;
                    currentMultiSelectAnalysisItem = convertedItem;
                }
            }
            if (currentMultiSelectAnalysisItem != null && testResultItem.isHasQualifiedResult()) {
                currentMultiSelectAnalysisItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
                currentMultiSelectAnalysisItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
                currentMultiSelectAnalysisItem.setHasQualifiedResult(true);
                currentMultiSelectAnalysisItem.setNormalRange(testResultItem.getNormalRange());
                currentMultiSelectAnalysisItem.setPatientName(testResultItem.getPatientName());
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
        analysisResultItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
        analysisResultItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
        analysisResultItem.setQualifiedResultId(testResultItem.getQualificationResultId());
        analysisResultItem.setHasQualifiedResult(testResultItem.isHasQualifiedResult());

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
        Set<Integer> excludedAnalysisStatus = new HashSet<>();
        excludedAnalysisStatus.addAll(this.notValidStatus);
        List<Analysis> analysisList = analysisService.getAnalysesBySampleIdExcludedByStatusId(sample.getId(),
                excludedAnalysisStatus);
        return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
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
