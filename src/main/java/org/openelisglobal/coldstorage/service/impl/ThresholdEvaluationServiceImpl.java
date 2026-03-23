package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.coldstorage.dao.FreezerThresholdProfileDAO;
import org.openelisglobal.coldstorage.service.ThresholdEvaluationService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdEvaluationServiceImpl implements ThresholdEvaluationService {

    private final FreezerThresholdProfileDAO freezerThresholdProfileDAO;

    public ThresholdEvaluationServiceImpl(FreezerThresholdProfileDAO freezerThresholdProfileDAO) {
        this.freezerThresholdProfileDAO = freezerThresholdProfileDAO;
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
}
