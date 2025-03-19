package org.openelisglobal.workplan.controller;

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
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.openelisglobal.workplan.form.WorkplanForm.PrintWorkplan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class WorkplanByPanelController extends BaseWorkplanController {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedSearchID" };

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/WorkPlanByPanel", method = RequestMethod.GET)
    public ModelAndView showWorkPlanByTest(@Validated(PrintWorkplan.class) @ModelAttribute("form") WorkplanForm oldForm,
            HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        WorkplanForm form = new WorkplanForm();

        request.getSession().setAttribute(SAVE_DISABLED, "true");

        List<TestResultItem> workplanTests;
        List<TestResultItem> filteredTests;

        String panelID = oldForm.getSelectedSearchID();

        if (!GenericValidator.isBlankOrNull(panelID)) {
            String panelName = getPanelName(panelID);
            workplanTests = getWorkplanByPanel(panelID);
            filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                    Constants.ROLE_RESULTS);

            // resultsLoadUtility.sortByAccessionAndSequence(workplanTests);
            form.setTestTypeID(panelID);
            form.setTestName(panelName);
            form.setWorkplanTests(filteredTests);
            form.setSearchFinished(Boolean.TRUE);
        } else {
            // no search done, set workplanTests as empty
            form.setSearchFinished(Boolean.FALSE);
            form.setTestName(null);
            form.setWorkplanTests(new ArrayList<TestResultItem>());
        }

        form.setType("panel");
        form.setSearchTypes(DisplayListService.getInstance().getList(DisplayListService.ListType.PANELS));
        form.setSearchLabel(MessageUtil.getMessage("workplan.panel.types"));
        form.setSearchAction("WorkPlanByPanel");

        return findForward(FWD_SUCCESS, form);
    }

    @SuppressWarnings("unchecked")
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

    private String getPanelName(String panelId) {
        return panelService.get(panelId).getLocalizedName(); // getName();
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

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "workplanResultDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "workplan.panel.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "workplan.panel.title";
    }
}
