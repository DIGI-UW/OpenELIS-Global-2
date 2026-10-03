package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.event.FreezerTransmissionRecoveredEvent;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.service.ThresholdEvaluationService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingIngestionServiceImpl implements ReadingIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadingIngestionServiceImpl.class);

    /**
     * Not admin-configurable: how many consecutive failed polls before raising an
     * offline alert is an internal alerting-sensitivity tuning knob, not something
     * a lab admin needs day to day.
     */
    private static final int OFFLINE_ALERT_CONSECUTIVE_FAILURES = 3;

    private final ThresholdEvaluationService thresholdEvaluationService;
    private final FreezerReadingService freezerReadingService;
    private final ApplicationEventPublisher eventPublisher;

    public ReadingIngestionServiceImpl(ThresholdEvaluationService thresholdEvaluationService,
            FreezerReadingService freezerReadingService, ApplicationEventPublisher eventPublisher) {
        this.thresholdEvaluationService = thresholdEvaluationService;
        this.freezerReadingService = freezerReadingService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void ingest(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature, BigDecimal humidity,
            BigDecimal temperature2, boolean transmissionOk, String errorMessage) {
        ThresholdProfile profile = thresholdEvaluationService.resolveActiveProfile(freezer, recordedAt);
        if (profile == null) {
            LOGGER.debug("No threshold profile configured for freezer {}, using default NORMAL status",
                    freezer.getName());
        }

        // Instantaneous status is what gets stored; escalation (hysteresis-gated) only
        // decides whether to alert. Escalation must be computed before saveReading, or
        // it would see this reading as its own evidence of a sustained breach.
        FreezerReading.Status instantaneousStatus = determineInstantaneousStatus(temperature, humidity, transmissionOk,
                profile);
        boolean escalationApplies = transmissionOk && profile != null;
        FreezerReading.Status temperatureEscalation = escalationApplies
                ? thresholdEvaluationService.evaluateTemperatureStatus(temperature, profile, freezer, recordedAt)
                : FreezerReading.Status.NORMAL;
        FreezerReading.Status humidityEscalation = escalationApplies
                ? thresholdEvaluationService.evaluateHumidityStatus(humidity, profile, freezer, recordedAt)
                : FreezerReading.Status.NORMAL;

        boolean recoveredFromOffline = transmissionOk && lastPollFailed(freezer.getId());

        FreezerReading savedReading = freezerReadingService.saveReading(freezer, recordedAt, temperature, humidity,
                temperature2, instantaneousStatus, transmissionOk, errorMessage);

        if (!transmissionOk) {
            if (hasReachedConsecutiveFailureThreshold(freezer.getId())) {
                publishTransmissionFailedEvent(freezer.getId(), errorMessage, savedReading.getId());
            }
            return;
        }

        if (recoveredFromOffline) {
            publishTransmissionRecoveredEvent(freezer.getId());
        }

        if (profile != null) {
            if (temperatureEscalation != FreezerReading.Status.NORMAL) {
                checkTemperatureThresholdsWithProfile(freezer, temperature, savedReading.getId(), profile);
            }
            if (humidityEscalation != FreezerReading.Status.NORMAL) {
                checkHumidityThresholdsWithProfile(freezer, humidity, savedReading.getId(), profile);
            }
        } else {
            // The simple fallback has no humidity thresholds to check - Freezer
            // carries only a target temperature and two deviation bounds.
            checkSimpleTemperatureThresholds(freezer, temperature, savedReading.getId());
        }
    }

    private boolean hasReachedConsecutiveFailureThreshold(Long freezerId) {
        int threshold = OFFLINE_ALERT_CONSECUTIVE_FAILURES;
        List<FreezerReading> recent = freezerReadingService.getRecentReadings(freezerId, threshold);
        if (recent.size() < threshold) {
            return false;
        }
        return recent.stream().allMatch(reading -> Boolean.FALSE.equals(reading.getTransmissionOk()));
    }

    /**
     * Whether the poll before this one failed. Read before the current reading is
     * saved, or it would answer about this poll instead of the previous one.
     */
    private boolean lastPollFailed(Long freezerId) {
        List<FreezerReading> previous = freezerReadingService.getRecentReadings(freezerId, 1);
        return !previous.isEmpty() && Boolean.FALSE.equals(previous.get(0).getTransmissionOk());
    }

    private void publishTransmissionRecoveredEvent(Long freezerId) {
        eventPublisher.publishEvent(new FreezerTransmissionRecoveredEvent(this, freezerId));
        LOGGER.info("Published transmission recovery event for freezer {}", freezerId);
    }

    private void publishTransmissionFailedEvent(Long freezerId, String errorMessage, Long readingId) {
        org.openelisglobal.coldstorage.event.FreezerTransmissionFailedEvent event = new org.openelisglobal.coldstorage.event.FreezerTransmissionFailedEvent(
                this, freezerId, errorMessage, readingId);
        eventPublisher.publishEvent(event);
        LOGGER.warn("Published transmission failure event for freezer {}: {}", freezerId, errorMessage);
    }

    private FreezerReading.Status determineInstantaneousStatus(BigDecimal temperature, BigDecimal humidity,
            boolean transmissionOk, ThresholdProfile profile) {
        if (!transmissionOk) {
            return FreezerReading.Status.CRITICAL;
        }
        return thresholdEvaluationService.evaluateStatus(temperature, humidity, profile);
    }

    /**
     * Check temperature thresholds using ThresholdProfile and publish
     * FreezerTemperatureThresholdViolatedEvent.
     *
     * <p>
     * Checks in priority order: CRITICAL_MAX, WARNING_MAX, CRITICAL_MIN,
     * WARNING_MIN
     */
    private void checkTemperatureThresholdsWithProfile(Freezer freezer, BigDecimal temperature, Long readingId,
            ThresholdProfile profile) {
        // Skip threshold checks if temperature is null
        if (temperature == null) {
            return;
        }

        // Check CRITICAL_MAX (critical high)
        if (profile.getCriticalMax() != null && temperature.compareTo(profile.getCriticalMax()) > 0) {
            publishThresholdViolatedEvent(freezer.getId(), temperature, profile.getCriticalMax(), "CRITICAL_HIGH",
                    readingId);
            return;
        }

        // Check WARNING_MAX (warning high)
        if (profile.getWarningMax() != null && temperature.compareTo(profile.getWarningMax()) > 0) {
            publishThresholdViolatedEvent(freezer.getId(), temperature, profile.getWarningMax(), "WARNING_HIGH",
                    readingId);
            return;
        }

        // Check CRITICAL_MIN (critical low)
        if (profile.getCriticalMin() != null && temperature.compareTo(profile.getCriticalMin()) < 0) {
            publishThresholdViolatedEvent(freezer.getId(), temperature, profile.getCriticalMin(), "CRITICAL_LOW",
                    readingId);
            return;
        }

        // Check WARNING_MIN (warning low)
        if (profile.getWarningMin() != null && temperature.compareTo(profile.getWarningMin()) < 0) {
            publishThresholdViolatedEvent(freezer.getId(), temperature, profile.getWarningMin(), "WARNING_LOW",
                    readingId);
        }
    }

    /**
     * Checks humidity against the profile's band and publishes
     * FreezerHumidityThresholdViolatedEvent.
     *
     * <p>
     * Checks in priority order: CRITICAL_MAX, WARNING_MAX, CRITICAL_MIN,
     * WARNING_MIN - so a reading beyond the critical bound never reports as a mere
     * warning.
     */
    private void checkHumidityThresholdsWithProfile(Freezer freezer, BigDecimal humidity, Long readingId,
            ThresholdProfile profile) {
        // A device with no humidity register reports nothing to check.
        if (humidity == null) {
            return;
        }

        if (profile.getHumidityCriticalMax() != null && humidity.compareTo(profile.getHumidityCriticalMax()) > 0) {
            publishHumidityViolatedEvent(freezer.getId(), humidity, profile.getHumidityCriticalMax(), "CRITICAL_HIGH",
                    readingId);
            return;
        }

        if (profile.getHumidityWarningMax() != null && humidity.compareTo(profile.getHumidityWarningMax()) > 0) {
            publishHumidityViolatedEvent(freezer.getId(), humidity, profile.getHumidityWarningMax(), "WARNING_HIGH",
                    readingId);
            return;
        }

        if (profile.getHumidityCriticalMin() != null && humidity.compareTo(profile.getHumidityCriticalMin()) < 0) {
            publishHumidityViolatedEvent(freezer.getId(), humidity, profile.getHumidityCriticalMin(), "CRITICAL_LOW",
                    readingId);
            return;
        }

        if (profile.getHumidityWarningMin() != null && humidity.compareTo(profile.getHumidityWarningMin()) < 0) {
            publishHumidityViolatedEvent(freezer.getId(), humidity, profile.getHumidityWarningMin(), "WARNING_LOW",
                    readingId);
        }
    }

    private void publishHumidityViolatedEvent(Long freezerId, BigDecimal humidity, BigDecimal thresholdValue,
            String thresholdType, Long readingId) {
        eventPublisher.publishEvent(new org.openelisglobal.coldstorage.event.FreezerHumidityThresholdViolatedEvent(this,
                freezerId, humidity, thresholdValue, thresholdType, readingId));
        LOGGER.info("Published humidity threshold violated event for freezer {}: {}% (threshold: {}%)", freezerId,
                humidity, thresholdValue);
    }

    /**
     * Check temperature thresholds using simple Freezer thresholds (fallback when
     * no ThresholdProfile assigned).
     *
     * <p>
     * The simple threshold system treats thresholds as absolute deviation from
     * target: - warningThreshold: temperature deviation that triggers WARNING
     * alerts - criticalThreshold: temperature deviation that triggers CRITICAL
     * alerts
     *
     * <p>
     * Example: If targetTemperature=-20°C, warningThreshold=2°C,
     * criticalThreshold=5°C - WARNING: temp > -18°C or temp < -22°C - CRITICAL:
     * temp > -15°C or temp < -25°C
     */
    private void checkSimpleTemperatureThresholds(Freezer freezer, BigDecimal temperature, Long readingId) {
        if (freezer.getTargetTemperature() == null) {
            LOGGER.debug("No target temperature set for freezer {}, skipping threshold checks", freezer.getName());
            return;
        }

        // Skip threshold checks if temperature is null
        if (temperature == null) {
            return;
        }

        BigDecimal target = freezer.getTargetTemperature();
        BigDecimal deviation = temperature.subtract(target).abs();

        // Check CRITICAL threshold (larger deviation)
        if (freezer.getCriticalThreshold() != null && deviation.compareTo(freezer.getCriticalThreshold()) > 0) {
            BigDecimal thresholdValue = temperature.compareTo(target) > 0 ? target.add(freezer.getCriticalThreshold())
                    : target.subtract(freezer.getCriticalThreshold());
            String thresholdType = temperature.compareTo(target) > 0 ? "CRITICAL_HIGH" : "CRITICAL_LOW";
            publishThresholdViolatedEvent(freezer.getId(), temperature, thresholdValue, thresholdType, readingId);
            return;
        }

        // Check WARNING threshold (smaller deviation)
        if (freezer.getWarningThreshold() != null && deviation.compareTo(freezer.getWarningThreshold()) > 0) {
            BigDecimal thresholdValue = temperature.compareTo(target) > 0 ? target.add(freezer.getWarningThreshold())
                    : target.subtract(freezer.getWarningThreshold());
            String thresholdType = temperature.compareTo(target) > 0 ? "WARNING_HIGH" : "WARNING_LOW";
            publishThresholdViolatedEvent(freezer.getId(), temperature, thresholdValue, thresholdType, readingId);
        }
    }

    private void publishThresholdViolatedEvent(Long freezerId, BigDecimal temperature, BigDecimal thresholdValue,
            String thresholdType, Long readingId) {
        org.openelisglobal.coldstorage.event.FreezerTemperatureThresholdViolatedEvent event = new org.openelisglobal.coldstorage.event.FreezerTemperatureThresholdViolatedEvent(
                this, freezerId, temperature, thresholdValue, thresholdType, readingId);
        eventPublisher.publishEvent(event);
        LOGGER.info("Published temperature threshold violated event for freezer {}: {} (threshold: {})", freezerId,
                temperature, thresholdValue);
    }
}
