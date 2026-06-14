package org.openelisglobal.search;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.search.mapper.orm.Search;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseCommittedFixtureTest;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lucene/Hibernate-Search variants of the patient search tests. These belong on
 * the committed base, not the rollback base: the patient is created through
 * {@code patientService.insert} (the Hibernate-indexed path), but Hibernate
 * Search applies its index updates on transaction COMMIT — which per-test
 * rollback never performs, leaving the index empty and every search returning
 * 0. {@code NOT_SUPPORTED} commits each write so it is indexed, and we rebuild
 * the in-heap index with a mass indexer ({@link #reindexPatients()}) after each
 * insert so it stays consistent with the raw-JDBC cleanup (which Hibernate
 * Search doesn't observe).
 *
 * <p>
 * The plain-SQL search variants stay on the rollback base in
 * {@link SearchResultsServiceTest} — they query the DB directly and need no
 * index.
 */
@RunWith(JUnitParamsRunner.class)
public class LuceneSearchResultsServiceTest extends BaseCommittedFixtureTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    PatientService patientService;

    @Autowired
    PersonService personService;

    @Autowired
    @Qualifier("luceneSearchResultsServiceImpl")
    SearchResultsService luceneSearchResultsServiceImpl;

    @Autowired
    private PlatformTransactionManager txManager;

    @Before
    public void seedAuditReferenceTables() throws Exception {
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
    }

    /**
     * Rebuild the in-heap Lucene index from committed DB state. Needed because the
     * raw-JDBC cleanup ({@link #cleanRowsInCurrentConnection}) bypasses Hibernate
     * Search, so stale entries from previous parameterized runs would otherwise
     * linger; a purge-and-rebuild keeps the index == current DB. Wrapped in an
     * explicit transaction because this committed base is NOT_SUPPORTED, and
     * {@code Search.session} requires a transactional EntityManager.
     */
    private void reindexPatients() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            try {
                Search.session(entityManager).massIndexer().startAndWait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Reindex interrupted", e);
            }
        });
    }

    /**
     * The tests write person/patient via the service (committed); clear them
     * between methods.
     */
    @Override
    protected String[] additionalCommittedTablesToClean() {
        return new String[] { "patient", "person" };
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
    public void getSearchResults_shouldGetSearchResultsFromLuceneIndexes(String searchFirstName, String searchLastName,
            String searchDateOfBirth, String searchGender) throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "patient" });

        String firstName = "John";
        String lastname = "Doe";
        String dob = "12/12/1992";
        String gender = "M";
        Patient pat = createPatient(firstName, lastname, dob, gender);
        String patientId = patientService.insert(pat);
        reindexPatients();

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
        reindexPatients();

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

    private Patient createPatient(String firstName, String lastName, String birthDate, String gender)
            throws ParseException {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setSysUserId("1");
        personService.save(person);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(birthDate);
        Timestamp dob = new Timestamp(date.getTime());

        Patient pat = new Patient();
        pat.setPerson(person);
        pat.setBirthDate(dob);
        pat.setGender(gender);
        pat.setSysUserId("1");
        return pat;
    }
}
