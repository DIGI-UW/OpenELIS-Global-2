package org.openelisglobal.orderentry.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.orderentry.dao.PossibleMatchOverrideDAO;
import org.openelisglobal.orderentry.service.PossibleMatchScorer.Scored;
import org.openelisglobal.orderentry.valueholder.PossibleMatchOverride;
import org.springframework.beans.factory.annotation.Autowired;

public class PossibleMatchServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PATIENT_ID = "987650";
    private static final String PROVIDER_ID = "987660";
    private static final String FACILITY_ID = "987670";

    @Autowired
    private PossibleMatchService possibleMatchService;

    @Autowired
    private PossibleMatchOverrideDAO overrideDAO;

    @Before
    public void setUpFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/possible-match-service.xml");
    }

    @Test
    public void patients_withSwappedFirstAndLastNames_shouldReturnMatchOnName() {
        List<Scored> matches = possibleMatchService.patients("Moreaqx", "Kilavuz", null, null);

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(PATIENT_ID, match.getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.NAME), match.getMatchedOn());
        assertEquals(LocalDate.of(1985, 3, 2), match.getCandidate().getBirthDate());
        assertEquals("Kilavuz", match.getCandidate().getFirstName());
        assertEquals("Moreaqx", match.getCandidate().getLastName());
    }

    @Test
    public void patients_withMisspelledNameAndBirthDateInSameYear_shouldMatchOnNameAndBirthDate() {
        List<Scored> matches = possibleMatchService.patients("Kilavus", "Moreaqz", LocalDate.of(1985, 9, 30), null);

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(PATIENT_ID, match.getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.NAME, PossibleMatchScorer.DATE_OF_BIRTH), match.getMatchedOn());
        assertEquals("Kilavuz", match.getCandidate().getFirstName());
        assertEquals("Moreaqx", match.getCandidate().getLastName());
        assertEquals(LocalDate.of(1985, 3, 2), match.getCandidate().getBirthDate());
    }

    @Test
    public void patients_withIdentifierOneCharacterOff_shouldMatchOnIdentifier() {
        List<Scored> matches = possibleMatchService.patients("Zebedee", "Quorn", null, "PNG8812346");

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(PATIENT_ID, match.getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.IDENTIFIER), match.getMatchedOn());
        assertEquals("PNG8812345", match.getCandidate().getIdentifier());
        assertEquals("Kilavuz", match.getCandidate().getFirstName());
        assertEquals("Moreaqx", match.getCandidate().getLastName());
    }

    @Test
    public void patients_withUnrelatedValues_shouldReturnEmptyList() {
        List<Scored> matches = possibleMatchService.patients("Xyzzyq", "Plughwv", LocalDate.of(1950, 1, 1), "OTHER999");

        assertEquals(0, matches.size());
    }

    @Test
    public void facilities_withMatchingNameIgnoringCommonWords_shouldMatchOnName() {
        List<Scored> matches = possibleMatchService.facilities("Tokaravu health center", null);

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(FACILITY_ID, match.getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.NAME), match.getMatchedOn());
        assertEquals("Tokaravu Health Centre", match.getCandidate().getName());
        assertEquals("TKV1", match.getCandidate().getCode());
        assertEquals("Port Moresby", match.getCandidate().getCity());
        assertTrue(match.getCandidate().isActive());
    }

    @Test
    public void facilities_withMatchingCode_shouldMatchOnCode() {
        List<Scored> matches = possibleMatchService.facilities(null, "TKV1");

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(FACILITY_ID, match.getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.CODE), match.getMatchedOn());
        assertEquals("Tokaravu Health Centre", match.getCandidate().getName());
        assertEquals("TKV1", match.getCandidate().getCode());
        assertEquals("Port Moresby", match.getCandidate().getCity());
        assertTrue(match.getCandidate().isActive());
    }

    @Test
    public void facilities_withBothNameAndCodeBlank_shouldReturnEmptyListImmediately() {
        List<Scored> matches = possibleMatchService.facilities("", "   ");

        assertEquals(0, matches.size());
    }

    @Test
    public void facilities_withUnrelatedNameAndCode_shouldReturnEmptyList() {
        List<Scored> matches = possibleMatchService.facilities("Unrelated Clinic Nowhere", "NON999");

        assertEquals(0, matches.size());
    }

    @Test
    public void providers_withSimilarName_shouldMatchActiveProviderWithTitle() {
        List<Scored> matches = possibleMatchService.providers("Agnes", "Tauvoq");

        assertEquals(1, matches.size());
        Scored match = matches.get(0);
        assertEquals(PROVIDER_ID, match.getCandidate().getId());
        assertEquals("Agnesz", match.getCandidate().getFirstName());
        assertEquals("Tauvoq", match.getCandidate().getLastName());
        assertEquals("Dr", match.getCandidate().getCode());
        assertEquals(List.of(PossibleMatchScorer.NAME), match.getMatchedOn());
    }

    @Test
    public void providers_withInactiveProvider_shouldNotMatchInactiveProvider() {
        List<Scored> matches = possibleMatchService.providers("Retired", "Surgeon");

        assertEquals(0, matches.size());
    }

    @Test
    public void recordOverride_withValidPatientOverride_shouldPersistAndReturnId() {
        String enteredJson = "{\"firstName\":\"Moreaqx\",\"lastName\":\"Kilavuz\"}";
        String matchesJson = "[{\"id\":\"987650\",\"matchedOn\":[\"name\"]}]";

        Integer overrideId = possibleMatchService.recordOverride("patient", enteredJson, matchesJson, "1");

        PossibleMatchOverride saved = overrideDAO.get(overrideId).orElseThrow();
        assertEquals(overrideId, saved.getId());
        assertEquals("patient", saved.getRecordKind());
        assertEquals(enteredJson, saved.getEnteredValues());
        assertEquals(matchesJson, saved.getMatchesShown());
        assertEquals(Integer.valueOf(1), saved.getRecordedById());
    }

    @Test
    public void recordOverride_withTrimmedCaseInsensitiveKind_shouldNormalizeAndPersist() {
        String enteredJson = "{\"name\":\"Clinic\"}";
        String matchesJson = "[]";

        Integer overrideId = possibleMatchService.recordOverride("  FACILITY  ", enteredJson, matchesJson, "2");

        PossibleMatchOverride saved = overrideDAO.get(overrideId).orElseThrow();
        assertEquals(overrideId, saved.getId());
        assertEquals("facility", saved.getRecordKind());
        assertEquals(enteredJson, saved.getEnteredValues());
        assertEquals(matchesJson, saved.getMatchesShown());
        assertEquals(Integer.valueOf(2), saved.getRecordedById());
    }

    @Test
    public void recordOverride_withUnknownKind_shouldThrowOrderEntryRequestRefusedException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> possibleMatchService.recordOverride("vehicle", "{}", "[]", "1"));
        assertEquals("unknown record kind: vehicle", thrown.getMessage());

        OrderEntryRequestRefusedException nullThrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> possibleMatchService.recordOverride(null, "{}", "[]", "1"));
        assertEquals("unknown record kind: null", nullThrown.getMessage());
    }
}
