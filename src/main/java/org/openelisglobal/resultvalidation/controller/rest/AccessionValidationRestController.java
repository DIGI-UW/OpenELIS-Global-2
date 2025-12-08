package org.openelisglobal.resultvalidation.controller.rest;

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ValidationUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.action.util.ResultValidationPaging;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.controller.BaseResultValidationController;
import org.openelisglobal.resultvalidation.form.ResultValidationForm;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.resultvalidation.util.ResultValidationSaveService;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = "/rest/")
public class AccessionValidationRestController extends BaseResultValidationController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @Autowired
    SearchResultsService searchService;
    @Autowired
    private SampleService sampleService;

    private static final String[] ALLOWED_FIELDS = new String[] { "testSectionId", "paging.currentPage", "testSection",
            "testName", "resultList*.accessionNumber", "resultList*.analysisId", "resultList*.testId",
            "resultList*.sampleId", "resultList*.resultType", "resultList*.sampleGroupingNumber", "resultList*.noteId",
            "resultList*.resultId", "resultList*.hasQualifiedResult", "resultList*.sampleIsAccepted",
            "resultList*.sampleIsRejected", "resultList*.result", "resultList*.qualifiedResultValue",
            "resultList*.multiSelectResultValues", "resultList*.isAccepted", "resultList*.isRejected",
            "resultList*.note" };

    // autowiring not needed, using constructor injection
    private AnalysisService analysisService;
    private TestResultService testResultService;
    private SampleHumanService sampleHumanService;
    private DocumentTrackService documentTrackService;
    private TestSectionService testSectionService;
    private SystemUserService systemUserService;
    private ResultValidationService resultValidationService;
    private NoteService noteService;
    private FhirTransformService fhirTransformService;

    private final String RESULT_SUBJECT = "Result Note";
    private final String RESULT_TABLE_ID;
    private final String RESULT_REPORT_ID;

    public AccessionValidationRestController(AnalysisService analysisService, TestResultService testResultService,
            SampleHumanService sampleHumanService, DocumentTrackService documentTrackService,
            TestSectionService testSectionService, SystemUserService systemUserService,
            ReferenceTablesService referenceTablesService, DocumentTypeService documentTypeService,
            ResultValidationService resultValidationService, NoteService noteService,
            FhirTransformService fhirTransformService) {

        this.analysisService = analysisService;
        this.testResultService = testResultService;
        this.sampleHumanService = sampleHumanService;
        this.documentTrackService = documentTrackService;
        this.testSectionService = testSectionService;
        this.systemUserService = systemUserService;
        this.resultValidationService = resultValidationService;
        this.noteService = noteService;
        this.fhirTransformService = fhirTransformService;

        RESULT_TABLE_ID = referenceTablesService.getReferenceTableByName("RESULT").getId();
        RESULT_REPORT_ID = documentTypeService.getDocumentTypeByName("resultExport").getId();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "AccessionValidation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResultValidationForm showAccessionValidationRange(HttpServletRequest request,
            @RequestParam(required = false) String accessionNumber, @RequestParam(required = false) String date,
            @RequestParam(required = false) String unitType, @RequestParam(defaultValue = "true") Boolean doRange)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        ResultValidationForm newForm = new ResultValidationForm();
        if (StringUtils.isNotBlank(accessionNumber)) {
            newForm.setAccessionNumber(accessionNumber);
        } else if (StringUtils.isNotBlank(date)) {
            newForm.setTestDate(date);
        } else if (StringUtils.isNotBlank(unitType)) {
            newForm.setTestSectionId(unitType);
        }
        return getResultValidation(request, newForm, doRange);
    }

    private ResultValidationForm getResultValidation(HttpServletRequest request, ResultValidationForm form,
            Boolean doRange) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String patientName = "";
        String patientInfo = "";
        Patient patient = null;
        List<AnalysisItem> filteredresultList = new ArrayList<>();

        request.getSession().setAttribute(SAVE_DISABLED, "true");

        ResultValidationPaging paging = new ResultValidationPaging();
        String newPage = request.getParameter("page");

        TestSection ts = null;
        form.setSearchFinished(false);

        if (GenericValidator.isBlankOrNull(newPage)) {

            // load testSections for drop down
            String resultsRoleId = roleService.getRoleByName(Constants.ROLE_VALIDATION).getId();
            List<IdValuePair> testSections = userService.getUserTestSections(getSysUserId(request), resultsRoleId);
            form.setTestSections(testSections);
            form.setTestSectionsByName(DisplayListService.getInstance().getList(ListType.TEST_SECTION_BY_NAME));

            if (!GenericValidator.isBlankOrNull(form.getTestSectionId())) {
                ts = testSectionService.get(form.getTestSectionId());
            }

            List<AnalysisItem> resultList = new ArrayList<>();

            ResultsValidationUtility resultsValidationUtility = SpringContext.getBean(ResultsValidationUtility.class);
            if (request.getRequestURI().contains("AccessionValidationRange")) {
                setRequestType(ts == null ? MessageUtil.getMessage("validation.range.title") : ts.getLocalizedName());
            } else if (request.getRequestURI().contains("ResultValidationByTestDate")) {
                setRequestType(ts == null ? MessageUtil.getMessage("validation.date.title") : ts.getLocalizedName());
            }
            if (!(GenericValidator.isBlankOrNull(form.getTestSectionId())
                    && GenericValidator.isBlankOrNull(form.getAccessionNumber())
                    && GenericValidator.isBlankOrNull(form.getTestDate()))) {

                if (doRange) {
                    resultList = resultsValidationUtility.getResultValidationList(getValidationStatus(),
                            form.getTestSectionId(), form.getAccessionNumber(), form.getTestDate());
                } else {
                    if (StringUtils.isNotBlank(form.getAccessionNumber())) {
                        Sample sample = getSample(form.getAccessionNumber());
                        if (sample == null) {
                            setEmptyResults(form);
                            return form;
                        } else {
                            resultList = resultsValidationUtility.getValidationAnalysisBySample(sample);
                        }
                    }
                }

                filteredresultList = userService.filterAnalysisResultsByLabUnitRoles(getSysUserId(request), resultList,
                        Constants.ROLE_VALIDATION);
                request.setAttribute("pageSize", filteredresultList.size());
                form.setSearchFinished(true);
            } else {
                resultList = new ArrayList<>();
            }
            paging.setDatabaseResults(request, form, filteredresultList);
        } else {
            paging.page(request, form, Integer.parseInt(newPage));
        }

        addFlashMsgsToRequest(request);

        for (AnalysisItem analysisItem : filteredresultList) {
            analysisItem.setPatientName(patientName);
        }

        return form;
    }

    public List<Integer> getValidationStatus() {
        List<Integer> validationStatus = new ArrayList<>();
        validationStatus.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
        if (ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.VALIDATE_REJECTED_TESTS, "true")) {
            validationStatus.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
        }

        return validationStatus;
    }

    @PostMapping(value = "AccessionValidation", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResultValidationForm showAccessionValidationRangeSave(HttpServletRequest request,
            @Validated(ResultValidationForm.ResultValidation.class) @RequestBody ResultValidationForm form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if ("true".equals(request.getParameter("pageResults"))) {
            return getResultValidation(request, form, false);
        }
        form.setSearchFinished(false);

        if (result.hasErrors()) {
            saveErrors(result);
        }
        List<IResultUpdate> updaters = ValidationUpdateRegister.getRegisteredUpdaters();
        boolean areListeners = !updaters.isEmpty();

        request.getSession().setAttribute(SAVE_DISABLED, "true");

        List<Result> checkPagedResults = (List<Result>) request.getSession()
                .getAttribute(IActionConstants.RESULTS_SESSION_CACHE);
        List<Result> checkResults = (List<Result>) checkPagedResults.get(0);
        if (checkResults.size() == 0) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "ResultValidation()", "Attempted save of stale page.");
            return form;
        }

        ResultValidationPaging paging = new ResultValidationPaging();
        paging.updatePagedResults(request, form);
        List<AnalysisItem> resultItemList = paging.getResults(request);

        String testSectionName = form.getTestSection();
        String testName = form.getTestName();
        setRequestType(testSectionName);
        // ----------------------
        String url = request.getRequestURL().toString();

        Errors errors = validateModifiedItems(resultItemList);
        if (errors.hasErrors()) {
            saveErrors(errors);
            // return findForward(FWD_VALIDATION_ERROR, form);
            return form;
        }

        createSystemUser();

        // Update Lists
        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<Result> deletableList = new ArrayList<>();

        // wrapper object for holding modifedResultSet and newResultSet
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        // if (testSectionName.equals("serology")) {
        // createUpdateElisaList(resultItemList, analysisUpdateList);
        // } else {
        createUpdateList(resultItemList, analysisUpdateList, resultUpdateList, noteUpdateList, deletableList,
                resultSaveService, areListeners);
        // }
        try {
            resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                    sampleUpdateList, noteUpdateList, resultSaveService, updaters, getSysUserId(request));

            try {
                fhirTransformService.transformPersistResultValidationFhirObjects(deletableList, analysisUpdateList,
                        resultUpdateList, resultItemList, sampleUpdateList, noteUpdateList);
            } catch (FhirLocalPersistingException e) {
                LogEvent.logError(e);
            }
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
        }

        for (IResultUpdate updater : updaters) {

            // updater.postTransactionalCommitUpdate(resultSaveService);
        }

        // route save back to RetroC specific ResultValidationRetroCAction
        // if
        // (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName,
        // "CI RetroCI"))
        // redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        if (isBlankOrNull(testSectionName)) {
            // return findForward(forward, form);
            return form;
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("type", testSectionName);
            params.put("test", testName);
            // return getForwardWithParameters(findForward(forward, form), params);
        }

        return (form);
    }

    private Errors validateModifiedItems(List<AnalysisItem> resultItemList) {
        Errors errors = new BaseErrors();

        for (AnalysisItem item : resultItemList) {
            Errors errorList = new BaseErrors();
            validateQuantifiableItems(item, errorList);

            if (errorList.hasErrors()) {
                StringBuilder augmentedAccession = new StringBuilder(item.getAccessionNumber());
                augmentedAccession.append(" : ");
                augmentedAccession.append(item.getTestName());
                String errorMsg = "errors.followingAccession";
                errors.reject(errorMsg, new String[] { augmentedAccession.toString() }, errorMsg);
                errors.addAllErrors(errorList);
            }
        }

        return errors;
    }

    public void validateQuantifiableItems(AnalysisItem analysisItem, Errors errorList) {
        if (analysisItem.isHasQualifiedResult() && isBlankOrNull(analysisItem.getQualifiedResultValue())
                && analysisItemWillBeUpdated(analysisItem)) {
            errorList.reject("errors.missing.result.details", new String[] { "Result" },
                    "errors.missing.result.details");
        }
        // verify that qualifiedResultValue has been entered if required
        if (!isBlankOrNull(analysisItem.getQualifiedDictionaryId())) {
            String[] qualifiedDictionaryIds = analysisItem.getQualifiedDictionaryId().replace("[", "").replace("]", "")
                    .split(",");
            Set<String> qualifiedDictIdsSet = new HashSet<>(Arrays.asList(qualifiedDictionaryIds));

            if (qualifiedDictIdsSet.contains(analysisItem.getResult())
                    && isBlankOrNull(analysisItem.getQualifiedResultValue())) {
                errorList.reject("errors.missing.result.details", new String[] { "Result" },
                        "errors.missing.result.details");
            }
        }
    }

    private void createUpdateList(List<AnalysisItem> analysisItems, List<Analysis> analysisUpdateList,
            List<Result> resultUpdateList, List<Note> noteUpdateList, List<Result> deletableList,
            IResultSaveService resultValidationSave, boolean areListeners) {

        List<String> analysisIdList = new ArrayList<>();

        for (AnalysisItem analysisItem : analysisItems) {
            if (!analysisItem.isReadOnly() && analysisItemWillBeUpdated(analysisItem)) {

                Analysis analysis = analysisService.get(analysisItem.getAnalysisId());
                analysis.setSysUserId(getSysUserId(request));

                if (!analysisIdList.contains(analysis.getId())) {

                    if (analysisItem.getIsAccepted()) {
                        analysis.setStatusId(
                                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                        analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                        analysisIdList.add(analysis.getId());
                        analysisUpdateList.add(analysis);
                    }

                    if (analysisItem.getIsRejected()) {
                        analysis.setStatusId(SpringContext.getBean(IStatusService.class)
                                .getStatusID(AnalysisStatus.BiologistRejected));
                        analysisIdList.add(analysis.getId());
                        analysisUpdateList.add(analysis);
                    }
                }

                createNeededNotes(analysisItem, analysis, noteUpdateList);

                if (areResults(analysisItem)) {
                    List<Result> results = createResultFromAnalysisItem(analysisItem, analysis, analysis,
                            noteUpdateList, deletableList);
                    for (Result result : results) {
                        resultUpdateList.add(result);

                        if (areListeners) {
                            addResultSets(analysis, result, resultValidationSave);
                        }
                    }
                }
            }
        }
    }

    private void createNeededNotes(AnalysisItem analysisItem, Analysis analysis, List<Note> noteUpdateList) {
        if (analysisItem.getIsRejected()) {
            Note note = noteService.createSavableNote(analysis, NoteType.INTERNAL,
                    MessageUtil.getMessage("validation.note.retest"), RESULT_SUBJECT, getSysUserId(request));
            noteUpdateList.add(note);
        }

        if (!GenericValidator.isBlankOrNull(analysisItem.getNote())) {
            NoteType noteType = analysisItem.getIsAccepted() ? NoteType.EXTERNAL : NoteType.INTERNAL;
            Note note = noteService.createSavableNote(analysis, noteType, analysisItem.getNote(), RESULT_SUBJECT,
                    getSysUserId(request));
            noteUpdateList.add(note);
        }
    }

    private void addResultSets(Analysis analysis, Result result, IResultSaveService resultValidationSave) {
        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (finalResultAlreadySent(result)) {
            result.setResultEvent(Event.CORRECTION);
            resultValidationSave.getModifiedResults()
                    .add(new ResultSet(result, null, null, patient, sample, null, false));
        } else {
            result.setResultEvent(Event.FINAL_RESULT);
            resultValidationSave.getNewResults().add(new ResultSet(result, null, null, patient, sample, null, false));
        }
    }

    // TO DO bug falsely triggered when preliminary result is sent, fails, retries
    // and succeeds
    private boolean finalResultAlreadySent(Result result) {
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTable(RESULT_REPORT_ID, RESULT_TABLE_ID,
                result.getId());
        return documents.size() > 0;
    }

    private boolean analysisItemWillBeUpdated(AnalysisItem analysisItem) {
        return analysisItem.getIsAccepted() || analysisItem.getIsRejected();
    }

    private void createUpdateElisaList(List<AnalysisItem> resultItems, List<Analysis> analysisUpdateList) {

        for (AnalysisItem resultItem : resultItems) {

            if (resultItem.getIsAccepted()) {

                List<Analysis> acceptedAnalysisList = createAnalysisFromElisaAnalysisItem(resultItem);

                for (Analysis analysis : acceptedAnalysisList) {
                    analysis.setStatusId(
                            SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                    analysisUpdateList.add(analysis);
                }
            }

            if (resultItem.getIsRejected()) {
                List<Analysis> rejectedAnalysisList = createAnalysisFromElisaAnalysisItem(resultItem);

                for (Analysis analysis : rejectedAnalysisList) {
                    analysis.setStatusId(
                            SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected));
                    analysisUpdateList.add(analysis);
                }
            }
        }
    }

    private List<Analysis> createAnalysisFromElisaAnalysisItem(AnalysisItem analysisItem) {

        List<Analysis> analysisList = new ArrayList<>();

        Analysis analysis = new Analysis();

        if (!isBlankOrNull(analysisItem.getMurexResult())) {
            analysis = getAnalysisFromId(analysisItem.getMurexAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getBiolineResult())) {
            analysis = getAnalysisFromId(analysisItem.getBiolineAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getIntegralResult())) {
            analysis = getAnalysisFromId(analysisItem.getIntegralAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getVironostikaResult())) {
            analysis = getAnalysisFromId(analysisItem.getVironostikaAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getGenieIIResult())) {
            analysis = getAnalysisFromId(analysisItem.getGenieIIAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getGenieII10Result())) {
            analysis = getAnalysisFromId(analysisItem.getGenieII10AnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getGenieII100Result())) {
            analysis = getAnalysisFromId(analysisItem.getGenieII100AnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getWesternBlot1Result())) {
            analysis = getAnalysisFromId(analysisItem.getWesternBlot1AnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getWesternBlot2Result())) {
            analysis = getAnalysisFromId(analysisItem.getWesternBlot2AnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getP24AgResult())) {
            analysis = getAnalysisFromId(analysisItem.getP24AgAnalysisId());
            analysisList.add(analysis);
        }
        if (!isBlankOrNull(analysisItem.getInnoliaResult())) {
            analysis = getAnalysisFromId(analysisItem.getInnoliaAnalysisId());
            analysisList.add(analysis);
        }

        analysisList.add(analysis);

        return analysisList;
    }

    private Analysis getAnalysisFromId(String id) {
        Analysis analysis = analysisService.get(id);
        analysis.setSysUserId(getSysUserId(request));

        return analysis;
    }

    private List<Result> createResultFromAnalysisItem(AnalysisItem analysisItem, Analysis analysis, Analysis analysis2,
            List<Note> noteUpdateList, List<Result> deletableList) {

        ResultSaveBean bean = ResultSaveBeanAdapter.fromAnalysisItem(analysisItem);
        ResultSaveService resultSaveService = new ResultSaveService(analysis, getSysUserId(request));
        List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, deletableList);
        if (analysisService.patientReportHasBeenDone(analysis) && resultSaveService.isUpdatedResult()) {
            Note note = noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                    MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT, getSysUserId(request));
            if (!noteService.duplicateNoteExists(note)) {
                analysis.setCorrectedSincePatientReport(true);
                noteUpdateList.add(noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                        MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT, getSysUserId(request)));
            }
        }
        return results;
    }

    protected TestResult getTestResult(AnalysisItem analysisItem) {
        TestResult testResult = null;
        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(analysisItem.getResultType())) {
            testResult = testResultService.getTestResultsByTestAndDictonaryResult(analysisItem.getTestId(),
                    analysisItem.getResult());
        } else {
            List<TestResult> testResultList = testResultService.getActiveTestResultsByTest(analysisItem.getTestId());
            // we are assuming there is only one testResult for a numeric type
            // result
            if (!testResultList.isEmpty()) {
                testResult = testResultList.get(0);
            }
        }
        return testResult;
    }

    private boolean areResults(AnalysisItem item) {
        return !(isBlankOrNull(item.getResult())
                || (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(item.getResultType())
                        && "0".equals(item.getResult())))
                || (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(item.getResultType())
                        && !isBlankOrNull(item.getMultiSelectResultValues()));
    }

    private SystemUser createSystemUser() {
        return systemUserService.get(getSysUserId(request));
    }

    private Sample getSample(String accessionNumber) {
        return sampleService.getSampleByAccessionNumber(accessionNumber);
    }

    private Patient getPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    private void setEmptyResults(ResultValidationForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setResultList(new ArrayList<AnalysisItem>());
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "accessionValidationRangeDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/AccessionValidationRange";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "accessionValidationRangeDefinition";
        } else {
            return "PageNotFound";
        }
    }
}
