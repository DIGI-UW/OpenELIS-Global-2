package org.openelisglobal.common.provider.query;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class PatientNamePrefixMatchTest {

    private static PatientSearchResults patient(String firstName, String lastName) {
        PatientSearchResults result = new PatientSearchResults();
        result.setFirstName(firstName);
        result.setLastName(lastName);
        return result;
    }

    private static final List<PatientSearchResults> FOUND = List.of(patient("John", "Smith"),
            patient("Jane", "Smithson"), patient("John", "TEST-Smith"), patient("Joan", "Smyth"),
            patient(null, "Smith"));

    private static List<String> names(List<PatientSearchResults> results) {
        return results.stream().map(r -> r.getFirstName() + " " + r.getLastName()).collect(Collectors.toList());
    }

    @Test
    public void keepsExactAndPrefixLastNamesOnly() {
        assertEquals(List.of("John Smith", "Jane Smithson", "null Smith"),
                names(PatientNamePrefixMatch.keepPrefixMatches(FOUND, "Smith", null)));
    }

    @Test
    public void ignoresCaseAndSurroundingSpaces() {
        assertEquals(List.of("John Smith", "Jane Smithson", "null Smith"),
                names(PatientNamePrefixMatch.keepPrefixMatches(FOUND, "  sMiTh ", "")));
    }

    @Test
    public void appliesBothNamesWhenBothAreGiven() {
        assertEquals(List.of("John Smith"), names(PatientNamePrefixMatch.keepPrefixMatches(FOUND, "smi", "jo")));
    }

    @Test
    public void aFirstNameAloneMatchesByPrefixToo() {
        assertEquals(List.of("John Smith", "John TEST-Smith", "Joan Smyth"),
                names(PatientNamePrefixMatch.keepPrefixMatches(FOUND, "", "Jo")));
    }

    @Test
    public void blankNamesLeaveTheResultsAsTheyAre() {
        assertEquals(FOUND, PatientNamePrefixMatch.keepPrefixMatches(FOUND, " ", null));
    }
}
