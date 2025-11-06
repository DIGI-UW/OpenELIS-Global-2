package org.openelisglobal.patient.validator;

import org.openelisglobal.patient.form.PatientEditByProjectForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PatientEditByProjectFormValidator implements Validator {

    @Autowired
    PatientEntryByProjectFormValidator subFormValidator;

    @Override
    public boolean supports(Class<?> clazz) {
        return PatientEditByProjectForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        subFormValidator.validate(target, errors);
    }
}
