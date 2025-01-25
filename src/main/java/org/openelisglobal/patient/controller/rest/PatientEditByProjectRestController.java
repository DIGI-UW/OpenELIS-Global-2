package org.openelisglobal.patient.controller.rest;

import java.lang.reflect.InvocationTargetException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.patient.controller.BasePatientEntryByProject;
import org.openelisglobal.patient.form.PatientEditByProjectForm;
import org.openelisglobal.patient.saving.Accessioner;
import org.openelisglobal.patient.saving.PatientEditUpdate;
import org.openelisglobal.patient.saving.PatientEntry;
import org.openelisglobal.patient.saving.PatientEntryAfterAnalyzer;
import org.openelisglobal.patient.saving.PatientEntryAfterSampleEntry;
import org.openelisglobal.patient.saving.PatientSecondEntry;
import org.openelisglobal.patient.saving.RequestType;
import org.openelisglobal.patient.validator.PatientEditByProjectFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest")
public class PatientEditByProjectRestController extends BasePatientEntryByProject {

    @Autowired
    private PatientEditByProjectFormValidator formValidator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        String[] allowedFields = getBasePatientEntryByProjectFields().toArray(new String[0]);
        binder.setAllowedFields(allowedFields);
    }

    @GetMapping(value = "/PatientEditByProject")
    public ResponseEntity<PatientEditByProjectForm> showPatientEditByProject() {
        PatientEditByProjectForm form = new PatientEditByProjectForm();

        // Set current date and entered date to today's date
        form.setCurrentDate(DateUtil.getCurrentDateAsText());
        PatientSearch patientSearch = new PatientSearch();
        patientSearch.setLoadFromServerWithPatient(false);
        form.setPatientSearch(patientSearch);
        return ResponseEntity.ok(form);
    }

    @PostMapping(value = "/PatientEditByProject")
    public ResponseEntity<PatientEditByProjectForm> showPatientEditByProjectSave(HttpServletRequest request,
            @RequestBody @Valid PatientEditByProjectForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Validate the form with the custom validator
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            // Save errors and return the form with validation errors
            saveErrors(result);
            return ResponseEntity.ok(form);
        }

        // Default to fail insert
        String forward = FWD_FAIL_INSERT;

        // Get the system user ID and add necessary patient form lists
        String sysUserId = getSysUserId(request);
        Accessioner accessioner;
        addAllPatientFormLists(form);

        // Try saving with different Accessioners if canAccession() is true
        accessioner = new PatientEditUpdate(form, sysUserId, request);
        if (accessioner.canAccession()) {
            forward = handleSave(request, accessioner);
        }

        accessioner = new PatientSecondEntry(form, sysUserId, request);
        if (accessioner.canAccession()) {
            forward = handleSave(request, accessioner);
        }

        accessioner = new PatientEntry(form, sysUserId, request);
        if (accessioner.canAccession()) {
            forward = handleSave(request, accessioner);
        }

        accessioner = new PatientEntryAfterSampleEntry(form, sysUserId, request);
        if (accessioner.canAccession()) {
            forward = handleSave(request, accessioner);
        }

        accessioner = new PatientEntryAfterAnalyzer(form, sysUserId, request);
        if (accessioner.canAccession()) {
            forward = handleSave(request, accessioner);
        }

        // Handle response based on the result of saving
        if (forward == null || FWD_FAIL_INSERT.equals(forward)) {
            logAndAddMessage(request, "performAction", "errors.UpdateException");
            forward = FWD_FAIL_INSERT;
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            // If success, return form with 200 OK status
            return ResponseEntity.ok(form);
        }

        // If no success, return form with 200 OK but indicating failure
        return ResponseEntity.ok(form);
    }

    @Override
    protected String findLocalForward(String forward) {
        return forward;

    }

    public String getProject() {
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        return "patient.project.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        RequestType requestType = getRequestType(request);
        String pageKey = null;
        switch (requestType) {
        case READWRITE: {
            pageKey = "banner.menu.editPatient.ReadWrite";
            break;
        }
        case READONLY: {
            pageKey = "banner.menu.editPatient.ReadOnly";
            break;
        }

        default: {
            pageKey = "banner.menu.editPatient.ReadOnly";
        }
        }

        return pageKey;
    }

}