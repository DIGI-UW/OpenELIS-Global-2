package org.openelisglobal.validation.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.openelisglobal.validation.constraintvalidator.HtmlValidator;

@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Constraint(validatedBy = HtmlValidator.class)
public @interface SafeHtml {
    String message() default "Unsafe HTML tags included";

    Class<?>[] groups() default {};

    SafeListLevel level() default SafeHtml.SafeListLevel.RELAXED;

    public abstract Class<? extends Payload>[] payload() default {};

    public enum SafeListLevel {
        NONE, BASIC, BASIC_WITH_IMAGES, SIMPLE_TEXT, RELAXED
    }
}
