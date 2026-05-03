package org.openelisglobal.result.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.controller.LogbookResultsBaseController;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Mirrors
 * {@link org.openelisglobal.result.controller.AccessionResultsController} data
 * loading for the React Accession Results page.
 */
@Controller
@RequestMapping(value = "/rest/")
public class AccessionResultsRestController extends LogbookResultsBaseController {

    private final Integer RESULT_EDIT_ROLE_ID;

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;

    public AccessionResultsRestController(RoleService roleService) {
        Role editRole = roleService.getRoleByName("Results");
        RESULT_EDIT_ROLE_ID = editRole != null ? editRole.getId() : null;
    }

    @GetMapping(value = "accession-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AccessionResultsRestResponse getAccessionResults(HttpServletRequest request,
            @RequestParam(required = false) String accessionNumber)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        AccessionResultsRestResponse body = new AccessionResultsRestResponse();
        body.setAccessionNumber(accessionNumber);

        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            body.setSearchFinished(false);
            body.setTestResult(new ArrayList<>());
            return body;
        }

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            body.setSearchFinished(false);
            body.setTestResult(new ArrayList<>());
            body.setError("sample.edit.sample.notFound");
            return body;
        }

        if (GenericValidator.isBlankOrNull(sample.getId())) {
            body.setSearchFinished(true);
            body.setTestResult(new ArrayList<>());
            return body;
        }

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsUtility.setSysUser(getSysUserId(request));
        resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.Canceled);
        resultsUtility.addExcludedAnalysisStatus(AnalysisStatus.SampleRejected);
        resultsUtility.setLockCurrentResults(modifyResultsRoleBased() && userNotInRole(request));

        Patient patient = sampleHumanService.getPatientForSample(sample);
        AccessionResultsForm patientForm = new AccessionResultsForm();
        resultsUtility.addIdentifingPatientInfo(patient, patientForm);
        copyPatientFields(patientForm, body);

        List<TestResultItem> results = resultsUtility.getGroupedTestsForSample(sample, patient);
        List<TestResultItem> filteredResults = userService.filterResultsByLabUnitRoles(getSysUserId(request), results,
                Constants.ROLE_RESULTS);

        body.setSearchFinished(Boolean.TRUE);
        body.setTestResult(filteredResults != null ? filteredResults : new ArrayList<>());
        return body;
    }

    private void copyPatientFields(AccessionResultsForm from, AccessionResultsRestResponse to) {
        to.setFirstName(from.getFirstName());
        to.setLastName(from.getLastName());
        to.setDob(from.getDob());
        to.setGender(from.getGender());
        to.setSt(from.getSt());
        to.setSubjectNumber(from.getSubjectNumber());
        to.setNationalId(from.getNationalId());
    }

    private boolean modifyResultsRoleBased() {
        return "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.roleRequiredForModifyResults));
    }

    private boolean userNotInRole(HttpServletRequest request) {
        if (userModuleService.isUserAdmin(request)) {
            return false;
        }

        List<Integer> roleIds = userRoleService.getRoleIdsForUser(getSysUserId(request));

        return RESULT_EDIT_ROLE_ID == null || !roleIds.contains(RESULT_EDIT_ROLE_ID);
    }

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }
}
