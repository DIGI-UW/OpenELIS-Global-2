package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
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

        // Dynamic/dual-format validation:
        // Accept both MM/dd/yyyy and dd/MM/yyyy regardless of DEFAULT_DATE_LOCALE.
        // When ambiguous (e.g. 03/04/2026), choose deterministically using
        // DEFAULT_DATE_LOCALE.
        LocalDate parsed = DateUtil.parseFlexibleLocalDate(datePortion,
                ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE));
        if (parsed == null) {
            return false;
        }

        Date asDate = Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
        String result = CustomDateValidator.getInstance().validateDate(asDate, validateDateConstraint.relative());
        return IActionConstants.VALID.equals(result);
    }
}
