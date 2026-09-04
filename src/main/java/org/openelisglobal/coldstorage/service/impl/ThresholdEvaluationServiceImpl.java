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

        // Only escalate once the breach has persisted for minExcursionMinutes.
        if (freezer == null || freezer.getId() == null || timestamp == null || profile.getMinExcursionMinutes() == null
                || profile.getMinExcursionMinutes() <= 0) {
            return instantaneousStatus;
        }

        return applyMinExcursionHysteresis(freezer, timestamp, profile, instantaneousStatus);
    }

    /**
     * Escalates only once every reading in a continuous breaching streak leading up
     * to {@code timestamp} spans at least {@code minExcursionMinutes}.
     *
     * <p>
     * {@code maxDurationMinutes} is a separate field left unwired here - its
     * intended semantics (hard cutoff? auto-acknowledge window?) are ambiguous from
     * the existing code/tests; see
     * {@link ThresholdProfile#getMaxDurationMinutes()}.
     */
    private FreezerReading.Status applyMinExcursionHysteresis(Freezer freezer, OffsetDateTime timestamp,
            ThresholdProfile profile, FreezerReading.Status instantaneousStatus) {
        int minExcursionMinutes = profile.getMinExcursionMinutes();
        // Widen the query rather than requiring a reading to land exactly on the window
        // boundary.
        OffsetDateTime lookupStart = timestamp.minusMinutes((long) minExcursionMinutes * 2);
        List<FreezerReading> priorReadings;
        try {
            priorReadings = freezerReadingService.getReadingsBetween(freezer.getId(), lookupStart, timestamp);
        } catch (Exception ex) {
            LOGGER.warn("Unable to load recent readings for hysteresis check on freezer {}: {}", freezer.getId(),
                    ex.getMessage());
            return instantaneousStatus;
        }

        if (priorReadings == null || priorReadings.isEmpty()) {
            return FreezerReading.Status.NORMAL;
        }

        // Walk backward accumulating a continuous same-or-worse-severity streak,
        // stopping at the first non-breaching or transmission-failed reading.
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
        // No bound against critical needed: isCriticalTemperature runs first, so a
        // value beyond critical never reaches here (an earlier version bounded this
        // too, leaving the exact critical value unclassified by either method).
        boolean warningLow = profile.getWarningMin() != null && temperature.compareTo(profile.getWarningMin()) <= 0;
        boolean warningHigh = profile.getWarningMax() != null && temperature.compareTo(profile.getWarningMax()) >= 0;
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
