package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
            boolean transmissionOk, String errorMessage) {
        ThresholdProfile profile = thresholdEvaluationService.resolveActiveProfile(freezer, recordedAt);
        if (profile == null) {
            LOGGER.debug("No threshold profile configured for freezer {}, using default NORMAL status",
                    freezer.getName());
        }

        FreezerReading.Status status = determineStatus(temperature, humidity, transmissionOk, profile);
        FreezerReading savedReading = freezerReadingService.saveReading(freezer, recordedAt, temperature, humidity,
                status, transmissionOk, errorMessage);

        // Check temperature thresholds and publish events for alert system
        if (profile != null) {
            // Use advanced ThresholdProfile if available
            checkTemperatureThresholdsWithProfile(freezer, temperature, savedReading.getId(), profile);
        } else {
            // Fall back to simple freezer thresholds if no profile assigned
            checkSimpleTemperatureThresholds(freezer, temperature, savedReading.getId());
        }
    }

    private FreezerReading.Status determineStatus(BigDecimal temperature, BigDecimal humidity, boolean transmissionOk,
            ThresholdProfile profile) {
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
