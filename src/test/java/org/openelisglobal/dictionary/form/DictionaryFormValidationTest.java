package org.openelisglobal.dictionary.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;

public class DictionaryFormValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DictionaryService dictionaryService;

    private Validator validator;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/dictionary.xml");

        ValidatorFactory factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();

        validator = factory.getValidator();

    }

    @Test
    public void DictionaryForm_shouldPassValidation_whenAllFieldsAreValid() {
        DictionaryForm form = new DictionaryForm();
        form.setId("123"); // Optional
        form.setSelectedDictionaryCategoryId("1");
        form.setIsActive("Y");
        form.setDictEntry("Valid Entry");
        form.setLocalAbbreviation("VALID");

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertTrue("Expected no constraint violations", violations.isEmpty());
    }

    @Test
    public void DictionaryForm_shouldFailValidation_whenRequiredFieldsAreBlank() {
        DictionaryForm form = new DictionaryForm();

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertFalse("Expected constraint violations", violations.isEmpty());

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("selectedDictionaryCategoryId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isActive")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dictEntry")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("localAbbreviation")));
    }

    @Test
    public void DictionaryForm_shouldFailValidation_whenIsActiveIsInvalid() {
        DictionaryForm form = createValidForm();
        form.setIsActive("maybe");

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isActive")));
    }

    @Test
    public void DictionaryForm_shouldFailValidation_whenLocalAbbreviationExceedsMaxLength() {
        DictionaryForm form = createValidForm();
        form.setLocalAbbreviation("TOO_LONG_TEXT");

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("localAbbreviation")));
    }

    @Test
    public void DictionaryForm_shouldFailValidation_whenIdPatternIsInvalid() {
        DictionaryForm form = createValidForm();
        form.setId("invalid-id!!"); // Violates ID_REGEX

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    public void DictionaryForm_shouldPassValidation_whenIdIsBlank() {
        DictionaryForm form = createValidForm();
        form.setId("");

        Set<ConstraintViolation<DictionaryForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    private DictionaryForm createValidForm() {
        DictionaryForm form = new DictionaryForm();
        form.setId("123");
        form.setSelectedDictionaryCategoryId("1");
        form.setIsActive("Y");
        form.setDictEntry("Valid Entry");
        form.setLocalAbbreviation("VAL");
        return form;
    }
}
