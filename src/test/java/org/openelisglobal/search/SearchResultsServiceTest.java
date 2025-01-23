package org.openelisglobal.search;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.*;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@RunWith(JUnitParamsRunner.class)
public class SearchResultsServiceTest extends BaseWebContextSensitiveTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    PatientService patientService;

    @Autowired
    PersonService personService;

    @Autowired
    @Qualifier("DBSearchResultsServiceImpl")
    SearchResultsService DBSearchResultsServiceImpl;

    @Autowired
    @Qualifier("luceneSearchResultsServiceImpl")
    SearchResultsService luceneSearchResultsServiceImpl;

    @Before
    public void init() throws Exception {
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @After
    public void tearDown() {
        patientService.deleteAll(patientService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResults_shouldGetSearchResultsFromDB() {
        return new Object[] { new Object[] { "Jo", "Do", "12/12/1992", "M" }, new Object[] { "Jo", null, null, null },
                new Object[] { null, "Do", null, null }, new Object[] { null, null, "12/12/1992", null },
                new Object[] { null, null, null, "M" } };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResultsExact_shouldGetExactSearchResultsFromDB() {
        return new Object[] { new Object[] { "John", "Doe", "12/12/1992", "M" },
                new Object[] { "John", null, null, null }, new Object[] { null, "Doe", null, null },
                new Object[] { null, null, "12/12/1992", null }, new Object[] { null, null, null, "M" } };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResults_shouldGetSearchResultsFromLuceneIndexes() {
        return new Object[] { new Object[] { "Johm", "Doee", "12/12/1992", "M" },
                new Object[] { "Johm", null, null, null }, new Object[] { null, "Doee", null, null },
                new Object[] { null, null, "12/12/1992", null }, new Object[] { null, null, null, "M" } };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResultsExact_shouldGetExactSearchResultsFromLuceneIndexes() {
        return new Object[] { new Object[] { "John", "Doe", "12/12/1992", "M" },
                new Object[] { "John", null, null, null }, new Object[] { null, "Doe", null, null },
                new Object[] { null, null, "12/12/1992", null }, new Object[] { null, null, null, "M" } };
    }

    @Test
    @Parameters
    public void getSearchResults_shouldGetSearchResultsFromDB(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(pat);

        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        Assert.assertEquals(patientId, result.getPatientID());
        Assert.assertEquals(firstName, result.getFirstName());
        Assert.assertEquals(lastname, result.getLastName());
        Assert.assertEquals(dob, result.getBirthdate());
        Assert.assertEquals(gender, result.getGender());
    }

    @Test
    @Parameters
    public void getSearchResultsExact_shouldGetExactSearchResultsFromDB(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(pat);

        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResultsExact(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        Assert.assertEquals(patientId, result.getPatientID());
        Assert.assertEquals(firstName, result.getFirstName());
        Assert.assertEquals(lastname, result.getLastName());
        Assert.assertEquals(dob, result.getBirthdate());
        Assert.assertEquals(gender, result.getGender());
    }

    @Test
    @Parameters
    public void getSearchResults_shouldGetSearchResultsFromLuceneIndexes(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(pat);

        List<PatientSearchResults> searchResults = luceneSearchResultsServiceImpl.getSearchResults(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        Assert.assertEquals(patientId, result.getPatientID());
        Assert.assertEquals(firstName, result.getFirstName());
        Assert.assertEquals(lastname, result.getLastName());
        Assert.assertEquals(dob, result.getBirthdate());
        Assert.assertEquals(gender, result.getGender());
    }

    @Test
    @Parameters
    public void getSearchResultsExact_shouldGetExactSearchResultsFromLuceneIndexes(String searchFirstName,
            String searchLastName, String searchDateOfBirth, String searchGender) throws Exception {
        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(pat);

        List<PatientSearchResults> searchResults = luceneSearchResultsServiceImpl.getSearchResultsExact(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        Assert.assertEquals(patientId, result.getPatientID());
        Assert.assertEquals(firstName, result.getFirstName());
        Assert.assertEquals(lastname, result.getLastName());
        Assert.assertEquals(dob, result.getBirthdate());
        Assert.assertEquals(gender, result.getGender());
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

}
