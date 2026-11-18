package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.payment.service.PaymentContactService;
import org.openelisglobal.payment.service.PaymentContactServiceImpl;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

public class PaymentContactServiceTest {

    private static final String SAMPLE_ID = "101";

    private SampleService sampleService;
    private SampleHumanService sampleHumanService;
    private PersonService personService;
    private PaymentContactService service;
    private Sample sample;
    private Person person;

    @Before
    public void setUp() {
        sampleService = mock(SampleService.class);
        sampleHumanService = mock(SampleHumanService.class);
        personService = mock(PersonService.class);
        service = new PaymentContactServiceImpl(sampleService, sampleHumanService, personService);

        sample = new Sample();
        sample.setId(SAMPLE_ID);
        person = new Person();
        Patient patient = new Patient();
        patient.setPerson(person);
        when(sampleService.getMatch("id", SAMPLE_ID)).thenReturn(Optional.of(sample));
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
    }

    @Test
    public void returnsThePatientsPhoneAndEmail() {
        when(personService.getPhone(person)).thenReturn("+91 98765 43210");
        person.setEmail("pat@example.org");

        CustomerContact contact = service.contactForSample(SAMPLE_ID);

        assertEquals("+919876543210", contact.phone());
        assertEquals("pat@example.org", contact.email());
    }

    @Test
    public void unknownSampleGivesNoContact() {
        when(sampleService.getMatch("id", "999")).thenReturn(Optional.empty());

        assertSame(CustomerContact.NONE, service.contactForSample("999"));
    }

    @Test
    public void sampleWithoutAPatientGivesNoContact() {
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(null);

        assertSame(CustomerContact.NONE, service.contactForSample(SAMPLE_ID));
    }
}
