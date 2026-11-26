package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * The acceptance rules of a blank and a duplicate, in one place for both of
 * their scopes: field items made at environmental order entry (S-08) and lab
 * items recorded on a result run (OGC-1200 FR-C10, FR-C11, FR-C13).
 *
 * <ul>
 * <li>Blank: its value is at or below the test's blank ceiling, or its outcome
 * is the expected blank outcome (Negative or Not detected).</li>
 * <li>Duplicate: the RPD, |a - b| / mean(a, b) x 100, is at or below the test's
 * limit; for a qualitative test both outcomes agree. A pair whose values are
 * both below the test's limit of quantitation is neither passed nor
 * failed.</li>
 * </ul>
 */
public final class QcItemRules {

    /** What a blank or duplicate came to. */
    public enum Evaluation {
        PASS, FAIL, NOT_EVALUATED, BELOW_LOQ
    }

    public record Outcome(Evaluation evaluation, String detail) {
    }

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final Set<String> BLANK_OUTCOMES = Set.of("NEGATIVE", "NOT_DETECTED", "NOT DETECTED");

    private QcItemRules() {
    }

    public static Outcome blank(BigDecimal value, BigDecimal ceiling) {
        if (ceiling == null) {
            return new Outcome(Evaluation.NOT_EVALUATED, "Blank threshold not configured");
        }
        if (value == null) {
            return new Outcome(Evaluation.NOT_EVALUATED, "Non-numeric result");
        }
        return new Outcome(value.compareTo(ceiling) <= 0 ? Evaluation.PASS : Evaluation.FAIL,
                String.format("Result = %s (threshold %s)", plain(value), plain(ceiling)));
    }

    /** A qualitative blank passes when it reads Negative or Not detected. */
    public static Outcome qualitativeBlank(String outcome) {
        if (outcome == null || outcome.isBlank()) {
            return new Outcome(Evaluation.NOT_EVALUATED, "No outcome");
        }
        return new Outcome(
                BLANK_OUTCOMES.contains(outcome.trim().toUpperCase(Locale.ROOT)) ? Evaluation.PASS : Evaluation.FAIL,
                "Outcome = " + outcome.trim());
    }

    public static BigDecimal rpd(BigDecimal a, BigDecimal b) {
        BigDecimal mean = a.add(b).divide(TWO, MC);
        if (mean.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return a.subtract(b).abs().divide(mean, MC).multiply(HUNDRED, MC);
    }

    public static Outcome duplicate(BigDecimal first, BigDecimal second, BigDecimal limit, BigDecimal loq) {
        if (first == null) {
            return new Outcome(Evaluation.NOT_EVALUATED, "Its sample has no result yet");
        }
        if (second == null) {
            return new Outcome(Evaluation.NOT_EVALUATED, "No value to compare");
        }
        if (loq != null && first.compareTo(loq) < 0 && second.compareTo(loq) < 0) {
            return new Outcome(Evaluation.BELOW_LOQ,
                    String.format("Both values below the limit of quantitation %s", plain(loq)));
        }
        if (limit == null) {
            return new Outcome(Evaluation.NOT_EVALUATED, "RPD limit not configured");
        }
        BigDecimal rpd = rpd(first, second);
        return new Outcome(rpd.compareTo(limit) <= 0 ? Evaluation.PASS : Evaluation.FAIL, String.format(
                "RPD = %s%% (threshold %s%%)", rpd.setScale(1, RoundingMode.HALF_UP).toPlainString(), plain(limit)));
    }

    /**
     * A number as written, without the trailing zeros of the column it was stored
     * in.
     */
    private static String plain(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    /** A qualitative duplicate passes when both outcomes agree. */
    public static Outcome qualitativeDuplicate(String first, String second) {
        if (first == null || first.isBlank()) {
            return new Outcome(Evaluation.NOT_EVALUATED, "Its sample has no result yet");
        }
        if (second == null || second.isBlank()) {
            return new Outcome(Evaluation.NOT_EVALUATED, "No value to compare");
        }
        return new Outcome(first.trim().equalsIgnoreCase(second.trim()) ? Evaluation.PASS : Evaluation.FAIL,
                "Outcomes = " + first.trim() + " / " + second.trim());
    }
}
