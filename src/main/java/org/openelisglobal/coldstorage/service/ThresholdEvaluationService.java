package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;

public interface ThresholdEvaluationService {

    ThresholdProfile resolveActiveProfile(Freezer freezer, OffsetDateTime timestamp);

    /**
     * Stateless instantaneous evaluation with no hysteresis (no freezer/timestamp
     * context to look up reading history against). Kept for callers that only need
     * a single-reading classification.
     */
    FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile);

    /**
     * Same instantaneous evaluation as
     * {@link #evaluateStatus(BigDecimal, BigDecimal, ThresholdProfile)}, but
     * additionally applies {@code minExcursionMinutes} hysteresis using
     * {@code freezer}'s reading history around {@code timestamp}: a breach only
     * escalates to WARNING/CRITICAL once it has persisted for the configured
     * window, so a reading oscillating exactly at a threshold boundary does not
     * flap an alert on every poll.
     */
    FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile,
            Freezer freezer, OffsetDateTime timestamp);

    /**
     * Derives a representative "target" temperature from a threshold profile for
     * display purposes, by averaging the warning band (preferred) or critical band,
     * falling back to whichever single bound is available. Returns {@code null} if
     * no profile (or no usable bounds) is available.
     */
    BigDecimal deriveTargetTemperature(ThresholdProfile profile);
}
