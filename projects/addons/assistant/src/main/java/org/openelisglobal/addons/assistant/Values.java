package org.openelisglobal.addons.assistant;

import java.util.Locale;

/** Result values as OpenELIS shows them: numbers, reference ranges, high/low flags. */
public final class Values {

    private Values() {
    }

    /** "55.6", "55,6", "<5", ">=150" → the number; anything else → null. */
    public static Double number(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim().replace(',', '.');
        if (!s.matches("[<>]?=?\\s*-?\\d+(\\.\\d+)?")) {
            return null;
        }
        try {
            return Double.parseDouble(s.replaceAll("[<>=\\s]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "H" above the range, "L" below it, "" inside or without a range. */
    public static String flag(String value, Double low, Double high) {
        Double v = number(value);
        if (v == null || !hasRange(low, high)) {
            return "";
        }
        if (high != null && high > 0 && v > high) {
            return "H";
        }
        if (low != null && low > 0 && v < low) {
            return "L";
        }
        return "";
    }

    static boolean hasRange(Double low, Double high) {
        return !(low == null && high == null) && !(nz(low) == 0 && nz(high) == 0);
    }

    private static double nz(Double d) {
        return d == null ? 0 : d;
    }

    /** "10–40", "< 3.6", "> 90", or "" without a range. */
    public static String range(Double low, Double high) {
        if (!hasRange(low, high)) {
            return "";
        }
        if (nz(low) == 0) {
            return "< " + num(high);
        }
        if (high == null || high == 0) {
            return "> " + num(low);
        }
        return num(low) + "–" + num(high);
    }

    /** 40.0 → "40", 0.4600 → "0.46". */
    public static String num(Double d) {
        if (d == null) {
            return "";
        }
        if (d == Math.rint(d)) {
            return String.valueOf(d.longValue());
        }
        String s = String.format(Locale.ROOT, "%.4f", d);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
