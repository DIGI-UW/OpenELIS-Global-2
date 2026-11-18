package org.openelisglobal.payment.service;

import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the patient and person inside one read-only transaction and returns
 * plain values, so callers outside a transaction (the payment link service) do
 * not touch lazy associations.
 */
@Service
public class PaymentContactServiceImpl implements PaymentContactService {

    private final SampleService sampleService;
    private final SampleHumanService sampleHumanService;
    private final PersonService personService;

    @Autowired
    public PaymentContactServiceImpl(SampleService sampleService, SampleHumanService sampleHumanService,
            PersonService personService) {
        this.sampleService = sampleService;
        this.sampleHumanService = sampleHumanService;
        this.personService = personService;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerContact contactForSample(String sampleId) {
        // getMatch, not get(): get() throws for a missing row, and catching that
        // inside this transaction would leave it marked rollback-only.
        Sample sample = sampleService.getMatch("id", sampleId).orElse(null);
        if (sample == null) {
            return CustomerContact.NONE;
        }
        Patient patient = sampleHumanService.getPatientForSample(sample);
        Person person = patient == null ? null : patient.getPerson();
        if (person == null) {
            return CustomerContact.NONE;
        }
        // getPhone picks the first of primary, home, cell and work phone that is set.
        return CustomerContact.of(personService.getPhone(person), person.getEmail());
    }
}
