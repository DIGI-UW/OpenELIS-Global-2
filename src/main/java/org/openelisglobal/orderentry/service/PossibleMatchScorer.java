package org.openelisglobal.orderentry.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.openelisglobal.orderentry.valueholder.PossibleMatchCandidate;

/**
 * Decides which existing records are possible duplicates of a record being
 * created, and on what (FRS clinical order entry v4, FR-B6a): names misspelled,
 * sounding alike or with first and last swapped; a date of birth within a year;
 * an identifier one character off; a facility name that is the same once case,
 * punctuation and common words are ignored, or the same code. Pure, so the
 * rules are tested without a database.
 */
public final class PossibleMatchScorer {

    public static final int MAX_MATCHES = 5;
    public static final String NAME = "name";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String IDENTIFIER = "identifier";
    public static final String CODE = "code";

    static final double NAME_SIMILARITY = 0.45;
    static final double FACILITY_SIMILARITY = 0.85;

    static final Set<String> FACILITY_COMMON_WORDS = new HashSet<>(Arrays.asList("hospital", "hospitals", "health",
            "healthcare", "centre", "center", "centres", "centers", "clinic", "clinics", "district", "office",
            "general", "medical", "laboratory", "laboratories", "lab", "labs", "post", "aid", "sub", "the", "of", "and",
            "de", "du", "la", "le", "des", "centro", "hopital", "sante", "poste", "dispensary", "unit", "facility",
            "provincial", "regional", "referral", "national"));

    private static final LevenshteinDistance ONE_EDIT = new LevenshteinDistance(1);

    private PossibleMatchScorer() {
    }

    /** One ranked possible match and the fields it matched on. */
    public static final class Scored {
        private final PossibleMatchCandidate candidate;
        private final double score;
        private final List<String> matchedOn;

        Scored(PossibleMatchCandidate candidate, double score, List<String> matchedOn) {
            this.candidate = candidate;
            this.score = score;
            this.matchedOn = matchedOn;
        }

        public PossibleMatchCandidate getCandidate() {
            return candidate;
        }

        public double getScore() {
            return score;
        }

        public List<String> getMatchedOn() {
            return matchedOn;
        }
    }

    public static List<Scored> patients(List<PossibleMatchCandidate> candidates, LocalDate birthDate,
            String identifier) {
        String wantedId = normalizeIdentifier(identifier);
        List<Scored> scored = new ArrayList<>();
        for (PossibleMatchCandidate candidate : candidates) {
            List<String> matchedOn = new ArrayList<>();
            double nameScore = nameScore(candidate);
            boolean nameMatch = namesMatch(candidate);
            if (nameMatch) {
                matchedOn.add(NAME);
            }
            boolean dobMatch = birthDate != null && candidate.getBirthDate() != null
                    && Math.abs(ChronoUnit.DAYS.between(birthDate, candidate.getBirthDate())) <= 366;
            boolean idMatch = !wantedId.isEmpty() && !normalizeIdentifier(candidate.getIdentifier()).isEmpty()
                    && ONE_EDIT.apply(wantedId, normalizeIdentifier(candidate.getIdentifier())) >= 0;
            boolean anyNameLikeness = nameScore >= 0.3 || candidate.isLastSoundsAlike()
                    || candidate.isFirstSoundsAlike();
            if (dobMatch && (nameMatch || anyNameLikeness)) {
                matchedOn.add(DATE_OF_BIRTH);
            }
            if (idMatch) {
                matchedOn.add(IDENTIFIER);
            }
            if (!nameMatch && !idMatch) {
                continue;
            }
            double score = nameScore + (matchedOn.contains(DATE_OF_BIRTH) ? 0.5 : 0) + (idMatch ? 1.0 : 0);
            scored.add(new Scored(candidate, score, matchedOn));
        }
        return top(scored);
    }

    public static List<Scored> providers(List<PossibleMatchCandidate> candidates) {
        List<Scored> scored = new ArrayList<>();
        for (PossibleMatchCandidate candidate : candidates) {
            if (namesMatch(candidate)) {
                scored.add(new Scored(candidate, nameScore(candidate), List.of(NAME)));
            }
        }
        return top(scored);
    }

    public static List<Scored> facilities(List<PossibleMatchCandidate> candidates, String name, String code) {
        String wantedCore = facilityCore(name);
        String wantedCode = code == null ? "" : code.trim();
        List<Scored> scored = new ArrayList<>();
        for (PossibleMatchCandidate candidate : candidates) {
            List<String> matchedOn = new ArrayList<>();
            String core = facilityCore(candidate.getName());
            double similarity = coreSimilarity(wantedCore, core);
            if (similarity >= FACILITY_SIMILARITY) {
                matchedOn.add(NAME);
            }
            if (!wantedCode.isEmpty() && candidate.getCode() != null
                    && wantedCode.equalsIgnoreCase(candidate.getCode().trim())) {
                matchedOn.add(CODE);
            }
            if (!matchedOn.isEmpty()) {
                scored.add(new Scored(candidate, similarity + (matchedOn.contains(CODE) ? 1.0 : 0), matchedOn));
            }
        }
        return top(scored);
    }

    /**
     * A facility name without case, accents, punctuation and the common words that
     * distinguish nothing ("Tokarara Health Center" and "Tokarara Health Centre"
     * are both "tokarara").
     */
    static String facilityCore(String name) {
        if (name == null) {
            return "";
        }
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
        if (plain.isEmpty()) {
            return "";
        }
        String core = Arrays.stream(plain.split(" ")).filter(word -> !isCommonFacilityWord(word))
                .collect(Collectors.joining(" "));
        return core.isEmpty() ? plain : core;
    }

    private static boolean isCommonFacilityWord(String word) {
        return FACILITY_COMMON_WORDS.contains(word) || (word.length() > 3 && word.endsWith("s")
                && FACILITY_COMMON_WORDS.contains(word.substring(0, word.length() - 1)));
    }

    static double coreSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 1;
        }
        int longest = Math.max(a.length(), b.length());
        if (longest < 4) {
            return 0;
        }
        int distance = LevenshteinDistance.getDefaultInstance().apply(a, b);
        return 1.0 - ((double) distance / longest);
    }

    static boolean namesMatch(PossibleMatchCandidate candidate) {
        double straight = (candidate.getLastToLast() + candidate.getFirstToFirst()) / 2;
        double swapped = (candidate.getLastToFirst() + candidate.getFirstToLast()) / 2;
        boolean bothSoundAlike = candidate.isLastSoundsAlike() && candidate.isFirstSoundsAlike();
        return Math.max(straight, swapped) >= NAME_SIMILARITY || bothSoundAlike
                || (candidate.getLastToLast() >= 0.6 && candidate.isFirstSoundsAlike())
                || (candidate.getFirstToFirst() >= 0.6 && candidate.isLastSoundsAlike());
    }

    static double nameScore(PossibleMatchCandidate candidate) {
        double straight = (candidate.getLastToLast() + candidate.getFirstToFirst()) / 2;
        double swapped = (candidate.getLastToFirst() + candidate.getFirstToLast()) / 2;
        double phonetic = (candidate.isLastSoundsAlike() ? 0.1 : 0) + (candidate.isFirstSoundsAlike() ? 0.1 : 0);
        return Math.max(straight, swapped) + phonetic;
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.replaceAll("[\\s-]", "").toLowerCase(Locale.ROOT);
    }

    private static List<Scored> top(List<Scored> scored) {
        return scored.stream().sorted(Comparator.comparingDouble(Scored::getScore).reversed()).limit(MAX_MATCHES)
                .collect(Collectors.toList());
    }
}
