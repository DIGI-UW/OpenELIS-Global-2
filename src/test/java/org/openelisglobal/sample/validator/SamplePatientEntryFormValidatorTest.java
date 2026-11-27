package org.openelisglobal.sample.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/**
 * An order naming a referring site, site department or requester that does not
 * exist is refused by the form validator with a field error naming the id,
 * instead of failing later with a missing row or a NullPointerException that
 * surfaced as a bare HTTP 500.
 */
public class SamplePatientEntryFormValidatorTest {

    private SamplePatientEntryFormValidator validator;
    private OrganizationService organizationService;
    private PersonService personService;

    @Before
    public void setUp() {
        validator = new SamplePatientEntryFormValidator();
        organizationService = mock(OrganizationService.class);
        personService = mock(PersonService.class);
        ProviderService providerService = mock(ProviderService.class);
        SampleOrderPartiesValidator parties = new SampleOrderPartiesValidator();
        ReflectionTestUtils.setField(parties, "organizationService", organizationService);
        ReflectionTestUtils.setField(parties, "personService", personService);
        ReflectionTestUtils.setField(parties, "providerService", providerService);
        ReflectionTestUtils.setField(validator, "orderPartiesValidator", parties);
        when(organizationService.getOrganizationById("12")).thenReturn(new Organization());
        Person requester = new Person();
        requester.setId("31");
        when(personService.getPersonById("31")).thenReturn(requester);
        when(providerService.getProviderByPerson(requester)).thenReturn(new Provider());
        Person patient = new Person();
        patient.setId("40");
        when(personService.getPersonById("40")).thenReturn(patient);
    }

    private BeanPropertyBindingResult validate(String referringSiteId, String departmentId) {
        SampleOrderItem order = new SampleOrderItem();
        order.setReferringSiteId(referringSiteId);
        order.setReferringSiteDepartmentId(departmentId);
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(order);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        validator.validate(form, errors);
        return errors;
    }

    @Test
    public void anExistingReferringSitePasses() {
        assertFalse(validate("12", "").hasErrors());
    }

    @Test
    public void noReferringSiteIsNotLookedUp() {
        assertFalse(validate("", null).hasErrors());
        verify(organizationService, never()).getOrganizationById("");
    }

    @Test
    public void anUnknownReferringSiteIsRefusedNamingTheId() {
        BeanPropertyBindingResult errors = validate("9000100", "");

        FieldError error = errors.getFieldError("sampleOrderItems.referringSiteId");
        assertNotNull("an unknown referring site must be a field error", error);
        assertEquals("error.organization.notFound", error.getCode());
        assertTrue(error.getDefaultMessage(), error.getDefaultMessage().contains("9000100"));
    }

    @Test
    public void aNonNumericReferringSiteIsRefusedWithoutALookup() {
        BeanPropertyBindingResult errors = validate("abc", "");

        assertNotNull(errors.getFieldError("sampleOrderItems.referringSiteId"));
        verify(organizationService, never()).getOrganizationById("abc");
    }

    @Test
    public void anUnknownSiteDepartmentIsRefused() {
        BeanPropertyBindingResult errors = validate("12", "777");

        assertNotNull(errors.getFieldError("sampleOrderItems.referringSiteDepartmentId"));
        assertEquals(null, errors.getFieldError("sampleOrderItems.referringSiteId"));
    }

    private BeanPropertyBindingResult validateRequester(String providerPersonId) {
        SampleOrderItem order = new SampleOrderItem();
        order.setProviderPersonId(providerPersonId);
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(order);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        validator.validate(form, errors);
        return errors;
    }

    @Test
    public void anExistingRequesterPasses() {
        assertFalse(validateRequester("31").hasErrors());
    }

    @Test
    public void noRequesterIdIsNotLookedUp() {
        assertFalse(validateRequester("").hasErrors());
        verify(personService, never()).getPersonById("");
    }

    @Test
    public void anUnknownRequesterIsRefusedNamingTheId() {
        FieldError error = validateRequester("9000002").getFieldError("sampleOrderItems.providerPersonId");

        assertNotNull("an unknown requester must be a field error", error);
        assertEquals("error.provider.notFound", error.getCode());
        assertTrue(error.getDefaultMessage(), error.getDefaultMessage().contains("9000002"));
    }

    @Test
    public void aPersonWhoIsNotARequesterIsRefused() {
        assertNotNull(validateRequester("40").getFieldError("sampleOrderItems.providerPersonId"));
    }

    private BeanPropertyBindingResult validateReceivedTime(String receivedTime) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(new SampleOrderItem());
        form.setSampleXML("<?xml version='1.0' encoding='utf-8'?><samples><sample sampleID='1' tests='' panels=''"
                + " date='' time='' receivedDate='' receivedTime='" + receivedTime + "'/></samples>");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        validator.validate(form, errors);
        return errors;
    }

    @Test
    public void aReceivedTimeOnTheClockPasses() {
        assertFalse(validateReceivedTime("14:20").hasErrors());
        assertFalse(validateReceivedTime("").hasErrors());
    }

    /**
     * OGC-1657: "1430" dropped the sample's receipt date and "25:99" stored the
     * next day, both under a saved order.
     */
    @Test
    public void aReceivedTimeThatIsNoTimeIsRefused() {
        for (String time : new String[] { "1430", "25:99", "14:2x" }) {
            FieldError error = validateReceivedTime(time).getFieldError("sampleXML");
            assertNotNull(time + " must be refused", error);
            assertEquals("error.field.time.invalid", error.getCode());
        }
    }

    /**
     * An environmental order omits patient data, so it reaches this validator with
     * patient field errors already bound; those stopped every sample date and time
     * check before it ran.
     */
    @Test
    public void aSampleTimeIsCheckedWhenPatientErrorsAreAlreadyBound() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(new SampleOrderItem());
        form.setSampleXML("<?xml version='1.0' encoding='utf-8'?><samples><sample sampleID='1' tests='' panels=''"
                + " date='' time='1430' receivedDate='' receivedTime='25:99'/></samples>");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        errors.rejectValue("patientProperties", "NotNull");

        validator.validate(form, errors);

        FieldError error = errors.getFieldError("sampleXML");
        assertNotNull("the collection time must still be refused", error);
        assertTrue(error.getDefaultMessage(), error.getDefaultMessage().contains("sampleXML time"));
    }

    /**
     * The sample checks now run for environmental and vector orders too, so a
     * sample element without the optional attributes must not fail with a
     * NullPointerException.
     */
    @Test
    public void aSampleWithoutOptionalAttributesPasses() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(new SampleOrderItem());
        form.setSampleXML("<?xml version='1.0' encoding='utf-8'?><samples><sample sampleID='1'/></samples>");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");

        validator.validate(form, errors);

        assertFalse(errors.getAllErrors().toString(), errors.hasErrors());
    }
}
