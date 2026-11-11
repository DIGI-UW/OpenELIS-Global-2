package org.openelisglobal.sample.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/**
 * Modify Order checks the parties an order names before anything else: an
 * unknown requester failed the save with a bare HTTP 500, and an unknown
 * referring site was ignored while the save answered 200 (OGC-1543 walk).
 */
public class SampleEditFormValidatorPartiesTest {

    private SampleEditFormValidator validator;

    @Before
    public void setUp() {
        OrganizationService organizationService = mock(OrganizationService.class);
        PersonService personService = mock(PersonService.class);
        when(personService.getPersonById("9000099")).thenReturn(null);
        SampleOrderPartiesValidator parties = new SampleOrderPartiesValidator();
        ReflectionTestUtils.setField(parties, "organizationService", organizationService);
        ReflectionTestUtils.setField(parties, "personService", personService);
        ReflectionTestUtils.setField(parties, "providerService", mock(ProviderService.class));
        validator = new SampleEditFormValidator();
        ReflectionTestUtils.setField(validator, "orderPartiesValidator", parties);
    }

    /**
     * The form and the accession checks that follow the parties read the site
     * configuration, which a unit test does not load: the form is a mock that holds
     * only the order, and the accession checks' failure is ignored here.
     */
    private BeanPropertyBindingResult validatePartiesOf(SampleOrderItem order) {
        SampleEditForm form = mock(SampleEditForm.class);
        when(form.getSampleOrderItems()).thenReturn(order);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        try {
            validator.validate(form, errors);
        } catch (RuntimeException accessionChecksNeedTheSiteConfiguration) {
        }
        return errors;
    }

    @Test
    public void anUnknownRequesterIsRefused() {
        SampleOrderItem order = new SampleOrderItem();
        order.setProviderPersonId("9000099");

        FieldError error = validatePartiesOf(order).getFieldError("sampleOrderItems.providerPersonId");

        assertNotNull(error);
        assertEquals("error.provider.notFound", error.getCode());
    }

    @Test
    public void anUnknownReferringSiteIsRefused() {
        SampleOrderItem order = new SampleOrderItem();
        order.setReferringSiteId("9000999");

        FieldError error = validatePartiesOf(order).getFieldError("sampleOrderItems.referringSiteId");

        assertNotNull(error);
        assertEquals("error.organization.notFound", error.getCode());
    }
}
