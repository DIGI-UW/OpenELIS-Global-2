package org.openelisglobal.workplan.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.action.util.WorkplanPaging;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("WorkplanByTestSectionRestController")
public class WorkplanByTestSectionRestController extends WorkplanRestController {

    @Autowired
    SampleService sampleService;

    @Autowired
    PatientService patientService;

    @Autowired
    PersonService personService;

    @Autowired
    ObservationHistoryService observationHistoryService;

    @Autowired
    SampleHumanService sampleHumanService;

    @Autowired
    SearchResultsService searchResultsService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleQaEventService sampleQaEventService;

    @Autowired
    private UserService userService;

    @GetMapping(value = "/rest/WorkPlanByTestSection", produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkplanForm showWorkPlanByTestSection(HttpServletRequest request,
            @RequestParam(name = "test_section_id", defaultValue = "0") String testSectionId)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        WorkplanForm form = new WorkplanForm();
        WorkplanPaging paging = new WorkplanPaging();
        List<TestResultItem> workplanTests = new ArrayList<TestResultItem>();
        List<TestResultItem> filteredTests = new ArrayList<TestResultItem>();
        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            if (!GenericValidator.isBlankOrNull(testSectionId)) {
                // get tests based on test section
                workplanTests = getWorkplanByTestSection(testSectionId);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                        Constants.ROLE_RESULTS);
            }
            ResultsLoadUtility resultsLoadUtility = new ResultsLoadUtility();
            resultsLoadUtility.sortByAccessionAndSequence(filteredTests);
            // add Patient Name to test table
            if (isPatientNameAdded()) {
                addPatientNamesToList(filteredTests);
            }
            paging.setDatabaseResults(request, form, filteredTests);
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }

        return form;
    }

    private List<TestResultItem> getWorkplanByTestSection(String testSectionId) {

        List<Analysis> testList = new ArrayList<>();
        List<TestResultItem> workplanTestList = new ArrayList<>();
        String currentAccessionNumber = new String();
        String subjectNumber = new String();
        String patientName = new String();
        String nextVisit = new String();
        int sampleGroupingNumber = 0;
        List<String> testIdList = new ArrayList<>();
        List<TestResultItem> nfsTestItemList = new ArrayList<>();
        boolean isNFSTest = false;
        TestResultItem testResultItem = new TestResultItem();

        if (!(GenericValidator.isBlankOrNull(testSectionId))) {

            String sectionId = testSectionId;
            testList = analysisService.getAllAnalysisByTestSectionAndStatus(sectionId, statusList, true);

            if (testList.isEmpty()) {
                return new ArrayList<>();
            }

            for (Analysis analysis : testList) {
                Sample sample = analysis.getSampleItem().getSample();
                String analysisAccessionNumber = sample.getAccessionNumber();

                if (!analysisAccessionNumber.equals(currentAccessionNumber)) {

                    if (isNFSTest) {
                        if (!allNFSTestsRequested(testIdList)) {
                            // add nfs subtests
                            for (TestResultItem nfsTestItem : nfsTestItemList) {
                                workplanTestList.add(nfsTestItem);
                            }
                        }
                    }

                    sampleGroupingNumber++;

                    currentAccessionNumber = analysisAccessionNumber;
                    testIdList = new ArrayList<>();
                    nfsTestItemList = new ArrayList<>();
                    isNFSTest = false;

                    subjectNumber = getSubjectNumber(analysis);
                    patientName = getPatientName(analysis);
                    nextVisit = SpringContext.getBean(ObservationHistoryService.class)
                            .getValueForSample(ObservationType.NEXT_VISIT_DATE, sample.getId());
                }

                testResultItem = new TestResultItem();
                testResultItem.setTestName(analysisService.getTestDisplayName(analysis));
                testResultItem.setAccessionNumber(currentAccessionNumber);
                testResultItem.setReceivedDate(getReceivedDateDisplay(sample));
                testResultItem.setSampleGroupingNumber(sampleGroupingNumber);
                testResultItem.setTestId(analysis.getTest().getId());
                boolean nonConforming = QAService.isAnalysisParentNonConforming(analysis);
                if (FormFields.getInstance().useField(Field.QaEventsBySection)) {
                    nonConforming = nonConforming || getQaEventByTestSection(analysis);
                }
                testResultItem.setNonconforming(nonConforming);

                testResultItem.setPatientInfo(subjectNumber);
                testResultItem.setNextVisitDate(nextVisit);
                if (isPatientNameAdded()) {
                    testResultItem.setPatientName(patientName);
                }
                testIdList.add(testResultItem.getTestId());

                if (nfsTestIdList.contains(testResultItem.getTestId())) {
                    isNFSTest = true;
                    nfsTestItemList.add(testResultItem);
                }
                if (isNFSTest) {

                    if (allNFSTestsRequested(testIdList)) {
                        testResultItem.setTestName("NFS");
                        workplanTestList.add(testResultItem);
                    } else if (!nfsTestItemList.isEmpty() && (testList.size() - 1) == testList.indexOf(analysis)) {
                        // add nfs subtests
                        for (TestResultItem nfsTestItem : nfsTestItemList) {
                            workplanTestList.add(nfsTestItem);
                        }
                    }
                } else {
                    workplanTestList.add(testResultItem);
                }
            }
        }

        return workplanTestList;
    }

    private void addPatientNamesToList(List<TestResultItem> workplanTestList) {
        String currentAccessionNumber = new String();
        int sampleGroupingNumber = 0;

        int newIndex = 0;
        int newElementsAdded = 0;
        int workplanTestListOrigSize = workplanTestList.size();

        for (int i = 0; newIndex < (workplanTestListOrigSize + newElementsAdded); i++) {

            TestResultItem testResultItem = workplanTestList.get(newIndex);

            if (!testResultItem.getAccessionNumber().equals(currentAccessionNumber)) {
                sampleGroupingNumber++;
                if (isPatientNameAdded()) {
                    addPatientNameToList(testResultItem, workplanTestList, newIndex, sampleGroupingNumber);
                    newIndex++;
                    newElementsAdded++;
                }

                currentAccessionNumber = testResultItem.getAccessionNumber();
            }
            testResultItem.setSampleGroupingNumber(sampleGroupingNumber);
            newIndex++;
        }
    }

    private void addPatientNameToList(TestResultItem firstTestResultItem, List<TestResultItem> workplanTestList,
            int insertPosition, int sampleGroupingNumber) {
        TestResultItem testResultItem = new TestResultItem();
        testResultItem.setAccessionNumber(firstTestResultItem.getAccessionNumber());
        testResultItem.setPatientInfo(firstTestResultItem.getPatientInfo());
        testResultItem.setReceivedDate(firstTestResultItem.getReceivedDate());
        // Add Patient Name to top of test list
        testResultItem.setTestName(firstTestResultItem.getPatientName());
        testResultItem.setSampleGroupingNumber(sampleGroupingNumber);
        testResultItem.setServingAsTestGroupIdentifier(true);
        workplanTestList.add(insertPosition, testResultItem);
    }

    private boolean isPatientNameAdded() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP");
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
}
