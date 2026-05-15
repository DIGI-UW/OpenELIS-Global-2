package org.openelisglobal.sample.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/**
 * OGC-743 — service-layer validation errors must surface as field-tagged
 * {@link FieldError}s in the
 * {@link org.springframework.validation.BindingResult} so they reach the
 * frontend's {@code fieldErrors[]} payload (built by
 * {@code SamplePatientEntryRestController.buildErrorBody}).
 *
 * <p>
 * Before this commit, {@link SamplePatientUpdateData#validateSample} called
 * {@code errors.reject(message)} for both the invalid-accession and
 * empty-sample cases. {@code reject} adds a global ObjectError, which the
 * controller's response shaper does NOT include in {@code fieldErrors[]} — so
 * consumers saw an empty array and a generic "Validation failed" top-level
 * message, with no way to surface the rejection on the right input field.
 *
 * <p>
 * After this commit, {@code validateSample} uses {@code rejectValue} with a
 * path that maps to the form bean, so the same errors flow through the existing
 * {@code buildErrorBody → fieldErrors[]} path with an actionable {@code field}
 * key.
 *
 * <p>
 * The accession-invalid path is not exercised here because
 * {@code AccessionNumberUtil} pulls validators from the Spring container; we
 * sidestep it by setting {@code sample} with a non-null id (the production
 * sample-edit path).
 */
public class SamplePatientUpdateDataValidateTest extends BaseWebContextSensitiveTest {

    @Test
    public void emptySampleItems_surfacesFieldErrorOnSampleOrderItems_notGlobalError() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1"); // bypasses the accession-validation branch (Spring-coupled)
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.emptyList());
        // validateSample reads patientErrors.hasErrors() unconditionally — give it
        // an empty BindingResult to bypass that path without merging extra errors.
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true);

        assertTrue("validateSample must surface at least one error when sampleItemsTests is empty", result.hasErrors());
        FieldError noSampleErr = result.getFieldError("sampleOrderItems");
        assertNotNull("empty sampleItemsTests must surface as a FieldError on 'sampleOrderItems' "
                + "(not a global ObjectError) so it lands in fieldErrors[]", noSampleErr);
        assertEquals("errors.no.sample", noSampleErr.getCode());
    }

    // Note: the "samples-without-tests" path (line 311 in
    // SamplePatientUpdateData) follows the same pattern as the
    // empty-sampleItemsTests case above. Building a non-static inner
    // SampleAddService.SampleTestCollection requires a Spring-managed
    // SampleAddService enclosing instance — not worth wiring just to
    // re-test the same reject→rejectValue conversion. Covered by code
    // inspection.
}
