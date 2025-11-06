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
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.workplan.action.util.WorkplanPaging;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("WorkplanByPriorityRestController")
public class WorkplanByPriorityRestController extends WorkplanRestController {

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/rest/WorkPlanByPriority", method = RequestMethod.GET)
    public WorkplanForm showWorkPlanByPriority(HttpServletRequest request,
            @RequestParam(name = "priority", defaultValue = "") OrderPriority priority)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        WorkplanPaging paging = new WorkplanPaging();
        WorkplanForm form = new WorkplanForm();
        List<TestResultItem> workplanTests = new ArrayList<TestResultItem>();
        List<TestResultItem> filteredTests = new ArrayList<TestResultItem>();

        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            workplanTests = getWorkplanByPriority(priority);
            filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), workplanTests,
                    Constants.ROLE_RESULTS);

            ResultsLoadUtility resultsLoadUtility = new ResultsLoadUtility();
            resultsLoadUtility.sortByAccessionAndSequence(filteredTests);

            paging.setDatabaseResults(request, form, filteredTests);
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }

        return form;
    }

    private List<TestResultItem> getWorkplanByPriority(OrderPriority priority) {

        List<TestResultItem> workplanTestList = new ArrayList<>();
        String currentAccessionNumber = null;
        String subjectNumber = null;
        String patientName = null;
        String nextVisit = null;
        int sampleGroupingNumber = 0;

        if (priority != null) {
            List<Analysis> analysisList = analysisService.getAnalysesByPriorityAndStatusId(priority, statusList);
            for (Analysis analysis : analysisList) {
                TestResultItem testResultItem = new TestResultItem();
                testResultItem.setTestId(analysis.getTest().getId());
                Sample sample = analysis.getSampleItem().getSample();
                testResultItem.setAccessionNumber(sample.getAccessionNumber());
                testResultItem.setReceivedDate(getReceivedDateDisplay(sample));
                testResultItem.setTestName(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
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
