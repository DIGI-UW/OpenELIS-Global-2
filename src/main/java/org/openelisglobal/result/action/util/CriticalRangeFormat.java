package org.openelisglobal.result.action.util;

import org.apache.commons.validator.GenericValidator;

import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * How a test's critical zone is worded for the bench: "&lt; 50", "&gt; 400", or
 * "&lt; 50 or &gt; 400" — and "" wherever there is no such zone.
 *
 * <p>
 * Built here rather than taken from ResultLimitService.getDisplayCriticalRange,
 * which renders a one-sided low bound as "Infinity - x": its unset sentinel is
 * POSITIVE_INFINITY, which getDisplayNormalRange does not recognise.
 */
public final class CriticalRangeFormat {

    private static final String NUMERIC_RESULT_TYPE = "N";

    private CriticalRangeFormat() {
    }

    /**
     * @param limit      the range selected for this patient, or null
     * @param resultType the type the result is entered as
     */
    public static String display(ResultLimit limit, String resultType) {
        if (limit == null || GenericValidator.isBlankOrNull(limit.getId()) || !NUMERIC_RESULT_TYPE.equals(resultType)) {
            return "";
        }
        boolean hasLow = Double.isFinite(limit.getLowCritical());
        boolean hasHigh = Double.isFinite(limit.getHighCritical());
        if (hasLow && hasHigh) {
            return "< " + String.valueOf(limit.getLowCritical()) + " or > " + String.valueOf(limit.getHighCritical());
        }
        if (hasLow) {
            return "< " + String.valueOf(limit.getLowCritical());
        }
        if (hasHigh) {
            return "> " + String.valueOf(limit.getHighCritical());
        }
        return "";
    }
}
