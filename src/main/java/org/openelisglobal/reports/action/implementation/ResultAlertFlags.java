package org.openelisglobal.reports.action.implementation;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.util.ValidationSignals;

/**
 * The patient report's Alert letters for the critical tier (OGC-1121). The
 * legacy letters B / E say only "below / above normal"; BB / EE say the value
 * is beyond an authored critical bound, the same rule Results Entry and
 * Validation flag with (see {@link ValidationSignals#isCritical}).
 */
public final class ResultAlertFlags {

    public static final String CRITICALLY_BELOW = "BB";
    public static final String CRITICALLY_ABOVE = "EE";

    private ResultAlertFlags() {
    }

    /**
     * BB, EE, or an empty string when the value is not critical or cannot be
     * judged.
     */
    public static String criticalLetter(ResultLimit limit, String value) {
        if (limit == null || GenericValidator.isBlankOrNull(value)) {
            return "";
        }
        try {
            double numeric = Double.parseDouble(value.trim());
            if (Double.isFinite(limit.getLowCritical()) && numeric < limit.getLowCritical()) {
                return CRITICALLY_BELOW;
            }
            if (Double.isFinite(limit.getHighCritical()) && numeric > limit.getHighCritical()) {
                return CRITICALLY_ABOVE;
            }
            return "";
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
