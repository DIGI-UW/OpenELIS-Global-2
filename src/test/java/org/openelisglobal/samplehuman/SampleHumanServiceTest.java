package org.openelisglobal.samplehuman;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleHumanServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    SampleHumanService humanService;

    @Autowired
    SampleService sampleService;

    @Autowired
    ProviderService providerService;

    @Autowired
    PatientService patientService;

    @Autowired
    PersonService personService;

    private static final String PATIENT_FIRSTNAME = "John";
    private static final String PATIENT_LASTNAME = "Doe";
    private static final String PROVIDER_FIRSTNAME = "Jane";
    private static final String PROVIDER_LASTNAME = "Loo";
    private static final String PATIENT_BIRTHDATE = "03/06/1993";
    private static final String SAMPLE_ACCESSION_NUMBER = "12345";
    private static final String PATIENT_GENDER = "M";
    private static final String SAMPLE_RECEIVED_TIMESTAMP = "012/06/2024";
    private static final String PROVIDER_TYPE = "P";
    private static final String SAMPLE_ENTERED_DATE = "2024-06-03";

    @Before
    public void init() throws Exception {
        humanService.deleteAll(humanService.getAll());
        sampleService.deleteAll(sampleService.getAll());
        providerService.deleteAll(providerService.getAll());
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @After
    public void tearDown() throws Exception {
        humanService.deleteAll(humanService.getAll());
        sampleService.deleteAll(sampleService.getAll());
        providerService.deleteAll(providerService.getAll());
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @Test
    public void createSampleHuman_shouldCreateNewSampleHuman() throws Exception {
        SampleHuman sampleHuman = creatSampleHuman(PATIENT_FIRSTNAME, PATIENT_LASTNAME, PROVIDER_FIRSTNAME,
                PROVIDER_LASTNAME, PATIENT_BIRTHDATE, SAMPLE_ACCESSION_NUMBER, PATIENT_GENDER,
                SAMPLE_RECEIVED_TIMESTAMP, PROVIDER_TYPE, SAMPLE_ENTERED_DATE);

        Assert.assertEquals(0, humanService.getAll().size());

        humanService.insert(sampleHuman);

        Assert.assertEquals(1, humanService.getAll().size());

    }

    @Test
    public void updateSampleHuman_shouldUpdateSampleHuman() throws Exception {

        Person person = new Person();
        person.setFirstName(PATIENT_FIRSTNAME);
        person.setLastName(PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(PROVIDER_FIRSTNAME);
        person2.setLastName(PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(PATIENT_BIRTHDATE).getTime()));
        pat.setPerson(person);
        pat.setGender(PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(PROVIDER_TYPE);
        String providerId = providerService.insert(prov);
        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(
                new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        humanService.insert(sampleHuman);
        Person updateSamplehuman = humanService.getPatientForSample(samp).getPerson();
        updateSamplehuman.setLastName("Nakibinge");
        personService.save(updateSamplehuman);

        Assert.assertEquals("Nakibinge", humanService.getPatientForSample(samp).getPerson().getLastName());

    }

    @Test
    public void deleteSampleHuman_shouldDeleteSampleHuman() throws Exception {
        SampleHuman sampleHuman = creatSampleHuman(PATIENT_FIRSTNAME, PATIENT_LASTNAME, PROVIDER_FIRSTNAME,
                PROVIDER_LASTNAME, PATIENT_BIRTHDATE, SAMPLE_ACCESSION_NUMBER, PATIENT_GENDER,
                SAMPLE_RECEIVED_TIMESTAMP, PROVIDER_TYPE, SAMPLE_ENTERED_DATE);
        Assert.assertEquals(0, humanService.getAll().size());

        String sampleHumanId = humanService.insert(sampleHuman);
        SampleHuman savedSampleHuman = humanService.get(sampleHumanId);

        humanService.delete(savedSampleHuman);

        Assert.assertEquals(0, humanService.getAll().size());

    }

    @Test
    public void getAllPatientsWithSampleEntered_shouldReturnPatientsWithSample() throws Exception {
        SampleHuman sampleHuman = creatSampleHuman(PATIENT_FIRSTNAME, PATIENT_LASTNAME, PROVIDER_FIRSTNAME,
                PROVIDER_LASTNAME, PATIENT_BIRTHDATE, SAMPLE_ACCESSION_NUMBER, PATIENT_GENDER,
                SAMPLE_RECEIVED_TIMESTAMP, PROVIDER_TYPE, SAMPLE_ENTERED_DATE);

        Assert.assertEquals(0, humanService.getAll().size());

        humanService.insert(sampleHuman);
        List<Patient> patients = humanService.getAllPatientsWithSampleEntered();
        ;

        Assert.assertEquals(1, patients.size());

    }

    private SampleHuman creatSampleHuman(String firstname, String lastname, String firstname2, String lastname2,
            String birthdate, String accessionNumber, String gender, String receivedTimestamp, String type,
            String entereddate) throws ParseException {
        Person person = new Person();
        person.setFirstName(firstname);
        person.setLastName(lastname);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(firstname2);
        person2.setLastName(lastname2);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(birthdate).getTime()));
        pat.setPerson(person);
        pat.setGender(gender);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(type);
        String providerId = providerService.insert(prov);

        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(receivedTimestamp).getTime()));
        samp.setAccessionNumber(accessionNumber);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        return sampleHuman;
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() throws Exception {
        Person person = new Person();
        person.setFirstName(PATIENT_FIRSTNAME);
        person.setLastName(PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(PROVIDER_FIRSTNAME);
        person2.setLastName(PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(PATIENT_BIRTHDATE).getTime()));
        pat.setPerson(person);
        pat.setGender(PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(PROVIDER_TYPE);
        String providerId = providerService.insert(prov);

        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(
                new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        String sampleHumanId = humanService.insert(sampleHuman);

        SampleHuman sHumanToUpdate = new SampleHuman();
        sHumanToUpdate.setId(sampleHumanId);

        humanService.getData(sHumanToUpdate);
        ;

        Assert.assertEquals(providerId, sHumanToUpdate.getProviderId());

    }

    @Test
    public void getPatientForSample_shouldReturnPatientForSample() throws Exception {
        Person person = new Person();
        person.setFirstName(PATIENT_FIRSTNAME);
        person.setLastName(PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(PROVIDER_FIRSTNAME);
        person2.setLastName(PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(PATIENT_BIRTHDATE).getTime()));
        pat.setPerson(person);
        pat.setGender(PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(PROVIDER_TYPE);
        String providerId = providerService.insert(prov);
        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(
                new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        humanService.insert(sampleHuman);
        Patient samplePatient = humanService.getPatientForSample(samp);

        Assert.assertEquals(PATIENT_FIRSTNAME, samplePatient.getPerson().getFirstName());

    }

    @Test
    public void getSamplesForPatient_shouldReturnSamplesForPatient() throws Exception {
        Person person = new Person();
        person.setFirstName(PATIENT_FIRSTNAME);
        person.setLastName(PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(PROVIDER_FIRSTNAME);
        person2.setLastName(PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(PATIENT_BIRTHDATE).getTime()));

        pat.setPerson(person);
        pat.setGender(PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(PROVIDER_TYPE);
        String providerId = providerService.insert(prov);

        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(
                new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        humanService.insert(sampleHuman);
        List<Sample> samples = humanService.getSamplesForPatient(patId);

        Assert.assertEquals(1, samples.size());
    }

    @Test
    public void getDataBySample_shouldReturnDataBySample() throws Exception {
        Person person = new Person();
        person.setFirstName(PATIENT_FIRSTNAME);
        person.setLastName(PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(PROVIDER_FIRSTNAME);
        person2.setLastName(PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(PATIENT_BIRTHDATE).getTime()));
        pat.setPerson(person);
        pat.setGender(PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(PROVIDER_TYPE);
        String providerId = providerService.insert(prov);

        DateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yyyy");
        Date date2 = dateFormat2.parse(SAMPLE_RECEIVED_TIMESTAMP);
        long time2 = date2.getTime();
        Timestamp doc = new Timestamp(time2);

        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(
                new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse(SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        humanService.insert(sampleHuman);

        SampleHuman sHumanToUpdate = humanService.getDataBySample(sampleHuman);

        Assert.assertEquals(patId, sHumanToUpdate.getPatientId());

    }
}
