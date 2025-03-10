package org.openelisglobal.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.validation.constraintvalidator.OptionalNotBlankConstraintValidator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OptionalNotBlankConstraintValidator.class)
@Documented
public @interface OptionalNotBlank {

    String message() default "Cannot be blank";

    Class<?>[] groups() default {};

    FormFields.Field[] formFields() default {};

    ConfigurationProperties.Property[] properties() default {};

    Class<? extends Payload>[] payload() default {};
}
