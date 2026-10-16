package org.openelisglobal.sample.daoimpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tolerances for the misspelled-name half of the patient search.
 *
 * <p>
 * Names are compared after folding case and accents in the database (see
 * search_fold() in the Liquibase changelog), so "tran" finds "Trần".
 *
 * <p>
 * Misspellings are matched by trigram similarity, which the GIN index serves
 * directly. Similarity alone is weak for the commonest typing slip though -
 * swapping two neighbouring letters scores only 0.14 on a three letter name -
 * so the swapped spellings are additionally matched literally, which the same
 * index also serves.
 */
final class FuzzyNameMatch {

    /**
     * Below three characters a term already matches a large share of the table
     * through the substring predicate, and fuzziness would add only noise.
     */
    private static final int MIN_FUZZY_LENGTH = 3;

    /**
     * Similarity floor for the trigram match, set explicitly so behaviour does not
     * depend on the server's own setting. 0.2 was tried and rejected: it starts
     * matching names that merely share a two letter prefix - "john" scores 0.25
     * against both "jose" and "joan". What similarity misses at this level are the
     * transpositions, and those are matched exactly by the swapped spellings below.
     */
    static final float SIMILARITY_THRESHOLD = 0.3f;

    private FuzzyNameMatch() {
    }

    static String normalize(String term) {
        return term.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isFuzzyMatchable(String term) {
        return term != null && normalize(term).length() >= MIN_FUZZY_LENGTH;
    }

    /**
     * The words of the term. A misspelling is judged one word at a time: compared
     * as a whole, two names that share a common word ("Thị Bé" and "Thị Dứt", or
     * "Mary Ann" and "Mary Jo") look similar enough to pass for a misspelling of
     * each other although the word that tells them apart has nothing in common.
     */
    static List<String> words(String term) {
        List<String> words = new ArrayList<>();
        for (String word : normalize(term).split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /** Whether any word of the term is long enough to be matched fuzzily. */
    static boolean hasFuzzyMatchableWord(String term) {
        return term != null && words(term).stream().anyMatch(FuzzyNameMatch::isFuzzyMatchable);
    }

    /** Every spelling of the term with one pair of neighbouring letters swapped. */
    static List<String> adjacentSwaps(String term) {
        String normalized = normalize(term);
        Set<String> swaps = new LinkedHashSet<>();
        for (int i = 0; i + 1 < normalized.length(); i++) {
            if (normalized.charAt(i) == normalized.charAt(i + 1)) {
                continue;
            }
            swaps.add(normalized.substring(0, i) + normalized.charAt(i + 1) + normalized.charAt(i)
                    + normalized.substring(i + 2));
        }
        return new ArrayList<>(swaps);
    }
}
