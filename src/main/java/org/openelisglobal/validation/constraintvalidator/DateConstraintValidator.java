package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.CustomDateValidator;
import org.openelisglobal.validation.annotations.ValidDate;

public class DateConstraintValidator implements ConstraintValidator<ValidDate, String> {

    ValidDate validateDateConstraint;

    @Override
    public void initialize(ValidDate constraint) {
        validateDateConstraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        String datePortion = value;

        if (validateDateConstraint.acceptTime()) {
            Character separator = null;
            if (value.contains("+")) {
                separator = '+';
            } else if (value.contains(" ")) {
                separator = ' ';
            }
            if (separator != null) {
                datePortion = value.substring(0, value.indexOf(separator));
                if (!CustomDateValidator.getInstance()
                        .validate24HourTime(value.substring(value.indexOf(separator) + 1))) {
                    return false;
                }
            }
        }
        datePortion = datePortion.replaceAll(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
        String result = CustomDateValidator.getInstance().validateDate(
                CustomDateValidator.getInstance().getDate(datePortion), validateDateConstraint.relative());
        if (!IActionConstants.VALID.equals(result)) {
            return false;
        }
        return true;
    }
}
