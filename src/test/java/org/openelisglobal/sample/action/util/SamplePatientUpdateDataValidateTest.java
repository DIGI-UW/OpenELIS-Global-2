package org.openelisglobal.sample.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
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

    @Test
    public void samplesWithoutTests_surfacesFieldErrorOnSampleOrderItems() {
        // OGC-743 follow-up coverage: lock the `errors.samples.with.no.tests`
        // branch (validateSample line ~316) — same reject→rejectValue conversion
        // as the empty-list branch above, but exercises allSamplesHaveTests().
        // SampleAddService isn't a Spring bean (no qualifying String for its
        // constructor), so construct it directly to host the inner class.
        SampleAddService outer = new SampleAddService(null, "1", null, null);
        SampleTestCollection collectionWithNoTests = outer.new SampleTestCollection(null, Collections.emptyList(), null,
                null, null, null, null);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1"); // bypass accession-validation branch (Spring-coupled)
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(collectionWithNoTests));
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true);

        assertTrue("validateSample must surface an error when a sample item has no tests", result.hasErrors());
        FieldError err = result.getFieldError("sampleOrderItems");
        assertNotNull("samples-with-no-tests must surface as a FieldError on 'sampleOrderItems' "
                + "(not a global ObjectError) so it lands in fieldErrors[]", err);
        assertEquals("errors.samples.with.no.tests", err.getCode());
    }
}
