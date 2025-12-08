package org.openelisglobal.analyzerimport.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("form")
public class AnalyzerTestNameController extends BaseController {

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

    @RequestMapping(value = "/AnalyzerTestName", method = RequestMethod.GET)
    public ModelAndView showAnalyzerTestName(HttpServletRequest request, @ModelAttribute("form") BaseForm oldForm,
            RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnalyzerTestNameForm newForm = resetSessionFormToType(oldForm, AnalyzerTestNameForm.class);
        newForm.setCancelAction("CancelAnalyzerTestName");

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "true");
        request.setAttribute(NEXT_DISABLED, "true");

        List<Analyzer> analyzerList = getAllAnalyzers();
        List<Test> testList = getAllTests();

        newForm.setAnalyzerList(analyzerList);
        newForm.setTestList(testList);

        if (request.getParameter("ID") != null && isValidID(request.getParameter("ID"))) {
            String[] splitId = request.getParameter("ID").split("#");
            newForm.setAnalyzerTestName(splitId[1]);
            newForm.setTestId(splitId[2]);
            newForm.setAnalyzerId(splitId[0]);
        }

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(newForm.getAnalyzerId())) {
            newForm.setNewMapping(true);
        } else {
            newForm.setNewMapping(false);
        }

        return findForward(FWD_SUCCESS, newForm);
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

    @RequestMapping(value = "/AnalyzerTestName", method = RequestMethod.POST)
    public ModelAndView showUpdateAnalyzerTestName(HttpServletRequest request,
            @ModelAttribute("form") @Valid AnalyzerTestNameForm form, BindingResult result, SessionStatus status,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        String forward;

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "false");
        request.setAttribute(NEXT_DISABLED, "false");

        forward = updateAnalyzerTestName(request, form, result);
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            status.setComplete();
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            return findForward(forward, form);
        }
        return findForward(forward, form);
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

    @RequestMapping(value = "/CancelAnalyzerTestName", method = RequestMethod.GET)
    public ModelAndView cancelAnalyzerTestName(HttpServletRequest request, SessionStatus status) {
        status.setComplete();
        return findForward(FWD_CANCEL, new AnalyzerTestNameForm());
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
