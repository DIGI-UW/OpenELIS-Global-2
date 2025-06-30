package org.openelisglobal.resultvalidation.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.resultvalidation.action.util.ResultValidationPaging;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.form.ResultValidationForm;
import org.openelisglobal.resultvalidation.util.ResultsValidationRetroCIUtility;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ResultValidationRetroCController extends BaseResultValidationRetroCIController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    private ResultsValidationRetroCIUtility resultsValidationUtility;

    public ResultValidationRetroCController(ResultsValidationRetroCIUtility resultsValidationUtility) {
        this.resultsValidationUtility = resultsValidationUtility;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ResultValidationRetroC")
    public ModelAndView showResultValidationRetroC(HttpServletRequest request, ResultValidationForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(SAVE_DISABLED, "true");
        String testSectionName = (request.getParameter("type"));
        String testName = (request.getParameter("test"));
        form.setTestSection(testSectionName);

        ResultValidationPaging paging = new ResultValidationPaging();
        String newPage = request.getParameter("page");

        if (GenericValidator.isBlankOrNull(newPage)) {
            form.setTestSectionsByName(new ArrayList<IdValuePair>()); // required on jsp page
            form.setDisplayTestSections(false);

            setRequestType(testSectionName);

            if (!GenericValidator.isBlankOrNull(testSectionName)) {
                String sectionName = Character.toUpperCase(testSectionName.charAt(0)) + testSectionName.substring(1);
                sectionName = getDBSectionName(sectionName);
                List<AnalysisItem> resultList = resultsValidationUtility.getResultValidationList(sectionName, testName,
                        getValidationStatus(testSectionName));
                form.setSearchFinished(true);
                paging.setDatabaseResults(request, form, resultList);
            }

        } else {
            form.setSearchFinished(true);
            paging.page(request, form, Integer.parseInt(newPage));
        }

        addFlashMsgsToRequest(request);
        if (testSectionName.equals("serology")) {
            return findForward("elisaSuccess", form);
        } else {
            return findForward(FWD_SUCCESS, form);
        }
    }

    public List<Integer> getValidationStatus(String testSection) {
        List<Integer> validationStatus = new ArrayList<>();

        if ("serology".equals(testSection)) {
            validationStatus.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
            validationStatus.add(
                    Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
            // This next status determines if NonConformity analysis can still
            // be displayed on bio. validation page. We are awaiting feedback on
            // RetroCI
            // validationStatus.add(Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming)));
        } else {
            validationStatus.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
            if (ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(ConfigurationProperties.Property.VALIDATE_REJECTED_TESTS, "true")) {
                validationStatus.add(Integer.parseInt(
                        SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
            }
        }

        return validationStatus;
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

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultValidationDefinition";
        } else if ("elisaSuccess".equals(forward)) {
            return "elisaAlgorithmResultValidationDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }
}
