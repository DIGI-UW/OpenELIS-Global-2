package org.openelisglobal.search;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Patient search over names written with diacritics, as Vietnamese names are.
 * Users type them with or without the accents, in any case, so the search has
 * to ignore both. Vietnamese given names are usually several short words that
 * share a common one (most women's include "Thị"), which is also what tests
 * that a search for one name does not return everybody who shares a word of it.
 */
public class PatientSearchAccentTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Autowired
    private SearchResultsService searchResultsService;

    private String thiBeId;

    private String thiBeNhungId;

    private String thiDutId;

    private String thiHueId;

    private String tranId;

    private String tramId;

    private String dangId;

    private String lowerCaseId;

    @Before
    public void seedPatients() throws Exception {
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });
        PatientIdentityTypeMap.reset();

        thiBeId = createPatient("THỊ BÉ", "NGUYỄN");
        thiBeNhungId = createPatient("THỊ BÉ NHUNG", "NGUYỄN");
        thiDutId = createPatient("THỊ DỨT", "NGUYỄN");
        thiHueId = createPatient("THỊ HUỆ", "NGUYỄN");
        tranId = createPatient("VĂN AN", "TRẦN");
        tramId = createPatient("KIM NHI", "TRẦM");
        dangId = createPatient("VĂN ĐỨC", "ĐẶNG");
        lowerCaseId = createPatient("thị hoa", "lê");
    }

    // ==================== accents and case are ignored ====================

    @Test
    public void search_byLastNameWithoutAccents_findsTheAccentedName() {
        List<String> expected = sorted(thiBeId, thiBeNhungId, thiDutId, thiHueId);

        Assert.assertEquals(expected, sortedIdsOf(searchByLastName("nguyen")));
        Assert.assertEquals(expected, sortedIdsOf(searchByLastName("NGUYEN")));
        Assert.assertEquals(expected, sortedIdsOf(searchByLastName("Nguyễn")));
        Assert.assertEquals(expected, sortedIdsOf(searchByLastName("NGUYỄN")));
    }

    @Test
    public void search_byShortLastNameWithoutAccents_findsTheAccentedName() {
        Assert.assertTrue(idsOf(searchByLastName("tran")).contains(tranId));
        Assert.assertTrue(idsOf(searchByLastName("TRAN")).contains(tranId));
    }

    @Test
    public void search_byTwoLetterTermWithoutAccents_findsTheAccentedName() {
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(searchByFirstName("be")));
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(searchByFirstName("bé")));
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(searchByFirstName("BÉ")));
    }

    @Test
    public void search_byLetterWithStroke_findsIt() {
        // Đ is a letter of its own rather than D with an accent, but nobody can be
        // expected to type it to find a patient.
        Assert.assertEquals(Arrays.asList(dangId), idsOf(searchByLastName("dang")));
        Assert.assertEquals(Arrays.asList(dangId), idsOf(searchByFirstName("duc")));
    }

    @Test
    public void search_byAccentedUpperCaseTerm_findsNameStoredInLowerCase() {
        Assert.assertEquals(Arrays.asList(lowerCaseId), idsOf(searchByLastName("LÊ")));
        Assert.assertEquals(Arrays.asList(lowerCaseId), idsOf(searchByLastName("le")));
    }

    @Test
    public void search_byFullNameWithoutAccents_findsThePatient() {
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(search("nguyen", "thi be")));
    }

    // ==================== a shared word is not a misspelling ====================

    @Test
    public void search_byFullName_doesNotReturnPatientsWhoOnlyShareAWordOfIt() {
        // "Thị Dứt" and "Thị Huệ" share "Thị" with "Thị Bé" and nothing else. Over
        // the whole value that is enough trigram similarity to pass as a misspelling.
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(search("NGUYỄN", "THỊ BÉ")));
        Assert.assertEquals(sorted(thiBeId, thiBeNhungId), sortedIdsOf(searchByFirstName("thị bé")));
        Assert.assertEquals(Arrays.asList(thiHueId), idsOf(search("nguyen", "thi hue")));
    }

    @Test
    public void search_byMultiWordNameWithAMisspelledWord_stillMatches() {
        Assert.assertEquals(Arrays.asList(thiHueId), idsOf(search("nguyen", "thi heu")));
        Assert.assertEquals(Arrays.asList(thiBeNhungId), idsOf(searchByFirstName("thi be nhugn")));
    }

    @Test
    public void search_byMisspelledNameWithoutAccents_stillMatches() {
        Assert.assertTrue(idsOf(searchByLastName("nguyne")).contains(thiBeId));
        Assert.assertTrue(idsOf(searchByLastName("tarn")).contains(tranId));
    }

    @Test
    public void search_byOneLetterDifference_stillFindsTheNeighbouringName() {
        // Trầm is one letter from Trần; that is the misspelling tolerance working.
        Assert.assertTrue(idsOf(searchByLastName("tran")).contains(tramId));
    }

    // ==================== helpers ====================

    private List<PatientSearchResults> searchByFirstName(String firstName) {
        return search(null, firstName);
    }

    private List<PatientSearchResults> searchByLastName(String lastName) {
        return search(lastName, null);
    }

    private List<PatientSearchResults> search(String lastName, String firstName) {
        return searchResultsService.getSearchResults(lastName, firstName, null, null, null, null, null, null, null,
                null);
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

    private String createPatient(String firstName, String lastName) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setSysUserId("1");
        personService.save(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setBirthDate(new Timestamp(0));
        patient.setGender("F");
        patient.setSysUserId("1");
        return patientService.insert(patient);
    }
}
