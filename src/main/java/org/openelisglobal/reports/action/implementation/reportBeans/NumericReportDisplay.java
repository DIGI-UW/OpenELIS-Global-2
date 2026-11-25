package org.openelisglobal.reports.action.implementation.reportBeans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.result.valueholder.Result;

/**
 * Report-only annotations; the received measurement remains the clinical value.
 */
public final class NumericReportDisplay {
    private NumericReportDisplay() {
    }

    public static String format(Result result, boolean displayLog10) {
        return format(result, displayLog10, UiMessages.current());
    }

    public static String format(Result result, boolean displayLog10, UiMessages messages) {
        String display = result.getDisplayValue();
        if (result.getValue() == null || result.getValue().isBlank()) {
            return display;
        }
        try {
            BigDecimal measured = new BigDecimal(StringUtil.normalizeScientificNotation(result.getValue()).trim());
            if (displayLog10 && measured.signum() > 0) {
                // Scaling avoids double overflow/underflow for valid decimal measurements.
                long exponent = (long) measured.precision() - measured.scale() - 1;
                double significand = new BigDecimal(measured.unscaledValue(), measured.precision() - 1).doubleValue();
                double log = Math.log10(significand) + exponent;
                String logarithm = BigDecimal.valueOf(log).setScale(2, RoundingMode.HALF_UP).toPlainString();
                // Unicode minus is a numeric sign, so PDF line breaking keeps it with its
                // digits.
                display += "\n" + messages.get("label.results.calculatedLog10").replace("{value}",
                        logarithm.replace('-', '\u2212'));
            }
            if (measured.compareTo(
                    new BigDecimal(StringUtil.normalizeScientificNotation(result.getDisplayValue()).trim())) != 0) {
                display += "\n("
                        + messages.get("label.results.clinicalValue").replace("{value}", result.getEnteredValue())
                        + ")";
            }
        } catch (NumberFormatException qualifiedMeasurement) {
            // Bounds are not exact measurements; do not invent a logarithm for them.
        }
        return display;
    }
}
