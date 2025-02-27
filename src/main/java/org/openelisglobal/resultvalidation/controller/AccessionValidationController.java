package org.openelisglobal.resultvalidation.controller;

import com.fasterxml.jackson.databind.module.*;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultvalidation.action.util.ResultValidationPaging;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.form.AccessionValidationForm;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccessionValidationController extends BaseController {

    private final String RESULT_EDIT_ROLE_ID;

    private InventoryUtility inventoryUtility = SpringContext.getBean(InventoryUtility.class);
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;

    public AccessionValidationController(RoleService roleService) {
        Role editRole = roleService.getRoleByName("Results modifier");
        if (editRole != null) {
            RESULT_EDIT_ROLE_ID = editRole.getId();
        } else {
            RESULT_EDIT_ROLE_ID = null;
        }
    }

    @RequestMapping(value = "/AccessionValidation", method = RequestMethod.GET)
    public ModelAndView showAccessionValidation(HttpServletRequest request, AccessionValidationForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // AccessionValidationForm form = new AccessionValidationForm();

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);
        form.setReferralReasons(DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
        form.setRejectReasons(DisplayListService.getInstance()
                .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));

        ResultValidationPaging paging = new ResultValidationPaging();
        String newPage = request.getParameter("page");
        int count = 0;
        if (GenericValidator.isBlankOrNull(newPage)) {

            String accessionNumber = request.getParameter("accessionNumber");
            form.setDisplayTestKit(false);

            if (!GenericValidator.isBlankOrNull(accessionNumber)) {
                Errors errors = new BeanPropertyBindingResult(form, "form");
                ResultsValidationUtility resultsUtility = SpringContext.getBean(ResultsValidationUtility.class);
                // resultsUtility.setSysUser(getSysUserId(request));
                // This is for Haiti_LNSP if it gets more complicated use the status set stuff
                // resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.Canceled);
                // resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.Finalized);
                // resultsUtility.setLockCurrentResults(modifyResultsRoleBased() &&
                // userNotInRole(request));
                validateAll(request, errors, form, accessionNumber);

                if (errors.hasErrors()) {
                    saveErrors(errors);
                    request.setAttribute(ALLOW_EDITS_KEY, "false");

                    setEmptyResults(form, accessionNumber);

                    return findForward(FWD_FAIL, form);
                }

                form.setSearchFinished(Boolean.TRUE);

                Sample sample = getSample(accessionNumber);

                if (!GenericValidator.isBlankOrNull(sample.getId())) {
                    Patient patient = getPatient(sample);
                    resultsUtility.addIdentifingPatientInfo(patient, form);

                    List<AnalysisItem> resultsAnalysises = resultsUtility.getValidationAnalysisBySample(sample);
                    List<AnalysisItem> filteredresultList = userService.filterAnalysisResultsByLabUnitRoles(
                            getSysUserId(request), resultsAnalysises, Constants.ROLE_VALIDATION);
                    count = filteredresultList.size();
                    // if (resultsUtility.inventoryNeeded()) {
                    // addInventory(form);
                    // form.setDisplayTestKit(true);
                    // } else {
                    // addEmptyInventoryList(form, accessionNumber);
                    // }

                    paging.setDatabaseResults(request, form, filteredresultList);
                } else {
                    setEmptyResults(form, accessionNumber);
                }
            } else {
                form.setResultList(new ArrayList<AnalysisItem>());
                form.setSearchFinished(Boolean.FALSE);
            }
        } else {
            paging.page(request, form, Integer.parseInt(newPage));
        }
        request.setAttribute("analysisCount", count);
        request.setAttribute("pageSize", count);

        return findForward(FWD_SUCCESS, form);
    }

    private boolean modifyResultsRoleBased() {
        return "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.roleRequiredForModifyResults));
    }

    // private boolean userNotInRole(HttpServletRequest request) {
    // if (userModuleService.isUserAdmin(request)) {
    // return false;
    // }
    //
    // List<String> roleIds =
    // userRoleService.getRoleIdsForUser(getSysUserId(request));
    //
    // return !roleIds.contains(RESULT_EDIT_ROLE_ID);
    // }

    private void setEmptyResults(AccessionValidationForm form, String accessionNumber)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setResultList(new ArrayList<AnalysisItem>());
        form.setDisplayTestKit(false);
        // addEmptyInventoryList(form, accessionNumber);
    }

    // private void addInventory(AccessionValidationForm form)
    // throws IllegalAccessException, InvocationTargetException,
    // NoSuchMethodException {
    //
    // List<InventoryKitItem> list = inventoryUtility.getExistingActiveInventory();
    // List<String> hivKits = new ArrayList<>();
    // List<String> syphilisKits = new ArrayList<>();
    // for (InventoryKitItem item : list) {
    // if (item.getType().equals("HIV")) {
    // hivKits.add(item.getInventoryLocationId());
    // } else {
    // syphilisKits.add(item.getInventoryLocationId());
    // }
    // }
    // form.setHivKits(hivKits);
    // form.setSyphilisKits(syphilisKits);
    // form.setInventoryItems(list);
    // }

    // private void addEmptyInventoryList(AccessionValidationForm form, String
    // accessionNumber)
    // throws IllegalAccessException, InvocationTargetException,
    // NoSuchMethodException {
    // form.setInventoryItems(new ArrayList<InventoryKitItem>());
    // form.setHivKits(new ArrayList<String>());
    // form.setSyphilisKits(new ArrayList<String>());
    // }

    private Errors validateAll(HttpServletRequest request, Errors errors, AccessionValidationForm form,
            String accessionNumber) {

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

        if (sample == null) {
            // ActionError error = new ActionError("sample.edit.sample.notFound",
            // accessionNumber, null, null);
            errors.reject("sample.edit.sample.notFound", new String[] {}, "sample.edit.sample.notFound");
        }

        return errors;
    }

    private Patient getPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    private Sample getSample(String accessionNumber) {
        return sampleService.getSampleByAccessionNumber(accessionNumber);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "accessionValidationDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "accessionValidationDefinition";
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
}
