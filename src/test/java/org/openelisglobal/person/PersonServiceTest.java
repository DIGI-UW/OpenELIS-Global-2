package org.openelisglobal.person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class PersonServiceTest extends BaseWebContextSensitiveTest {
    private static final String PERSON1_FIRSTNAME = "John";
    private static final String PERSON1_LASTNAME = "Doe";
    private static final String CITY = "Kampala";
    private static final String STATE = "Kampala metropolitan";
    private static final String ZIPCODE = "256";
    private static final String COUNTRY = "Uganda";

    @Autowired
    PersonService personService;

    @Autowired
    PatientService patientService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/person.xml");
        resetSequence("PERSON", "ID", "person_seq");
        resetSequence("PATIENT", "ID", "patient_seq");
    }

    @After
    public void cleanup() throws Exception {
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @Test
    public void createPerson_shouldCreatePerson() throws Exception {
        Person person = new Person();
        person.setFirstName(PERSON1_FIRSTNAME);
        person.setLastName(PERSON1_LASTNAME);

        Person savedPerson = personService.save(person);

        Assert.assertEquals(PERSON1_FIRSTNAME, savedPerson.getFirstName());

        personService.delete(savedPerson);
    }

    // @Test
    // @Transactional
    // @SuppressWarnings("unchecked")
    // public void createPersonWithMultiplePatients_shouldLinkPatientsToPerson()
    // throws Exception {

    // Person person = new Person();
    // String personId = personService.insert(person);
    // Person savedPerson = personService.get(personId);

    // Patient patient1 = patientService.get("1");
    // Patient patient2 = patientService.get("2");

    // savedPerson.addPatient(patient1);
    // savedPerson.addPatient(patient2);

    // Set<Patient> patients = personService.get(savedPerson.getId()).getPatients();
    // Assert.assertEquals(2, patients.size());
    // Assert.assertTrue(patients.stream().anyMatch(p -> p.getId().equals("1")));
    // Assert.assertTrue(patients.stream().anyMatch(p -> p.getId().equals("2")));
    // for (Patient patient : patients) {
    // Assert.assertEquals(savedPerson.getId(), patient.getPerson().getId());
    // }
    // }

    @Test
    public void getAllPerson_shouldGetAllPerson() throws Exception {
        Assert.assertEquals(3, personService.getAllPersons().size());
    }

    @Test
    public void getLastName_shouldReturnEmptyStringForNullPerson() {
        String retrievedLastName = personService.getLastName(null);

        Assert.assertEquals("", retrievedLastName);
    }

    @Test
    public void getLastName_shouldReturnLastName() {
        Person person = personService.get("1");

        String retrievedLastName = personService.getLastName(person);

        Assert.assertEquals(PERSON1_LASTNAME, retrievedLastName);
    }

    @Test
    public void getFirstName_shouldReturnFirstName() {
        Person person = personService.get("1");
        String retrievedFirstName = personService.getFirstName(person);

        Assert.assertEquals(PERSON1_FIRSTNAME, retrievedFirstName);
    }

    @Test
    public void getLastFirstName_shouldReturnCorrectFormat() throws Exception {
        Person person = personService.get("1");
        String lastFirstName = personService.getLastFirstName(person);

        Assert.assertEquals(PERSON1_LASTNAME + ", " + PERSON1_FIRSTNAME, lastFirstName);
    }

    @Test
    public void getWorkPhone_shouldReturnWorkPhone() throws Exception {
        Person person = personService.get("1");
        Assert.assertEquals("12345678", personService.getWorkPhone(person));
    }

    @Test
    public void getCellPhone_shouldReturnCellPhone() throws Exception {
        Person person = personService.get("1");
        Assert.assertEquals("09785432", personService.getCellPhone(person));
    }

    @Test
    public void getFax_shouldReturnFax() throws Exception {
        Person person = personService.get("1");
        Assert.assertEquals("3456", personService.getFax(person));
    }

    @Test
    public void getPersonById_shouldReturngetPersonById() throws Exception {
        Person person = personService.get("2");
        Assert.assertEquals("USA", person.getCountry());
        Assert.assertEquals("Mulizi", person.getLastName());
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() throws Exception {
        Person personToUpdate = new Person();
        personToUpdate.setId("3");

        personService.getData(personToUpdate);

        assertNotNull(personToUpdate.getId());
        assertEquals("0002", personToUpdate.getFax());
    }

    @Test
    public void getData_shouldReturnpersonNotFound() throws Exception {
        Person personToUpdate = new Person();
        personToUpdate.setId("345");

        personService.getData(personToUpdate);

        assertNull(personToUpdate.getId());
    }

    @Test
    public void getPageOfPersons_shouldReturnPageOfPersons() {

        List<Person> personsPage = personService.getPageOfPersons(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        Assert.assertTrue(personsPage.size() <= expectedPageSize);

        if (expectedPageSize >= 3) {
            Assert.assertTrue(personsPage.stream().anyMatch(p -> p.getFirstName().equals("John")));
            Assert.assertTrue(personsPage.stream().anyMatch(p -> p.getFirstName().equals("James")));
            Assert.assertTrue(personsPage.stream().anyMatch(p -> p.getFirstName().equals("Faith")));
        }
    }

    @Test
    public void getData_shouldRetrieveDataForPerson() throws Exception {

        String firstName = "John";
        String lastName = "Doe";

        Person savedPerson = new Person();
        savedPerson.setId("1");
        personService.getData(savedPerson);

        Assert.assertEquals(firstName, savedPerson.getFirstName());
        Assert.assertEquals(lastName, savedPerson.getLastName());
    }

    @Test
    public void getEmail_shouldReturnCorrectEmail() throws Exception {
        Person savedPerson = personService.get("3");

        String retrievedEmail = personService.getEmail(savedPerson);

        Assert.assertEquals("siannah@gmail.com", retrievedEmail);
    }

    @Test
    public void updatePerson_shouldUpdatePersonInformation() throws Exception {
        Person savedPerson = personService.get("1");

        savedPerson.setCity("Los Angeles");
        savedPerson.setStreetAddress("456 Oak St");
        savedPerson.setSysUserId("admin");
        personService.update(savedPerson);

        Person updatedPerson = personService.get("1");

        Assert.assertEquals("Los Angeles", updatedPerson.getCity());
        Assert.assertEquals("456 Oak St", updatedPerson.getStreetAddress());
    }

    @Test
    public void getPhone_shouldReturnCorrectPhoneNumber() throws Exception {
        Person savedPerson = personService.get("1");

        String phoneNumber = personService.getPhone(savedPerson);
        Assert.assertEquals("12345678", phoneNumber);
    }

    @Test
    public void getPersonByLastName_shouldReturnCorrectPerson() throws Exception {
        Person retrievedPerson = personService.getPersonByLastName(PERSON1_LASTNAME);
        Assert.assertNotNull(retrievedPerson);
        Assert.assertEquals(PERSON1_FIRSTNAME, retrievedPerson.getFirstName());
        Assert.assertEquals(PERSON1_LASTNAME, retrievedPerson.getLastName());
    }

    @Test
    public void getAddressComponents_shouldReturngetAddressComponents() throws Exception {
        Person person = personService.get("1");

        Map<String, String> result = personService.getAddressComponents(person);
        assertEquals(CITY, result.get("City"));
        assertEquals(COUNTRY, result.get("Country"));
        assertEquals(STATE, result.get("State"));
        assertEquals(ZIPCODE, result.get("Zip"));
    }

    @Test
    public void testGetAddressComponents_handlesNullPerson() {
        Map<String, String> result = personService.getAddressComponents(null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void deletePerson_shouldDeletePerson() {
        Person savedPerson = personService.get("3");
        savedPerson.setSysUserId("admin");

        personService.delete(savedPerson);

        Assert.assertThrows(RuntimeException.class, () -> personService.get("3"));
    }
}