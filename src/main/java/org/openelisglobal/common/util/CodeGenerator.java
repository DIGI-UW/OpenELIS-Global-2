package org.openelisglobal.common.util;

import java.util.Locale;
import java.util.function.Predicate;
import org.openelisglobal.common.exception.LocalizedValidationException;

/**
 * Shared UPPER_SNAKE code generation used by admin-managed reference lists that
 * let a user optionally type an explicit code, auto-generating one from a
 * display name otherwise (collision-suffixed {@code _2}, {@code _3}, ...).
 * First introduced for {@code inventory_item_type.code} (OGC-658 Part A),
 * reused for {@code inventory_item.code} (OGC-658 Part C).
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    /**
     * Derives an UPPER_SNAKE code from {@code name}, truncated to fit
     * {@code maxLength} (leaving room for a collision suffix), falling back to
     * {@code fallbackBase} if the name has no alphanumeric characters at all.
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
     * Normalizes a user-supplied explicit code: uppercased, stripped to
     * {@code [A-Z0-9_]}, truncated to {@code maxLength}. Throws if nothing usable
     * remains.
     */
    public static String normalize(String code, int maxLength) {
        String normalized = toCode(code);
        if (normalized.isEmpty()) {
            throw new LocalizedValidationException("common.codeGenerator.error.invalidCode",
                    "Code must contain at least one letter, digit, or underscore");
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
