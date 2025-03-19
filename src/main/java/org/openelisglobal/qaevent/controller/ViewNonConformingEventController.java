package org.openelisglobal.qaevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
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
public class ViewNonConformingEventController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "id", "currentUserId", "status", "reporterName",
            "site", "severityScore", "colorCode", "nceNumber", "reportDate", "dateOfEvent", "name", "labOrderNumber",
            "reportingUnit", "specimen", "prescriberName", "laboratoryComponent", "description", "nceCategory",
            "nceType", "suspectedCauses", "consequences", "proposedAction", "recurrence", "correctiveAction",
            "controlAction", "comments", };

    @Autowired
    private NonConformingEventWorker nonConformingEventWorker;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ViewNonConformingEvent", method = RequestMethod.GET)
    public ModelAndView showReportNonConformingEvent(HttpServletRequest request)
            throws LIMSInvalidConfigurationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        NonConformingEventForm form = new NonConformingEventForm();
        form.setCurrentUserId(getSysUserId(request));
        form.setPatientSearch(new PatientSearch());
        String nceNumber = request.getParameter("nceNumber");

        if (!GenericValidator.isBlankOrNull(nceNumber)) {
            nonConformingEventWorker.initFormForFollowUp(nceNumber, form);
        }

        addFlashMsgsToRequest(request);
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/ViewNonConformingEvent", method = RequestMethod.POST)
    public ModelAndView showReportNonConformingEventUpdate(HttpServletRequest request,
            @ModelAttribute("form") NonConformingEventForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);

        boolean updated = nonConformingEventWorker.updateFollowUp(form);
        if (updated) {
            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            return findForward(FWD_FAIL_INSERT, form);
        }
    }

    @Override
    protected String getPageSubtitleKey() {
        return "nonconforming.page.title";
    }

    @Override
    protected String getPageTitleKey() {
        return "nonconforming.page.title";
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "viewNonConformingEventDefiniton";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ViewNonConformingEvent";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "viewNonConformingEventDefiniton";
        } else {
            return "PageNotFound";
        }
    }
}
