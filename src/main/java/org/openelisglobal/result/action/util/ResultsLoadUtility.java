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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.result.action.util;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analysis.valueholder.ResultFile;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.TestIdentityService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.patient.form.PatientInfoForm;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.beanItems.TestResultItem.ResultDisplayType;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class ResultsLoadUtility {

    private static final boolean SORT_FORWARD = true;

    public static final String TESTKIT = "TestKit";

    private static final String NO_PATIENT_NAME = " ";
    private static final String NO_PATIENT_INFO = " ";

    private List<Sample> samples;
    private String currentDate = "";
    private Sample currSample;

    private Set<Integer> excludedAnalysisStatus = new HashSet<>();
    private List<Integer> analysisStatusList = new ArrayList<>();
    private List<Integer> sampleStatusList = new ArrayList<>();

    private List<InventoryKitItem> activeKits;

    private Patient currentPatient;

    @Autowired
    private PatientService patientService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private ResultSignatureService resultSignatureService;
    @Autowired
    private ResultInventoryService resultInventoryService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private AnalyteService analyteService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private TestResultService testResultService;

    private final StatusRules statusRules = new StatusRules();

    private boolean inventoryNeeded = false;

    private String ANALYTE_CONCLUSION_ID;
    private String ANALYTE_CD4_CNT_CONCLUSION_ID;
    private static final String NUMERIC_RESULT_TYPE = "N";
    private static boolean depersonalize = FormFields.getInstance().useField(Field.DepersonalizedResults);
    private boolean useTechSignature = ConfigurationProperties.getInstance()
            .isPropertyValueEqual(Property.resultTechnicianName, "true");
    private static boolean supportReferrals = FormFields.getInstance().useField(Field.ResultsReferral);
    private static boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);
    private boolean useCurrentUserAsTechDefault = ConfigurationProperties.getInstance()
            .isPropertyValueEqual(Property.autoFillTechNameUser, "true");
    private String currentUserName = "";
    private int reflexGroup = 1;
    private boolean lockCurrentResults = false;

    @PostConstruct
    public void initializeGlobalVariables() {
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        analyte = analyteService.getAnalyteByName(analyte, false);
        ANALYTE_CONCLUSION_ID = analyte == null ? "" : analyte.getId();
        analyte = new Analyte();
        analyte.setAnalyteName("generated CD4 Count");
        analyte = analyteService.getAnalyteByName(analyte, false);
        ANALYTE_CD4_CNT_CONCLUSION_ID = analyte == null ? "" : analyte.getId();
    }

    public void setSysUser(String currentUserId) {
        if (useCurrentUserAsTechDefault) {
            SystemUser systemUser = new SystemUser();
            systemUser.setId(currentUserId);
            systemUserService.getData(systemUser);

            if (systemUser.getId() != null) {
                currentUserName = systemUser.getFirstName() + " " + systemUser.getLastName();
            }
        }
    }

    /*
     * N.B. The patient info is used to determine the limits for the results, not
     * for including patient information
     */
    public List<TestResultItem> getGroupedTestsForSample(Sample sample) {
        return getGroupedTestsForSample(sample, sampleHumanService.getPatientForSample(sample));
    }

    /*
     * N.B. The patient info is used to determine the limits for the results, not
     * for including patient information
     */
    public List<TestResultItem> getGroupedTestsForSample(Sample sample, Patient patient) {

        reflexGroup = 1;
        activeKits = null;
        samples = new ArrayList<>();

        if (sample != null) {
            samples.add(sample);
        }

        currentPatient = patient;
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());

        return getGroupedTestsForSamples();
    }

    public List<TestResultItem> getGroupedTestsForPatient(Patient patient) {
        reflexGroup = 1;
        activeKits = null;
        inventoryNeeded = false;

        currentPatient = patient;
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());

        samples = sampleHumanService.getSamplesForPatient(patient.getId());

        return getGroupedTestsForSamples();
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

    public List<TestResultItem> getUnfinishedTestResultItemsInTestSection(String testSectionId) {

        List<Analysis> fullAnalysisList = analysisService.getAllAnalysisByTestSectionAndStatus(testSectionId,
                analysisStatusList, sampleStatusList);
        // request.setAttribute("analysisesSize", fullAnalysisList.size());
        // List<Analysis> analysisList =
        // analysisService.getPageAnalysisByTestSectionAndStatus(testSectionId,
        // analysisStatusList, sampleStatusList);

        return getGroupedTestsForAnalysisList(fullAnalysisList, SORT_FORWARD);
    }

    public int getTotalCountAnalysisByTestSectionAndStatus(String testSectionId) {
        return analysisService.getCountAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList,
                sampleStatusList);
    }

    public List<TestResultItem> getGroupedTestsForAnalysisList(List<Analysis> filteredAnalysisList, boolean forwardSort)
            throws LIMSRuntimeException {

        activeKits = null;
        inventoryNeeded = false;
        reflexGroup = 1;

        List<TestResultItem> selectedTestList = new ArrayList<>();

        for (Analysis analysis : filteredAnalysisList) {
            patientService = SpringContext.getBean(PatientService.class);
            SampleService sampleService = SpringContext.getBean(SampleService.class);
            Sample sample = analysis.getSampleItem().getSample();
            currentPatient = sampleService.getPatient(sample);

            String patientName = "";
            String patientInfo;
            String nationalId = patientService.getNationalId(currentPatient);
            if (depersonalize) {
                patientInfo = GenericValidator.isBlankOrNull(nationalId) ? patientService.getExternalId(currentPatient)
                        : nationalId;
            } else {
                patientName = patientService.getLastFirstName(currentPatient);
                patientInfo = nationalId + ", " + patientService.getGender(currentPatient) + ", "
                        + patientService.getBirthdayForDisplay(currentPatient);
            }

            currSample = analysis.getSampleItem().getSample();
            List<TestResultItem> testResultItemList = getTestResultItemFromAnalysis(analysis, patientName, patientInfo,
                    nationalId);

            for (TestResultItem selectionItem : testResultItemList) {
                selectedTestList.add(selectionItem);
            }
        }

        if (forwardSort) {
            sortByAccessionAndSequence(selectedTestList);
        } else {
            reverseSortByAccessionAndSequence(selectedTestList);
        }

        setSampleGroupingNumbers(selectedTestList);
        addUserSelectionReflexes(selectedTestList);

        return selectedTestList;
    }

    private void reverseSortByAccessionAndSequence(List<? extends ResultItem> selectedTest) {
        Collections.sort(selectedTest, new Comparator<ResultItem>() {
            @Override
            public int compare(ResultItem a, ResultItem b) {
                int accessionSort = b.getSequenceAccessionNumber().compareTo(a.getSequenceAccessionNumber());

                if (accessionSort == 0) { // only the accession number sorting is reversed
                    if (!GenericValidator.isBlankOrNull(a.getTestSortOrder())
                            && !GenericValidator.isBlankOrNull(b.getTestSortOrder())) {
                        try {
                            return Integer.parseInt(a.getTestSortOrder()) - Integer.parseInt(b.getTestSortOrder());
                        } catch (NumberFormatException e) {
                            return a.getTestName().compareTo(b.getTestName());
                        }

                    } else {
                        return a.getTestName().compareTo(b.getTestName());
                    }
                }

                return accessionSort;
            }
        });
    }

    public void sortByAccessionAndSequence(List<? extends ResultItem> selectedTest) {
        Collections.sort(selectedTest, new Comparator<ResultItem>() {
            @Override
            public int compare(ResultItem a, ResultItem b) {
                int accessionSort = a.getSequenceAccessionNumber().compareTo(b.getSequenceAccessionNumber());

                if (accessionSort == 0) {
                    if (!GenericValidator.isBlankOrNull(a.getTestSortOrder())
                            && !GenericValidator.isBlankOrNull(b.getTestSortOrder())) {
                        try {
                            return Integer.parseInt(a.getTestSortOrder()) - Integer.parseInt(b.getTestSortOrder());
                        } catch (NumberFormatException e) {
                            return a.getTestName().compareTo(b.getTestName());
                        }

                    } else if (!GenericValidator.isBlankOrNull(a.getTestName())
                            && !GenericValidator.isBlankOrNull(b.getTestName())) {
                        return a.getTestName().compareTo(b.getTestName());
                    }
                }

                return accessionSort;
            }
        });
    }

    public void setSampleGroupingNumbers(List<? extends ResultItem> selectedTests) {
        int groupingNumber = 1; // the header is always going to be 0

        String currentSequenceAccession = "";

        for (ResultItem item : selectedTests) {
            if (!currentSequenceAccession.equals(item.getSequenceAccessionNumber()) || item.getIsGroupSeparator()) {
                groupingNumber++;
                currentSequenceAccession = item.getSequenceAccessionNumber();
                item.setShowSampleDetails(true);
            } else {
                item.setShowSampleDetails(false);
            }

            item.setSampleGroupingNumber(groupingNumber);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Test> getTestsInSection(String id) {

        return testService.getTestsByTestSection(id);
    }

    private List<TestResultItem> getTestResultItemFromAnalysis(Analysis analysis, String patientName,
            String patientInfo, String nationalId) throws LIMSRuntimeException {
        List<TestResultItem> testResultList = new ArrayList<>();

        SampleItem sampleItem = analysis.getSampleItem();
        List<Result> resultList = resultService.getResultsByAnalysis(analysis);

        ResultInventory testKit = null;

        String techSignature = "";
        String techSignatureId = "";

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

        boolean multiSelectionResult = false;
        for (Result result : resultList) {
            // If the parentResult has a value then this result was handled with
            // the parent
            if (result != null && result.getParentResult() != null) {
                continue;
            }

            if (result != null) {
                if (useTechSignature) {
                    List<ResultSignature> signatures = resultSignatureService.getResultSignaturesByResults(resultList);

                    for (ResultSignature signature : signatures) {
                        // we no longer use supervisor signature but there may be some in db
                        if (!signature.getIsSupervisor()) {
                            techSignature = signature.getNonUserName();
                            techSignatureId = signature.getId();
                        }
                    }
                }

                testKit = getInventoryForResult(result);

                multiSelectionResult = TypeOfTestResultServiceImpl.ResultType
                        .isMultiSelectVariant(result.getResultType());
            }

            String initialConditions = getInitialSampleConditionString(sampleItem);
            NoteType[] noteTypes = { NoteType.EXTERNAL, NoteType.INTERNAL, NoteType.REJECTION_REASON,
                    NoteType.NON_CONFORMITY };
            NoteService noteService = SpringContext.getBean(NoteService.class);
            String notes = noteService.getNotesAsString(analysis, true, true, "<br/>", noteTypes, false);

            TestResultItem resultItem = createTestResultItem(analysis, testKit, notes, sampleItem.getSortOrder(),
                    result, sampleItem.getSample().getAccessionNumber(), patientName, patientInfo, techSignature,
                    techSignatureId, initialConditions, SpringContext.getBean(TypeOfSampleService.class)
                            .getTypeOfSampleNameForId(sampleItem.getTypeOfSampleId()));
            resultItem.setNationalId(nationalId);
            testResultList.add(resultItem);

            if (multiSelectionResult) {
                break;
            }
        }

        return testResultList;
    }

    private String getInitialSampleConditionString(SampleItem sampleItem) {
        if (useInitialSampleCondition) {
            List<ObservationHistory> observationList = observationHistoryService
                    .getObservationHistoriesBySampleItemId(sampleItem.getId());
            StringBuilder conditions = new StringBuilder();

            for (ObservationHistory observation : observationList) {
                if (ValueType.DICTIONARY.getCode().equals(observation.getValueType())) {
                    Dictionary dictionary = dictionaryService.getDictionaryById(observation.getValue());
                    if (dictionary != null) {
                        conditions.append(dictionary.getLocalizedName());
                        conditions.append(", ");
                    }
                } else if (ValueType.LITERAL.getCode().equals(observation.getValueType())) {
                    conditions.append(observation.getValue());
                    conditions.append(", ");
                } else if (ValueType.KEY.getCode().equals(observation.getValueType())) {
                    Localization localization = localizationService.get(observation.getValue());
                    conditions.append(localization.getLocalizedValue());
                    conditions.append(", ");
                }
            }

            if (conditions.length() > 2) {
                return conditions.substring(0, conditions.length() - 2);
            }
        }

        return null;
    }

    private ResultInventory getInventoryForResult(Result result) throws LIMSRuntimeException {
        List<ResultInventory> inventoryList = resultInventoryService.getResultInventorysByResult(result);

        return inventoryList.size() > 0 ? inventoryList.get(0) : null;
    }

    private List<TestResultItem> getGroupedTestsForSamples() {

        List<TestResultItem> testList = new ArrayList<>();

        TestResultItem[] tests = getSortedTestsFromSamples();

        String currentAccessionNumber = "";

        for (TestResultItem testItem : tests) {
            if (!currentAccessionNumber.equals(testItem.getAccessionNumber())) {

                TestResultItem separatorItem = new TestResultItem();
                separatorItem.setIsGroupSeparator(true);
                separatorItem.setAccessionNumber(testItem.getAccessionNumber());
                separatorItem.setReceivedDate(testItem.getReceivedDate());
                testList.add(separatorItem);

                currentAccessionNumber = testItem.getAccessionNumber();
                reflexGroup++;
            }

            testList.add(testItem);
        }

        return testList;
    }

    private TestResultItem[] getSortedTestsFromSamples() {

        List<TestResultItem> testList = new ArrayList<>();

        for (Sample sample : samples) {
            currSample = sample;
            List<SampleItem> sampleItems = getSampleItemsForSample(sample);

            for (SampleItem item : sampleItems) {
                List<Analysis> analysisList = getAnalysisForSampleItem(item);

                for (Analysis analysis : analysisList) {

                    List<TestResultItem> selectedItemList = getTestResultItemFromAnalysis(analysis, NO_PATIENT_NAME,
                            NO_PATIENT_INFO, "");

                    for (TestResultItem selectedItem : selectedItemList) {
                        testList.add(selectedItem);
                    }
                }
            }
        }

        reverseSortByAccessionAndSequence(testList);
        setSampleGroupingNumbers(testList);
        addUserSelectionReflexes(testList);

        TestResultItem[] testArray = new TestResultItem[testList.size()];
        testList.toArray(testArray);

        return testArray;
    }

    private void addUserSelectionReflexes(List<TestResultItem> testList) {
        TestReflexUtil reflexUtil = new TestReflexUtil();

        Map<String, TestResultItem> groupedSibReflexMapping = new HashMap<>();

        for (TestResultItem resultItem : testList) {
            // N.B. showSampleDetails should be renamed. It means that it is the first
            // result for that group of accession numbers
            if (resultItem.isShowSampleDetails()) {
                groupedSibReflexMapping = new HashMap<>();
                reflexGroup++;
            }

            if (resultItem.isReflexGroup()) {
                resultItem.setReflexParentGroup(reflexGroup);
            }

            List<TestReflex> reflexList = reflexUtil.getPossibleUserChoiceTestReflexsForTest(resultItem.getTestId());
            resultItem.setUserChoiceReflex(reflexList.size() > 0);

            boolean possibleSibs = !groupedSibReflexMapping.isEmpty();

            for (TestReflex testReflex : reflexList) {
                if (!GenericValidator.isBlankOrNull(testReflex.getSiblingReflexId())) {
                    if (possibleSibs) {
                        TestResultItem sibTestResultItem = groupedSibReflexMapping.get(testReflex.getSiblingReflexId());
                        if (sibTestResultItem != null) {
                            Random r = new Random();
                            String key1 = Long.toString(Math.abs(r.nextLong()), 36);
                            String key2 = Long.toString(Math.abs(r.nextLong()), 36);

                            sibTestResultItem.setThisReflexKey(key1);
                            sibTestResultItem.setSiblingReflexKey(key2);

                            resultItem.setThisReflexKey(key2);
                            resultItem.setSiblingReflexKey(key1);

                            break;
                        }
                    }
                    groupedSibReflexMapping.put(testReflex.getId(), resultItem);
                }
            }
        }
    }

    private List<SampleItem> getSampleItemsForSample(Sample sample) {
        return sampleItemService.getSampleItemsBySampleId(sample.getId());
    }

    private List<Analysis> getAnalysisForSampleItem(SampleItem item) {
        return analysisService.getAnalysesBySampleItemsExcludingByStatusIds(item, excludedAnalysisStatus);
    }

    private TestResultItem createTestResultItem(Analysis analysis, ResultInventory testKit, String notes,
            String sequenceNumber, Result result, String accessionNumber, String patientName, String patientInfo,
            String techSignature, String techSignatureId, String initialSampleConditions, String sampleType) {

        TestService testService = SpringContext.getBean(TestService.class);
        Test test = analysisService.getTest(analysis);
        ResultLimit resultLimit = SpringContext.getBean(ResultLimitService.class).getResultLimitForTestAndPatient(test,
                currentPatient);

        String receivedDate = currSample == null ? getCurrentDate() : currSample.getReceivedDateForDisplay();
        String testMethodName = testService.getTestMethodName(test);
        List<TestResult> testResults = testService.getPossibleTestResults(test);

        String testKitId = null;
        String testKitInventoryId = null;
        Result testKitResult = new Result();
        boolean testKitInactive = false;

        if (testKit != null) {
            testKitId = testKit.getId();
            testKitInventoryId = testKit.getInventoryLocationId();
            testKitResult.setId(testKit.getResultId());
            resultService.getData(testKitResult);
            testKitInactive = kitNotInActiveKitList(testKitInventoryId);
        }

        String displayTestName = analysisService.getTestDisplayName(analysis);

        boolean isConclusion = false;
        boolean isCD4Conclusion = false;

        if (result != null && result.getAnalyte() != null) {
            isConclusion = result.getAnalyte().getId().equals(ANALYTE_CONCLUSION_ID);
            isCD4Conclusion = result.getAnalyte().getId().equals(ANALYTE_CD4_CNT_CONCLUSION_ID);

            if (isConclusion) {
                displayTestName = MessageUtil.getMessage("result.conclusion");
            } else if (isCD4Conclusion) {
                displayTestName = MessageUtil.getMessage("result.conclusion.cd4");
            }
        }

        String referralId = null;
        String referralReasonId = null;
        boolean referralCanceled = false;
        if (supportReferrals) {
            Referral referral = referralService.getReferralByAnalysisId(analysis.getId());
            if (referral != null) {
                referralCanceled = referral.isCanceled();
                referralId = referral.getId();
                if (!referral.isCanceled()) {
                    referralReasonId = referral.getReferralReasonId();
                }
            }
        }

        String uom = testService.getUOM(test, isCD4Conclusion);

        String testDate = GenericValidator.isBlankOrNull(analysisService.getCompletedDateForDisplay(analysis))
                ? getCurrentDate()
                : analysisService.getCompletedDateForDisplay(analysis);
        ResultDisplayType resultDisplayType = testService.getDisplayTypeForTestMethod(test);
        if (resultDisplayType != ResultDisplayType.TEXT) {
            inventoryNeeded = true;
        }
        ResultFile file = analysis.getResultFile();

        TestResultItem.ResultFileForm form = new TestResultItem.ResultFileForm();
        if (file != null) {
            form.setFileName(file.getFileName());
            form.setFileType(file.getFileType());
            form.setContent(file.getContent());
            form.setUploadedAt(file.getUploadedAt());
            form.setLastupdated(file.getLastupdated());
        }

        TestResultItem testItem = new TestResultItem();

        testItem.setAccessionNumber(accessionNumber);
        testItem.setAnalysisId(analysis.getId());
        testItem.setSequenceNumber(sequenceNumber);
        testItem.setReceivedDate(receivedDate);
        testItem.setTestName(displayTestName);
        testItem.setTestId(test.getId());
        setResultLimitDependencies(resultLimit, testItem, testResults);
        testItem.setPatientName(patientName);
        testItem.setPatientInfo(patientInfo);
        testItem.setReportable(testService.isReportable(test));
        testItem.setUnitsOfMeasure(uom);
        testItem.setTestDate(testDate);
        testItem.setResultDisplayType(resultDisplayType);
        testItem.setAnalysisMethod(analysisService.getAnalysisType(analysis));
        testItem.setTestMethod(analysisService.getMethodId(analysis));
        testItem.setResult(result);
        testItem.setResultValue(getFormattedResultValue(result));
        testItem.setMultiSelectResultValues(analysisService.getJSONMultiSelectResults(analysis));
        testItem.setAnalysisStatusId(analysisService.getStatusId(analysis));
        // setDictionaryResults must come after setResultType, it may override it
        testItem.setResultType(testService.getResultType(test));
        setDictionaryResults(testItem, isConclusion, result, testResults);

        testItem.setTechnician(techSignature);
        testItem.setTechnicianSignatureId(techSignatureId);
        testItem.setTestKitId(testKitId);
        testItem.setTestKitInventoryId(testKitInventoryId);
        testItem.setTestKitInactive(testKitInactive);
        testItem.setReadOnly(isReadOnly(isConclusion, isCD4Conclusion) && result != null && result.getId() != null);
        testItem.setReferralId(referralId);
        testItem.setReferredOut(!GenericValidator.isBlankOrNull(referralId) && !referralCanceled);
        testItem.setShadowReferredOut(testItem.isReferredOut());
        testItem.setReferralReasonId(referralReasonId);
        testItem.setReferralCanceled(referralCanceled);
        testItem.setInitialSampleCondition(initialSampleConditions);
        testItem.setSampleType(sampleType);
        testItem.setTestSortOrder(testService.getSortOrder(test));
        testItem.setFailedValidation(statusRules.hasFailedValidation(analysisService.getStatusId(analysis)));
        if (useCurrentUserAsTechDefault && GenericValidator.isBlankOrNull(testItem.getTechnician())) {
            testItem.setTechnician(currentUserName);
        }
        testItem.setReflexGroup(analysisService.getTriggeredReflex(analysis));
        testItem.setChildReflex(
                analysisService.getTriggeredReflex(analysis) && analysisService.resultIsConclusion(result, analysis));
        testItem.setPastNotes(notes);
        testItem.setDisplayResultAsLog(hasLogValue(test));
        testItem.setResultFile(form);
        testItem.setNonconforming(
                analysisService.isParentNonConforming(analysis) || SpringContext.getBean(IStatusService.class)
                        .matches(analysisService.getStatusId(analysis), AnalysisStatus.TechnicalRejected));
        if (FormFields.getInstance().useField(Field.QaEventsBySection)) {
            testItem.setNonconforming(testItem.isNonconforming() || getQaEventByTestSection(analysis));
        }

        Result quantifiedResult = analysisService.getQuantifiedResult(analysis);
        if (quantifiedResult != null) {
            testItem.setQualifiedResultId(quantifiedResult.getId());
            testItem.setQualifiedResultValue(quantifiedResult.getValue());
            testItem.setHasQualifiedResult(true);
        }

        if (!testResults.isEmpty() && NUMERIC_RESULT_TYPE.equals(testResults.get(0).getTestResultType())
                && !GenericValidator.isBlankOrNull(testResults.get(0).getSignificantDigits())) {
            testItem.setSignificantDigits(Integer.parseInt(testResults.get(0).getSignificantDigits()));
        }

        if (test.getDefaultTestResult() != null) {
            testItem.setDefaultResultValue(test.getDefaultTestResult().getValue());
        }
        return testItem;
    }

    private boolean isReadOnly(boolean isConclusion, boolean isCD4Conclusion) {
        return isConclusion || isCD4Conclusion || isLockCurrentResults();
    }

    private void setResultLimitDependencies(ResultLimit resultLimit, TestResultItem testItem,
            List<TestResult> testResults) {
        if (resultLimit != null) {
            testItem.setResultLimitId(resultLimit.getId());
            testItem.setLowerNormalRange(
                    resultLimit.getLowNormal() == Double.NEGATIVE_INFINITY ? 0 : resultLimit.getLowNormal());
            testItem.setUpperNormalRange(
                    resultLimit.getHighNormal() == Double.POSITIVE_INFINITY ? 0 : resultLimit.getHighNormal());
            testItem.setLowerAbnormalRange(
                    resultLimit.getLowValid() == Double.NEGATIVE_INFINITY ? 0 : resultLimit.getLowValid());
            testItem.setUpperAbnormalRange(
                    resultLimit.getHighValid() == Double.POSITIVE_INFINITY ? 0 : resultLimit.getHighValid());
            testItem.setLowerCritical(
                    resultLimit.getLowCritical() == Double.NEGATIVE_INFINITY ? 0 : resultLimit.getLowCritical());
            testItem.setHigherCritical(
                    resultLimit.getHighCritical() == Double.POSITIVE_INFINITY ? 0 : resultLimit.getHighCritical());

            testItem.setValid(getIsValid(testItem.getResultValue(), resultLimit));
            testItem.setNormal(getIsNormal(testItem.getResultValue(), resultLimit));
            testItem.setNormalRange(SpringContext.getBean(ResultLimitService.class).getDisplayReferenceRange(
                    resultLimit, testResults.isEmpty() ? "0" : testResults.get(0).getSignificantDigits(), " - "));
        }
    }

    private void setDictionaryResults(TestResultItem testItem, boolean isConclusion, Result result,
            List<TestResult> testResults) {
        if (isConclusion) {
            testItem.setDictionaryResults(getAnyDictionaryValues(result));
        } else {
            setDictionaryResults(testItem, testResults, result);
        }
    }

    private void setDictionaryResults(TestResultItem testItem, List<TestResult> testResults, Result result) {

        List<IdValuePair> values = null;
        Dictionary dictionary;

        if (testResults != null && !testResults.isEmpty()
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResults.get(0).getTestResultType())) {
            values = new ArrayList<>();

            Collections.sort(testResults, new Comparator<TestResult>() {
                @Override
                public int compare(TestResult o1, TestResult o2) {
                    if (GenericValidator.isBlankOrNull(o1.getSortOrder())
                            || GenericValidator.isBlankOrNull(o2.getSortOrder())) {
                        return 1;
                    }

                    return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
                }
            });

            String qualifiedDictionaryIds = "";
            for (TestResult testResult : testResults) {
                if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
                    dictionary = new Dictionary();
                    dictionary.setId(testResult.getValue());
                    dictionaryService.getData(dictionary);
                    String displayValue = dictionary.getLocalizedName();

                    if ("unknown".equals(displayValue)) {
                        displayValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                                ? dictionary.getDictEntry()
                                : dictionary.getLocalAbbreviation();
                    }
                    values.add(new IdValuePair(testResult.getValue(), displayValue));
                    if (testResult.getIsQuantifiable()) {
                        if (!GenericValidator.isBlankOrNull(qualifiedDictionaryIds)) {
                            qualifiedDictionaryIds += ",";
                        }
                        qualifiedDictionaryIds += testResult.getValue();
                        setQualifiedValues(testItem, result);
                    }
                }
            }

            if (!GenericValidator.isBlankOrNull(qualifiedDictionaryIds)) {
                testItem.setQualifiedDictionaryId("[" + qualifiedDictionaryIds + "]");
            }
        }
        if (!GenericValidator.isBlankOrNull(testItem.getQualifiedResultValue())) {
            testItem.setHasQualifiedResult(true);
        }

        testItem.setDictionaryResults(values);
    }

    private void setQualifiedValues(TestResultItem testItem, Result result) {
        if (result != null) {
            List<Result> results = resultService.getChildResults(result.getId());
            if (!results.isEmpty()) {
                Result childResult = results.get(0);
                testItem.setQualifiedResultId(childResult.getId());
                testItem.setQualifiedResultValue(childResult.getValue());
            }
        }
    }

    private String getFormattedResultValue(Result result) {
        ResultService resultResultService = SpringContext.getBean(ResultService.class);
        return result != null ? resultResultService.getResultValue(result, false) : "";
    }

    private boolean hasLogValue(Test test) { // Analysis analysis, String resultValue) {
        // TO-DO refactor
        // if ( ){
        // if (GenericValidator.isBlankOrNull(resultValue)) {
        // return true;
        // }
        // try {
        // Double.parseDouble(resultValue);
        // return true;
        // } catch (NumberFormatException e) {
        // return false;
        // }

        // return true;
        // }

        // return false;
        return TestIdentityService.getInstance().isTestNumericViralLoad(test);
    }

    private List<IdValuePair> getAnyDictionaryValues(Result result) {
        List<IdValuePair> values = null;

        if (result != null && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            values = new ArrayList<>();

            Dictionary dictionaryValue = new Dictionary();
            dictionaryValue.setId(result.getValue());
            dictionaryService.getData(dictionaryValue);

            List<Dictionary> dictionaryList = dictionaryService
                    .getDictionaryEntriesByCategoryId(dictionaryValue.getDictionaryCategory().getId());

            for (Dictionary dictionary : dictionaryList) {
                String displayValue = dictionary.getLocalizedName();

                if ("unknown".equals(displayValue)) {
                    displayValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                            ? dictionary.getDictEntry()
                            : dictionary.getLocalAbbreviation();
                }
                values.add(new IdValuePair(dictionary.getId(), displayValue));
            }
        }

        return values;
    }

    private boolean getIsValid(String resultValue, ResultLimit resultLimit) {
        boolean valid = true;

        if (!GenericValidator.isBlankOrNull(resultValue) && resultLimit != null) {
            try {
                double value = Double.valueOf(resultValue);

                valid = value >= resultLimit.getLowValid() && value <= resultLimit.getHighValid();

            } catch (NumberFormatException e) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getIsValid", e.getMessage());
                // no-op
            }
        }
        return valid;
    }

    private boolean getIsNormal(String resultValue, ResultLimit resultLimit) {
        boolean normal = true;

        if (!GenericValidator.isBlankOrNull(resultValue) && resultLimit != null) {
            try {
                double value = Double.valueOf(resultValue);

                normal = value >= resultLimit.getLowNormal() && value <= resultLimit.getHighNormal();
            } catch (NumberFormatException e) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getIsNormal", e.getMessage());
                // no-op
            }
        }

        return normal;
    }

    private boolean kitNotInActiveKitList(String testKitId) {
        List<InventoryKitItem> activeKits = getActiveKits();

        for (InventoryKitItem kit : activeKits) {
            // The locationID is the reference held in the DB
            if (testKitId.equals(kit.getInventoryLocationId())) {
                return false;
            }
        }

        return true;
    }

    private String getCurrentDate() {
        if (GenericValidator.isBlankOrNull(currentDate)) {
            currentDate = DateUtil.getCurrentDateAsText();
        }

        return currentDate;
    }

    public boolean inventoryNeeded() {
        return inventoryNeeded;
    }

    public void addExcludedAnalysisStatus(AnalysisStatus status) {
        excludedAnalysisStatus.add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(status)));
    }

    public void addIncludedSampleStatus(OrderStatus status) {
        sampleStatusList.add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(status)));
    }

    public void addIncludedAnalysisStatus(AnalysisStatus status) {
        analysisStatusList.add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(status)));
    }

    private List<InventoryKitItem> getActiveKits() {
        if (activeKits == null) {
            InventoryUtility inventoryUtil = SpringContext.getBean(InventoryUtility.class);
            activeKits = inventoryUtil.getExistingActiveInventory();
        }

        return activeKits;
    }

    public void setLockCurrentResults(boolean lockCurrentResults) {
        this.lockCurrentResults = lockCurrentResults;
    }

    public boolean isLockCurrentResults() {
        return lockCurrentResults;
    }

    private boolean getQaEventByTestSection(Analysis analysis) {

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

    public List<SampleQaEvent> getSampleQaEvents(Sample sample) {
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    public List<TestResultItem> getUnfinishedTestResultItemsByAccession(String accessionNumber) {
        List<Analysis> analysisList = analysisService.getPageAnalysisByStatusFromAccession(analysisStatusList,
                sampleStatusList, accessionNumber);

        return getGroupedTestsForAnalysisList(analysisList, SORT_FORWARD);
    }

    public List<TestResultItem> getUnfinishedTestResultItemsByAccession(String accessionNumber,
            String upperRangeAccessionNumber, boolean doRange, boolean finished) {
        List<Analysis> analysisList = analysisService.getPageAnalysisByStatusFromAccession(analysisStatusList,
                sampleStatusList, accessionNumber, upperRangeAccessionNumber, doRange, finished);

        return getGroupedTestsForAnalysisList(analysisList, SORT_FORWARD);
    }

    public int getTotalCountAnalysisByAccessionAndStatus(String accessionNumber) {
        return analysisService.getCountAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                accessionNumber);
    }
}
