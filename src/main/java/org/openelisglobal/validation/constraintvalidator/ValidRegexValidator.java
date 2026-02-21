package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.validation.annotations.ValidRegex;

public class ValidRegexValidator implements ConstraintValidator<ValidRegex, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        try {
            Pattern.compile(value);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
