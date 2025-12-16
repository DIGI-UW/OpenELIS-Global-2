package org.openelisglobal.result.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsPaging;
import org.openelisglobal.result.form.AccessionResultsForm;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccessionResultsController extends BaseController {

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

    public AccessionResultsController(RoleService roleService) {
        Role editRole = roleService.getRoleByName("Results");
        if (editRole != null) {
            RESULT_EDIT_ROLE_ID = editRole.getId();
        } else {
            RESULT_EDIT_ROLE_ID = null;
        }
    }

    @RequestMapping(value = "/AccessionResults", method = RequestMethod.GET)
    public ModelAndView showAccessionResults(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AccessionResultsForm form = new AccessionResultsForm();
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);
        form.setReferralReasons(DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
        form.setRejectReasons(DisplayListService.getInstance()
                .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));
        form.setReferralOrganizations(DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setMethods(DisplayListService.getInstance().getList(ListType.METHODS));

        ResultsPaging paging = new ResultsPaging();
        String newPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(newPage)) {

            String accessionNumber = request.getParameter("accessionNumber");
            form.setDisplayTestKit(false);

            if (!GenericValidator.isBlankOrNull(accessionNumber)) {
                Errors errors = new BeanPropertyBindingResult(form, "form");
                ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
                resultsUtility.setSysUser(getSysUserId(request));
                // This is for Haiti_LNSP if it gets more complicated use the status set stuff
                resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.Canceled);
                resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.SampleRejected);
                // resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.Finalized);
                resultsUtility.setLockCurrentResults(modifyResultsRoleBased() && userNotInRole(request));
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

                    List<TestResultItem> results = resultsUtility.getGroupedTestsForSample(sample, patient);
                    List<TestResultItem> filteredResults = userService
                            .filterResultsByLabUnitRoles(getSysUserId(request), results, Constants.ROLE_RESULTS);

                    if (resultsUtility.inventoryNeeded()) {
                        addInventory(form);
                        form.setDisplayTestKit(true);
                    } else {
                        addEmptyInventoryList(form, accessionNumber);
                    }

                    paging.setDatabaseResults(request, form, filteredResults);
                } else {
                    setEmptyResults(form, accessionNumber);
                }
            } else {
                form.setTestResult(new ArrayList<TestResultItem>());
                form.setSearchFinished(Boolean.FALSE);
            }
        } else {
            paging.page(request, form, Integer.parseInt(newPage));
        }

        return findForward(FWD_SUCCESS, form);
    }

    private boolean modifyResultsRoleBased() {
        return "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.roleRequiredForModifyResults));
    }

    private boolean userNotInRole(HttpServletRequest request) {
        if (userModuleService.isUserAdmin(request)) {
            return false;
        }

        List<String> roleIds = userRoleService.getRoleIdsForUser(getSysUserId(request));

        return !roleIds.contains(RESULT_EDIT_ROLE_ID);
    }

    private void setEmptyResults(AccessionResultsForm form, String accessionNumber)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setTestResult(new ArrayList<TestResultItem>());
        form.setDisplayTestKit(false);
        addEmptyInventoryList(form, accessionNumber);
    }

    private void addInventory(AccessionResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        List<InventoryKitItem> list = inventoryUtility.getExistingActiveInventory();
        List<String> hivKits = new ArrayList<>();
        List<String> syphilisKits = new ArrayList<>();
        for (InventoryKitItem item : list) {
            if (item.getType().equals("HIV")) {
                hivKits.add(item.getInventoryLocationId());
            } else {
                syphilisKits.add(item.getInventoryLocationId());
            }
        }
        form.setHivKits(hivKits);
        form.setSyphilisKits(syphilisKits);
        form.setInventoryItems(list);
    }

    private void addEmptyInventoryList(AccessionResultsForm form, String accessionNumber)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setInventoryItems(new ArrayList<InventoryKitItem>());
        form.setHivKits(new ArrayList<String>());
        form.setSyphilisKits(new ArrayList<String>());
    }

    private Errors validateAll(HttpServletRequest request, Errors errors, AccessionResultsForm form,
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
            return "accessionResultDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "accessionResultDefinition";
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
