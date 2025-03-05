package org.openelisglobal.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.openelisglobal.validation.constraintvalidator.PasswordConstraintValidator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Documented
public @interface Password {

    public enum PasswordState {
        PRE_HASH, POST_HASH
    }

    String message() default "must be complex enough";

    Class<?>[] groups() default {};

    PasswordState state() default PasswordState.PRE_HASH;

    Class<? extends Payload>[] payload() default {};
}
