package org.openelisglobal.patient;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientTypeService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PatientService patientService;

    @Autowired
    PatientTypeService patientTypeService;

    @Autowired
    PersonService personService;

    @Autowired
    PatientIdentityTypeService identityTypeService;

    @Autowired
    PatientIdentityService identityService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/patient.xml");
        resetSequence("PATIENT", "ID", "patient_seq");
        resetSequence("PERSON", "ID", "person_seq");
    }

    @After
    public void cleanup() throws Exception {
        identityService.deleteAll(identityService.getAll());
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @Test
    public void createPatient_shouldCreateNewPatient() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);

        Assert.assertEquals(4, patientService.getAllPatients().size());

        String patientId = patientService.insert(pat);
        Patient savedPatient = patientService.get(patientId);

        Assert.assertEquals(5, patientService.getAllPatients().size());
        Assert.assertEquals(firstName, savedPatient.getPerson().getFirstName());
        Assert.assertEquals(lastname, savedPatient.getPerson().getLastName());
        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String gender = "M";

        Patient savedPatient = new Patient();
        savedPatient.setId("1");
        patientService.getData(savedPatient);

        Assert.assertEquals(firstName, savedPatient.getPerson().getFirstName());
        Assert.assertEquals(lastname, savedPatient.getPerson().getLastName());
        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getSubjectNumber_shouldReturnSubjectNumber() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("334-422-A", patientService.getSubjectNumber(patient));
    }

    @Test
    public void getIdentityList_shouldReturnIdentityList() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals(17, patientService.getIdentityList(patient).size());
    }

    @Test
    public void getNationality_shouldReturnNationality() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("Ugandan", patientService.getNationality(patient));
    }

    @Test
    public void getOtherNationality_shouldReturnOtherNationality() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("USA", patientService.getOtherNationality(patient));
    }

    @Test
    public void getMother_shouldReturnMother() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("Jackie Moore", patientService.getMother(patient));
    }

    @Test
    public void getMothersInitial_shouldReturnMothersInitial() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("JM", patientService.getMothersInitial(patient));
    }

    @Test
    public void getInsurance_shouldReturnInsurance() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("US119a36", patientService.getInsurance(patient));
    }

    @Test
    public void getOccupation_shouldReturnOccupation() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("Truck Driver", patientService.getOccupation(patient));
    }

    @Test
    public void getOrgSite_shouldReturnOrgSite() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("orgSite", patientService.getOrgSite(patient));
    }

    @Test
    public void getEducation_shouldReturnEducationQualification() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("MBA Certificate", patientService.getEducation(patient));
    }

    @Test
    public void getHealthDistrict_shouldReturnHealthDistrict() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("Jinja", patientService.getHealthDistrict(patient));
    }

    @Test
    public void getHealthRegion_shouldReturnHealthRegion() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("EastAfrica", patientService.getHealthRegion(patient));
    }

    @Test
    public void getMaritalStatus_shouldReturnMaritalStatus() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("Married", patientService.getMaritalStatus(patient));
    }

    @Test
    public void getObNumber_shouldReturngObNumber() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("1234", patientService.getObNumber(patient));
    }

    @Test
    public void getPCNumber_shouldReturngPCNumber() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("124", patientService.getPCNumber(patient));
    }

    @Test
    public void getSTNumber_shouldReturngSTNumber() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("123", patientService.getSTNumber(patient));
    }

    @Test
    public void getAKA_shouldReturnAKA() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("BigMan", patientService.getAKA(patient));
    }

    @Test
    public void getGUID_shouldReturnGUID() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("EA400A1", patientService.getGUID(patient));
    }

    @Test
    public void getGUID_shouldReturnEmptyStringForNullPatient() throws Exception {
        Assert.assertEquals("", patientService.getGUID(null));
    }

    @Test
    public void getGUID_shouldReturnEmptyStringForNullPatientWithNoGUID() throws Exception {
        Patient patient = patientService.get("3");

        Assert.assertEquals("", patientService.getGUID(patient));
    }

    @Test
    public void getPatientForGuid_shouldReturnPatientForGuid() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String gender = "M";

        Patient savedPatient = patientService.getPatientForGuid("EA400A1");

        Assert.assertEquals(firstName, savedPatient.getPerson().getFirstName());
        Assert.assertEquals(lastname, savedPatient.getPerson().getLastName());
        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabaseById() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String gender = "M";

        Patient patient2 = patientService.getData("1");

        Assert.assertEquals(firstName, patient2.getPerson().getFirstName());
        Assert.assertEquals(lastname, patient2.getPerson().getLastName());
        Assert.assertEquals(gender, patient2.getGender());
    }

    @Test
    public void getEnteredDOB_shouldReturnEnteredDOB() throws Exception {
        Patient patient = patientService.get("1");
        Assert.assertEquals("1992-12-12", patientService.getEnteredDOB(patient));
    }

    /*
     * needs to be looked into! test Fails patient.getBirthDate() returns 'null' if
     * we opt to fetch it from loaded dataset Implies BirthDate recorded doesn't
     * reflect in it's field when loeded from the dataset
     * 
     * @Test public void getDOB_shouldReturnDOB() throws Exception { Patient pat =
     * patientService.get("1");
     * 
     * Assert.assertEquals("1992-12-12 00:00:00.0",
     * patientService.getDOB(patient).toString()); }
     */

    @Test
    public void getDOB_shouldReturnDOB() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        patientService.insert(patient);
        Assert.assertEquals("1992-12-12 00:00:00.0", patientService.getDOB(patient).toString());
    }

    @Test
    public void getPhone_shouldReturnPhone() throws Exception {
        Patient patient = patientService.get("1");
        Assert.assertEquals("12345678", patientService.getPhone(patient));
    }

    @Test
    public void getPerson_shouldReturnPerson() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        Patient patient = patientService.get("1");

        Person retrievedPerson = patientService.getPerson(patient);
        Assert.assertEquals(firstName, retrievedPerson.getFirstName());
        Assert.assertEquals(lastname, retrievedPerson.getLastName());
    }

    @Test
    public void getPatientId_shouldReturngPatientId() throws Exception {
        Patient patient = patientService.get("1");

        Assert.assertEquals("1", patientService.getPatientId(patient));
    }

    @Test
    public void getAllPatients_shouldGetAllPatients() throws Exception {
        Assert.assertEquals(4, patientService.getAllPatients().size());
    }

    @Test
    public void getAllMissingFhirUuid_shouldGetAllPatientsMissingFhirUuid() throws Exception {
        UUID generatedUUID = UUID.randomUUID();
        Patient patient = patientService.get("1");
        patient.setFhirUuid(generatedUUID);
        patientService.update(patient);

        Assert.assertEquals(4, patientService.getAll().size());
        List<Patient> patients = patientService.getAllMissingFhirUuid();

        Assert.assertEquals(3, patients.size());
        Assert.assertEquals("The first element should be 2", patients.get(0).getId(), "2");
        Assert.assertEquals("The first element should be 3", patients.get(1).getId(), "3");
    }

    public void getByExternalId_shouldGetAllPatients() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String gender = "M";
        Assert.assertEquals(firstName, patientService.getByExternalId("EX456").getPerson().getFirstName());
        Assert.assertEquals(lastname, patientService.getByExternalId("EX456").getPerson().getLastName());
        Assert.assertEquals(gender, patientService.getByExternalId("EX456").getGender());
    }

    @Test
    public void getPatientByPerson_shouldReturnPatientByPerson() throws Exception {
        String gender = "M";

        Person person = personService.get("1");

        Patient patient = patientService.getPatientByPerson(person);

        Assert.assertEquals(gender, patient.getGender());

    }

    private Patient createPatient(String firstName, String LastName, String birthDate, String gender)
            throws ParseException {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(LastName);
        personService.save(person);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(birthDate);
        long time = date.getTime();
        Timestamp dob = new Timestamp(time);

        Patient pat = new Patient();
        pat.setPerson(person);
        pat.setBirthDate(dob);
        pat.setGender(gender);

        return pat;
    }

    @Test
    public void updatePatient_shouldUpdateExistingPatient() throws Exception {
        Patient savedPatient = patientService.get("2");
        savedPatient.getPerson().setFirstName("Alicia");
        savedPatient.setGender("M");

        personService.save(savedPatient.getPerson());

        patientService.update(savedPatient);

        Patient updatedPatient = patientService.get("2");
        Assert.assertEquals("Alicia", updatedPatient.getPerson().getFirstName());
        Assert.assertEquals("M", updatedPatient.getGender());
    }

    @Test
    public void deletePatient_shouldRemovePatient() throws Exception {
        Assert.assertEquals(4, patientService.getAllPatients().size());

        Patient savedPatient = patientService.get("4");
        Assert.assertNotNull(savedPatient);

        patientService.delete(savedPatient);

        Assert.assertEquals(3, patientService.getAllPatients().size());
    }

    @Test
    public void createPatientType_shouldCreateNewPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(6, patientTypeService.getAllPatientTypes().size());

        String patientTypeId = patientTypeService.insert(patientType);
        PatientType savedPatientType = patientTypeService.get(patientTypeId);

        Assert.assertEquals(7, patientTypeService.getAllPatientTypes().size());
        Assert.assertEquals("Test Type Description", savedPatientType.getDescription());
        Assert.assertEquals("Test Type", savedPatientType.getType());
    }

    @Test
    public void getNationalId_shouldReturnNationalId() throws Exception {
        Patient pat = patientService.get("1");

        Assert.assertEquals("1234", patientService.getNationalId(pat));
    }

    @Test
    public void getAddressComponents_shouldReturnAddressComponents() throws Exception {
        String city = "Kampala";
        String country = "Uganda";
        String state = "Kampala metropolitan";
        String zipCode = "256";
        Patient pat = patientService.get("1");

        Map<String, String> result = patientService.getAddressComponents(pat);

        assertEquals(city, result.get("City"));
        assertEquals(country, result.get("Country"));
        assertEquals(state, result.get("State"));
        assertEquals(zipCode, result.get("Zip"));
    }

    @Test
    public void getPatientByNationalId_shouldReturnCorrectPatient() throws Exception {
        Patient fetchedPatient = patientService.getPatientByNationalId("1234");

        Assert.assertNotNull(fetchedPatient);
        Assert.assertEquals("1", fetchedPatient.getId());
    }

    @Test
    public void getPatientsByNationalId_shouldReturnListOfPatients() throws Exception {
        List<Patient> fetchedPatients = patientService.getPatientsByNationalId("1234");

        Assert.assertNotNull(fetchedPatients);
        Assert.assertEquals(3, fetchedPatients.size());
    }

    @Test
    public void getPatientByExternalId_shouldReturnCorrectPatient() throws Exception {
        Patient fetchedPatient = patientService.getPatientByExternalId("EX123");

        Assert.assertNotNull(fetchedPatient);
        Assert.assertEquals("Faith", fetchedPatient.getPerson().getFirstName());
        Assert.assertEquals("F", fetchedPatient.getGender());
    }

    @Test
    public void externalIDExists_shouldReturnExternalIDExists() throws Exception {

        Assert.assertTrue(patientService.externalIDExists("EX123"));
    }

    @Test
    public void getExternalID_shouldReturnExternalID() throws Exception {
        Patient pat = patientService.get("2");

        Assert.assertEquals("EX123", patientService.getExternalId(pat));
    }

    @Test
    public void readPatient_shouldReadPatient() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String gender = "M";

        Patient patient = patientService.readPatient("1");

        Assert.assertEquals(firstName, patient.getPerson().getFirstName());
        Assert.assertEquals(lastname, patient.getPerson().getLastName());
        Assert.assertEquals(gender, patient.getGender());
    }

    @Test
    public void getFirstName_shouldReturnCorrectFirstName() throws Exception {
        String firstName = "James";
        Patient pat = patientService.get("3");

        String fetchedFirstName = patientService.getFirstName(pat);

        Assert.assertEquals(firstName, fetchedFirstName);
    }

    @Test
    public void getLastName_shouldReturnCorrectLastName() throws Exception {
        String lastName = "Kukki";
        Patient pat = patientService.get("2");

        String fetchedLastName = patientService.getLastName(pat);

        Assert.assertEquals(lastName, fetchedLastName);
    }

    @Test
    public void getLastFirstName_shouldReturnCorrectLastFirstName() throws Exception {
        String firstName = "Faith";
        String lastName = "Kukki";
        Patient pat = patientService.get("2");

        String lastFirstName = patientService.getLastFirstName(pat);

        Assert.assertEquals(lastName + ", " + firstName, lastFirstName);
    }

    @Test
    public void getGender_shouldReturnCorrectGender() throws Exception {
        String gender = "F";
        Patient pat = patientService.get("2");

        String fetchedGender = patientService.getGender(pat);

        Assert.assertEquals(gender, fetchedGender);
    }

    @Test
    public void getLocalizedGender_shouldReturnCorrectLocalizedGender() throws Exception {

        Patient pat = patientService.get("1");

        String localizedGender = patientService.getLocalizedGender(pat);

        Assert.assertEquals("MALE", localizedGender);
    }

    /*
     * *patient.getBirthDate() returns 'null' if we opt to fetch it from loaded
     * dataset Implies BirthDate recorded doesn't reflect in it's field when loeded
     * from the dataset
     * 
     * 
     * @Test public void getBirthdayForDisplay_shouldReturnBirthdayForDisplay()
     * throws Exception { String dob = "1992-12-12"; Patient pat =
     * patientService.get("1");
     * 
     * Assert.assertEquals(dob, patientService.getBirthdayForDisplay(pat)); }
     */

    @Test
    public void getBirthdayForDisplay_shouldReturnBirthdayForDisplay() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        patientService.insert(pat);

        Assert.assertEquals(dob, patientService.getBirthdayForDisplay(pat).toString());
    }

    @Test
    public void getPageOfPatients_shouldReturnCorrectPatients() throws Exception {
        String firstName1 = "John";

        String firstName2 = "Faith";

        List<Patient> patientsPage = patientService.getPageOfPatients(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        Assert.assertTrue(patientsPage.size() <= expectedPageSize);

        if (expectedPageSize >= 3) {
            Assert.assertTrue(patientsPage.stream().anyMatch(p -> p.getPerson().getFirstName().equals(firstName1)));
            Assert.assertTrue(patientsPage.stream().anyMatch(p -> p.getPerson().getFirstName().equals(firstName2)));
        }
    }

}
