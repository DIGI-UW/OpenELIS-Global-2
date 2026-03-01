package org.openelisglobal.samplehuman;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
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
    private static final String SAMPLE_ACCESSION_NUMBER = "10000";
    private static final String PATIENT_GENDER = "M";
    private static final String SAMPLE_RECEIVED_TIMESTAMP = "012/06/2024";
    private static final String PROVIDER_TYPE = "P";
    private static final String SAMPLE_ENTERED_DATE = "2024-06-03";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/samplehuman.xml");
    }

    @Test
    public void createSampleHuman_shouldCreateNewSampleHuman() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "patient", "provider", "sample", "sample_human" });
        SampleHuman sampleHuman = creatSampleHuman(SAMPLE_ENTERED_DATE);

        Assert.assertEquals(0, humanService.getAll().size());

        humanService.insert(sampleHuman);

        Assert.assertEquals(1, humanService.getAll().size());

        humanService.delete(sampleHuman);
    }

    @Test
    public void updateSampleHuman_shouldUpdateSampleHuman() throws Exception {
        Sample samp = sampleService.get("1");
        Assert.assertEquals("Doe", humanService.getPatientForSample(samp).getPerson().getLastName());

        Person updateSamplehuman = humanService.getPatientForSample(samp).getPerson();
        updateSamplehuman.setLastName("Nakibinge");
        personService.save(updateSamplehuman);

        Assert.assertEquals("Nakibinge", humanService.getPatientForSample(samp).getPerson().getLastName());

    }

    @Test
    public void deleteSampleHuman_shouldDeleteSampleHuman() throws Exception {
        Assert.assertEquals(3, humanService.getAll().size());

        SampleHuman savedSampleHuman = humanService.get("3");

        humanService.delete(savedSampleHuman);

        Assert.assertEquals(2, humanService.getAll().size());

    }

    @Test
    public void getAllPatientsWithSampleEntered_shouldReturnPatientsWithSample() throws Exception {
        List<Patient> patients = humanService.getAllPatientsWithSampleEntered();

        patients.sort(Comparator.comparing(p -> p.getPerson().getFirstName()));

        Assert.assertEquals(3, patients.size());
        Assert.assertEquals("Faith", patients.get(0).getPerson().getFirstName());
        Assert.assertEquals("James", patients.get(1).getPerson().getFirstName());
        Assert.assertEquals(PATIENT_FIRSTNAME, patients.get(2).getPerson().getFirstName());
    }

    private SampleHuman creatSampleHuman(String entereddate) throws ParseException {
        Person person = new Person();
        person.setFirstName(SampleHumanServiceTest.PATIENT_FIRSTNAME);
        person.setLastName(SampleHumanServiceTest.PATIENT_LASTNAME);
        personService.save(person);

        Person person2 = new Person();
        person2.setFirstName(SampleHumanServiceTest.PROVIDER_FIRSTNAME);
        person2.setLastName(SampleHumanServiceTest.PROVIDER_LASTNAME);
        personService.save(person2);

        Patient pat = new Patient();
        pat.setBirthDate(new Timestamp(
                new SimpleDateFormat("dd/MM/yyyy").parse(SampleHumanServiceTest.PATIENT_BIRTHDATE).getTime()));
        pat.setPerson(person);
        pat.setGender(SampleHumanServiceTest.PATIENT_GENDER);
        String patId = patientService.insert(pat);

        Provider prov = new Provider();
        prov.setPerson(person2);
        prov.setProviderType(SampleHumanServiceTest.PROVIDER_TYPE);
        String providerId = providerService.insert(prov);

        java.sql.Date enteredDate = java.sql.Date.valueOf(SAMPLE_ENTERED_DATE);

        Sample samp = new Sample();
        samp.setEnteredDate(enteredDate);
        samp.setReceivedTimestamp(new Timestamp(
                new SimpleDateFormat("dd/MM/yyyy").parse(SampleHumanServiceTest.SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SampleHumanServiceTest.SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setPatientId(patId);
        sampleHuman.setProviderId(providerId);
        sampleHuman.setSampleId(sampId);

        return sampleHuman;
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() throws Exception {
        SampleHuman sHumanToUpdate = new SampleHuman();
        sHumanToUpdate.setId("3");

        humanService.getData(sHumanToUpdate);
        Assert.assertEquals("3", sHumanToUpdate.getProviderId());
    }

    @Test
    public void getPatientForSample_shouldReturnPatientForSample() throws Exception {
        Sample samp = sampleService.get("1");

        Patient samplePatient = humanService.getPatientForSample(samp);

        Assert.assertEquals(PATIENT_FIRSTNAME, samplePatient.getPerson().getFirstName());
    }

    @Test
    public void getSamplesForPatient_shouldReturnSamplesForPatient() throws Exception {
        List<Sample> samples = humanService.getSamplesForPatient("1");

        Assert.assertEquals(1, samples.size());
        Assert.assertEquals("The first element should be 12345", samples.get(0).getAccessionNumber(), "12345");
    }

    @Test
    public void getDataBySample_shouldReturnDataBySample() throws Exception {
        SampleHuman sampleHuman = humanService.get("2");

        SampleHuman sHumanToUpdate = humanService.getDataBySample(sampleHuman);

        Assert.assertEquals("2", sHumanToUpdate.getPatientId());

    }

    // ----------------------------------------------------------------
    // NEW TESTS — covering previously untested methods
    // ----------------------------------------------------------------

    @Test
    public void getProviderForSample_shouldReturnProviderForSample() throws Exception {
        // sample id=1 is linked to provider id=1 (person_id=4, Jane Linn) in test data
        Sample sample = sampleService.get("1");

        Provider provider = humanService.getProviderForSample(sample);

        Assert.assertNotNull(provider);
        Assert.assertEquals("1", provider.getId());
        Assert.assertEquals("Jane", provider.getPerson().getFirstName());
        Assert.assertEquals("Linn", provider.getPerson().getLastName());
    }

    @Test
    public void getProviderForSample_shouldReturnCorrectProviderType() throws Exception {
        // sample id=1 is linked to provider id=1 with type "P"
        Sample sample = sampleService.get("1");

        Provider provider = humanService.getProviderForSample(sample);

        Assert.assertEquals("P", provider.getProviderType());
    }

    @Test
    public void getProviderForSample_shouldReturnDifferentProvidersForDifferentSamples() throws Exception {
        Sample sample1 = sampleService.get("1");
        Sample sample2 = sampleService.get("2");

        Provider provider1 = humanService.getProviderForSample(sample1);
        Provider provider2 = humanService.getProviderForSample(sample2);

        Assert.assertNotNull(provider1);
        Assert.assertNotNull(provider2);
        Assert.assertNotEquals(provider1.getId(), provider2.getId());
    }

    @Test
    public void getAllPatientsWithSampleEnteredMissingFhirUuid_shouldReturnPatientsMissingFhirUuid() throws Exception {
        // None of the test data patients have a fhir_uuid set, so all 3 should be
        // returned
        List<Patient> patients = humanService.getAllPatientsWithSampleEnteredMissingFhirUuid();

        Assert.assertNotNull(patients);
        Assert.assertEquals(3, patients.size());
    }

    @Test
    public void getAllPatientsWithSampleEnteredMissingFhirUuid_shouldReturnEmptyAfterFhirUuidSet() throws Exception {
        // Assign fhir uuids to all patients then verify list is empty
        List<Patient> allPatients = humanService.getAllPatientsWithSampleEntered();
        for (Patient patient : allPatients) {
            patient.setFhirUuid(java.util.UUID.randomUUID());
            patientService.save(patient);
        }

        List<Patient> missingFhir = humanService.getAllPatientsWithSampleEnteredMissingFhirUuid();

        Assert.assertNotNull(missingFhir);
        Assert.assertEquals(0, missingFhir.size());
    }

    @Test
    public void getSamplesForPatient_shouldReturnEmptyListForPatientWithNoSamples() throws Exception {
        // patient id=2 has sample id=2 in the dataset, use a non-existent patient id
        List<Sample> samples = humanService.getSamplesForPatient("9999");

        Assert.assertNotNull(samples);
        Assert.assertTrue(samples.isEmpty());
    }

    @Test
    public void getSamplesForPatient_shouldReturnCorrectAccessionNumber() throws Exception {
        // patient id=2 is linked to sample id=2 with accession number "13333"
        List<Sample> samples = humanService.getSamplesForPatient("2");

        Assert.assertEquals(1, samples.size());
        Assert.assertEquals("13333", samples.get(0).getAccessionNumber());
    }

    @Test
    public void getDataBySample_shouldReturnNullForSampleWithNoLink() throws Exception {
        // Create a sample human with a non-existent sample id
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId("9999");

        SampleHuman result = humanService.getDataBySample(sampleHuman);

        Assert.assertNull(result);
    }

}
