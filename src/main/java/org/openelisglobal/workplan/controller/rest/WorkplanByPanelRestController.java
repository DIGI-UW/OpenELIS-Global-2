package org.openelisglobal.workplan.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.workplan.action.util.WorkplanPaging;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("WorkplanByPanelRestController")
public class WorkplanByPanelRestController extends WorkplanRestController {

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
    private PanelItemService panelItemService;

    @Autowired
    private SampleQaEventService sampleQaEventService;

    @Autowired
    private UserService userService;

    @GetMapping(value = "/rest/WorkPlanByPanel", produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkplanForm showWorkPlanByPanel(HttpServletRequest request,
            @RequestParam(name = "panel_id", defaultValue = "0") String panelID)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        WorkplanForm form = new WorkplanForm();
        WorkplanPaging paging = new WorkplanPaging();
        List<TestResultItem> workplanTests = new ArrayList<TestResultItem>();
        List<TestResultItem> filteredTests = new ArrayList<TestResultItem>();

        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            workplanTests = getWorkplanByPanel(panelID);
            filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                    Constants.ROLE_RESULTS);
            paging.setDatabaseResults(request, form, filteredTests);
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }

        return form;
    }

    private List<TestResultItem> getWorkplanByPanel(String panelId) {

        List<TestResultItem> workplanTestList = new ArrayList<>();
        // check for patient name addition
        boolean addPatientName = isPatientNameAdded();

        if (!(GenericValidator.isBlankOrNull(panelId) || panelId.equals("0"))) {

            List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);

            for (PanelItem panelItem : panelItems) {
                List<Analysis> analysisList = analysisService.getAllAnalysisByTestAndStatus(panelItem.getTest().getId(),
                        statusList);

                for (Analysis analysis : analysisList) {
                    TestResultItem testResultItem = new TestResultItem();
                    testResultItem.setTestId(analysis.getTest().getId());
                    Sample sample = analysis.getSampleItem().getSample();
                    testResultItem.setAccessionNumber(sample.getAccessionNumber());
                    testResultItem.setPatientInfo(getSubjectNumber(analysis));
                    testResultItem.setNextVisitDate(SpringContext.getBean(ObservationHistoryService.class)
                            .getValueForSample(ObservationType.NEXT_VISIT_DATE, sample.getId()));
                    testResultItem.setReceivedDate(getReceivedDateDisplay(sample));
                    testResultItem.setTestName(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
                    boolean nonConforming = QAService.isAnalysisParentNonConforming(analysis);
                    if (FormFields.getInstance().useField(Field.QaEventsBySection)) {
                        nonConforming = nonConforming || getQaEventByTestSection(analysis);
                    }
                    testResultItem.setNonconforming(nonConforming);
                    if (addPatientName) {
                        testResultItem.setPatientName(getPatientName(analysis));
                    }

                    workplanTestList.add(testResultItem);
                }
            }

            Collections.sort(workplanTestList, new Comparator<TestResultItem>() {

                @Override
                public int compare(TestResultItem o1, TestResultItem o2) {
                    return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
                }
            });

            String currentAccessionNumber = null;
            int sampleGroupingNumber = 0;

            int newIndex = 0;
            int newElementsAdded = 0;
            int workplanTestListOrigSize = workplanTestList.size();

            for (int i = 0; newIndex < (workplanTestListOrigSize + newElementsAdded); i++) {

                TestResultItem testResultItem = workplanTestList.get(newIndex);

                if (!testResultItem.getAccessionNumber().equals(currentAccessionNumber)) {
                    sampleGroupingNumber++;
                    if (addPatientName) {
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

        return workplanTestList;
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

    @GetMapping(value = "panels", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createPanelList() {
        return DisplayListService.getInstance().getList(ListType.PANELS);
    }
}
