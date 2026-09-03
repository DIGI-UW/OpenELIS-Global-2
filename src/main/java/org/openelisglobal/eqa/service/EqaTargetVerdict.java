package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;

/**
 * The verdict a reported value earns against the panel material it answers.
 *
 * <p>
 * A panel sample carries what the right answer is: an acceptance range for a
 * quantitative analyte, or a target value for a qualitative one. Judging
 * against it is the only comparison that works on its own — a peer statistic
 * needs a crowd, and with a small roster its own thresholds are out of reach —
 * so both the in-house lane and the provider lane decide the same way, here.
 *
 * <p>
 * A value outside a sealed range is unacceptable, and so is a word that does
 * not match the target. A quantitative target with no range compares as a
 * number rather than as text, so "100.0" answers a target of "100"; a caller
 * that does not want equality that strict checks {@link #hasRange} first and
 * keeps its own judgement.
 */
final class EqaTargetVerdict {

    private EqaTargetVerdict() {
    }

    /**
     * Whether the sample seals a range, which is what makes a numeric verdict safe.
     */
    static boolean hasRange(EQAPanelSample target) {
        return target != null && (target.getAcceptanceRangeLow() != null || target.getAcceptanceRangeHigh() != null);
    }

    /** The target as a number, or null when it is a word. */
    static BigDecimal numericTarget(EQAPanelSample target) {
        return target == null ? null : parseOrNull(target.getTargetValue());
    }

    /**
     * Inside a sealed range, or an exact match against the target, is acceptable. A
     * non-numeric report against a numeric target is a mismatch, not an error.
     */
    static EQAPerformanceStatus of(EQAPanelSample target, String reported) {
        String value = reported == null ? "" : reported.trim();
        if (hasRange(target)) {
            BigDecimal numeric = parseOrNull(value);
            if (numeric == null) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            if (target.getAcceptanceRangeLow() != null && numeric.compareTo(target.getAcceptanceRangeLow()) < 0) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            if (target.getAcceptanceRangeHigh() != null && numeric.compareTo(target.getAcceptanceRangeHigh()) > 0) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            return EQAPerformanceStatus.ACCEPTABLE;
        }
        String targetValue = target == null || target.getTargetValue() == null ? "" : target.getTargetValue().trim();
        BigDecimal targetNumber = parseOrNull(targetValue);
        BigDecimal reportedNumber = parseOrNull(value);
        if (targetNumber != null && reportedNumber != null) {
            return targetNumber.compareTo(reportedNumber) == 0 ? EQAPerformanceStatus.ACCEPTABLE
                    : EQAPerformanceStatus.UNACCEPTABLE;
        }
        return targetValue.equalsIgnoreCase(value) ? EQAPerformanceStatus.ACCEPTABLE
                : EQAPerformanceStatus.UNACCEPTABLE;
    }

    private static BigDecimal parseOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
