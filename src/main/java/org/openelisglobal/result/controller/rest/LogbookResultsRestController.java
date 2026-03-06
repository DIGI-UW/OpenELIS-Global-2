
package org.openelisglobal.result.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.service.ReferralTypeService;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsPaging;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.controller.LogbookResultsBaseController;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.result.form.LogbookResultsForm.LogbookResults;
import org.openelisglobal.result.form.StatusResultsForm;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class LogbookResultsRestController extends LogbookResultsBaseController {

    private String RESULT_EDIT_ROLE_ID;

    private final String[] ALLOWED_FIELDS = new String[] { "accessionNumber", "collectionDate", "recievedDate",
            "selectedTest", "selectedAnalysisStatus", "selectedSampleStatus", "testSectionId", "methodId", "type",
            "currentPageID", "testResult*.accessionNumber", "testResult*.isModified", "testResult*.analysisId",
            "testResult*.resultId", "testResult*.testId", "testResult*.technicianSignatureId", "testResult*.testKitId",
            "testResult*.resultLimitId", "testResult*.resultType", "testResult*.valid", "testResult*.referralId",
            "testResult*.referralCanceled", "testResult*.considerRejectReason", "testResult*.hasQualifiedResult",
            "testResult*.shadowResultValue", "testResult*.reflexJSONResult", "testResult*.testDate",
            "testResult*.analysisMethod", "testResult*.testMethod", "testResult*.testKitInventoryId",
            "testResult*.forceTechApproval", "testResult*.lowerNormalRange", "testResult*.upperNormalRange",
            "testResult*.significantDigits", "testResult*.resultValue", "testResult*.qualifiedResultValue",
            "testResult*.multiSelectResultValues", "testResult*.testMethod", "testResult*.multiSelectResultValues",
            "testResult*.qualifiedResultValue", "testResult*.qualifiedResultValue", "testResult*.shadowReferredOut",
            "testResult*.referredOut", "testResult*.referralReasonId", "testResult*.technician",
            "testResult*.shadowRejected", "testResult*.rejected", "testResult*.rejectReasonId", "testResult*.note",
            "paging.currentPage", "testResult*.resultFile", "testResult*.resultFile.fileName",
            "testResult*.resultFile.fileType", "testResult*.resultFile.base64Content", "testResult*.refer",
            "testResult*.referralItem.referralReasonId", "testResult*.referralItem.referredInstituteId",
            "testResult*.referralItem.referredTestId", "testResult*.referralItem.referredSendDate" };

    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private LogbookResultsPersistService logbookPersistService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    PatientService patientService;
    @Autowired
    SearchResultsService searchService;
    @Autowired
    SampleItemService sampleItemService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private NotificationDAO notificationDAO;
    @Autowired
    private SystemUserService systemUserService;

    private final String RESULT_SUBJECT = "Result Note";
    private final String REFERRAL_CONFORMATION_ID;
    private static final String REFLEX_ACCESSIONS = "reflex_accessions";

    private LogbookResultsRestController(ReferralTypeService referralTypeService) {
        ReferralType referralType = referralTypeService.getReferralTypeByName("Confirmation");
        if (referralType != null) {
            REFERRAL_CONFORMATION_ID = referralType.getId();
        } else {
            REFERRAL_CONFORMATION_ID = null;
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "LogbookResults", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LogbookResultsForm showRestLogbookResults(@RequestParam(required = false) String labNumber,
            @RequestParam(required = false) String patientPK, @RequestParam(required = false) String collectionDate,
            @RequestParam(required = false) String recievedDate, @RequestParam(required = false) String selectedTest,
            @RequestParam(required = false) String selectedSampleStatus,
            @RequestParam(required = false) String selectedAnalysisStatus,
            @RequestParam(required = false) String upperRangeAccessionNumber,
            @RequestParam(required = false) boolean doRange,
            @RequestParam(required = false, defaultValue = "false") boolean finished,
            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        StatusResultsForm statusResultsForm = new StatusResultsForm();
        statusResultsForm.setCollectionDate(collectionDate);
        statusResultsForm.setRecievedDate(recievedDate);
        statusResultsForm.setSelectedTest(selectedTest);
        statusResultsForm.setSelectedSampleStatus(selectedSampleStatus);
        statusResultsForm.setSelectedAnalysisStatus(selectedAnalysisStatus);

        LogbookResultsForm newForm = new LogbookResultsForm();
        if (!(result.hasFieldErrors("type") || result.hasFieldErrors("testSectionId")
                || result.hasFieldErrors("methodId") || result.hasFieldErrors("accessionNumber"))) {
            newForm.setType(form.getType());
            newForm.setTestSectionId(form.getTestSectionId());

            String currentDate = getCurrentDate();
            newForm.setCurrentDate(currentDate);
            newForm.setAccessionNumber(labNumber);
        }
        newForm.setDisplayTestSections(true);
        newForm.setSearchByRange(false);

        return getLogbookResults(request, newForm, statusResultsForm, labNumber, patientPK, upperRangeAccessionNumber,
                doRange, finished);
    }

    private LogbookResultsForm getLogbookResults(HttpServletRequest request, LogbookResultsForm form,
            StatusResultsForm statusResultsForm, String labNumber, String patientPK, String upperRangeAccessionNumber,
            boolean doRange, boolean finished)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String patientName = "";
        String patientInfo = "";
        Patient patient = null;

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        List<TestResultItem> tests = new ArrayList<>();
        List<TestResultItem> filteredTests = new ArrayList<>();

        ResultsPaging paging = new ResultsPaging();
        // TODO: Re-enable after new inventory frontend integration
        // List<InventoryKitItem> inventoryList = new ArrayList<>();
        ResultsLoadUtility resultsLoadUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsLoadUtility.setSysUser(getSysUserId(request));

        String requestedPage = request.getParameter("page");

        if (GenericValidator.isBlankOrNull(requestedPage)) {
            requestedPage = "1";
            resultsLoadUtility.addExcludedAnalysisStatus(AnalysisStatus.Canceled);
            resultsLoadUtility.addExcludedAnalysisStatus(AnalysisStatus.SampleRejected);
            new StatusRules().setAllowableStatusForLoadingResults(resultsLoadUtility);

            if (!GenericValidator.isBlankOrNull(form.getTestSectionId())) {
                tests = resultsLoadUtility.getUnfinishedTestResultItemsInTestSection(form.getTestSectionId());
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);
                int count = resultsLoadUtility.getTotalCountAnalysisByTestSectionAndStatus(form.getTestSectionId());
                request.setAttribute("analysisCount", count);
                request.setAttribute("pageSize", filteredTests.size());

                TestSection ts = null;
                if (!GenericValidator.isBlankOrNull(form.getTestSectionId())) {
                    ts = testSectionService.get(form.getTestSectionId());
                }
                setRequestType(ts == null ? MessageUtil.getMessage("workplan.unit.types") : ts.getLocalizedName());

                if (ts != null) {
                    // this does not look right what happens after a new page!!!
                    boolean isHaitiClinical = ConfigurationProperties.getInstance()
                            .isPropertyValueEqual(Property.configurationName, "Haiti Clinical");
                    if (resultsLoadUtility.inventoryNeeded()
                            || (isHaitiClinical && ("VCT").equals(ts.getTestSectionName()))) {
                        // TODO: Re-enable after new inventory frontend integration
                        // InventoryUtility inventoryUtility =
                        // SpringContext.getBean(InventoryUtility.class);
                        // inventoryList = inventoryUtility.getExistingActiveInventory();

                        form.setDisplayTestKit(true);
                    }
                }
                form.setSearchFinished(true);
            } else if (!GenericValidator.isBlankOrNull(statusResultsForm.getCollectionDate())
                    || !GenericValidator.isBlankOrNull(statusResultsForm.getRecievedDate())
                    || !GenericValidator.isBlankOrNull(statusResultsForm.getSelectedTest())
                    || !GenericValidator.isBlankOrNull(statusResultsForm.getSelectedAnalysisStatus())
                    || !GenericValidator.isBlankOrNull(statusResultsForm.getSelectedSampleStatus())) {
                tests.clear();
                LogbookStatusResults reactLogbookStatusResults = new LogbookStatusResults(analysisService,
                        sampleService, sampleItemService);

                tests = reactLogbookStatusResults.setSearchResults(statusResultsForm, resultsLoadUtility);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);

                request.setAttribute("pageSize", filteredTests.size());

            } else if (!GenericValidator.isBlankOrNull(form.getAccessionNumber())
                    || !GenericValidator.isBlankOrNull(patientPK)) {
                tests.clear();
                if (doRange) {
                    tests = resultsLoadUtility.getUnfinishedTestResultItemsByAccession(labNumber,
                            upperRangeAccessionNumber, doRange, finished);
                } else {
                    resultsLoadUtility.setLockCurrentResults(
                            ResultUtil.modifyResultsRoleBased() && ResultUtil.userNotInRole(request));
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getLogbookResults",
                            "Searching for sample with labNumber: " + labNumber);
                    Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
                    if (sample != null) {
                        LogEvent.logInfo(this.getClass().getSimpleName(), "getLogbookResults", "Found sample: id="
                                + sample.getId() + ", accessionNumber=" + sample.getAccessionNumber());
                        if (!GenericValidator.isBlankOrNull(sample.getId())) {
                            patient = ResultUtil.getPatient(sample);

                            tests = resultsLoadUtility.getGroupedTestsForSample(sample, patient);
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getLogbookResults",
                                    "getGroupedTestsForSample returned " + tests.size() + " tests for sample "
                                            + sample.getId());
                            if (patient != null) {
                                patientName = patientService.getLastFirstName(patient);
                                patientInfo = patient.getNationalId() + ", " + patient.getGender() + ", "
                                        + patient.getBirthDateForDisplay();
                            }
                        }
                    } else {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "getLogbookResults",
                                "No sample found for labNumber: " + labNumber);
                    }
                }

                // if no test try patientID
                if (tests.isEmpty()) {
                    String statusRules = ConfigurationProperties.getInstance()
                            .getPropertyValueUpperCase(Property.StatusRules);
                    if (statusRules.equals(STATUS_RULES_RETROCI)) {
                        resultsLoadUtility.addExcludedAnalysisStatus(AnalysisStatus.TechnicalRejected);
                    }

                    if (StringUtils.isBlank(patientPK)) {
                        return (form);
                    }
                    patient = patientService.get(patientPK);

                    tests = resultsLoadUtility.getGroupedTestsForPatient(patient);
                    patientName = patientService.getLastFirstName(patient);
                    patientInfo = patient.getNationalId() + ", " + patient.getGender() + ", "
                            + patient.getBirthDateForDisplay();
                }

                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);
                LogEvent.logInfo(this.getClass().getSimpleName(), "getLogbookResults",
                        "After filterResultsByLabUnitRoles: tests.size()=" + tests.size() + ", filteredTests.size()="
                                + filteredTests.size());

                int count = resultsLoadUtility.getTotalCountAnalysisByAccessionAndStatus(form.getAccessionNumber());

                request.setAttribute("analysisCount", count);
                request.setAttribute("pageSize", filteredTests.size());
                form.setSearchFinished(true);
            } else {
                tests = new ArrayList<>();
            }

            if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.PATIENT_DATA_ON_RESULTS_BY_ROLE,
                    "true") && !userHasPermissionForModule(request, "PatientResults")) {
                for (TestResultItem resultItem : filteredTests) {
                    resultItem.setPatientInfo("---");
                }
            }

            for (TestResultItem resultItem : filteredTests) {
                Result newResult = new Result();
                if (resultItem.getResult() != null) {
                    newResult.setId(resultItem.getResult().getId());
                    resultItem.setResult(newResult);
                }
            }

            paging.setDatabaseResults(request, form, filteredTests);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getLogbookResults",
                    "After setDatabaseResults: form.getTestResult() size="
                            + (form.getTestResult() != null ? form.getTestResult().size() : 0));

        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }
        form.setDisplayTestKit(false);
        // TODO: Re-enable after new inventory frontend integration
        // List<String> hivKits = new ArrayList<>();
        // List<String> syphilisKits = new ArrayList<>();
        // for (InventoryKitItem item : inventoryList) {
        // if (item.getType().equals("HIV")) {
        // hivKits.add(item.getInventoryLocationId());
        // } else {
        // syphilisKits.add(item.getInventoryLocationId());
        // }
        // }
        // form.setHivKits(hivKits);
        // form.setSyphilisKits(syphilisKits);

        // Temporary fix: Set empty lists
        form.setHivKits(new ArrayList<String>());
        form.setSyphilisKits(new ArrayList<String>());
        // TODO: Re-enable after new inventory frontend integration
        // form.setInventoryItems(inventoryList);

        addFlashMsgsToRequest(request);

        for (TestResultItem resultItem : filteredTests) {
            AddPatientIdToResult(patient, resultItem);
            if (patientName != "")
                resultItem.setPatientName(patientName);
            if (patientInfo != "")
                resultItem.setPatientInfo(patientInfo);
        }

        return (form);
    }

    private void AddPatientIdToResult(Patient patient, TestResultItem resultItem) {
        if (patient != null) {
            resultItem.setPatientId(patient.getId());
        } else if (resultItem.getAccessionNumber() != null) {
            // Si le patient n'est pas défini globalement, le récupérer via l'échantillon
            Sample sample = sampleService.getSampleByAccessionNumber(resultItem.getAccessionNumber());
            if (sample != null) {
                Patient itemPatient = sampleHumanService.getPatientForSample(sample);
                if (itemPatient != null) {
                    resultItem.setPatientId(itemPatient.getId());
                }
            }
        }
    }

    private String getCurrentDate() {
        Date today = Calendar.getInstance().getTime();
        return DateUtil.formatDateAsText(today);
    }

    @PostMapping(value = "LogbookResults", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, List<String>> showReactLogbookResultsUpdate(HttpServletRequest request,
            @Validated(LogbookResultsForm.LogbookResults.class) @RequestBody LogbookResultsForm form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        boolean useTechnicianName = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.resultTechnicianName, "true");
        boolean alwaysValidate = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        boolean supportReferrals = FormFields.getInstance().useField(Field.ResultsReferral);
        Map<String, List<String>> reflexMap = new HashMap<>();
        String statusRuleSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        if ("true".equals(request.getParameter("pageResults"))) {
            getLogbookResults(request, form, null, "", "", null, true, true);
            return reflexMap;
        }

        if (result.hasErrors()) {
            saveErrors(result);
        }

        List<Result> checkPagedResults = (List<Result>) request.getSession()
                .getAttribute(IActionConstants.RESULTS_SESSION_CACHE);
        List<Result> checkResults = (List<Result>) checkPagedResults.get(0);
        if (checkResults.size() == 0) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "LogbookResults()", "Attempted save of stale page.");

            List<TestResultItem> resultList = form.getTestResult();
            for (TestResultItem item : resultList) {
                item.setFailedValidation(true);
                item.setNote("Result has been saved by another user.");
            }

            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
            actionDataSet.filterModifiedItems(form.getTestResult());

            Errors errors = actionDataSet.validateModifiedItems();

            if (true) {
                saveErrors(errors);
            }
        }

        List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

        ResultsPaging paging = new ResultsPaging();
        paging.updatePagedResults(request, form);
        List<TestResultItem> tests = paging.getResults(request);

        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
        actionDataSet.filterModifiedItems(tests);

        Errors errors = actionDataSet.validateModifiedItems();

        if (errors.hasErrors()) {
            saveErrors(errors);
        }

        ResultUtil.createResultsFromItems(actionDataSet, supportReferrals, alwaysValidate, useTechnicianName,
                statusRuleSet, request);
        ResultUtil.createAnalysisOnlyUpdates(actionDataSet, request);

        try {
            List<Analysis> reflexAnalysises = logbookPersistService.persistDataSet(actionDataSet, updaters,
                    getSysUserId(request));
            reflexMap.put("reflex", reflexAnalysises.stream().filter(e -> !e.getResultCalculated())
                    .map(e -> analysisService.getOrderAccessionNumber(e)).collect(Collectors.toList()));
            reflexMap.put("calculated", reflexAnalysises.stream().filter(e -> e.getResultCalculated())
                    .map(e -> analysisService.getOrderAccessionNumber(e)).collect(Collectors.toList()));
            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
            List<Analysis> newResultAnalyses = actionDataSet.getNewResults().stream().map(a -> a.result.getAnalysis())
                    .collect(Collectors.toList());
            List<String> systemUserIds = userRoleService.getUserIdsForRole(Constants.ROLE_VALIDATION);
            String message = MessageUtil.getMessage("notification.result.stat");
            StringBuffer sb = new StringBuffer(message);
            for (String userId : systemUserIds) {
                List<Analysis> userAnalyses = userService
                        .filterAnalysesByLabUnitRoles(userId, newResultAnalyses, Constants.ROLE_VALIDATION).stream()
                        .filter(a -> a.getSampleItem().getSample().getPriority().equals(OrderPriority.STAT))
                        .collect(Collectors.toList());

                if (userAnalyses != null && !userAnalyses.isEmpty()) {
                    List<String> userTests = userAnalyses.stream()
                            .map(a -> AlphanumAccessionValidator
                                    .convertAlphaNumLabNumForDisplay(a.getSampleItem().getSample().getAccessionNumber())
                                    + " - " + a.getTest().getLocalizedName())
                            .collect(Collectors.toList());
                    String testString = String.join(", ", userTests);
                    sb.append(testString);
                    try {
                        Notification notification = new Notification();
                        notification.setMessage(sb.toString());
                        notification.setUser(systemUserService.getUserById(userId));
                        notification.setCreatedDate(OffsetDateTime.now());
                        notification.setReadAt(null);
                        notificationDAO.save(notification);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (LIMSRuntimeException e) {
            String errorMsg;
            if (e.getCause() instanceof StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                LogEvent.logDebug(e);
                errorMsg = "errors.UpdateException";
            }

            errors.reject(errorMsg, errorMsg);
            saveErrors(errors);
        }

        for (IResultUpdate updater : updaters) {
            try {
                updater.postTransactionalCommitUpdate(actionDataSet);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "showLogbookResultsUpdate",
                        "error doing a post transactional commit");
                LogEvent.logError(e);
            }
        }

        if (GenericValidator.isBlankOrNull(form.getType())) {
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("type", form.getType());
        }
        return reflexMap;
    }

    private String findLogBookForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/LogbookResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultsLogbookDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findAccessionForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/AccessionResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "accessionResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "accessionResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findPatientForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PatientResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "patientResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "patientResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findStatusForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/StatusResults?blank=true";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "statusResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "statusResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findRangeForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/RangeResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultsLogbookDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (request.getRequestURL().indexOf("RangeResults") >= 0) {
            return findRangeForward(forward);
        } else if (request.getRequestURL().indexOf("LogbookResults") >= 0) {
            return findLogBookForward(forward);
        } else if (request.getRequestURL().indexOf("AccessionResults") >= 0) {
            return findAccessionForward(forward);
        } else if (request.getRequestURL().indexOf("PatientResults") >= 0) {
            return findPatientForward(forward);
        } else if (request.getRequestURL().indexOf("StatusResults") >= 0) {
            return findStatusForward(forward);
        } else {
            return "PageNotFound";
        }
    }

    private Patient getPatient(String patientID) {
        return patientService.get(patientID);
    }
}
