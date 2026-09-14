package org.openelisglobal.common.util;

import java.util.Locale;
import java.util.function.Predicate;
import org.openelisglobal.common.exception.LocalizedValidationException;

/**
 * Turns display names and user-typed strings into UPPER_SNAKE codes, suffixing
 * {@code _2}, {@code _3}, ... when a generated candidate is already taken.
 */
public final class CodeGenerator {

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
            String suffixText = "_" + suffix;
            candidate = truncate(base, maxLength - suffixText.length()) + suffixText;
            suffix++;
        }
        return candidate;
    }

    /**
     * Normalizes a user-supplied code to {@code [A-Z0-9_]} within
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
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        String cut = value.substring(0, maxLength).replaceAll("_+$", "");
        return cut.isEmpty() ? value.substring(0, maxLength) : cut;
    }
}
