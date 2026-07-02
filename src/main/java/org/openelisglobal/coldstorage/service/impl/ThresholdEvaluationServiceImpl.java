package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.coldstorage.dao.FreezerThresholdProfileDAO;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.ThresholdEvaluationService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdEvaluationServiceImpl implements ThresholdEvaluationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThresholdEvaluationServiceImpl.class);

    private final FreezerThresholdProfileDAO freezerThresholdProfileDAO;
    private final FreezerReadingService freezerReadingService;

    public ThresholdEvaluationServiceImpl(FreezerThresholdProfileDAO freezerThresholdProfileDAO,
            FreezerReadingService freezerReadingService) {
        this.freezerThresholdProfileDAO = freezerThresholdProfileDAO;
        this.freezerReadingService = freezerReadingService;
    }

    @Override
    @Transactional(readOnly = true)
    public ThresholdProfile resolveActiveProfile(Freezer freezer, OffsetDateTime timestamp) {
        List<FreezerThresholdProfile> assignments = freezerThresholdProfileDAO.findActiveAssignments(freezer.getId(),
                timestamp);
        ThresholdProfile profile = assignments.stream()
                .max(Comparator.comparing(FreezerThresholdProfile::getEffectiveStart))
                .map(FreezerThresholdProfile::getThresholdProfile).orElse(null);

        // Initialize the profile to prevent LazyInitializationException
        if (profile != null) {
            profile.getName(); // Force initialization
        }

        return profile;
    }

    @Override
    public FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile) {
        return evaluateInstantaneousStatus(temperature, humidity, profile);
    }

    @Override
    public FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile,
            Freezer freezer, OffsetDateTime timestamp) {
        FreezerReading.Status instantaneousStatus = evaluateInstantaneousStatus(temperature, humidity, profile);

        if (instantaneousStatus == FreezerReading.Status.NORMAL || profile == null) {
            return instantaneousStatus;
        }

        // Hysteresis: only escalate a breach to WARNING/CRITICAL once it has
        // persisted for at least minExcursionMinutes, so a reading oscillating
        // exactly at a threshold boundary does not create+resolve an alert on
        // every single poll.
        if (freezer == null || freezer.getId() == null || timestamp == null || profile.getMinExcursionMinutes() == null
                || profile.getMinExcursionMinutes() <= 0) {
            return instantaneousStatus;
        }

        return applyMinExcursionHysteresis(freezer, timestamp, profile, instantaneousStatus);
    }

    /**
     * Requires that the breach has been continuously present (every prior reading
     * in the lookback window classifies as WARNING-or-worse for a WARNING
     * candidate, or CRITICAL for a CRITICAL candidate) for the full
     * {@code minExcursionMinutes} window before escalating. If there is no prior
     * reading history at all in the window (i.e. this is the first reading since
     * the breach began) it has by definition not yet persisted for the full window,
     * so the reading is reported as NORMAL until enough time/history accumulates to
     * confirm a sustained excursion.
     *
     * <p>
     * Note: {@code maxDurationMinutes} is a distinct, separately-modeled field
     * intended for something like auto-escalating severity once a WARNING has
     * persisted past a duration. Its intended semantics are ambiguous from the
     * existing code/tests (e.g. is it a hard alerting cutoff, an auto-acknowledge
     * window, or something else) so it is intentionally left unwired here rather
     * than guessing at behavior - see the field javadoc on
     * {@link ThresholdProfile#getMaxDurationMinutes()}.
     */
    private FreezerReading.Status applyMinExcursionHysteresis(Freezer freezer, OffsetDateTime timestamp,
            ThresholdProfile profile, FreezerReading.Status instantaneousStatus) {
        int minExcursionMinutes = profile.getMinExcursionMinutes();
        // Query a window wider than minExcursionMinutes (double it) as a safety
        // margin. Poll timestamps land wherever the poll actually ran, essentially
        // never exactly on a `timestamp - minExcursionMinutes` boundary, so requiring
        // a reading to land exactly at the window edge (an earlier version of this
        // check did) would make escalation practically unreachable. Instead, the
        // real excursion duration is reconstructed below from the readings
        // themselves by walking the continuous breaching streak back from `timestamp`.
        OffsetDateTime lookupStart = timestamp.minusMinutes((long) minExcursionMinutes * 2);
        // The current reading is evaluated before it is saved (see
        // ReadingIngestionServiceImpl.ingest()), so this only returns *prior*
        // readings, ordered oldest-first (see
        // FreezerReadingDAOImpl.findByFreezerWithin).
        List<FreezerReading> priorReadings;
        try {
            priorReadings = freezerReadingService.getReadingsBetween(freezer.getId(), lookupStart, timestamp);
        } catch (Exception ex) {
            LOGGER.warn("Unable to load recent readings for hysteresis check on freezer {}: {}", freezer.getId(),
                    ex.getMessage());
            return instantaneousStatus;
        }

        if (priorReadings == null || priorReadings.isEmpty()) {
            // No history at all in the lookback window - this is the first reading of a
            // (possibly new) excursion, so it cannot yet have persisted for the required
            // duration. Don't escalate yet.
            return FreezerReading.Status.NORMAL;
        }

        // Walk backward from the most recent prior reading, accumulating a
        // continuous breaching streak at the same-or-worse severity as the current
        // reading. Stop at the first non-breaching or transmission-failed reading,
        // or when history runs out. `earliestContinuousBreachTime` starts at
        // `timestamp` itself (the current, not-yet-saved reading) so the duration
        // calculation below always includes the current sample.
        OffsetDateTime earliestContinuousBreachTime = timestamp;
        for (int i = priorReadings.size() - 1; i >= 0; i--) {
            FreezerReading reading = priorReadings.get(i);
            if (Boolean.FALSE.equals(reading.getTransmissionOk())) {
                break;
            }
            FreezerReading.Status pastStatus = evaluateInstantaneousStatus(reading.getTemperatureCelsius(),
                    reading.getHumidityPercentage(), profile);
            boolean breaching = instantaneousStatus == FreezerReading.Status.CRITICAL
                    ? pastStatus == FreezerReading.Status.CRITICAL
                    : (pastStatus == FreezerReading.Status.WARNING || pastStatus == FreezerReading.Status.CRITICAL);
            if (!breaching) {
                break;
            }
            earliestContinuousBreachTime = reading.getRecordedAt();
        }

        long breachDurationMinutes = java.time.Duration.between(earliestContinuousBreachTime, timestamp).toMinutes();
        if (breachDurationMinutes >= minExcursionMinutes) {
            return instantaneousStatus;
        }
        return FreezerReading.Status.NORMAL;
    }

    private FreezerReading.Status evaluateInstantaneousStatus(BigDecimal temperature, BigDecimal humidity,
            ThresholdProfile profile) {
        if (profile == null || temperature == null) {
            return FreezerReading.Status.NORMAL;
        }

        boolean critical = isCriticalTemperature(temperature, profile) || isCriticalHumidity(humidity, profile);
        if (critical) {
            return FreezerReading.Status.CRITICAL;
        }

        boolean warning = isWarningTemperature(temperature, profile) || isWarningHumidity(humidity, profile);
        if (warning) {
            return FreezerReading.Status.WARNING;
        }

        return FreezerReading.Status.NORMAL;
    }

    private boolean isCriticalTemperature(BigDecimal temperature, ThresholdProfile profile) {
        return (profile.getCriticalMin() != null && temperature.compareTo(profile.getCriticalMin()) < 0)
                || (profile.getCriticalMax() != null && temperature.compareTo(profile.getCriticalMax()) > 0);
    }

    private boolean isWarningTemperature(BigDecimal temperature, ThresholdProfile profile) {
        // Warning range is between warning and critical thresholds
        // Inclusive of warning boundary, exclusive of critical boundary
        boolean warningLow = profile.getWarningMin() != null && profile.getCriticalMin() != null
                && temperature.compareTo(profile.getCriticalMin()) >= 0
                && temperature.compareTo(profile.getWarningMin()) <= 0;

        boolean warningHigh = profile.getWarningMax() != null && profile.getCriticalMax() != null
                && temperature.compareTo(profile.getWarningMax()) >= 0
                && temperature.compareTo(profile.getCriticalMax()) < 0;

        return warningLow || warningHigh;
    }

    private boolean isCriticalHumidity(BigDecimal humidity, ThresholdProfile profile) {
        if (humidity == null) {
            return false;
        }
        return (profile.getHumidityCriticalMin() != null && humidity.compareTo(profile.getHumidityCriticalMin()) < 0)
                || (profile.getHumidityCriticalMax() != null
                        && humidity.compareTo(profile.getHumidityCriticalMax()) > 0);
    }

    private boolean isWarningHumidity(BigDecimal humidity, ThresholdProfile profile) {
        if (humidity == null) {
            return false;
        }
        return (profile.getHumidityWarningMin() != null && humidity.compareTo(profile.getHumidityWarningMin()) < 0)
                || (profile.getHumidityWarningMax() != null && humidity.compareTo(profile.getHumidityWarningMax()) > 0);
    }

    @Override
    public BigDecimal deriveTargetTemperature(ThresholdProfile profile) {
        if (profile == null) {
            return null;
        }
        if (profile.getWarningMin() != null && profile.getWarningMax() != null) {
            return profile.getWarningMin().add(profile.getWarningMax()).divide(BigDecimal.valueOf(2), 2,
                    java.math.RoundingMode.HALF_UP);
        }
        if (profile.getCriticalMin() != null && profile.getCriticalMax() != null) {
            return profile.getCriticalMin().add(profile.getCriticalMax()).divide(BigDecimal.valueOf(2), 2,
                    java.math.RoundingMode.HALF_UP);
        }
        if (profile.getWarningMax() != null) {
            return profile.getWarningMax();
        }
        if (profile.getCriticalMax() != null) {
            return profile.getCriticalMax();
        }
        if (profile.getWarningMin() != null) {
            return profile.getWarningMin();
        }
        return profile.getCriticalMin();
    }
}
