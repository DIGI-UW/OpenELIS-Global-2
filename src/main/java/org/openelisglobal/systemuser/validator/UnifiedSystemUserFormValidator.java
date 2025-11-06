package org.openelisglobal.systemuser.validator;

import org.openelisglobal.common.provider.validation.ILoginPasswordValidation;
import org.openelisglobal.common.provider.validation.PasswordValidationFactory;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.form.UnifiedSystemUserForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class UnifiedSystemUserFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return UnifiedSystemUserForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UnifiedSystemUserForm form = (UnifiedSystemUserForm) target;

        if (!form.getUserPassword().matches(UnifiedSystemUserController.DEFAULT_OBFUSCATED_CHARACTER + "+")) {
            ILoginPasswordValidation passValidator = PasswordValidationFactory.getPasswordValidator();
            if (!form.getUserPassword().equals(form.getConfirmPassword())) {
                errors.reject("login.error.password.notmatch");
            }
            if (!passValidator.passwordValid(form.getUserPassword())) {
                errors.reject("login.error.message");
            }
        }
    }
}
