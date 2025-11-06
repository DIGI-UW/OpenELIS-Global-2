package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.util.validator.CustomDateValidator;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.validation.annotations.ValidTime;

public class TimeConstraintValidator implements ConstraintValidator<ValidTime, String> {

    ValidTime validateTimeeConstraint;

    @Override
    public void initialize(ValidTime constraint) {
        validateTimeeConstraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        return CustomDateValidator.getInstance().validate24HourTime(value);
    }
}
