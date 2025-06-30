package org.openelisglobal.analyzerimport.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.form.AnalyzerTestNameForm;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.validator.AnalyzerTestMappingValidator;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

@RestController
@RequestMapping("/rest")
@SessionAttributes("form")
public class AnalyzerTestNameRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "analyzerId", "analyzerTestName", "testId" };

    @Autowired
    private AnalyzerTestMappingValidator analyzerTestMappingValidator;
    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private TestService testService;

    @ModelAttribute("form")
    public AnalyzerTestNameForm initForm() {
        return new AnalyzerTestNameForm();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/AnalyzerTestName", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showAnalyzerTestName(HttpServletRequest request, BaseForm oldForm,
            @RequestParam(value = "ID", required = false) String id)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnalyzerTestNameForm newForm = resetSessionFormToType(oldForm, AnalyzerTestNameForm.class);
        newForm.setCancelAction("CancelAnalyzerTestName");

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "true");
        request.setAttribute(NEXT_DISABLED, "true");

        List<Analyzer> analyzerList = getAllAnalyzers();

        newForm.setAnalyzerList(analyzerList);

        if (request.getParameter("ID") != null && isValidID(request.getParameter("ID"))) {
            String[] splitId = request.getParameter("ID").split("#");
            newForm.setAnalyzerTestName(splitId[1]);
            newForm.setAnalyzerId(splitId[0]);
        }

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(newForm.getAnalyzerId())) {
            newForm.setNewMapping(true);
        } else {
            newForm.setNewMapping(false);
        }

        return ResponseEntity.ok(newForm);
    }

    private boolean isValidID(String ID) {
        return ID.matches("^[0-9]+#[^#/\\<>?]*#[0-9]+");
    }

    private List<Analyzer> getAllAnalyzers() {
        return analyzerService.getAll();
    }

    private List<Test> getAllTests() {
        return testService.getAllActiveTests(false);
    }

    @PostMapping(value = "/AnalyzerTestName", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showUpdateAnalyzerTestName(HttpServletRequest request,
            @RequestBody @Valid AnalyzerTestNameForm form, BindingResult result, SessionStatus status) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        String forward = updateAnalyzerTestName(request, form, result);
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            status.setComplete();
            return ResponseEntity.status(HttpStatus.CREATED).body(form);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.getAllErrors());
    }

    public String updateAnalyzerTestName(HttpServletRequest request, AnalyzerTestNameForm form, Errors errors) {
        String forward = FWD_SUCCESS_INSERT;
        String analyzerId = form.getAnalyzerId();
        String analyzerTestName = form.getAnalyzerTestName();
        String testId = form.getTestId();
        boolean newMapping = form.isNewMapping();

        AnalyzerTestMapping analyzerTestNameMapping;
        if (newMapping) {
            analyzerTestNameMapping = new AnalyzerTestMapping();
            analyzerTestNameMapping.setAnalyzerId(analyzerId);
            analyzerTestNameMapping.setAnalyzerTestName(analyzerTestName);
            analyzerTestNameMapping.setTestId(testId);
            analyzerTestNameMapping.setSysUserId(getSysUserId(request));
        } else {
            analyzerTestNameMapping = getAnalyzerAndTestName(analyzerId, analyzerTestName, testId);
        }

        try {
            if (newMapping) {
                analyzerTestMappingValidator.preInsertValidate(analyzerTestNameMapping, errors);
                if (errors.hasErrors()) {
                    saveErrors(errors);
                    return FWD_FAIL_INSERT;
                }
                analyzerTestMappingService.insert(analyzerTestNameMapping);
            } else {
                analyzerTestMappingValidator.preUpdateValidate(analyzerTestNameMapping, errors);
                if (errors.hasErrors()) {
                    saveErrors(errors);
                    return FWD_FAIL_INSERT;
                }
                analyzerTestNameMapping.setSysUserId(getSysUserId(request));
                analyzerTestMappingService.update(analyzerTestNameMapping);
            }

        } catch (LIMSRuntimeException e) {
            String errorMsg = null;
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                errorMsg = "errors.UpdateException";
            }

            persistError(request, errorMsg);

            disableNavigationButtons(request);
            forward = FWD_FAIL_INSERT;
        }

        AnalyzerTestNameCache.getInstance().reloadCache();

        return forward;
    }

    private AnalyzerTestMapping getAnalyzerAndTestName(String analyzerId, String analyzerTestName, String testId) {

        AnalyzerTestMapping existingMapping = null;
        List<AnalyzerTestMapping> testMappingList = analyzerTestMappingService.getAll();
        for (AnalyzerTestMapping testMapping : testMappingList) {
            if (analyzerId.equals(testMapping.getAnalyzerId())
                    && analyzerTestName.equals(testMapping.getAnalyzerTestName())) {
                existingMapping = testMapping;
                testMapping.setTestId(testId);
                break;
            }
        }

        return existingMapping;
    }

    private void persistError(HttpServletRequest request, String errorMsg) {
        Errors errors;
        errors = new BaseErrors();

        errors.reject(errorMsg);
        saveErrors(errors);
    }

    private void disableNavigationButtons(HttpServletRequest request) {
        request.setAttribute(PREVIOUS_DISABLED, TRUE);
        request.setAttribute(NEXT_DISABLED, TRUE);
    }

    @GetMapping(value = "/CancelAnalyzerTestName", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalyzerTestNameForm cancelAnalyzerTestName(HttpServletRequest request, SessionStatus status) {
        status.setComplete();
        return (new AnalyzerTestNameForm());
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "analyzerTestNameDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/AnalyzerTestNameMenu";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/AnalyzerTestNameMenu";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "analyzerTestNameDefinition";
        } else if (FWD_CANCEL.equals(forward)) {
            return "redirect:/AnalyzerTestNameMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "analyzerTestName.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "analyzerTestName.browse.title";
    }
}
