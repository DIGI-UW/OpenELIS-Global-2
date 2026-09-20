package org.openelisglobal.search;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Patient search behaviour that has to hold for every field the form offers:
 * short terms keep matching anywhere inside a value, misspelled names still
 * find their patient, and a whole-value search stays strict.
 */
public class PatientSearchFuzzyTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private SearchResultsService searchResultsService;

    private String johnId;

    private String mariaId;

    private String anaId;

    private String obrienId;

    private String vanDerBergId;

    private String joseId;

    private String namelessId;

    @Before
    public void seedPatients() throws Exception {
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });
        // PatientIdentityTypeMap caches type ids in a JVM-wide singleton; a sibling
        // test that reloads patient_identity_type leaves those ids dangling.
        PatientIdentityTypeMap.reset();
        resyncSequence("patient_identity_seq", "patient_identity");

        johnId = createPatient("John", "Doe", "12/12/1992", "M", "NAT-1001", "EXT-2001");
        addIdentity(johnId, "ST", "ST-3001");
        addIdentity(johnId, "SUBJECT", "SUBJ-4001");
        addIdentity(johnId, "GUID", "GUID-5001");

        mariaId = createPatient("Maria", "Gomez", "05/06/1985", "F", "NAT-1002", "EXT-2002");
        addIdentity(mariaId, "ST", "ST-3002");
        addIdentity(mariaId, "SUBJECT", "SUBJ-4002");
        addIdentity(mariaId, "GUID", "GUID-5002");

        anaId = createPatient("Ana", "Doe", "12/12/1992", "F", "NAT-1003", "EXT-2003");
        obrienId = createPatient("Sean", "O'Brien", "03/03/1975", "M", "NAT-1004", null);
        vanDerBergId = createPatient("Jan", "Van Der Berg", "04/04/1980", "M", "NAT-1005", null);
        joseId = createPatient("Jose", "Muñoz", "05/05/1988", "M", null, null);
        namelessId = createPatient(null, null, "06/06/1970", "F", "NAT-1007", null);
    }

    // ==================== short terms keep matching ====================

    @Test
    public void search_bySingleCharacterFirstName_matchesAnywhereInTheValue() {
        Assert.assertEquals(sorted(johnId, vanDerBergId, joseId), sortedIdsOf(searchByFirstName("j")));
    }

    @Test
    public void search_bySingleCharacterLastName_matchesEveryPatientContainingIt() {
        Assert.assertEquals(sorted(johnId, anaId, vanDerBergId), sortedIdsOf(searchByLastName("d")));
    }

    @Test
    public void search_byTwoCharacterFirstName_matchesAnywhereInTheValue() {
        Assert.assertEquals(sorted(johnId, joseId), sortedIdsOf(searchByFirstName("jo")));
    }

    @Test
    public void search_byThreeCharacterFirstName_matchesAnywhereInTheValue() {
        Assert.assertEquals(Arrays.asList(mariaId), idsOf(searchByFirstName("mar")));
    }

    @Test
    public void search_byTermInsideTheName_matches() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("ohn")));
        Assert.assertEquals(sorted(johnId, anaId), sortedIdsOf(searchByLastName("oe")));
    }

    @Test
    public void search_byName_isCaseInsensitive() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("JOHN")));
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("jOhN")));
    }

    @Test
    public void search_byShortTerm_doesNotDragInUnrelatedNames() {
        Assert.assertEquals(sorted(johnId, anaId), sortedIdsOf(searchByLastName("do")));
    }

    // ==================== misspellings ====================

    @Test
    public void search_byTransposedFirstName_stillMatches() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("Jhon")));
    }

    @Test
    public void search_byTransposedLastName_stillMatches() {
        Assert.assertEquals(sorted(johnId, anaId), sortedIdsOf(searchByLastName("Deo")));
    }

    @Test
    public void search_bySubstitutedLetter_stillMatches() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("Johm")));
    }

    @Test
    public void search_byDoubledLetter_stillMatches() {
        Assert.assertEquals(sorted(johnId, anaId), sortedIdsOf(searchByLastName("Doee")));
    }

    @Test
    public void search_byMisspelledLongerName_stillMatches() {
        Assert.assertEquals(Arrays.asList(mariaId), idsOf(searchByFirstName("Marria")));
    }

    @Test
    public void search_byNameThatResemblesNothing_returnsNothing() {
        Assert.assertTrue(searchByFirstName("Zzzzzzq").isEmpty());
    }

    // ==================== every remaining criterion ====================

    @Test
    public void search_byPartialNationalId_matches() {
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search(null, null, null, null, "1001", null, null, null, null, null)));
    }

    @Test
    public void search_byPartialExternalId_matches() {
        Assert.assertEquals(Arrays.asList(mariaId),
                idsOf(search(null, null, null, null, null, "2002", null, null, null, null)));
    }

    @Test
    public void search_byPartialSTNumber_matches() {
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search(null, null, "3001", null, null, null, null, null, null, null)));
    }

    @Test
    public void search_byPartialSubjectNumber_matches() {
        Assert.assertEquals(Arrays.asList(mariaId),
                idsOf(search(null, null, null, "4002", null, null, null, null, null, null)));
    }

    @Test
    public void search_bySingleCharacterSubjectNumber_matchesEveryPatientContainingIt() {
        Assert.assertEquals(sorted(johnId, mariaId),
                sortedIdsOf(search(null, null, null, "4", null, null, null, null, null, null)));
    }

    @Test
    public void search_byGuid_matchesExactly() {
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search(null, null, null, null, null, null, null, "GUID-5001", null, null)));
        Assert.assertTrue(search(null, null, null, null, null, null, null, "GUID-500", null, null).isEmpty());
    }

    @Test
    public void search_byPatientId_matchesExactly() {
        Assert.assertEquals(Arrays.asList(mariaId),
                idsOf(search(null, null, null, null, null, null, mariaId, null, null, null)));
    }

    @Test
    public void search_byGender_matches() {
        Assert.assertEquals(sorted(mariaId, anaId, namelessId),
                sortedIdsOf(search(null, null, null, null, null, null, null, null, null, "F")));
    }

    @Test
    public void search_byPartialDateOfBirth_matches() {
        Assert.assertEquals(sorted(johnId, anaId),
                sortedIdsOf(search(null, null, null, null, null, null, null, null, "1992", null)));
    }

    @Test
    public void search_byFullDateOfBirth_matches() {
        Assert.assertEquals(sorted(johnId, anaId),
                sortedIdsOf(search(null, null, null, null, null, null, null, null, "12/12/1992", null)));
    }

    /**
     * The search form sends the one value typed into "Patient Id" as the ST number,
     * the subject number and the national id at once, so a match on any one of them
     * has to return the patient.
     */
    @Test
    public void search_byOneValueAcrossEveryIdentifierParameter_matchesAnyOfThem() {
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search(null, null, "ST-3001", "ST-3001", "ST-3001", null, null, null, null, null)));
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search(null, null, "SUBJ-4001", "SUBJ-4001", "SUBJ-4001", null, null, null, null, null)));
        Assert.assertEquals(Arrays.asList(mariaId),
                idsOf(search(null, null, "NAT-1002", "NAT-1002", "NAT-1002", null, null, null, null, null)));
    }

    @Test
    public void search_byNameAndGenderAndDateOfBirth_narrowsToTheSinglePatient() {
        Assert.assertEquals(Arrays.asList(johnId),
                idsOf(search("do", "jo", null, null, null, null, null, null, "1992", "M")));
    }

    @Test
    public void search_withNoCriteria_returnsNothing() {
        Assert.assertTrue(search(null, null, null, null, null, null, null, null, null, null).isEmpty());
    }

    // ==================== awkward but real names ====================

    @Test
    public void search_byNameContainingAnApostrophe_matches() {
        Assert.assertEquals(Arrays.asList(obrienId), idsOf(searchByLastName("O'Brien")));
        Assert.assertEquals(Arrays.asList(obrienId), idsOf(searchByLastName("o'brien")));
        Assert.assertEquals(Arrays.asList(obrienId), idsOf(searchByLastName("Brien")));
    }

    @Test
    public void search_byMisspelledNameContainingAnApostrophe_matches() {
        Assert.assertEquals(Arrays.asList(obrienId), idsOf(searchByLastName("O'Brein")));
    }

    @Test
    public void search_byOneWordOfAMultiWordName_matches() {
        Assert.assertEquals(Arrays.asList(vanDerBergId), idsOf(searchByLastName("Berg")));
        Assert.assertEquals(Arrays.asList(vanDerBergId), idsOf(searchByLastName("Der")));
    }

    @Test
    public void search_byAccentedName_matchesWithAndWithoutTheAccent() {
        Assert.assertEquals(Arrays.asList(joseId), idsOf(searchByLastName("Muñoz")));
        Assert.assertEquals(Arrays.asList(joseId), idsOf(searchByLastName("muñoz")));
    }

    @Test
    public void search_doesNotFallOverOnPatientsWithNoName() {
        // A row with a null name must simply not match, not break the query.
        Assert.assertFalse(idsOf(searchByLastName("a")).contains(namelessId));
        Assert.assertEquals(Arrays.asList(namelessId),
                idsOf(search(null, null, null, null, "NAT-1007", null, null, null, null, null)));
    }

    // ==================== awkward input ====================

    @Test
    public void search_ignoresSurroundingWhitespace() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("  John  ")));
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchByFirstName("  Jhon  ")));
    }

    @Test
    public void search_byBlankAndWhitespaceOnlyTerms_isTreatedAsNoCriterion() {
        Assert.assertTrue(searchByLastName("").isEmpty());
        Assert.assertTrue(searchByLastName("   ").isEmpty());
    }

    @Test
    public void search_byTermCarryingSqlLikeWildcards_doesNotThrow() {
        // '%' and '_' reach LIKE as wildcards, exactly as they always have; what
        // matters is that they cannot break the query.
        Assert.assertFalse(searchByLastName("%").isEmpty());
        Assert.assertNotNull(searchByLastName("_"));
        Assert.assertNotNull(searchByLastName("%_%"));
    }

    @Test
    public void search_byTermsCarryingSqlSyntax_staysHarmless() {
        Assert.assertTrue(searchByLastName("';drop table patient;--").isEmpty());
        Assert.assertTrue(searchByLastName("' or '1'='1").isEmpty());
        Assert.assertTrue(searchByFirstName("\\").isEmpty());
    }

    @Test
    public void search_byAnAbsurdlyLongTerm_doesNotBreakTheQuery() {
        String longTerm = new String(new char[400]).replace('\0', 'a');

        Assert.assertTrue(searchByLastName(longTerm).isEmpty());
        Assert.assertTrue(searchByFirstName(longTerm).isEmpty());
    }

    @Test
    public void search_byTermOfRepeatedLetters_hasNoTranspositionsAndStillWorks() {
        // adjacentSwaps() yields nothing for "aaa"; the query must still be valid.
        Assert.assertNotNull(searchByLastName("aaa"));
    }

    // ==================== whole-value search stays strict ====================

    @Test
    public void exactSearch_matchesWholeValuesOnly() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchResultsService.getSearchResultsExact("Doe", "John", null,
                null, null, null, null, null, null, null)));
        Assert.assertTrue(searchResultsService
                .getSearchResultsExact("Do", "Jo", null, null, null, null, null, null, null, null).isEmpty());
    }

    @Test
    public void exactSearch_doesNotTolerateMisspellings() {
        Assert.assertTrue(searchResultsService
                .getSearchResultsExact("Doee", "John", null, null, null, null, null, null, null, null).isEmpty());
        Assert.assertTrue(searchResultsService
                .getSearchResultsExact("Doe", "Jhon", null, null, null, null, null, null, null, null).isEmpty());
    }

    @Test
    public void exactSearch_isCaseInsensitive() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchResultsService.getSearchResultsExact("doe", "JOHN", null,
                null, null, null, null, null, null, null)));
    }

    @Test
    public void exactSearch_byNationalId_matchesWholeValueOnly() {
        Assert.assertEquals(Arrays.asList(johnId), idsOf(searchResultsService.getSearchResultsExact(null, null, null,
                null, "NAT-1001", null, null, null, null, null)));
        Assert.assertTrue(searchResultsService
                .getSearchResultsExact(null, null, null, null, "1001", null, null, null, null, null).isEmpty());
    }

    // ==================== helpers ====================

    private List<PatientSearchResults> searchByFirstName(String firstName) {
        return search(null, firstName, null, null, null, null, null, null, null, null);
    }

    private List<PatientSearchResults> searchByLastName(String lastName) {
        return search(lastName, null, null, null, null, null, null, null, null, null);
    }

    private List<PatientSearchResults> search(String lastName, String firstName, String stNumber, String subjectNumber,
            String nationalID, String externalID, String patientID, String guid, String dateOfBirth, String gender) {
        return searchResultsService.getSearchResults(lastName, firstName, stNumber, subjectNumber, nationalID,
                externalID, patientID, guid, dateOfBirth, gender);
    }

    private List<String> idsOf(List<PatientSearchResults> results) {
        return results.stream().map(PatientSearchResults::getPatientID).collect(Collectors.toList());
    }

    private List<String> sortedIdsOf(List<PatientSearchResults> results) {
        return idsOf(results).stream().sorted().collect(Collectors.toList());
    }

    private List<String> sorted(String... ids) {
        return Arrays.stream(ids).sorted().collect(Collectors.toList());
    }

    private String createPatient(String firstName, String lastName, String birthDate, String gender, String nationalId,
            String externalId) throws ParseException {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setMiddleName(null);
        person.setSysUserId("1");
        personService.save(person);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(birthDate);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setBirthDate(new Timestamp(date.getTime()));
        patient.setGender(gender);
        patient.setNationalId(nationalId);
        patient.setExternalId(externalId);
        patient.setSysUserId("1");

        return patientService.insert(patient);
    }

    private void addIdentity(String patientId, String identityType, String identityData) {
        PatientIdentity identity = new PatientIdentity();
        identity.setPatientId(patientId);
        identity.setIdentityTypeId(PatientIdentityTypeMap.getInstance().getIDForType(identityType));
        identity.setIdentityData(identityData);
        identity.setSysUserId("1");
        patientIdentityService.insert(identity);
    }
}
