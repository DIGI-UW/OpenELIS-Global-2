package org.openelisglobal.sample.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

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

    /**
     * Env/vector orders need at least one of Requesting Organization or Requestor
     * contact. Neither is present here, so validateSample must reject.
     */
    @Test
    public void envVectorWorkflow_missingOrgAndRequestor_surfacesFieldError() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(nonEmptySampleTestCollection()));
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        SampleOrderItem sampleOrder = new SampleOrderItem();

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true, sampleOrder, "environmental");

        assertTrue("must reject when neither Requesting Organization nor Requestor is present", result.hasErrors());
        FieldError err = result.getFieldError("sampleOrderItems");
        assertNotNull(err);
        assertEquals("errors.requester.org.or.requestor.required", err.getCode());
    }

    @Test
    public void envVectorWorkflow_requestorOnly_passesOrgOrRequestorCheck() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(nonEmptySampleTestCollection()));
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setRequestorFirstName("Jane");
        sampleOrder.setRequestorLastName("Doe");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true, sampleOrder, "environmental");

        assertFalse("Requestor alone must satisfy the org-or-requestor requirement", result.hasErrors());
    }

    @Test
    public void envVectorWorkflow_organizationOnly_passesOrgOrRequestorCheck() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(nonEmptySampleTestCollection()));
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setReferringSiteId("42");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true, sampleOrder, "environmental");

        assertFalse("Requesting Organization alone must satisfy the org-or-requestor requirement", result.hasErrors());
    }

    @Test
    public void clinicalWorkflow_missingOrgAndRequestor_doesNotTriggerCheck() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(nonEmptySampleTestCollection()));
        updateData.setPatientErrors(new BeanPropertyBindingResult(new Object(), "ignored"));

        SampleOrderItem sampleOrder = new SampleOrderItem();

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true, sampleOrder, "clinical");

        assertFalse("the org-or-requestor check is env/vector-only and must not fire for clinical orders",
                result.hasErrors());
    }

    /**
     * OGC-1407: the patient validator collects its errors on a holder of its own
     * (BaseErrors, object name "Non bound errors"). Merging that holder into the
     * order's binding result as a whole threw "Errors object needs to have same
     * object name", so a refused duplicate national id reached the user as a 500.
     * Each patient error is carried over on its own instead.
     */
    @Test
    public void aRefusedPatientReachesTheOrdersErrorsWithItsMessage() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = new Sample();
        sample.setId("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(Collections.singletonList(nonEmptySampleTestCollection()));
        BaseErrors patientErrors = new BaseErrors();
        patientErrors.reject("error.duplicate.nationalId", null,
                "National ID QA1407F1 already belongs to patient Qadup, Fone (ID 118)");
        patientErrors.addError(new FieldError("Non bound errors", "birthDateForDisplay", "31/31/1988", false,
                new String[] { "error.birthdate.format" }, null, "error.birthdate.format"));
        updateData.setPatientErrors(patientErrors);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new SamplePatientEntryForm(),
                "samplePatientEntryForm");

        updateData.validateSample(result, true, new SampleOrderItem(), "clinical");

        ObjectError duplicate = result.getGlobalError();
        assertNotNull("the duplicate national id must reach the order's errors", duplicate);
        assertEquals("error.duplicate.nationalId", duplicate.getCode());
        assertEquals("National ID QA1407F1 already belongs to patient Qadup, Fone (ID 118)",
                duplicate.getDefaultMessage());
        FieldError birthDate = result.getFieldError("patientProperties.birthDateForDisplay");
        assertNotNull("a patient field error must land under the form's patientProperties path", birthDate);
        assertEquals("error.birthdate.format", birthDate.getCode());
    }

    private SampleTestCollection nonEmptySampleTestCollection() {
        SampleAddService outer = new SampleAddService(null, "1", null, null);
        return outer.new SampleTestCollection(null,
                Collections.singletonList(new org.openelisglobal.test.valueholder.Test()), null, null, null, null,
                null);
    }
}
