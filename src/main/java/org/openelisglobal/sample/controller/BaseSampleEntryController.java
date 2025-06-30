package org.openelisglobal.sample.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.patient.saving.IAccessioner;
import org.openelisglobal.patient.saving.RequestType;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

public abstract class BaseSampleEntryController extends BaseController {

    protected RequestType requestType = RequestType.UNKNOWN;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private LocalizationService localizationService;

    @Override
    protected String getPageTitleKey() {
        return MessageUtil.getContextualKey("sample.entry.title");
    }

    @Override
    protected String getPageSubtitleKey() {
        return MessageUtil.getContextualKey("sample.entry.title");
    }

    protected void addBillingLabel() {
        request.setAttribute("billingReferenceNumberLabel", localizationService.getLocalizedValueById(
                ConfigurationProperties.getInstance().getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL)));
    }

    // protected void addGenderList(BaseForm form)
    // throws LIMSRuntimeException, IllegalAccessException,
    // InvocationTargetException,
    // NoSuchMethodException {
    //
    // List genders = genderService.getAll();
    // form.setGenders(genders);
    // }

    /**
     * various maps full of a various lists used by the entry form (typically for
     * drop downs) and other forms who want to do patient entry. @ all from
     * setProperty problems caused by developer mistakes.
     */
    protected void addProjectList(SamplePatientEntryForm form)
            throws LIMSRuntimeException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        List<Project> projects = projectService.getAll();
        form.setProjects(projects);
    }

    // public void addAllPatientFormLists(BaseForm form) {
    // Map<String, Object> resultMap = new HashMap<>();
    // resultMap.put("GENDERS", PatientUtil.findGenders());
    //
    // form.setFormLists(resultMap);
    // form.setDictionaryLists(ObservationHistoryList.MAP);
    // form.setOrganizationTypeLists(OrganizationTypeList.MAP);
    //
    // return;
    // }

    /**
     * @param requestTypeStr
     */
    protected void setRequestType(String requestTypeStr) {
        if (!GenericValidator.isBlankOrNull(requestTypeStr)) {
            requestType = RequestType.valueOf(requestTypeStr.toUpperCase());
        }
    }

    /**
     * If the URL parameter says we want to do a particular type of request, figure
     * out which type and pass that on to any other controller page in the flow via
     * an attribute of the session TODO PAHill is putting it in the session just
     * clutter? Should this be in the request instead?
     */
    protected void updateRequestType(HttpServletRequest httpRequest) {
        setRequestType(httpRequest.getParameter("type"));
        if (requestType != null) {
            httpRequest.getSession().setAttribute("type", requestType.toString().toLowerCase());
        }
    }

    /**
     * Put the projectFormName in the right place, so that ends up driving the next
     * patientEntry form to select the right form. The projectFormName is retained
     * even through form edit/entry(add) reselection, so that all forms know the
     * current projectForm, so that the use is presented with a good guess on what
     * they might want to keep doing.
     *
     * @param projectFormName @ all from property utils if we've coded things wrong
     *                        in the form def or in this class.
     */
    // protected void setProjectFormName(BaseForm form, String projectFormName)
    // throws IllegalAccessException, InvocationTargetException,
    // NoSuchMethodException {
    // ((ObservationData) (PropertyUtils.getProperty(form,
    // "observations"))).setProjectFormName(projectFormName);
    // }

    /**
     * This method captures how we deal with the curious accession objects I
     * (PaHill) created. If someone comes up with a better API (Maybe inject a
     * messages list to be filled in?) then we can change this bit of plumbing.
     *
     * @param request           original request
     * @param sampleSecondEntry the object to use to attempt to save.
     * @return a forward string or null; null => this attempt to save failed @ if
     *         things go really bad. Normally, the errors are caught internally an
     *         appropriate message is added and a forward fail is returned.
     */
    protected String handleSave(HttpServletRequest request, IAccessioner accessioner, IAccessionerForm form) {
        String forward;
        try {
            forward = accessioner.save();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LIMSException e) {
            Errors errors = accessioner.getMessages();
            if (errors.hasErrors()) {
                saveErrors(errors);
            }
            return FWD_FAIL_INSERT;
        }
        return forward;
    }

    /**
     * Just a utility routine for doing the obvious logging and and appending a
     * global message.
     */
    protected void logAndAddMessage(HttpServletRequest request, String methodName, String messageKey) {
        LogEvent.logError(this.getClass().getSimpleName(), methodName, "Unable to enter sample into system");
        Errors errors = new BaseErrors();
        errors.reject(messageKey, messageKey);
        addErrors(request, errors);
        /*
         * ActionError error = new ActionError(messageKey, null, null); ActionMessages
         * errors = new ActionMessages(); errors.add(ActionMessages.GLOBAL_MESSAGE,
         * error); addErrors(request, errors);
         */
    }

    private void addErrors(HttpServletRequest request, Errors errors) {
        saveErrors(errors);
    }
}
