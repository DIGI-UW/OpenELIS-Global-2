package org.openelisglobal.sample.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

/**
 * Validation tests for the informed-consent fields on SampleOrderItem.
 *
 * Covers FRS OGC-557 §10 + BR-005: - consentFormReference max length = 100 -
 * consentFormReference regex: alphanumeric, hyphens, and spaces only
 *
 * Mirrors the pattern used in StorageDeviceFormTest. Validation groups matter
 * here because the consent annotations are scoped to SamplePatientEntry* /
 * SampleEdit groups rather than the default group.
 */
public class SampleOrderItemValidationTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpValidator() {
        // Custom factory that falls back to a no-op validator when a
        // ConstraintValidator can't be instantiated (e.g., OpenELIS validators
        // that depend on SpringContext at construction time like NameValidator).
        // This scopes our test to validators with no Spring dependency —
        // @Pattern, @Size, and @SafeHtml's HtmlValidator — which is exactly
        // the surface we care about.
        ConstraintValidatorFactory factory = new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                try {
                    return key.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    T noop = (T) new ConstraintValidator() {
                        @Override
                        public boolean isValid(Object value, ConstraintValidatorContext ctx) {
                            return true;
                        }
                    };
                    return noop;
                }
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
            }
        };
        ValidatorFactory vf = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).constraintValidatorFactory(factory)
                .buildValidatorFactory();
        validator = vf.getValidator();
    }

    private SampleOrderItem buildBase() {
        SampleOrderItem item = new SampleOrderItem();
        item.setConsentGiven(true);
        return item;
    }

    @Test
    public void consentFormReference_null_passes() {
        SampleOrderItem item = buildBase();
        item.setConsentFormReference(null);
        Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item, "consentFormReference",
                SamplePatientEntryForm.SamplePatientEntry.class);
        assertNoConsentFormReferenceViolations(violations);
    }

    @Test
    public void consentFormReference_empty_passes() {
        SampleOrderItem item = buildBase();
        item.setConsentFormReference("");
        Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item, "consentFormReference",
                SamplePatientEntryForm.SamplePatientEntry.class);
        assertNoConsentFormReferenceViolations(violations);
    }

    @Test
    public void consentFormReference_acceptsValidAlphanumericAndHyphensAndSpaces() {
        String[] valid = { "CF-2026-00123", "CF 2026 123", "ABC123", "A", "a", "1", "Form Reference 1234 5678 9 0",
                "ABCDEFGHIJ-1234567890" };
        for (String ref : valid) {
            SampleOrderItem item = buildBase();
            item.setConsentFormReference(ref);
            Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item,
                    "consentFormReference", SamplePatientEntryForm.SamplePatientEntry.class);
            assertNoConsentFormReferenceViolations(violations, ref);
        }
    }

    @Test
    public void consentFormReference_rejectsInvalidCharacters() {
        // FRS §10 BR-005: only alphanumeric, hyphens, and spaces are allowed.
        String[] invalid = { "CF_2026_001", "CF-2026#1", "CF-2026/1", "CF.2026", "CF(2026)",
                "CF-2026; DROP TABLE sample;", "<script>alert(1)</script>" };
        for (String ref : invalid) {
            SampleOrderItem item = buildBase();
            item.setConsentFormReference(ref);
            Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item,
                    "consentFormReference", SamplePatientEntryForm.SamplePatientEntry.class);
            assertTrue("Expected a @Pattern violation for invalid reference: " + ref,
                    hasMessageContaining(violations, "formReferenceInvalidChars"));
        }
    }

    @Test
    public void consentFormReference_exactly100Chars_passes() {
        SampleOrderItem item = buildBase();
        // 100-char string of letters (within pattern)
        String ref = repeat('A', 100);
        item.setConsentFormReference(ref);
        Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item, "consentFormReference",
                SamplePatientEntryForm.SamplePatientEntry.class);
        assertNoConsentFormReferenceViolations(violations);
    }

    @Test
    public void consentFormReference_over100Chars_triggersSizeViolation() {
        SampleOrderItem item = buildBase();
        String ref = repeat('A', 101);
        item.setConsentFormReference(ref);
        Set<ConstraintViolation<SampleOrderItem>> violations = validator.validateProperty(item, "consentFormReference",
                SamplePatientEntryForm.SamplePatientEntry.class);
        assertTrue("Expected a @Size violation for 101-char reference",
                hasMessageContaining(violations, "formReferenceMaxLength"));
    }

    /**
     * Ensures the consent annotations are group-scoped (they must NOT fire on the
     * default group, to avoid interfering with non-SamplePatientEntry flows). Uses
     * validateProperty to avoid firing Spring-managed validators on other fields.
     */
    @Test
    public void consentFormReference_invalidChars_doNotFireOnDefaultGroup() {
        SampleOrderItem item = buildBase();
        item.setConsentFormReference("CF_invalid#chars");
        Set<ConstraintViolation<SampleOrderItem>> defaultViolations = validator.validateProperty(item,
                "consentFormReference");
        assertEquals("Consent annotations must be group-scoped — no violations expected on the default group", 0,
                defaultViolations.size());
    }

    // --- helpers -----------------------------------------------------------

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean hasMessageContaining(Set<ConstraintViolation<SampleOrderItem>> violations, String fragment) {
        for (ConstraintViolation<SampleOrderItem> v : violations) {
            if (v.getMessage().contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static void assertNoConsentFormReferenceViolations(Set<ConstraintViolation<SampleOrderItem>> violations) {
        assertNoConsentFormReferenceViolations(violations, null);
    }

    private static void assertNoConsentFormReferenceViolations(Set<ConstraintViolation<SampleOrderItem>> violations,
            String label) {
        int count = 0;
        for (ConstraintViolation<SampleOrderItem> v : violations) {
            if (v.getPropertyPath().toString().equals("consentFormReference")) {
                count++;
            }
        }
        assertEquals("Expected no consentFormReference violations for: " + (label == null ? "" : label), 0, count);
    }
}
