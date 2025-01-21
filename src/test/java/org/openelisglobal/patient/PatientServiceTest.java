package org.openelisglobal.patient;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
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
        // identityTypeService.deleteAll(identityTypeService.getAll());
        identityService.deleteAll(identityService.getAll());
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
        patientTypeService.deleteAll(patientTypeService.getAll());

    }

    @After
    public void tearDown() {
        // identityTypeService.deleteAll(identityTypeService.getAll());
        identityService.deleteAll(identityService.getAll());
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
        patientTypeService.deleteAll(patientTypeService.getAll());
    }

    @Test
    public void createPatient_shouldCreateNewPatient() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);

        Assert.assertEquals(0, patientService.getAllPatients().size());

        String patientId = patientService.insert(pat);
        Patient savedPatient = patientService.get(patientId);

        Assert.assertEquals(1, patientService.getAllPatients().size());
        Assert.assertEquals(firstName, savedPatient.getPerson().getFirstName());
        Assert.assertEquals(lastname, savedPatient.getPerson().getLastName());
        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        Patient savedPatient = new Patient();
        savedPatient.setId(patientId);
        patientService.getData(savedPatient);

        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getSubjectNumber_shouldReturnSubjectNumber() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("SUBJECT").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("334-422-A");
        identityService.insert(patientIdentity);

        Assert.assertEquals("334-422-A", patientService.getSubjectNumber(patient));
    }

    @Test
    public void getIdentityList_shouldReturnIdentityList() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("AKA").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("BigMan");
        identityService.insert(patientIdentity);

        String typeID2 = identityTypeService.getNamedIdentityType("GUID").getId();

        PatientIdentity patientIdentity2 = new PatientIdentity();
        patientIdentity2.setIdentityTypeId(typeID2);
        patientIdentity2.setPatientId(patientId);
        patientIdentity2.setIdentityData("EA400A1");
        identityService.insert(patientIdentity2);

        Assert.assertEquals(2, patientService.getIdentityList(patient).size());
    }

    @Test
    public void getNationality_shouldReturnNationality() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("NATIONALITY").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Ugandan");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Ugandan", patientService.getNationality(patient));
    }

    @Test
    public void getOtherNationality_shouldReturnOtherNationality() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("OTHER NATIONALITY").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("USA");
        identityService.insert(patientIdentity);

        Assert.assertEquals("USA", patientService.getOtherNationality(patient));
    }

    @Test
    public void getMother_shouldReturnMother() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("MOTHER").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Jackie Moore");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Jackie Moore", patientService.getMother(patient));
    }

    @Test
    public void getMothersInitial_shouldReturnMothersInitial() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("MOTHERS_INITIAL").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Jackie Moore");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Jackie Moore", patientService.getMothersInitial(patient));
    }

    @Test
    public void getInsurance_shouldReturnInsurance() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("INSURANCE").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("US119a36");
        identityService.insert(patientIdentity);

        Assert.assertEquals("US119a36", patientService.getInsurance(patient));
    }

    @Test
    public void getOccupation_shouldReturnOccupation() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("OCCUPATION").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Truck Driver");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Truck Driver", patientService.getOccupation(patient));
    }

    @Test
    public void getOrgSite_shouldReturnOrgSite() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("ORG_SITE").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("orgSite");
        identityService.insert(patientIdentity);

        Assert.assertEquals("orgSite", patientService.getOrgSite(patient));
    }

    @Test
    public void getEducation_shouldReturnEducationQualification() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("EDUCATION").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("MBA Certificate");
        identityService.insert(patientIdentity);

        Assert.assertEquals("MBA Certificate", patientService.getEducation(patient));
    }

    @Test
    public void getHealthDistrict_shouldReturnHealthDistrict() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("HEALTH DISTRICT").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Jinja");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Jinja", patientService.getHealthDistrict(patient));
    }

    @Test
    public void getHealthRegion_shouldReturnHealthRegion() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("HEALTH REGION").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("EastAfrica");
        identityService.insert(patientIdentity);

        Assert.assertEquals("EastAfrica", patientService.getHealthRegion(patient));
    }

    @Test
    public void getMaritalStatus_shouldReturnMaritalStatus() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("MARITIAL").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("Married");
        identityService.insert(patientIdentity);

        Assert.assertEquals("Married", patientService.getMaritalStatus(patient));
    }

    @Test
    public void getObNumber_shouldReturngObNumber() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("OB_NUMBER").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("1234");
        identityService.insert(patientIdentity);

        Assert.assertEquals("1234", patientService.getObNumber(patient));
    }

    @Test
    public void getPCNumber_shouldReturngPCNumber() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("PC_NUMBER").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("1234");
        identityService.insert(patientIdentity);

        Assert.assertEquals("1234", patientService.getPCNumber(patient));
    }

    @Test
    public void getSTNumber_shouldReturngSTNumber() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("ST").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("1234");
        identityService.insert(patientIdentity);

        Assert.assertEquals("1234", patientService.getSTNumber(patient));
    }

    @Test
    public void getAKA_shouldReturnAKA() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("AKA").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("BigMan");
        identityService.insert(patientIdentity);

        Assert.assertEquals("BigMan", patientService.getAKA(patient));
    }

    @Test
    public void getGUID_shouldReturnGUID() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("GUID").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("EA400A1");
        identityService.insert(patientIdentity);

        Assert.assertEquals("EA400A1", patientService.getGUID(patient));
    }

    @Test
    public void getGUID_shouldReturnEmptyStringForNullPatient() throws Exception {
        Assert.assertEquals("", patientService.getGUID(null));
    }

    @Test
    public void getGUID_shouldReturnEmptyStringForNullPatientWithNoGUID() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        Assert.assertEquals("", patientService.getGUID(patient));
    }

    @Test
    public void getPatientForGuid_shouldReturnPatientForGuid() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        String typeID = identityTypeService.getNamedIdentityType("GUID").getId();

        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId(typeID);
        patientIdentity.setPatientId(patientId);
        patientIdentity.setIdentityData("EA400A1");
        identityService.insert(patientIdentity);

        Patient savedPatient = patientService.getPatientForGuid("EA400A1");

        Assert.assertEquals(gender, savedPatient.getGender());
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabaseById() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(patient);

        Patient patient2 = patientService.getData(patientId);

        Assert.assertEquals(gender, patient2.getGender());
    }

    @Test
    public void getEnteredDOB_shouldReturnEnteredDOB() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        patient.setBirthDateForDisplay("12/12/1992");
        patientService.insert(patient);
        Assert.assertEquals(dob, patientService.getEnteredDOB(patient));
    }

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
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        patient.getPerson().setPrimaryPhone("12345");
        patientService.insert(patient);
        Assert.assertEquals("12345", patientService.getPhone(patient));
    }

    @Test
    public void getPerson_shouldReturnPerson() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);

        patientService.insert(patient);

        Person retrievedPerson = patientService.getPerson(patient);
        Assert.assertEquals(firstName, retrievedPerson.getFirstName());
        Assert.assertEquals(lastname, retrievedPerson.getLastName());
    }

    @Test
    public void getPatientId_shouldReturngetPatientId() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);

        String patientId = patientService.insert(patient);

        Assert.assertEquals(patientId, patientService.getPatientId(patient));
    }

    @Test
    public void getAllPatients_shouldGetAllPatients() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        patientService.insert(patient);
        Assert.assertEquals(1, patientService.getAllPatients().size());
    }

    @Test
    public void getByExternalId_shouldGetAllPatients() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient patient = createPatient(firstName, lastname, dob, gender);
        patient.setExternalId("432");
        patientService.insert(patient);
        Assert.assertEquals(gender, patientService.getByExternalId("432").getGender());
    }

    @Test
    public void getPatientByPerson_shouldReturnPatientByPerson() throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dobs = "12/12/1992";
        String gender = "M";

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastname);
        personService.save(person);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(dobs);
        long time = date.getTime();
        Timestamp dob = new Timestamp(time);

        Patient pat = new Patient();
        pat.setPerson(person);
        pat.setBirthDate(dob);
        pat.setGender(gender);

        patientService.insert(pat);

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
        String firstName = "Alice";
        String lastName = "Johnson";
        String dob = "15/05/1985";
        String gender = "F";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        Patient savedPatient = patientService.get(patientId);
        savedPatient.getPerson().setFirstName("Alicia");
        savedPatient.setGender("F");

        personService.save(savedPatient.getPerson());

        patientService.update(savedPatient);

        Patient updatedPatient = patientService.get(patientId);
        Assert.assertEquals("Alicia", updatedPatient.getPerson().getFirstName());
        Assert.assertEquals("F", updatedPatient.getGender());
    }

    @Test
    public void deletePatient_shouldRemovePatient() throws Exception {
        String firstName = "Bob";
        String lastName = "Marley";
        String dob = "06/02/1945";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        Assert.assertEquals(0, patientService.getAllPatients().size());

        String patientId = patientService.insert(pat);
        Assert.assertNotNull(patientId);

        Patient savedPatient = patientService.get(patientId);
        Assert.assertNotNull(savedPatient);

        patientService.delete(savedPatient);

        Assert.assertEquals(0, patientService.getAllPatients().size());
    }

    @Test
    public void createPatientType_shouldCreateNewPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, patientTypeService.getAllPatientTypes().size());

        String patientTypeId = patientTypeService.insert(patientType);
        PatientType savedPatientType = patientTypeService.get(patientTypeId);

        Assert.assertEquals(1, patientTypeService.getAllPatientTypes().size());
        Assert.assertEquals("Test Type Description", savedPatientType.getDescription());
        Assert.assertEquals("Test Type", savedPatientType.getType());
    }

    @Test
    public void getNationalId_shouldReturnNationalId() throws Exception {
        String firstName = "Bruce";
        String lastName = "Wayne";
        String dob = "10/10/1975";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setNationalId("12345");

        String patientId = patientService.insert(pat);

        Assert.assertEquals("12345", patientService.getNationalId(pat));
    }

    @Test
    public void getAddressComponents_shouldReturnAddressComponents() throws Exception {
        String firstName = "Bruce";
        String lastName = "Wayne";
        String dob = "10/10/1975";
        String gender = "M";
        String city = "Kampala";
        String country = "Uganda";
        String state = "Kisumali";
        String streetAdress = "Bakuli";
        String zipCode = "256";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.getPerson().setCity(city);
        pat.getPerson().setCountry(country);
        pat.getPerson().setState(state);
        pat.getPerson().setStreetAddress(streetAdress);
        pat.getPerson().setZipCode(zipCode);

        String patientId = patientService.insert(pat);

        Map<String, String> result = patientService.getAddressComponents(pat);

        assertEquals(city, result.get("City"));
        assertEquals(country, result.get("Country"));
        assertEquals(state, result.get("State"));
        assertEquals(streetAdress, result.get("Street"));
        assertEquals(zipCode, result.get("Zip"));
    }

    @Test
    public void getPatientByNationalId_shouldReturnCorrectPatient() throws Exception {
        String firstName = "Bruce";
        String lastName = "Wayne";
        String dob = "10/10/1975";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setNationalId("12345");

        String patientId = patientService.insert(pat);

        Patient fetchedPatient = patientService.getPatientByNationalId("12345");

        Assert.assertNotNull(fetchedPatient);
        Assert.assertEquals(patientId, fetchedPatient.getId());
    }

    @Test
    public void getPatientsByNationalId_shouldReturnListOfPatients() throws Exception {
        String firstName = "Bruce";
        String lastName = "Wayne";
        String dob = "10/10/1975";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setNationalId("12345");

        String patientId = patientService.insert(pat);

        String firstName2 = "Bruce";
        String lastName2 = "Wayne";
        String dob2 = "10/10/1975";
        String gender2 = "M";
        Patient pat2 = createPatient(firstName2, lastName2, dob2, gender2);
        pat2.setNationalId("12345");

        String patientId2 = patientService.insert(pat2);

        List<Patient> fetchedPatients = patientService.getPatientsByNationalId("12345");

        Assert.assertNotNull(fetchedPatients);
        Assert.assertEquals(2, fetchedPatients.size());
    }

    @Test
    public void getPatientByExternalId_shouldReturnCorrectPatient() throws Exception {
        String firstName = "Oliver";
        String lastName = "Queen";
        String dob = "27/09/1985";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setExternalId("EX123");

        String patientId = patientService.insert(pat);

        Patient fetchedPatient = patientService.getPatientByExternalId("EX123");

        Assert.assertNotNull(fetchedPatient);
        Assert.assertEquals(patientId, fetchedPatient.getId());
    }

    @Test
    public void externalIDExists_shouldReturnExternalIDExists() throws Exception {
        String firstName = "Oliver";
        String lastName = "Queen";
        String dob = "27/09/1985";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setExternalId("EX123");

        String patientId = patientService.insert(pat);

        Assert.assertTrue(patientService.externalIDExists("EX123"));
    }

    @Test
    public void getExternalID_shouldReturnExternalID() throws Exception {
        String firstName = "Oliver";
        String lastName = "Queen";
        String dob = "27/09/1985";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setExternalId("EX123");

        Assert.assertEquals("EX123", patientService.getExternalId(pat));
    }

    @Test
    public void readPatient_shouldReadPatient() throws Exception {
        String firstName = "Oliver";
        String lastName = "Queen";
        String dob = "27/09/1985";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setExternalId("EX123");

        String patientId = patientService.insert(pat);

        Patient patient = patientService.readPatient(patientId);
        Assert.assertEquals(gender, patient.getGender());
        Assert.assertEquals("1985-09-27 00:00:00.0", patient.getBirthDate().toString());
    }

    @Test
    public void getFirstName_shouldReturnCorrectFirstName() throws Exception {
        String firstName = "Tony";
        String lastName = "Stark";
        String dob = "10/05/1970";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        String fetchedFirstName = patientService.getFirstName(pat);

        Assert.assertEquals(firstName, fetchedFirstName);
    }

    @Test
    public void getLastName_shouldReturnCorrectLastName() throws Exception {
        String firstName = "Natasha";
        String lastName = "Romanoff";
        String dob = "03/12/1985";
        String gender = "F";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        String fetchedLastName = patientService.getLastName(pat);

        Assert.assertEquals(lastName, fetchedLastName);
    }

    @Test
    public void getLastFirstName_shouldReturnCorrectLastFirstName() throws Exception {
        String firstName = "Steve";
        String lastName = "Rogers";
        String dob = "04/07/1920";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        String lastFirstName = patientService.getLastFirstName(pat);

        Assert.assertEquals(lastName + ", " + firstName, lastFirstName);
    }

    @Test
    public void getGender_shouldReturnCorrectGender() throws Exception {
        String firstName = "Derrick";
        String lastName = "Junior";
        String dob = "13/02/1989";
        String gender = "F";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        String fetchedGender = patientService.getGender(pat);

        Assert.assertEquals(gender, fetchedGender);
    }

    @Test
    public void getLocalizedGender_shouldReturnCorrectLocalizedGender() throws Exception {
        String firstName = "Tayebwa";
        String lastName = "Noah";
        String dob = "01/01/2020";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);

        String patientId = patientService.insert(pat);

        String localizedGender = patientService.getLocalizedGender(pat);

        Assert.assertEquals("MALE", localizedGender);
    }

    @Test
    public void getBirthdayForDisplay_shouldReturnBirthdayForDisplay() throws Exception {
        String firstName = "Tayebwa";
        String lastName = "Noah";
        String dob = "01/01/2020";
        String gender = "M";
        Patient pat = createPatient(firstName, lastName, dob, gender);
        pat.setBirthDateForDisplay("01/01/2020");

        Assert.assertEquals(dob, patientService.getBirthdayForDisplay(pat));
    }

    @Test
    public void getPageOfPatients_shouldReturnCorrectPatients() throws Exception {
        String firstName1 = "Josh";
        String lastName1 = "Nsereko";
        String dob1 = "20/03/1980";
        String gender1 = "M";
        Patient pat1 = createPatient(firstName1, lastName1, dob1, gender1);

        String firstName2 = "John";
        String lastName2 = "Stewart";
        String dob2 = "22/04/1982";
        String gender2 = "M";
        Patient pat2 = createPatient(firstName2, lastName2, dob2, gender2);

        patientService.insert(pat1);
        patientService.insert(pat2);

        List<Patient> patientsPage = patientService.getPageOfPatients(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        Assert.assertTrue(patientsPage.size() <= expectedPageSize);

        if (expectedPageSize >= 2) {
            Assert.assertTrue(patientsPage.stream().anyMatch(p -> p.getPerson().getFirstName().equals(firstName1)));
            Assert.assertTrue(patientsPage.stream().anyMatch(p -> p.getPerson().getFirstName().equals(firstName2)));
        }
    }

}
