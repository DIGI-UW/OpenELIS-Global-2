package org.openelisglobal.person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.valueholder.Patient;
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

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/person.xml");
    }

    @Test
    public void verifyTestData() {
        List<Person> personList = personService.getAll();
        System.out.println("Persons we have in db: " + personList.size());
        personList.forEach(person -> System.out.println(person.getId() + " - " + person.getFirstName()));
    }

    @Test
    public void createPerson_shouldCreateNewPerson() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "patient" });
        String firstName = "John";
        String lastname = "moe";

        Person pat = new Person();
        pat.setFirstName(firstName);
        pat.setLastName(lastname);

        String personIdId = personService.insert(pat);
        Person savedPerson = personService.get(personIdId);

        Assert.assertEquals(1, personService.getAllPersons().size());
        Assert.assertEquals(firstName, savedPerson.getFirstName());
        Assert.assertEquals(lastname, savedPerson.getLastName());
    }

    public Patient createPatient(String firstName, String LastName, String birthDate, String gender)
            throws ParseException {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(LastName);
        personService.get("1");

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
        // Create a new person
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
        Person savedPerson = personService.get("2");
        personService.delete(savedPerson);

        Throwable throwable = assertThrows(ObjectNotFoundException.class, () -> {
            personService.get("2");
        });

        assertEquals("No row with the given identifier exists: [org.openelisglobal.person.valueholder.Person#2]",
                throwable.getMessage());
    }

}