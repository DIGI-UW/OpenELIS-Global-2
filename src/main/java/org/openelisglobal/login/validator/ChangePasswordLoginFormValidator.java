package org.openelisglobal.login.validator;

import org.openelisglobal.login.form.ChangePasswordLoginForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ChangePasswordLoginFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ChangePasswordLoginForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ChangePasswordLoginForm form = (ChangePasswordLoginForm) target;

        if (form.getPassword().equals(form.getNewPassword())) {
            errors.reject("login.error.newpassword.required", "New password cannot match old password");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.reject("login.error.password.notmatch");
        }
    }
}
