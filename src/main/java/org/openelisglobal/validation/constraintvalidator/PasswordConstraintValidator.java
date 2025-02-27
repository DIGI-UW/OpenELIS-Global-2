package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.provider.validation.PasswordValidationFactory;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.validation.annotations.Password;
import org.openelisglobal.validation.annotations.Password.PasswordState;

public class PasswordConstraintValidator implements ConstraintValidator<Password, String> {

    Password passwordConstraint;

    @Override
    public void initialize(Password constraint) {
        passwordConstraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        if (passwordConstraint.state().equals(PasswordState.PRE_HASH)) {
            return PasswordValidationFactory.getPasswordValidator().passwordValid(value);
        } else {
            return true;
        }
    }
}
