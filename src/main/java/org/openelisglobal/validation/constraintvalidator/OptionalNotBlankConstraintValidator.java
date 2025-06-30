package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.validation.annotations.OptionalNotBlank;

public class OptionalNotBlankConstraintValidator implements ConstraintValidator<OptionalNotBlank, String> {

    private FormFields.Field[] fields;
    private ConfigurationProperties.Property[] properties;

    @Override
    public void initialize(OptionalNotBlank constraint) {
        fields = constraint.formFields();
        properties = constraint.properties();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean valid = true;
        for (FormFields.Field field : fields) {
            if (FormFields.getInstance().useField(field)) {
                valid &= !GenericValidator.isBlankOrNull(value);
            }
        }
        for (ConfigurationProperties.Property property : properties) {
            if (Boolean.valueOf(ConfigurationProperties.getInstance().getPropertyValue(property))) {
                valid &= !GenericValidator.isBlankOrNull(value);
            }
        }
        return valid;
    }
}
