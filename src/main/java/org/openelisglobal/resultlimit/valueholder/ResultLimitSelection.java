package org.openelisglobal.resultlimit.valueholder;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * The reference range chosen for a result, and why none was applied when the
 * patient's sex or birth date is missing and a range depends on it.
 */
public class ResultLimitSelection {

    public enum RangeNotAppliedReason {
        NONE(null), SEX_NOT_RECORDED("result.rangeNotApplied.sexMissing"),
        AGE_NOT_RECORDED("result.rangeNotApplied.ageMissing"),
        SEX_AND_AGE_NOT_RECORDED("result.rangeNotApplied.sexAndAgeMissing");

        private final String messageKey;

        RangeNotAppliedReason(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public static RangeNotAppliedReason of(boolean sexMissing, boolean ageMissing) {
            if (sexMissing && ageMissing) {
                return SEX_AND_AGE_NOT_RECORDED;
            }
            return sexMissing ? SEX_NOT_RECORDED : ageMissing ? AGE_NOT_RECORDED : NONE;
        }

        boolean sexMissing() {
            return this == SEX_NOT_RECORDED || this == SEX_AND_AGE_NOT_RECORDED;
        }

        boolean ageMissing() {
            return this == AGE_NOT_RECORDED || this == SEX_AND_AGE_NOT_RECORDED;
        }
    }

    private final ResultLimit resultLimit;
    private final RangeNotAppliedReason reason;

    public ResultLimitSelection(ResultLimit resultLimit, RangeNotAppliedReason reason) {
        this.resultLimit = resultLimit;
        this.reason = reason == null ? RangeNotAppliedReason.NONE : reason;
    }

    public static ResultLimitSelection of(ResultLimit resultLimit) {
        return new ResultLimitSelection(resultLimit, RangeNotAppliedReason.NONE);
    }

    public ResultLimit getResultLimit() {
        return resultLimit;
    }

    public RangeNotAppliedReason getReason() {
        return reason;
    }

    public boolean hasResultLimit() {
        return resultLimit != null && !GenericValidator.isBlankOrNull(resultLimit.getId());
    }

    public boolean isRangeNotApplied() {
        return !hasResultLimit() && reason != RangeNotAppliedReason.NONE;
    }

    /**
     * This selection, carrying the missing-value reasons of {@code other} as well
     * when neither found a range.
     */
    public ResultLimitSelection combinedWith(ResultLimitSelection other) {
        if (hasResultLimit() || other == null) {
            return this;
        }
        return new ResultLimitSelection(resultLimit, RangeNotAppliedReason.of(
                reason.sexMissing() || other.reason.sexMissing(), reason.ageMissing() || other.reason.ageMissing()));
    }
}
