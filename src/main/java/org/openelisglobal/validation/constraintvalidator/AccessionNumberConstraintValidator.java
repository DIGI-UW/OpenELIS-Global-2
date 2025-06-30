package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;

public class AccessionNumberConstraintValidator implements ConstraintValidator<ValidAccessionNumber, String> {

    ValidAccessionNumber validateAccessionNumberConstraint;

    @Override
    public void initialize(ValidAccessionNumber constraint) {
        validateAccessionNumberConstraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        if (value.contains(".") && validateAccessionNumberConstraint.searchValue()) {
            int dotIndex = value.indexOf('.');
            if (!value.substring(dotIndex + 1).matches("[0-9]*")) {
                return false;
            }
            value = value.substring(0, dotIndex);
        }
        if (!Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
            return !AccessionNumberUtil.containsBlackListCharacters(value);
        }
        if (AccessionFormat.UNFORMATTED.equals(validateAccessionNumberConstraint.format())) {
            return value.matches("^[a-zA-Z0-9-]*$");
        }
        try {
            return ValidationResults.SUCCESS
                    .equals(AccessionNumberUtil.getAccessionNumberValidator(validateAccessionNumberConstraint.format())
                            .validFormat(value, validateAccessionNumberConstraint.dateValidate()));
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return false;
        }
    }
}
