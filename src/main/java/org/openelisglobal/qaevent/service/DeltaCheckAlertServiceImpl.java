package org.openelisglobal.qaevent.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.dao.DeltaCheckAlertDAO;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing delta check alerts. Provides business
 * logic for alert management and resolving. Evaluation logic is delegated to
 * {@link DeltaCheckEvaluationService}.
 */
@Service
@DependsOn({ "entityManagerFactory" })
@Transactional
public class DeltaCheckAlertServiceImpl extends AuditableBaseObjectServiceImpl<DeltaCheckAlert, Integer>
        implements DeltaCheckAlertService {

    @Autowired
    protected DeltaCheckAlertDAO baseObjectDAO;

    @Autowired
    private DeltaCheckEvaluationService deltaCheckEvaluationService;

    public DeltaCheckAlertServiceImpl() {
        super(DeltaCheckAlert.class);
    }

    @Override
    protected DeltaCheckAlertDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    // --- Evaluation methods delegated to DeltaCheckEvaluationService ---

    @Override
    public DeltaCheckAlert evaluateResultForDeltaCheck(Result result) {
        return deltaCheckEvaluationService.evaluateResultForDeltaCheck(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Result findPreviousResultForComparison(Result result) {
        return deltaCheckEvaluationService.findPreviousResultForComparison(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> findPreviousResultsForPatient(Patient patient, Integer testId, Integer analyteId,
            int maxResults) {
        return deltaCheckEvaluationService.findPreviousResultsForPatient(patient, testId, analyteId, maxResults);
    }

    @Override
    public BigDecimal calculatePercentageChange(BigDecimal currentValue, BigDecimal previousValue) {
        return deltaCheckEvaluationService.calculatePercentageChange(currentValue, previousValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNumericValue(Result result) {
        return deltaCheckEvaluationService.hasNumericValue(result);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal extractNumericValue(Result result) {
        return deltaCheckEvaluationService.extractNumericValue(result);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldPerformDeltaCheck(Result result) {
        return deltaCheckEvaluationService.shouldPerformDeltaCheck(result);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getThresholdForResult(Result result) {
        return deltaCheckEvaluationService.getThresholdForResult(result);
    }

    @Override
    public List<DeltaCheckAlert> batchEvaluateResults(List<Result> results) {
        return deltaCheckEvaluationService.batchEvaluateResults(results);
    }

    @Override
    public DeltaCheckAlert reEvaluateAlert(Integer alertId) {
        return deltaCheckEvaluationService.reEvaluateAlert(alertId);
    }

    // --- Alert management methods ---

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getActiveAlertsForResult(String resultId) {
        return baseObjectDAO.getActiveAlertsForResult(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAllAlertsForResult(String resultId) {
        return baseObjectDAO.getAllAlertsForResult(resultId);
    }

    @Override
    public DeltaCheckAlert dismissAlert(Integer alertId, String reason, String dismissedBy) {
        DeltaCheckAlert alert;
        try {
            alert = get(alertId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }

        if (!alert.isActive()) {
            throw new IllegalArgumentException("Alert is not active and cannot be dismissed");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Dismissal reason is required");
        }

        if (dismissedBy == null || dismissedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("DismissedBy is required");
        }

        alert.dismiss(reason, dismissedBy);
        return save(alert);
    }

    @Override
    public DeltaCheckAlert escalateAlertToNCE(Integer alertId, NcEvent ncEvent) {
        DeltaCheckAlert alert;
        try {
            alert = get(alertId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }

        if (!alert.isActive()) {
            throw new IllegalArgumentException("Alert is not active and cannot be escalated");
        }

        if (ncEvent == null) {
            throw new IllegalArgumentException("NCE cannot be null");
        }

        alert.escalateToNCE(ncEvent);
        return save(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsByStatus(AlertStatus status) {
        return baseObjectDAO.getAlertsByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsNeedingAttention(int hoursOld) {
        return baseObjectDAO.getAlertsNeedingAttention(hoursOld);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertStatistics(int daysSince) {
        Timestamp cutoffDate = Timestamp.from(Instant.now().minus(daysSince, ChronoUnit.DAYS));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", baseObjectDAO.countAlertsSince(cutoffDate, null));
        stats.put("activeAlerts", baseObjectDAO.countAlertsSince(cutoffDate, AlertStatus.ACTIVE));
        stats.put("dismissedAlerts", baseObjectDAO.countAlertsSince(cutoffDate, AlertStatus.DISMISSED));
        stats.put("escalatedAlerts", baseObjectDAO.countAlertsSince(cutoffDate, AlertStatus.ESCALATED_NCE));
        stats.put("averageChangePercent", baseObjectDAO.getAverageChangePercentSince(cutoffDate));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getRecentAlerts(int daysSince) {
        return baseObjectDAO.getRecentAlerts(daysSince);
    }

    @Override
    public int cleanupOldResolvedAlerts(int daysSinceResolution) {
        return baseObjectDAO.deleteOldResolvedAlerts(daysSinceResolution);
    }

    @Override
    public int bulkDismissAlerts(List<Integer> alertIds, String reason, String dismissedBy) {
        int dismissed = 0;

        for (Integer alertId : alertIds) {
            try {
                dismissAlert(alertId, reason, dismissedBy);
                dismissed++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "bulkDismissAlerts",
                        "Failed to dismiss alert " + alertId + ": " + e.getMessage());
            }
        }

        return dismissed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getFilteredAlerts(AlertStatus status, List<String> analysisIds, int recentDays) {
        // Delegate analysis-ID filtering to the database to avoid N+1 lazy loading
        if (analysisIds != null && !analysisIds.isEmpty()) {
            return baseObjectDAO.getAlertsForAnalyses(analysisIds, status);
        }

        if (status != null) {
            return getAlertsByStatus(status);
        }

        return getRecentAlerts(recentDays);
    }
}
