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
    SearchResultsService DBSearchResultsServiceImpl;

    @Autowired
    @Qualifier("luceneSearchResultsServiceImpl")
    SearchResultsService luceneSearchResultsServiceImpl;

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResults_shouldGetSearchResultsFromDB() {
        return new Object[] { new Object[] { "Jo", "Do", "1992-12-12", "M" }, new Object[] { "Jo", null, null, null },
                new Object[] { null, "Do", null, null }, new Object[] { null, null, "1992-12-12", null },
                new Object[] { null, null, null, "M" } };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForGetSearchResultsExact_shouldGetExactSearchResultsFromDB() {
        return new Object[] { new Object[] { "John", "Doe", "1992-12-12", "M" },
                new Object[] { "John", null, null, null }, new Object[] { null, "Doe", null, null },
                new Object[] { null, null, "1992-12-12", null }, new Object[] { null, null, null, "M" } };
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

        executeDataSetWithStateManagement("testdata/patient-person-search.xml");

        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";

        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        assertSearchResult(result, "1", firstName, lastname, dob, gender);
    }

    @Test
    @Parameters
    public void getSearchResultsExact_shouldGetExactSearchResultsFromDB(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {

        executeDataSetWithStateManagement("testdata/patient-person-search.xml");

        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";

        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResultsExact(searchLastName,
                searchFirstName, null, null, null, null, null, null, searchDateOfBirth, searchGender);

        Assert.assertEquals(1, searchResults.size());
        PatientSearchResults result = searchResults.get(0);
        assertSearchResult(result, "1", firstName, lastname, dob, gender);
    }

    @Test
    @Parameters
    public void getSearchResults_shouldGetSearchResultsFromLuceneIndexes(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "patient" });

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
        cleanRowsInCurrentConnection(new String[] { "person", "patient" });

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

    private void assertSearchResult(PatientSearchResults result, String patientID, String firstName, String lastName,
            String birthdate, String gender) throws ParseException {
        Assert.assertEquals(patientID, result.getPatientID());
        Assert.assertEquals(firstName, result.getFirstName());
        Assert.assertEquals(lastName, result.getLastName());
        Assert.assertEquals(
                new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd/MM/yyyy").parse(birthdate)),
                result.getBirthdate().replace("Invalid date format: ", ""));
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

    // ==================== FR-015: Merged Patient Redirect Tests
    // ====================

    /**
     * FR-015: When searching by nationalID for a merged patient, should return the
     * primary patient instead.
     */
    @Test
    public void getSearchResults_shouldRedirectMergedPatientToPrimary_whenSearchingByNationalID() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });

        // Create primary patient
        Patient primaryPatient = createPatient("John", "Primary", "01/01/1990", "M");
        primaryPatient.setNationalId("PRIMARY-001");
        String primaryId = patientService.insert(primaryPatient);

        // Create merged patient with different national ID
        Patient mergedPatient = createPatient("Jane", "Merged", "01/01/1990", "F");
        mergedPatient.setNationalId("MERGED-001");
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryId);
        mergedPatient.setMergeDate(new Timestamp(System.currentTimeMillis()));
        patientService.insert(mergedPatient);

        // Search by merged patient's national ID
        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults(null, null, null, null,
                "MERGED-001", null, null, null, null, null);

        // Should return primary patient, not merged patient
        Assert.assertEquals(1, searchResults.size());
        Assert.assertEquals(primaryId, searchResults.get(0).getPatientID());
        Assert.assertEquals("John", searchResults.get(0).getFirstName());
        Assert.assertEquals("Primary", searchResults.get(0).getLastName());
    }

    /**
     * FR-015: When searching by name for a merged patient, should NOT redirect -
     * return the merged patient as-is. Redirect only applies to identifier-based
     * searches.
     */
    @Test
    public void getSearchResults_shouldNotRedirect_whenSearchingByName() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });

        // Create primary patient
        Patient primaryPatient = createPatient("John", "Primary", "01/01/1990", "M");
        primaryPatient.setNationalId("PRIMARY-001");
        String primaryId = patientService.insert(primaryPatient);

        // Create merged patient
        Patient mergedPatient = createPatient("Jane", "Merged", "01/01/1990", "F");
        mergedPatient.setNationalId("MERGED-001");
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryId);
        mergedPatient.setMergeDate(new Timestamp(System.currentTimeMillis()));
        String mergedId = patientService.insert(mergedPatient);

        // Search by merged patient's name - should return the merged patient (no
        // redirect)
        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults("Merged", "Jane", null,
                null, null, null, null, null, null, null);

        // Should return merged patient since we searched by name, not identifier
        Assert.assertEquals(1, searchResults.size());
        Assert.assertEquals(mergedId, searchResults.get(0).getPatientID());
        Assert.assertEquals("Jane", searchResults.get(0).getFirstName());
    }

    /**
     * FR-015: When searching returns both merged and primary patients, should
     * deduplicate to only return the primary patient once.
     */
    @Test
    public void getSearchResults_shouldDeduplicateWhenBothMergedAndPrimaryReturned() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });

        // Create primary patient with national ID that starts with "TEST"
        Patient primaryPatient = createPatient("John", "Primary", "01/01/1990", "M");
        primaryPatient.setNationalId("TEST-PRIMARY");
        String primaryId = patientService.insert(primaryPatient);

        // Create merged patient with national ID that also starts with "TEST"
        Patient mergedPatient = createPatient("Jane", "Merged", "01/01/1990", "F");
        mergedPatient.setNationalId("TEST-MERGED");
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryId);
        mergedPatient.setMergeDate(new Timestamp(System.currentTimeMillis()));
        patientService.insert(mergedPatient);

        // Search by partial national ID that matches both
        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults(null, null, null, null,
                "TEST", null, null, null, null, null);

        // Should return only the primary patient (deduplicated)
        Assert.assertEquals(1, searchResults.size());
        Assert.assertEquals(primaryId, searchResults.get(0).getPatientID());
    }

    /**
     * FR-015: Non-merged patients should be returned normally without any redirect.
     */
    @Test
    public void getSearchResults_shouldReturnNonMergedPatientNormally() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });

        // Create a normal (non-merged) patient
        Patient normalPatient = createPatient("Normal", "Patient", "01/01/1990", "M");
        normalPatient.setNationalId("NORMAL-001");
        String normalId = patientService.insert(normalPatient);

        // Search by national ID
        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResults(null, null, null, null,
                "NORMAL-001", null, null, null, null, null);

        // Should return the patient normally
        Assert.assertEquals(1, searchResults.size());
        Assert.assertEquals(normalId, searchResults.get(0).getPatientID());
        Assert.assertEquals("Normal", searchResults.get(0).getFirstName());
    }

    /**
     * FR-015: getSearchResultsExact should also redirect merged patients when
     * searching by identifier.
     */
    @Test
    public void getSearchResultsExact_shouldRedirectMergedPatientToPrimary_whenSearchingByNationalID()
            throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });

        // Create primary patient
        Patient primaryPatient = createPatient("John", "Primary", "01/01/1990", "M");
        primaryPatient.setNationalId("PRIMARY-EXACT");
        String primaryId = patientService.insert(primaryPatient);

        // Create merged patient
        Patient mergedPatient = createPatient("Jane", "Merged", "01/01/1990", "F");
        mergedPatient.setNationalId("MERGED-EXACT");
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryId);
        mergedPatient.setMergeDate(new Timestamp(System.currentTimeMillis()));
        patientService.insert(mergedPatient);

        // Search by merged patient's exact national ID
        List<PatientSearchResults> searchResults = DBSearchResultsServiceImpl.getSearchResultsExact(null, null, null,
                null, "MERGED-EXACT", null, null, null, null, null);

        // Should return primary patient
        Assert.assertEquals(1, searchResults.size());
        Assert.assertEquals(primaryId, searchResults.get(0).getPatientID());
    }
}
