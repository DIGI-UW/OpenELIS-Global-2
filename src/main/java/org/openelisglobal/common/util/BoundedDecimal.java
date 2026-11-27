package org.openelisglobal.common.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * A plain decimal of bounded size, optionally with a short exponent: the form
 * the Analyzer Bridge sends numeric results in. Anything else (a comparison
 * such as {@code <5}, text, or an exponent long enough that expanding it to
 * plain digits would exhaust memory) is not one.
 */
public final class BoundedDecimal {

    private static final int MAX_LENGTH = 48;
    private static final Pattern GRAMMAR = Pattern
            .compile("[+-]?(?:\\d{1,18}(?:\\.\\d{0,18})?|\\.\\d{1,18})(?:[eE][+-]?\\d{1,3})?");

    private BoundedDecimal() {
    }

    public static boolean isBoundedDecimal(String value) {
        return value != null && value.length() <= MAX_LENGTH && GRAMMAR.matcher(value).matches();
    }

    /**
     * {@link BigDecimal#toString()} keeps the exponent rather than expanding it, so
     * this judges the number without materializing its digits.
     */
    public static boolean isBounded(BigDecimal value) {
        return value != null && isBoundedDecimal(value.toString());
    }
}
