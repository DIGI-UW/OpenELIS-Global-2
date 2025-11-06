package org.openelisglobal.login.validator;

import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class LoginValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return LoginUser.class.equals(clazz);
    }

    // basic validation at any point in execution
    @Override
    public void validate(Object target, Errors errors) {

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "login.password.error");
    }

    // validate before persisting for the first time
    // TODO needs expansion
    public void preInsertValidate(LoginUser login, Errors errors) {
        validate(login, errors);
    }

    // validate before updating
    // TODO possibly needs expansion
    public void preUpdateValidate(LoginUser login, Errors errors) {
        validate(login, errors);
        // validate password expired day
        // bugzilla 2286
        if (login.getPasswordExpiredDayNo() <= 0) {
            errors.reject("login.error.password.expired");
        }
        // validate user id exists in system_user table
        if (login.getSystemUserId() == 0) {
            errors.reject("login.error.system.user.id", login.getLoginName());
        }
    }

    // complex validation for password update when user is not authenticated (ie
    // /UpdateLoginChangePassword)
    public void unauthenticatedPasswordUpdateValidate(LoginUser login, Errors errors) {
        preUpdateValidate(login, errors);

        // validate account disabled
        if (login.getAccountDisabled().equals(IActionConstants.YES)) {
            errors.reject("login.error.account.disable");
        }
        // validate account locked
        if (login.getAccountLocked().equals(IActionConstants.YES)) {
            errors.reject("login.error.account.disable");
        }
    }
}
