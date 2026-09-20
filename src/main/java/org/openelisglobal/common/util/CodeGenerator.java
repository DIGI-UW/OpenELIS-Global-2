package org.openelisglobal.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.openelisglobal.common.exception.LocalizedValidationException;

/**
 * Turns display names and user-typed strings into UPPER-KEBAB codes; the
 * frontend's InventoryItemForm.toCode mirrors {@link #normalize}.
 */
public final class CodeGenerator {

    private static final int PREFIX_LETTERS = 3;
    private static final int PREFIX_DIGIT_TOKENS = 2;
    private static final int PREFIX_DIGIT_TOKEN_LENGTH = 8;
    private static final String PREFIX_FALLBACK = "ITEM";

    private CodeGenerator() {
    }

    /**
     * Derives a code from {@code name}, truncated to {@code maxLength} and falling
     * back to {@code fallbackBase} when the name has no alphanumerics.
     */
    public static String generateFromName(String name, int maxLength, String fallbackBase,
            Predicate<String> existsByCode) {
        String base = toCode(name);
        if (base.isEmpty()) {
            base = fallbackBase;
        }
        base = truncate(base, maxLength);

        String candidate = base;
        int suffix = 2;
        while (existsByCode.test(candidate)) {
            String suffixText = "-" + suffix;
            candidate = truncate(base, maxLength - suffixText.length()) + suffixText;
            suffix++;
        }
        return candidate;
    }

    /**
     * The sequence key for a name: 3 letters of its first word with a letter, then
     * up to 2 other words holding a digit (PAR-500MG, SOD-09-500ML, HIV-12).
     */
    public static String prefixFor(String name) {
        List<String> tokens = new ArrayList<>();
        if (name != null) {
            for (String raw : name.trim().split("\\s+")) {
                String token = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }

        String letters = PREFIX_FALLBACK;
        int letterSource = -1;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.matches(".*[A-Z].*")) {
                String onlyLetters = token.replaceAll("[^A-Z]", "");
                letters = onlyLetters.substring(0, Math.min(PREFIX_LETTERS, onlyLetters.length()));
                letterSource = i;
                break;
            }
        }

        StringBuilder prefix = new StringBuilder(letters);
        int digitTokens = 0;
        for (int i = 0; i < tokens.size() && digitTokens < PREFIX_DIGIT_TOKENS; i++) {
            String token = tokens.get(i);
            if (i != letterSource && token.matches(".*[0-9].*")) {
                prefix.append('-').append(token, 0, Math.min(PREFIX_DIGIT_TOKEN_LENGTH, token.length()));
                digitTokens++;
            }
        }
        return prefix.toString();
    }

    /**
     * Normalizes a user-supplied code to {@code [A-Z0-9-]} within
     * {@code maxLength}, throwing if nothing usable remains.
     */
    public static String normalize(String code, int maxLength) {
        String normalized = toCode(code);
        if (normalized.isEmpty()) {
            throw new LocalizedValidationException("common.codeGenerator.error.invalidCode",
                    "Code must contain at least one letter or number");
        }
        return truncate(normalized, maxLength);
    }

    private static String toCode(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        String cut = value.substring(0, maxLength).replaceAll("-+$", "");
        return cut.isEmpty() ? value.substring(0, maxLength) : cut;
    }
}
