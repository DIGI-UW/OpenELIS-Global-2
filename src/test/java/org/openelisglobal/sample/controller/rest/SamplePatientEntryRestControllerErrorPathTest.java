package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * OGC-744 — error-path lock for SamplePatientEntry submission.
 *
 * <p>
 * Two regressions to lock:
 *
 * <ol>
 * <li>An empty JSON body ({@code {}}) used to slip past validation because
 * {@code @Valid} on the nested form fields only cascaded into non-null objects;
 * the controller then dereferenced {@code sampleOrder} and the resulting NPE
 * was caught by the global handler in ControllerSetup, which returned 500 with
 * the literal "Check server logs" — an info-leak surface AND a worse-than-400
 * status for a client error.
 * <li>The default exception handler responded with the literal string "Check
 * server logs" — leaks implementation detail to API consumers.
 * </ol>
 *
 * <p>
 * Tested directly (no MockMvc): the AppTestConfig component scan excludes
 * {@code org.openelisglobal.sample.controller.*} and {@code config.*}, so an
 * end-to-end POST test isn't possible without a custom test context. Instead we
 * test the two units the fix touches:
 *
 * <ol>
 * <li>Bean-Validation cascade on {@link SamplePatientEntryForm} — empty form
 * must surface field errors for {@code sampleOrderItems} and
 * {@code patientProperties} so {@code formValidator.validate(...)} flips
 * {@code BindingResult.hasErrors() == true} and the controller returns 400 from
 * the existing {@code buildErrorBody} path.
 * <li>{@link ControllerSetup} default exception handler body — must not contain
 * the "Check server logs" literal AND must include a structured
 * {@code timestamp / status / error} payload instead.
 * </ol>
 */
public class SamplePatientEntryRestControllerErrorPathTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeClass
    public static void initValidator() {
        factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterClass
    public static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    public void emptyForm_violatesNotNullOnNestedRequiredFields() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();

        Set<jakarta.validation.ConstraintViolation<SamplePatientEntryForm>> violations = validator.validate(form,
                SamplePatientEntryForm.SamplePatientEntry.class);

        boolean sampleOrderViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("sampleOrderItems"));
        boolean patientPropertiesViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("patientProperties"));

        assertTrue("Empty form must surface a @NotNull violation on sampleOrderItems "
                + "so the controller returns 400 instead of NPE-ing at line 301", sampleOrderViolation);
        assertTrue(
                "Empty form must surface a @NotNull violation on patientProperties "
                        + "so /rest/SamplePatientEntry rejects empty bodies with a structured 400",
                patientPropertiesViolation);
    }

    /**
     * Subclass that exposes ControllerSetup's protected handlers for unit-level
     * testing.
     */
    private static final class ProbeControllerSetup extends ControllerSetup {
        ResponseEntity<Object> exposeRuntime(RuntimeException ex) {
            return handleRuntimeException(ex,
                    new ServletWebRequest(new MockHttpServletRequest("POST", "/rest/SamplePatientEntry")));
        }

        ResponseEntity<Object> exposeLims(LIMSRuntimeException ex) {
            return handleLIMSRuntimeException(ex,
                    new ServletWebRequest(new MockHttpServletRequest("POST", "/rest/SamplePatientEntry")));
        }
    }

    @Test
    public void defaultExceptionHandler_doesNotLeakCheckServerLogsLiteral() {
        ProbeControllerSetup handler = new ProbeControllerSetup();

        ResponseEntity<Object> runtime = handler.exposeRuntime(new RuntimeException("simulated"));
        ResponseEntity<Object> lims = handler.exposeLims(new LIMSRuntimeException("simulated"));

        for (ResponseEntity<Object> r : new ResponseEntity[] { runtime, lims }) {
            assertNotNull(r.getBody());
            String rendered = r.getBody().toString();
            assertFalse("Default 500 response must NOT include the 'Check server logs' literal — see OGC-744",
                    rendered.contains("Check server logs"));
            assertTrue("Default 500 body must surface a structured 'status' field", rendered.contains("status="));
            assertTrue("Default 500 body must surface a structured 'error' field", rendered.contains("error="));
            assertTrue("Default 500 body must surface a structured 'timestamp' field", rendered.contains("timestamp="));
        }

        assertNull("Sanity: handler must not accidentally include a Location header",
                runtime.getHeaders().getLocation());
    }
}
