package org.openelisglobal.patient.validator;

import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PatientEntryByProjectFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PatientEntryByProjectForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PatientEntryByProjectForm form = (PatientEntryByProjectForm) target;

        if ("InitialARV_Id".equals(form.getObservations().getProjectFormName())) {
            validateInitialARVForm(form, errors);
        } else if ("FollowUpARV_Id".equals(form.getObservations().getProjectFormName())) {
            validateFollowUpARVForm(form, errors);
        } else if ("RTN_Id".equals(form.getObservations().getProjectFormName())) {
            validateRTNForm(form, errors);
        } else if ("VL_Id".equals(form.getObservations().getProjectFormName())) {
            validateVLForm(form, errors);
        } else if ("EID_Id".equals(form.getObservations().getProjectFormName())) {
            validateEIDForm(form, errors);
        } else if ("Recency_Id".equals(form.getObservations().getProjectFormName())) {
            validateRecencyForm(form, errors);
        } else if ("HPV_Id".equals(form.getObservations().getProjectFormName())) {
            validateHPVForm(form, errors);
        } else {
            errors.reject("error.formname.unrecognized", "The provided form name is unrecognized");
        }
    }

    private void validateEIDForm(PatientEntryByProjectForm form, Errors errors) {
        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        ValidationHelper.validateFieldRequired(form.getProjectData().getEIDSiteName(), "ProjectData.EIDSiteName",
                errors);

        ValidationHelper.validateFieldRequired(form.getProjectData().getEIDsiteCode(), "ProjectData.EIDSiteCode",
                errors);

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSubjectNumber())
                && org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSiteSubjectNumber())) {
            ValidationHelper.validateFieldRequired(form.getSubjectNumber(), "subjectNumber", errors);

            ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);
        }

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);
    }

    private void validateVLForm(PatientEntryByProjectForm form, Errors errors) {
        // ValidationHelper.validateFieldRequired(form.getCenterName(), "centerName",
        // errors);
        //
        // if (form.getCenterCode() == null) {
        // errors.rejectValue("centerCode", "field.error.required");
        // }

        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSubjectNumber())
                && org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSiteSubjectNumber())) {
            ValidationHelper.validateFieldRequired(form.getSubjectNumber(), "subjectNumber", errors);

            ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);
        }

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);
    }

    private void validateRecencyForm(PatientEntryByProjectForm form, Errors errors) {
        // ValidationHelper.validateFieldRequired(form.getCenterName(), "centerName",
        // errors);
        //
        // if (form.getCenterCode() == null) {
        // errors.rejectValue("centerCode", "field.error.required");
        // }

        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);
    }

    private void validateHPVForm(PatientEntryByProjectForm form, Errors errors) {

        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);
    }

    private void validateRTNForm(PatientEntryByProjectForm form, Errors errors) {
        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        ValidationHelper.validateFieldRequired(form.getObservations().getNameOfDoctor(), "observations.nameOfDoctor",
                errors);

        if (form.getCenterCode() == null) {
            errors.rejectValue("centerCode", "field.error.required");
        }

        ValidationHelper.validateFieldRequired(form.getObservations().getService(), "observations.service", errors);

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);
    }

    private void validateFollowUpARVForm(PatientEntryByProjectForm form, Errors errors) {
        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSubjectNumber())
                && org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSiteSubjectNumber())) {
            ValidationHelper.validateFieldRequired(form.getSubjectNumber(), "subjectNumber", errors);

            ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);
        }

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        if (form.getCenterCode() == null) {
            errors.rejectValue("centerCode", "field.error.required");
        }

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);
    }

    private void validateInitialARVForm(PatientEntryByProjectForm form, Errors errors) {
        ValidationHelper.validateFieldRequired(form.getReceivedDateForDisplay(), "receivedDateForDisplay", errors);

        ValidationHelper.validateFieldRequired(form.getInterviewDate(), "interviewDate", errors);

        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSubjectNumber())
                && org.apache.commons.validator.GenericValidator.isBlankOrNull(form.getSiteSubjectNumber())) {
            ValidationHelper.validateFieldRequired(form.getSubjectNumber(), "subjectNumber", errors);

            ValidationHelper.validateFieldRequired(form.getSiteSubjectNumber(), "siteSubjectNumber", errors);
        }

        ValidationHelper.validateFieldRequired(form.getLabNo(), "labNo", errors);

        ValidationHelper.validateFieldRequired(form.getCenterName(), "centerName", errors);

        if (form.getCenterCode() == null) {
            errors.rejectValue("centerCode", "field.error.required");
        }

        ValidationHelper.validateFieldRequired(form.getGender(), "gender", errors);

        ValidationHelper.validateFieldRequired(form.getBirthDateForDisplay(), "birthDateForDisplay", errors);
    }
}
