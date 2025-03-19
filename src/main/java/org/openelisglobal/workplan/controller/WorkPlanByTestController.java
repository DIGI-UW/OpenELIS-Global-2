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
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class WorkPlanByTestController extends BaseWorkplanController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private UserService userService;
    private static boolean HAS_NFS_PANEL = false;

    static {
        HAS_NFS_PANEL = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.CONDENSE_NFS_PANEL, "true");
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/WorkPlanByTest", method = RequestMethod.GET)
    public ModelAndView showWorkPlanByPanel(HttpServletRequest request,
            @ModelAttribute("form") @Validated(PrintWorkplan.class) WorkplanForm oldForm, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        WorkplanForm form = new WorkplanForm();

        request.getSession().setAttribute(SAVE_DISABLED, "true");

        List<TestResultItem> workplanTests;
        List<TestResultItem> filteredTests;

        String testType = "";
        if (!result.hasFieldErrors("selectedSearchID")) {
            testType = oldForm.getSelectedSearchID();
        }
        String testName;

        if (!GenericValidator.isBlankOrNull(testType)) {

            if (testType.equals("NFS")) {
                testName = "NFS";
                workplanTests = getWorkplanForNFSTest(testType);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                        Constants.ROLE_RESULTS);
            } else {
                testName = getTestName(testType);
                workplanTests = getWorkplanByTest(testType);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                        Constants.ROLE_RESULTS);
            }
            ResultsLoadUtility resultsLoadUtility = new ResultsLoadUtility();
            resultsLoadUtility.sortByAccessionAndSequence(filteredTests);
            form.setTestTypeID(testType);
            form.setTestName(testName);
            form.setWorkplanTests(filteredTests);
            form.setSearchFinished(Boolean.TRUE);

        } else {
            // no search done, set workplanTests as empty
            form.setSearchFinished(Boolean.FALSE);
            form.setTestName(null);
            form.setWorkplanTests(new ArrayList<TestResultItem>());
        }

        form.setSearchTypes(getTestDropdownList(request));
        if (!result.hasFieldErrors("type")) {
            form.setType(oldForm.getType());
        }
        form.setType("test");
        form.setSearchLabel(MessageUtil.getMessage("workplan.test.types"));
        form.setSearchAction("WorkPlanByTest");

        return findForward(FWD_SUCCESS, form);
    }

    private List<IdValuePair> getTestDropdownList(HttpServletRequest request) {
        List<IdValuePair> testList = userService.getAllDisplayUserTestsByLabUnit(getSysUserId(request),
                Constants.ROLE_RESULTS);

        if (HAS_NFS_PANEL) {
            testList = adjustNFSTests(testList);
        }
        Collections.sort(testList, new ValueComparator());
        return testList;
    }

    private List<IdValuePair> adjustNFSTests(List<IdValuePair> allTestsList) {
        List<IdValuePair> adjustedList = new ArrayList<>(allTestsList.size());
        for (IdValuePair idValuePair : allTestsList) {
            if (!nfsTestIdList.contains(idValuePair.getId())) {
                adjustedList.add(idValuePair);
            }
        }
        // add NFS to the list
        adjustedList.add(new IdValuePair("NFS", "NFS"));
        return adjustedList;
    }

    @SuppressWarnings("unchecked")
    private List<TestResultItem> getWorkplanByTest(String testType) {

        List<Analysis> testList;
        List<TestResultItem> workplanTestList = new ArrayList<>();
        String currentAccessionNumber = null;
        String subjectNumber = null;
        String patientName = null;
        String nextVisit = null;
        int sampleGroupingNumber = 0;

        if (!(GenericValidator.isBlankOrNull(testType) || testType.equals("0"))) {

            testList = analysisService.getAllAnalysisByTestAndStatus(testType, statusList);

            if (testList.isEmpty()) {
                return new ArrayList<>();
            }

            for (Analysis analysis : testList) {
                TestResultItem testResultItem = new TestResultItem();
                testResultItem.setTestId(testType);
                Sample sample = analysis.getSampleItem().getSample();
                testResultItem.setAccessionNumber(sample.getAccessionNumber());
                testResultItem.setReceivedDate(getReceivedDateDisplay(sample));
                boolean nonConforming = QAService.isAnalysisParentNonConforming(analysis);
                if (FormFields.getInstance().useField(Field.QaEventsBySection)) {
                    nonConforming = nonConforming || getQaEventByTestSection(analysis);
                }
                testResultItem.setNonconforming(nonConforming);

                if (!testResultItem.getAccessionNumber().equals(currentAccessionNumber)) {
                    sampleGroupingNumber++;
                    currentAccessionNumber = testResultItem.getAccessionNumber();
                    subjectNumber = getSubjectNumber(analysis);
                    patientName = getPatientName(analysis);
                    nextVisit = SpringContext.getBean(ObservationHistoryService.class)
                            .getValueForSample(ObservationType.NEXT_VISIT_DATE, sample.getId());
                }
                testResultItem.setSampleGroupingNumber(sampleGroupingNumber);
                testResultItem.setPatientInfo(subjectNumber);
                testResultItem.setPatientName(patientName);
                testResultItem.setNextVisitDate(nextVisit);

                workplanTestList.add(testResultItem);
            }
        }

        return workplanTestList;
    }

    @SuppressWarnings("unchecked")
    private List<TestResultItem> getWorkplanForNFSTest(String testType) {

        List<Analysis> testList;
        List<TestResultItem> workplanTestList = new ArrayList<>();
        String currentAccessionNumber = null;
        int sampleGroupingNumber = 0;

        TestResultItem testResultItem;

        List<String> testIdList = new ArrayList<>();

        if (!(GenericValidator.isBlankOrNull(testType) || testType.equals("0"))) {

            testList = analysisService.getAllAnalysisByTestsAndStatus(nfsTestIdList, statusList);

            if (testList.isEmpty()) {
                return new ArrayList<>();
            }

            for (Analysis analysis : testList) {

                Sample sample = analysis.getSampleItem().getSample();
                String analysisAccessionNumber = sample.getAccessionNumber();

                if (!analysisAccessionNumber.equals(currentAccessionNumber)) {
                    sampleGroupingNumber++;
                    currentAccessionNumber = analysisAccessionNumber;
                    testIdList = new ArrayList<>();
                }
                testResultItem = new TestResultItem();
                testResultItem.setTestId(testType);
                testResultItem.setAccessionNumber(currentAccessionNumber);
                testResultItem.setReceivedDate(sample.getReceivedDateForDisplay());
                testResultItem.setSampleGroupingNumber(sampleGroupingNumber);
                testResultItem.setNonconforming(QAService.isAnalysisParentNonConforming(analysis));
                testIdList.add(analysis.getTest().getId());

                if (allNFSTestsRequested(testIdList)) {
                    workplanTestList.add(testResultItem);
                }
            }
        }

        return workplanTestList;
    }

    private String getTestName(String testId) {
        return TestServiceImpl.getUserLocalizedTestName(testId);
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

    class ValueComparator implements Comparator<IdValuePair> {

        @Override
        public int compare(IdValuePair p1, IdValuePair p2) {
            return p1.getValue().toUpperCase().compareTo(p2.getValue().toUpperCase());
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "workplan.test.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "workplan.test.title";
    }
}
