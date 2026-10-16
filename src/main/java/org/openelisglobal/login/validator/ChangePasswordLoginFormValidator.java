package org.openelisglobal.login.validator;

import org.openelisglobal.login.form.ChangePasswordLoginForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
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

        // 1. Required field validation (consistent with project)
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "login.password.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "newPassword", "login.newpassword.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "confirmPassword", "login.confirmpassword.required");

        // Stop if basic validation fails
        if (errors.hasErrors())
            return;

        String password = form.getPassword();
        String newPassword = form.getNewPassword();
        String confirmPassword = form.getConfirmPassword();

        // . New password should not match old password
        if (password.equals(newPassword)) {
            errors.reject("login.error.newpassword.required", "New password cannot match old password");
        }

        // 3. Confirm password match
        if (!newPassword.equals(confirmPassword)) {
            errors.reject("login.error.password.notmatch");
        }

        // 4. Prevent leading/trailing spaces (without trimming)
        if (!password.equals(password.trim()) || !newPassword.equals(newPassword.trim())
                || !confirmPassword.equals(confirmPassword.trim())) {

            errors.reject("login.error.password.whitespace", "Passwords should not contain leading or trailing spaces");
        }
    }
}