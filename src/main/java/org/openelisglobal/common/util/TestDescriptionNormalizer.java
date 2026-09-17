package org.openelisglobal.common.util;

import java.text.Normalizer;

/**
 * The normalisation the database applies to {@code test.normalized_description}
 * (trigger {@code update_test_normalized_description}): the base name and the
 * first parenthesised part are each stripped of accents and of everything that
 * is not a letter or digit, lower-cased and concatenated. Java callers use the
 * same rules so a lookup by normalised name agrees with what the trigger
 * stored.
 */
public final class TestDescriptionNormalizer {

    private TestDescriptionNormalizer() {
    }

    public static String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        int startParen = description.indexOf('(');
        int endParen = description.indexOf(')');
        if (startParen >= 0 && endParen > startParen) {
            return normalizeText(description.substring(0, startParen))
                    + normalizeText(description.substring(startParen + 1, endParen));
        }
        return normalizeText(description);
    }

    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
