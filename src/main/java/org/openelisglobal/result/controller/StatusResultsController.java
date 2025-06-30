package org.openelisglobal.result.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsPaging;
import org.openelisglobal.result.form.StatusResultsForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class StatusResultsController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "collectionDate", "recievedDate", "selectedTest",
            "selectedAnalysisStatus", "selectedSampleStatus" };

    private static final boolean REVERSE_SORT_ORDER = false;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private UserService userService;

    private final InventoryUtility inventoryUtility = SpringContext.getBean(InventoryUtility.class);
    private static final ConfigurationProperties configProperties = ConfigurationProperties.getInstance();

    private static Set<Integer> excludedStatusIds;

    static {
        // currently this is the only one being excluded for Haiti_LNSP. If it
        // gets more complicate use the status sets
        excludedStatusIds = new HashSet<>();
        excludedStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        excludedStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.SampleRejected)));
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/StatusResults", method = RequestMethod.GET)
    public ModelAndView showStatusResults(HttpServletRequest request, @Valid StatusResultsForm form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            setSelectionLists(form, request);
            return findForward(FWD_FAIL, form);
        }

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsUtility.setSysUser(getSysUserId(request));

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        String newRequest = request.getParameter("blank");

        form.setReferralReasons(DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
        form.setRejectReasons(DisplayListService.getInstance()
                .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));
        form.setReferralOrganizations(DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setMethods(DisplayListService.getInstance().getList(ListType.METHODS));

        ResultsPaging paging = new ResultsPaging();

        String newPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(newPage)) {
            List<TestResultItem> tests;
            List<TestResultItem> filteredTests = new ArrayList();
            if (GenericValidator.isBlankOrNull(newRequest) || newRequest.equals("false")) {
                tests = setSearchResults(form, resultsUtility);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);

                if (configProperties.isPropertyValueEqual(Property.PATIENT_DATA_ON_RESULTS_BY_ROLE, "true")
                        && !userHasPermissionForModule(request, "PatientResults")) {
                    for (TestResultItem resultItem : filteredTests) {
                        resultItem.setPatientInfo("---");
                    }
                }

                paging.setDatabaseResults(request, form, filteredTests);
            } else {
                setEmptyResults(form);
            }

            setSelectionLists(form, request);
        } else {
            paging.page(request, form, Integer.parseInt(newPage));
        }
        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    private List<TestResultItem> setSearchResults(StatusResultsForm form, ResultsLoadUtility resultsUtility)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<TestResultItem> tests = getSelectedTests(form, resultsUtility);
        form.setSearchFinished(Boolean.TRUE);

        if (resultsUtility.inventoryNeeded()) {
            addInventory(form);
            form.setDisplayTestKit(true);
        } else {
            addEmptyInventoryList(form);
            form.setDisplayTestKit(false);
        }

        return tests;
    }

    private void setEmptyResults(StatusResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setTestResult(new ArrayList<TestResultItem>());
        form.setDisplayTestKit(false);
        form.setCollectionDate("");
        form.setRecievedDate("");
        form.setSelectedAnalysisStatus("");
        form.setSelectedTest("");
        form.setSearchFinished(Boolean.FALSE);
    }

    private void addInventory(StatusResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        List<InventoryKitItem> list = inventoryUtility.getExistingActiveInventory();
        form.setInventoryItems(list);
    }

    private void addEmptyInventoryList(StatusResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setInventoryItems(new ArrayList<InventoryKitItem>());
    }

    private void setSelectionLists(StatusResultsForm form, HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        List<DropPair> analysisStatusList = getAnalysisStatusTypes();

        form.setAnalysisStatusSelections(analysisStatusList);
        form.setTestSelections(
                userService.getAllDisplayUserTestsByLabUnit(getSysUserId(request), Constants.ROLE_RESULTS));

        List<DropPair> sampleStatusList = getSampleStatusTypes();
        form.setSampleStatusSelections(sampleStatusList);
    }

    private List<TestResultItem> getSelectedTests(StatusResultsForm form, ResultsLoadUtility resultsUtility) {
        String collectionDate = form.getCollectionDate();
        String receivedDate = form.getRecievedDate();
        String analysisStatus = form.getSelectedAnalysisStatus();
        String sampleStatus = form.getSelectedSampleStatus();
        String test = form.getSelectedTest();

        List<Analysis> analysisList = new ArrayList<>();

        if (!GenericValidator.isBlankOrNull(collectionDate)) {
            analysisList = getAnalysisForCollectionDate(collectionDate);
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!GenericValidator.isBlankOrNull(receivedDate)) {
            analysisList = blendLists(analysisList, getAnalysisForRecievedDate(receivedDate));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(analysisStatus) || analysisStatus.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForAnalysisStatus(analysisStatus));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(sampleStatus) || sampleStatus.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForSampleStatus(sampleStatus));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(test) || test.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForTest(test));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        return buildTestItems(analysisList, resultsUtility);
    }

    private List<Analysis> blendLists(List<Analysis> masterList, List<Analysis> newList) {
        if (masterList.isEmpty()) {
            return newList;
        } else {
            List<Analysis> blendedList = new ArrayList<>();
            for (Analysis master : masterList) {
                for (Analysis newAnalysis : newList) {
                    if (master.getId().equals(newAnalysis.getId())) {
                        blendedList.add(master);
                    }
                }
            }
            return blendedList;
        }
    }

    private List<Analysis> getAnalysisForCollectionDate(String collectionDate) {
        Date date = DateUtil.convertStringDateToSqlDate(collectionDate);
        return analysisService.getAnalysisCollectedOnExcludedByStatusId(date, excludedStatusIds);
    }

    private List<Analysis> getAnalysisForRecievedDate(String recievedDate) {
        List<Sample> sampleList = sampleService.getSamplesReceivedOn(recievedDate);

        return getAnalysisListForSampleItems(sampleList);
    }

    private List<Analysis> getAnalysisListForSampleItems(List<Sample> sampleList) {
        List<Analysis> analysisList = new ArrayList<>();

        for (Sample sample : sampleList) {
            List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleId(sample.getId());

            for (SampleItem sampleItem : sampleItemList) {
                List<Analysis> analysisListForItem = analysisService
                        .getAnalysesBySampleItemsExcludingByStatusIds(sampleItem, excludedStatusIds);

                analysisList.addAll(analysisListForItem);
            }
        }

        return analysisList;
    }

    private List<Analysis> getAnalysisForAnalysisStatus(String status) {
        return analysisService.getAnalysesForStatusId(status);
    }

    private List<Analysis> getAnalysisForSampleStatus(String sampleStatus) {
        return analysisService.getAnalysesBySampleStatusIdExcludingByStatusId(sampleStatus, excludedStatusIds);
    }

    @SuppressWarnings("unchecked")
    private List<Analysis> getAnalysisForTest(String testId) {
        List<Integer> excludedStatusIntList = new ArrayList<>();
        excludedStatusIntList.addAll(excludedStatusIds);
        return analysisService.getAllAnalysisByTestAndExcludedStatus(testId, excludedStatusIntList);
    }

    private List<TestResultItem> buildTestItems(List<Analysis> analysisList, ResultsLoadUtility resultsUtility) {
        if (analysisList.isEmpty()) {
            return new ArrayList<>();
        }

        return resultsUtility.getGroupedTestsForAnalysisList(analysisList, REVERSE_SORT_ORDER);
    }

    private List<DropPair> getAnalysisStatusTypes() {

        List<DropPair> list = new ArrayList<>();
        list.add(new DropPair("0", ""));

        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.NotStarted)));
        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.Canceled)));
        list.add(new DropPair(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.TechnicalAcceptance)));
        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.TechnicalRejected)));
        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.BiologistRejected)));

        return list;
    }

    private List<DropPair> getSampleStatusTypes() {

        List<DropPair> list = new ArrayList<>();
        list.add(new DropPair("0", ""));

        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered),
                SpringContext.getBean(IStatusService.class).getStatusName(OrderStatus.Entered)));
        list.add(new DropPair(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Started),
                SpringContext.getBean(IStatusService.class).getStatusName(OrderStatus.Started)));

        return list;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "statusResultDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "statusResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "banner.menu.results";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "banner.menu.results";
    }

    public class DropPair {

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        private final String id;
        private final String description;

        public DropPair(String id, String description) {
            this.id = id;
            this.description = description;
        }
    }
}
