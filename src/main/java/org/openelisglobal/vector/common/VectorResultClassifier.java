package org.openelisglobal.vector.common;

import java.util.Set;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

/**
 * Shared classifier for vector test result values. Centralised so the worklist,
 * deconvolution watcher, and completion evaluator all agree on what "positive"
 * means — adding a new token (e.g. "REACTIVE") only has to happen here.
 *
 * <p>
 * Tokenized exact-match, NOT substring: {@code "UNDETECTED"} or
 * {@code "NOT DETECTED"} must NOT count as positive even though they contain
 * the {@code DETECTED} substring.
 */
public final class VectorResultClassifier {

    // Split on whitespace + punctuation. Tokens compared exactly
    // (case-insensitive).
    private static final Set<String> POSITIVE_TOKENS = Set.of("POSITIVE", "POS", "DETECTED");

    // Negation tokens preceding a positive token flip the verdict.
    private static final Set<String> NEGATION_TOKENS = Set.of("NOT", "NO", "NON", "UN", "NEG", "NEGATIVE", "ND");

    private VectorResultClassifier() {
    }

    /**
     * Resolves dictionary-coded results to their text label before classifying. For
     * dictionary result types (D/M/C) {@link Result#getValue()} returns the numeric
     * dictionary entry id, not the displayed text — comparing those ids to
     * {@code DETECTED} would always say "not positive". Free-text and numeric
     * result types pass through unchanged.
     */
    public static boolean isPositiveResult(Result result) {
        return isPositiveValue(resolveResultText(result));
    }

    /**
     * Returns the human-readable text for a Result, resolving dictionary IDs to
     * their localized name. Used by reflex evaluators that need to compare the
     * value against rule conditions stored as text.
     */
    public static String resolveResultText(Result result) {
        if (result == null) {
            return null;
        }
        String text = result.getValue();
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            try {
                Dictionary d = SpringContext.getBean(DictionaryService.class).getDictionaryById(result.getValue());
                if (d != null) {
                    text = d.getLocalizedName();
                }
            } catch (RuntimeException lookupFailed) {
                // Fall through with the raw id.
            }
        }
        return text;
    }

    public static boolean isPositiveValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String[] tokens = value.trim().toUpperCase().split("[^A-Z0-9]+");
        String prev = null;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (POSITIVE_TOKENS.contains(token)) {
                if (prev != null && NEGATION_TOKENS.contains(prev)) {
                    return false; // "NOT DETECTED", "NO POS", etc.
                }
                return true;
            }
            prev = token;
        }
        return false;
    }
}
