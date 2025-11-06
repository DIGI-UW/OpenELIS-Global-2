package org.openelisglobal.validation.constraintvalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.SafeHtml.SafeListLevel;
import org.springframework.stereotype.Component;

@Component
public class HtmlValidator implements ConstraintValidator<SafeHtml, String> {

    private SafeListLevel level;

    @Override
    public void initialize(SafeHtml constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        level = constraintAnnotation.level();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            return true;
        }
        if (!s.matches(".*\\<[^>]+>.*")) { // allow <LL or >log7 as result values
            return true;
        }
        switch (level) {
        case NONE:
            return Jsoup.isValid(s, Safelist.none());
        case BASIC:
            return Jsoup.isValid(s, Safelist.basic());
        case BASIC_WITH_IMAGES:
            return Jsoup.isValid(s, Safelist.basicWithImages());
        case SIMPLE_TEXT:
            return Jsoup.isValid(s, Safelist.simpleText());
        case RELAXED:
            return Jsoup.isValid(s, Safelist.relaxed());
        default:
            return Jsoup.isValid(s, Safelist.none());
        }
    }
}
