package org.openelisglobal.login.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChangePasswordLoginFormTest {

    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        validator = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
    }

    private ChangePasswordLoginForm form(String current, String newPw, String confirm) {
        ChangePasswordLoginForm form = new ChangePasswordLoginForm();
        form.setLoginName("admin");
        form.setPassword(current);
        form.setNewPassword(newPw);
        form.setConfirmPassword(confirm);
        return form;
    }

    private boolean hasViolationOn(ChangePasswordLoginForm form, String property) {
        Set<ConstraintViolation<ChangePasswordLoginForm>> violations =
                validator.validateProperty(form, property);

        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(property));
    }

    @Test
    public void validChange_hasNoConfirmationViolation() {
        ChangePasswordLoginForm f =
                form("OldPass#123", "NewPass#456", "NewPass#456");

        assertFalse(hasViolationOn(f, "newPasswordConfirmed"));
    }

    @Test
    public void mismatchedConfirmation_isRejected() {
        ChangePasswordLoginForm f =
                form("OldPass#123", "NewPass#456", "Different#789");

        assertTrue(hasViolationOn(f, "newPasswordConfirmed"));
    }

    @Test
    public void emptyFields_areLeftToFieldLevelValidation() {
        ChangePasswordLoginForm f = form(null, null, null);

        assertFalse(hasViolationOn(f, "newPasswordConfirmed"));
    }
}

