package org.openelisglobal.patient.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.openelisglobal.patient.saving.Accessioner;
import org.openelisglobal.patient.saving.IPatientEditUpdate;
import org.openelisglobal.patient.saving.IPatientEntry;
import org.openelisglobal.patient.saving.IPatientEntryAfterAnalyzer;
import org.openelisglobal.patient.saving.IPatientEntryAfterSampleEntry;
import org.openelisglobal.patient.saving.IPatientSecondEntry;
import org.openelisglobal.patient.saving.RequestType;
import org.openelisglobal.patient.validator.PatientEntryByProjectFormValidator;
import org.openelisglobal.spring.util.SpringContext;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PatientEntryByProjectController extends BasePatientEntryByProject {

    @Autowired
    private PatientEntryByProjectFormValidator formValidator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        String[] allowedFields = getBasePatientEntryByProjectFields().toArray(new String[0]);
        binder.setAllowedFields(allowedFields);
    }

    @RequestMapping(value = "/PatientEntryByProject", method = RequestMethod.GET)
    public ModelAndView showPatientEntryByProject(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        PatientEntryByProjectForm form = new PatientEntryByProjectForm();

        String todayAsText = DateUtil.formatDateAsText(new Date());

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        // retrieve the current project, before clearing, so that we can set it on
        // later.
        String projectFormName = Accessioner.findProjectFormName(form);
        updateRequestType(request);

        addAllPatientFormLists(form);

        form.setCurrentDate(todayAsText);
        form.setReceivedDateForDisplay(todayAsText);
        form.setInterviewDate(todayAsText);
        // put the projectFormName back in.
        setProjectFormName(form, projectFormName);

        addFlashMsgsToRequest(request);

        return findForward(FWD_SUCCESS, form);
    }

    // TODO consider making separate method for each type of form entry so can use
    // @Validated(VL.class, EID.class, etc..) to access seperate logic
    @RequestMapping(value = "/PatientEntryByProject", method = RequestMethod.POST)
    public ModelAndView showPatientEntryByProjectUpdate(HttpServletRequest request,
            @ModelAttribute("form") @Valid PatientEntryByProjectForm form, BindingResult result,
            RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        String sysUserId = getSysUserId(request);
        addAllPatientFormLists(form);
        IPatientEditUpdate patientEditUpdateAccessioner = SpringContext.getBean(IPatientEditUpdate.class);
        patientEditUpdateAccessioner.setRequest(request);
        patientEditUpdateAccessioner.setFieldsFromForm(form);
        patientEditUpdateAccessioner.setSysUserId(sysUserId);
        String forward = FWD_FAIL_INSERT;
        if (patientEditUpdateAccessioner.canAccession()) {
            forward = handleSave(request, patientEditUpdateAccessioner);
        }

        IPatientSecondEntry patientSecondEntryAccessioner = SpringContext.getBean(IPatientSecondEntry.class);
        patientSecondEntryAccessioner.setRequest(request);
        patientSecondEntryAccessioner.setFieldsFromForm(form);
        patientSecondEntryAccessioner.setSysUserId(sysUserId);
        if (patientSecondEntryAccessioner.canAccession()) {
            forward = handleSave(request, patientSecondEntryAccessioner);
        }
        IPatientEntry patientEntryAccessioner = SpringContext.getBean(IPatientEntry.class);
        patientEntryAccessioner.setRequest(request);
        patientEntryAccessioner.setFieldsFromForm(form);
        patientEntryAccessioner.setSysUserId(sysUserId);
        if (patientEntryAccessioner.canAccession()) {
            forward = handleSave(request, patientEntryAccessioner);
        }
        IPatientEntryAfterSampleEntry patientEntryAfterSampleEntryAccessioner = SpringContext
                .getBean(IPatientEntryAfterSampleEntry.class);
        patientEntryAfterSampleEntryAccessioner.setRequest(request);
        patientEntryAfterSampleEntryAccessioner.setFieldsFromForm(form);
        patientEntryAfterSampleEntryAccessioner.setSysUserId(sysUserId);
        if (patientEntryAfterSampleEntryAccessioner.canAccession()) {
            forward = handleSave(request, patientEntryAfterSampleEntryAccessioner);
        }
        IPatientEntryAfterAnalyzer patientEntryAfterAnalyzerAccessioner = SpringContext
                .getBean(IPatientEntryAfterAnalyzer.class);
        patientEntryAfterAnalyzerAccessioner.setRequest(request);
        patientEntryAfterAnalyzerAccessioner.setFieldsFromForm(form);
        patientEntryAfterAnalyzerAccessioner.setSysUserId(sysUserId);
        if (patientEntryAfterAnalyzerAccessioner.canAccession()) {
            forward = handleSave(request, patientEntryAfterAnalyzerAccessioner);
        }
        if (FWD_FAIL_INSERT.equals(forward) || forward == null) {
            logAndAddMessage(request, "performAction", "errors.UpdateException");
            forward = FWD_FAIL_INSERT;
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        }

        return findForward(forward, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "patientEntryByProjectDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            String redirectURL = "/PatientEntryByProject?type=" + Encode.forUriComponent(request.getParameter("type"));
            return "redirect:" + redirectURL;
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "patientEntryByProjectDefinition";
        } else {
            return "PageNotFound";
        }
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
        case INITIAL: {
            pageKey = "banner.menu.createPatient.Initial";
            break;
        }
        case VERIFY: {
            pageKey = "banner.menu.createPatient.Verify";
            break;
        }

        default: {
            pageKey = "banner.menu.createPatient.Initial";
        }
        }

        return pageKey;
    }
}
