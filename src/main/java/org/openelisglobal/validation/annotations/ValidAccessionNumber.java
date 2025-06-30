package org.openelisglobal.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.validation.constraintvalidator.AccessionNumberConstraintValidator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccessionNumberConstraintValidator.class)
@Documented
public @interface ValidAccessionNumber {

    String message() default "Invalid accession number format";

    Class<?>[] groups() default {};

    AccessionFormat format() default AccessionFormat.GENERAL;

    boolean searchValue() default false;

    boolean dateValidate() default false;

    Class<? extends Payload>[] payload() default {};
}
