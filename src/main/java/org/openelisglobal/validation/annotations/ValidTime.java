package org.openelisglobal.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.openelisglobal.validation.constraintvalidator.TimeConstraintValidator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeConstraintValidator.class)
@Documented
public @interface ValidTime {

    String message() default "Invalid time format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
