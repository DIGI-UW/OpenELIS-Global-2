package org.openelisglobal.qaevent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.dao.DeltaCheckAlertDAO;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.result.dao.ResultDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for delta check evaluation logic. Handles numeric
 * value extraction, threshold comparison, and alert generation for results.
 */
@Service
@DependsOn({ "entityManagerFactory" })
@Transactional
public class DeltaCheckEvaluationServiceImpl implements DeltaCheckEvaluationService {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d*\\.?\\d+([eE][+-]?\\d+)?$");
    private static final int DEFAULT_COMPARISON_PERIOD_DAYS = 90;

    @Autowired
    private DeltaCheckAlertDAO deltaCheckAlertDAO;

    @Autowired
    private DeltaCheckAlertService deltaCheckAlertService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private ResultDAO resultDAO;

    @Autowired
    private IStatusService statusService;

    @Override
    public DeltaCheckAlert evaluateResultForDeltaCheck(Result result) {
        if (!shouldPerformDeltaCheck(result)) {
            return null;
        }

        // Guard: skip if result has no persisted ID yet (inserted but in-memory object
        // not yet updated) to prevent storing alerts with resultId = null/"0"
        if (result.getId() == null || result.getId().isBlank() || "0".equals(result.getId())) {
            LogEvent.logWarn(this.getClass().getName(), "evaluateResultForDeltaCheck",
                    "Skipping delta check for result with unpersisted ID");
            return null;
        }

        Result previousResult = findPreviousResultForComparison(result);
        if (previousResult == null) {
            return null;
        }

        BigDecimal currentValue = extractNumericValue(result);
        BigDecimal previousValue = extractNumericValue(previousResult);

        if (currentValue == null || previousValue == null) {
            return null;
        }

        BigDecimal threshold = getThresholdForResult(result);
        if (threshold == null) {
            return null;
        }

        BigDecimal changePercent = calculatePercentageChange(currentValue, previousValue);

        boolean percentThresholdExceeded = changePercent.compareTo(threshold) > 0;
        boolean absoluteThresholdExceeded = isAbsoluteThresholdExceeded(currentValue, previousValue);

        if (percentThresholdExceeded || absoluteThresholdExceeded) {
            if (!deltaCheckAlertDAO.alertExistsForResultComparison(result.getId(), previousResult.getId())) {
                DeltaCheckAlert alert = new DeltaCheckAlert(result.getId(), previousResult.getId(), currentValue,
                        previousValue, threshold);
                alert.setPreviousResultDate(extractCollectionDate(previousResult));
                return deltaCheckAlertService.save(alert);
            }
        }

        return null;
    }

    /**
     * Returns true if the absolute difference between current and previous values
     * exceeds the configured {@code nceDeltaCheckAbsoluteThreshold}. Returns false
     * when the threshold is not configured (empty/null).
     */
    private boolean isAbsoluteThresholdExceeded(BigDecimal currentValue, BigDecimal previousValue) {
        String rawThreshold = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ABSOLUTE_THRESHOLD);
        if (rawThreshold == null || rawThreshold.isBlank()) {
            return false;
        }
        BigDecimal absoluteThreshold;
        try {
            absoluteThreshold = new BigDecimal(rawThreshold.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (absoluteThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal absoluteChange = currentValue.subtract(previousValue).abs();
        return absoluteChange.compareTo(absoluteThreshold) > 0;
    }

    /**
     * Extracts the sample collection date from the previous result's analysis
     * chain. Returns null if any part of the chain is unavailable.
     */
    private java.sql.Timestamp extractCollectionDate(Result previousResult) {
        try {
            if (previousResult.getAnalysis() == null || previousResult.getAnalysis().getSampleItem() == null
                    || previousResult.getAnalysis().getSampleItem().getSample() == null) {
                return null;
            }
            return previousResult.getAnalysis().getSampleItem().getSample().getCollectionDate();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result findPreviousResultForComparison(Result result) {
        if (result == null || result.getAnalysis() == null) {
            return null;
        }

        Analysis analysis = result.getAnalysis();
        if (analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
            return null;
        }

        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient == null) {
            return null;
        }

        Integer testId = analysis.getTest() != null ? Integer.parseInt(analysis.getTest().getId()) : null;
        Integer analyteId = result.getAnalyte() != null ? Integer.parseInt(result.getAnalyte().getId()) : null;

        if (testId == null) {
            return null;
        }

        // Exclude the current sample so we don't compare a result to another from the
        // same visit/encounter.
        Integer currentSampleId = Integer.parseInt(sample.getId());
        List<Result> previousResults = queryPreviousResults(Integer.parseInt(patient.getId()), testId, analyteId,
                currentSampleId, 1);
        return previousResults.isEmpty() ? null : previousResults.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> findPreviousResultsForPatient(Patient patient, Integer testId, Integer analyteId,
            int maxResults) {
        if (patient == null || testId == null) {
            return List.of();
        }
        return queryPreviousResults(Integer.parseInt(patient.getId()), testId, analyteId, null, maxResults);
    }

    /**
     * Internal query: finds finalized results for a patient+test, excluding the
     * current sample and restricting to the configured comparison period.
     */
    private List<Result> queryPreviousResults(Integer patientId, Integer testId, Integer analyteId,
            Integer excludeSampleId, int maxResults) {
        int days = getComparisonPeriodDays();
        Date cutoffDate = new Date(Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli());
        Integer finalizedStatusId;
        try {
            finalizedStatusId = Integer.parseInt(statusService.getStatusID(AnalysisStatus.Finalized));
        } catch (NumberFormatException e) {
            LogEvent.logError(this.getClass().getName(), "queryPreviousResults",
                    "Could not parse finalized status ID; skipping delta check history query");
            return List.of();
        }

        return resultDAO.getPreviousResultsForPatient(patientId, testId, analyteId, excludeSampleId, maxResults,
                cutoffDate, finalizedStatusId);
    }

    /**
     * Gets the delta check comparison period in days from the global configuration.
     * If the configuration value is empty, null, or cannot be parsed as an integer,
     * the default comparison period of {@link #DEFAULT_COMPARISON_PERIOD_DAYS} is
     * used.
     * 
     * @return the comparison period in days
     */
    private int getComparisonPeriodDays() {
        String value = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_COMPARISON_PERIOD);
        if (value == null || value.isBlank()) {
            return DEFAULT_COMPARISON_PERIOD_DAYS;
        }
        try {
            int days = Integer.parseInt(value.trim());
            return days > 0 ? days : DEFAULT_COMPARISON_PERIOD_DAYS;
        } catch (NumberFormatException e) {
            return DEFAULT_COMPARISON_PERIOD_DAYS;
        }
    }

    @Override
    public BigDecimal calculatePercentageChange(BigDecimal currentValue, BigDecimal previousValue) {
        if (previousValue == null || previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (currentValue == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = currentValue.subtract(previousValue).abs();
        BigDecimal percentChange = difference.divide(previousValue.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percentChange.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNumericValue(Result result) {
        if (result == null || result.getValue() == null) {
            return false;
        }

        String value = result.getValue().trim();
        if (value.isEmpty()) {
            return false;
        }

        if (value.startsWith("<") || value.startsWith(">")) {
            value = value.substring(1).trim();
        }

        return NUMERIC_PATTERN.matcher(value).matches();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal extractNumericValue(Result result) {
        if (!hasNumericValue(result)) {
            return null;
        }

        String value = result.getValue().trim();

        if (value.startsWith("<") || value.startsWith(">")) {
            value = value.substring(1).trim();
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldPerformDeltaCheck(Result result) {
        if (result == null) {
            return false;
        }

        if (!hasNumericValue(result)) {
            return false;
        }

        if (result.getAnalysis() == null) {
            return false;
        }

        String enabledValue = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED);
        return "true".equalsIgnoreCase(enabledValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getThresholdForResult(Result result) {
        if (result == null || result.getAnalysis() == null) {
            return null;
        }

        String thresholdValue = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_THRESHOLD);
        if (thresholdValue == null || thresholdValue.isBlank()) {
            return new BigDecimal("30");
        }
        try {
            return new BigDecimal(thresholdValue);
        } catch (NumberFormatException e) {
            return new BigDecimal("30");
        }
    }

    @Override
    public List<DeltaCheckAlert> batchEvaluateResults(List<Result> results) {
        List<DeltaCheckAlert> alerts = new ArrayList<>();

        for (Result result : results) {
            DeltaCheckAlert alert = evaluateResultForDeltaCheck(result);
            if (alert != null) {
                alerts.add(alert);
            }
        }

        return alerts;
    }

    @Override
    public DeltaCheckAlert reEvaluateAlert(Integer alertId) {
        DeltaCheckAlert alert;
        try {
            alert = deltaCheckAlertService.get(alertId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }

        BigDecimal recalculatedChange = calculatePercentageChange(alert.getCurrentValue(), alert.getPreviousValue());
        alert.setChangePercent(recalculatedChange);

        return deltaCheckAlertService.save(alert);
    }
}
