package org.openelisglobal.dictionary.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Test;

/**
 * The dictionary form takes a local abbreviation as long as the column stores
 * it: shipped checklist items keep their sentence there and could not be edited
 * under the earlier 10-character limit (OGC-1507 walk).
 */
public class DictionaryFormValidationTest {

    private final Validator validator = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

    @Test
    public void acceptsAnAbbreviationUpToTheColumnLength() {
        DictionaryForm form = form("Patient information is correct");
        assertTrue(violations(form).isEmpty());
        form.setLocalAbbreviation("x".repeat(60));
        assertTrue(violations(form).isEmpty());
    }

    @Test
    public void refusesALongerOrBlankAbbreviation() {
        assertEquals(1, violations(form("x".repeat(61))).size());
        assertEquals(1, violations(form(" ")).size());
    }

    private Set<ConstraintViolation<DictionaryForm>> violations(DictionaryForm form) {
        return validator.validateProperty(form, "localAbbreviation");
    }

    private static DictionaryForm form(String abbreviation) {
        DictionaryForm form = new DictionaryForm();
        form.setLocalAbbreviation(abbreviation);
        return form;
    }
}
