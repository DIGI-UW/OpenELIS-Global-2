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
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/**
 * An order naming a referring site (or site department) that does not exist is
 * refused by the form validator with a field error naming the id, instead of
 * failing later with a NullPointerException that surfaced as a bare HTTP 500.
 */
public class SamplePatientEntryFormValidatorTest {

    private SamplePatientEntryFormValidator validator;
    private OrganizationService organizationService;

    @Before
    public void setUp() {
        validator = new SamplePatientEntryFormValidator();
        organizationService = mock(OrganizationService.class);
        ReflectionTestUtils.setField(validator, "organizationService", organizationService);
        when(organizationService.getOrganizationById("12")).thenReturn(new Organization());
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
}
