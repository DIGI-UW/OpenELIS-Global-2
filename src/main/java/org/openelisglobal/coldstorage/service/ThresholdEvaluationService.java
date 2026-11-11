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
     * Classifies {@code temperature} against the profile's band, then gates
     * escalation on {@code minExcursionMinutes} of continuous breach.
     */
    FreezerReading.Status evaluateTemperatureStatus(BigDecimal temperature, ThresholdProfile profile, Freezer freezer,
            OffsetDateTime timestamp);

    /**
     * Measures the excursion streak over humidity alone, so each metric escalates
     * on its own accumulated breach time.
     */
    FreezerReading.Status evaluateHumidityStatus(BigDecimal humidity, ThresholdProfile profile, Freezer freezer,
            OffsetDateTime timestamp);

    /**
     * Derives a representative "target" temperature from a threshold profile for
     * display purposes, by averaging the warning band (preferred) or critical band,
     * falling back to whichever single bound is available. Returns {@code null} if
     * no profile (or no usable bounds) is available.
     */
    BigDecimal deriveTargetTemperature(ThresholdProfile profile);
}
