package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;

/**
 * Whether a reported value meets the target sealed on a panel sample.
 *
 * <p>
 * Numeric when an acceptance range is sealed (inside the closed range is
 * acceptable), a numeric comparison when a quantitative target carries no range
 * so that "100.0" matches a target of "100", and a case-insensitive exact match
 * otherwise. A non-numeric report against a numeric target is a mismatch, not
 * an error.
 *
 * <p>
 * Both lanes judge against the same target. In-house blinding has no peer group
 * and never had another rule. The provider lane also has a peer group, but the
 * peer z-score cannot carry the verdict on its own: with the sample standard
 * deviation over every reported value, the largest z any participant can reach
 * is (n-1)/sqrt(n), which is 1.79 at the five participants scoring requires —
 * so a laboratory reporting double the target scored acceptable until this
 * became the verdict and the z became the reported statistic beside it.
 */
final class EqaPanelVerdict {

    private EqaPanelVerdict() {
    }

    /** The verdict for one reported value against one sealed panel sample. */
    static EQAPerformanceStatus of(EQAPanelSample target, String reported) {
        String value = reported == null ? "" : reported.trim();
        if (target.getAcceptanceRangeLow() != null || target.getAcceptanceRangeHigh() != null) {
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
        String targetValue = target.getTargetValue() == null ? "" : target.getTargetValue().trim();
        BigDecimal targetNumber = parseOrNull(targetValue);
        BigDecimal reportedNumber = parseOrNull(value);
        if (targetNumber != null && reportedNumber != null) {
            return targetNumber.compareTo(reportedNumber) == 0 ? EQAPerformanceStatus.ACCEPTABLE
                    : EQAPerformanceStatus.UNACCEPTABLE;
        }
        return targetValue.equalsIgnoreCase(value) ? EQAPerformanceStatus.ACCEPTABLE
                : EQAPerformanceStatus.UNACCEPTABLE;
    }

    /** The numeric form of a sealed target, or null when the target is a word. */
    static BigDecimal numericTargetOf(EQAPanelSample sample) {
        return sample.getTargetValue() == null ? null : parseOrNull(sample.getTargetValue().trim());
    }

    private static BigDecimal parseOrNull(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
