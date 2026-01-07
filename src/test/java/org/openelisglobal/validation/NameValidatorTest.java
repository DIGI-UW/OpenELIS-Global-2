package org.openelisglobal.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidatorContext;

public class NameValidatorTest extends BaseWebContextSensitiveTest {

    @Autowired
    NameValidator nameValidator;

    @Mock
    ValidName validNameAnnotation;

    @Mock
    ConstraintValidatorContext context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void isValid_nullValue_shouldReturnTrue() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FIRST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid(null, context));
    }

    @Test
    public void isValid_emptyString_shouldReturnTrue() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FIRST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("", context));
    }

    @Test
    public void isValid_firstName_shouldReturnTrueForValidName() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FIRST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("John", context));
    }

    @Test
    public void isValid_firstName_shouldReturnTrueForNameWithApostrophe() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FIRST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("O'Brien", context));
    }

    @Test
    public void isValid_lastName_shouldReturnTrueForValidName() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.LAST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("Smith", context));
    }

    @Test
    public void isValid_lastName_shouldReturnTrueForUnknownValue() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.LAST_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("UNKNOWN_", context));
    }

    @Test
    public void isValid_username_shouldReturnTrueForValidUsername() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.USERNAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("john_doe", context));
    }

    @Test
    public void isValid_username_shouldReturnTrueForUsernameWithNumbers() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.USERNAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("user123", context));
    }

    @Test
    public void isValid_fullName_shouldReturnTrueForValidFullName() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FULL_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("John Smith", context));
    }

    @Test
    public void isValid_fullName_shouldReturnTrueForSingleName() {
        Mockito.when(validNameAnnotation.nameType()).thenReturn(NameType.FULL_NAME);
        nameValidator.initialize(validNameAnnotation);

        Assert.assertTrue(nameValidator.isValid("Madonna", context));
    }
}
