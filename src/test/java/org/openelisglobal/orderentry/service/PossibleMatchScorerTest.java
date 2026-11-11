package org.openelisglobal.orderentry.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.orderentry.service.PossibleMatchScorer.Scored;
import org.openelisglobal.orderentry.valueholder.PossibleMatchCandidate;

/**
 * OGC-1424 (FR-B6a): which existing records count as possible duplicates of a
 * new patient, facility or provider, and on what.
 */
public class PossibleMatchScorerTest {

    private static PossibleMatchCandidate person(String id, double lastToLast, double firstToFirst, double lastToFirst,
            double firstToLast, boolean lastSounds, boolean firstSounds) {
        PossibleMatchCandidate candidate = new PossibleMatchCandidate();
        candidate.setId(id);
        candidate.setLastToLast(lastToLast);
        candidate.setFirstToFirst(firstToFirst);
        candidate.setLastToFirst(lastToFirst);
        candidate.setFirstToLast(firstToLast);
        candidate.setLastSoundsAlike(lastSounds);
        candidate.setFirstSoundsAlike(firstSounds);
        return candidate;
    }

    private static PossibleMatchCandidate facility(String id, String name, String code) {
        PossibleMatchCandidate candidate = new PossibleMatchCandidate();
        candidate.setId(id);
        candidate.setName(name);
        candidate.setCode(code);
        return candidate;
    }

    @Test
    public void firstAndLastNamesSwappedMatchOnName() {
        PossibleMatchCandidate swapped = person("1", 0.05, 0.0, 1.0, 1.0, false, false);

        List<Scored> matches = PossibleMatchScorer.patients(List.of(swapped), null, null);

        assertEquals(1, matches.size());
        assertEquals(List.of(PossibleMatchScorer.NAME), matches.get(0).getMatchedOn());
    }

    @Test
    public void namesThatSoundAlikeMatchEvenWhenSpelledApart() {
        PossibleMatchCandidate soundAlike = person("1", 0.2, 0.25, 0, 0, true, true);

        assertEquals(1, PossibleMatchScorer.patients(List.of(soundAlike), null, null).size());
    }

    @Test
    public void unrelatedNamesDoNotMatch() {
        PossibleMatchCandidate unrelated = person("1", 0.31, 0.1, 0.0, 0.0, false, false);

        assertTrue(PossibleMatchScorer.patients(List.of(unrelated), null, null).isEmpty());
    }

    @Test
    public void aBirthDateWithinAYearAddsToANameMatchButNeverMatchesAlone() {
        LocalDate entered = LocalDate.of(1985, 3, 2);
        PossibleMatchCandidate sameName = person("1", 0.9, 0.9, 0, 0, true, true);
        sameName.setBirthDate(LocalDate.of(1985, 11, 30));
        PossibleMatchCandidate sameBirthOnly = person("2", 0.0, 0.0, 0.0, 0.0, false, false);
        sameBirthOnly.setBirthDate(entered);
        PossibleMatchCandidate twoYearsApart = person("3", 0.9, 0.9, 0, 0, true, true);
        twoYearsApart.setBirthDate(LocalDate.of(1983, 1, 1));

        List<Scored> matches = PossibleMatchScorer.patients(List.of(sameName, sameBirthOnly, twoYearsApart), entered,
                null);

        assertEquals(2, matches.size());
        assertEquals("1", matches.get(0).getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.NAME, PossibleMatchScorer.DATE_OF_BIRTH),
                matches.get(0).getMatchedOn());
        assertEquals("3", matches.get(1).getCandidate().getId());
        assertEquals(List.of(PossibleMatchScorer.NAME), matches.get(1).getMatchedOn());
    }

    @Test
    public void anIdentifierOneCharacterOffMatchesEvenWithAnotherName() {
        PossibleMatchCandidate oneOff = person("1", 0, 0, 0, 0, false, false);
        oneOff.setIdentifier("PNG-12346");
        PossibleMatchCandidate twoOff = person("2", 0, 0, 0, 0, false, false);
        twoOff.setIdentifier("PNG12399");

        List<Scored> matches = PossibleMatchScorer.patients(List.of(oneOff, twoOff), null, "png12345");

        assertEquals(1, matches.size());
        assertEquals(List.of(PossibleMatchScorer.IDENTIFIER), matches.get(0).getMatchedOn());
    }

    @Test
    public void atMostFiveMatchesBestFirst() {
        List<PossibleMatchCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            candidates.add(person(String.valueOf(i), 0.5 + i * 0.05, 0.5 + i * 0.05, 0, 0, false, false));
        }

        List<Scored> matches = PossibleMatchScorer.patients(candidates, null, null);

        assertEquals(PossibleMatchScorer.MAX_MATCHES, matches.size());
        assertEquals("7", matches.get(0).getCandidate().getId());
    }

    @Test
    public void facilityNamesMatchIgnoringCasePunctuationAndCommonWords() {
        List<PossibleMatchCandidate> facilities = List.of(facility("1", "Tokarara Health Centre", null),
                facility("2", "TOKARARA, HEALTH-CENTER", null), facility("3", "Gerehu Hospital", null),
                facility("4", "Kila Kila General Hospital", null));

        List<Scored> matches = PossibleMatchScorer.facilities(facilities, "Tokarara Health Center", null);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(m -> m.getMatchedOn().contains(PossibleMatchScorer.NAME)));
        assertEquals("tokarara", PossibleMatchScorer.facilityCore("Tokarara Health Center"));
        assertEquals("plural common words are common too", "bekasi riverside",
                PossibleMatchScorer.facilityCore("BEKASI RIVERSIDE District. HEALTH Offices"));
    }

    @Test
    public void aFacilityWithTheSameCodeMatchesOnCode() {
        List<PossibleMatchCandidate> facilities = List.of(facility("1", "Port Moresby General", "PMGH"),
                facility("2", "Somewhere Else", "XYZ"));

        List<Scored> matches = PossibleMatchScorer.facilities(facilities, "Brand New Clinic", "pmgh");

        assertEquals(1, matches.size());
        assertEquals(List.of(PossibleMatchScorer.CODE), matches.get(0).getMatchedOn());
    }

    @Test
    public void aFacilityNameOneLetterOffStillMatchesButADifferentPlaceDoesNot() {
        List<PossibleMatchCandidate> facilities = List.of(facility("1", "Waigani Clinic", null),
                facility("2", "Wabag Clinic", null));

        List<Scored> matches = PossibleMatchScorer.facilities(facilities, "Waigany Clinic", null);

        assertEquals(1, matches.size());
        assertEquals("1", matches.get(0).getCandidate().getId());
    }

    @Test
    public void providersMatchOnSimilarNames() {
        PossibleMatchCandidate agnes = person("1", 1.0, 0.6, 0, 0, true, true);
        PossibleMatchCandidate other = person("2", 0.2, 0.1, 0, 0, false, false);

        List<Scored> matches = PossibleMatchScorer.providers(List.of(agnes, other));

        assertEquals(1, matches.size());
        assertEquals("1", matches.get(0).getCandidate().getId());
    }
}
