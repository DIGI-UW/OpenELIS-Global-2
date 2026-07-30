package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.event.QCResultCreatedEvent;
import org.openelisglobal.qc.service.calculator.StatisticsCalculator;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Result management (T140)
 *
 * Primary integration point for Feature 004 ASTM interface
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers) Following Constitution IV.4: Services compile ALL data within
 * transaction
 */
@Service
public class QCResultServiceImpl extends BaseObjectServiceImpl<QCResult, String> implements QCResultService {

    /**
     * System user ID used for automated analyzer pipeline operations (no user
     * session).
     */
    private static final int SYSTEM_AUTOMATION_USER_ID = 1;

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private List<StatisticsCalculator> statisticsCalculators;

    @Autowired
    private TestService testService;

    public QCResultServiceImpl() {
        super(QCResult.class);
    }

    @Override
    protected QCResultDAO getBaseObjectDAO() {
        return resultDAO;
    }

    /**
     * Create a QC result from analyzer data (Task T140)
     *
     * This method is called by Feature 004 after parsing ASTM Q-segments.
     *
     * Supports three lot statuses: - ACTIVE: normal operation, z-score computed
     * from existing statistics - ESTABLISHMENT: bootstrapping phase for
     * ROLLING/INITIAL_RUNS lots; results are saved with null z-score until enough
     * data accumulates to compute statistics, at which point the lot transitions to
     * ACTIVE - Any other status (EXPIRED, ARCHIVED): rejected
     *
     * @throws IllegalArgumentException if validation fails
     */
    @Override
    @Transactional
    public QCResult createQCResult(String analyzerId, String testId, String controlLotId, String controlLevel,
            BigDecimal resultValue, String unit, LocalDateTime timestamp) throws IllegalArgumentException {

        // Validation
        if (resultValue == null) {
            throw new IllegalArgumentException("Result value cannot be null");
        }

        // Retrieve control lot (compile data within transaction per Constitution IV.4)
        Optional<QCControlLot> lotOpt = controlLotDAO.get(controlLotId);
        if (!lotOpt.isPresent()) {
            throw new IllegalArgumentException("Control lot not found: " + controlLotId);
        }

        QCControlLot controlLot = lotOpt.get();
        String status = controlLot.getStatus();
        boolean isEstablishment = "ESTABLISHMENT".equals(status);

        // Allow ACTIVE and ESTABLISHMENT; reject everything else (EXPIRED, ARCHIVED)
        if (!"ACTIVE".equals(status) && !isEstablishment) {
            throw new IllegalArgumentException(
                    "Control lot is not active: " + controlLotId + " (status: " + status + ")");
        }

        // Retrieve latest statistics for z-score calculation
        QCStatistics statistics = statisticsDAO.findLatestByControlLot(controlLotId);

        // During establishment, statistics may not exist yet — that's expected.
        // For ACTIVE lots, statistics must exist.
        if (statistics == null && !isEstablishment) {
            throw new IllegalArgumentException(
                    "No statistics found for control lot: " + controlLotId + ". Cannot calculate z-score.");
        }

        // Calculate z-score (null during establishment when no statistics exist)
        BigDecimal zScore = null;
        if (statistics != null) {
            BigDecimal mean = statistics.getMean();
            BigDecimal stdDev = statistics.getStandardDeviation();
            if (stdDev == null || stdDev.compareTo(BigDecimal.ZERO) == 0) {
                zScore = BigDecimal.ZERO;
            } else {
                zScore = resultValue.subtract(mean).divide(stdDev, 4, RoundingMode.HALF_UP);
            }
        }

        // Create QC Result entity
        QCResult result = new QCResult();
        result.setId(UUID.randomUUID().toString());
        result.setControlLotId(controlLotId);
        result.setTestId(testId);
        result.setInstrumentId(analyzerId);
        result.setResultValue(resultValue);
        result.setUnitOfMeasure(resolveUnit(unit, testId));
        result.setZScore(zScore);
        result.setRunDateTime(Timestamp.valueOf(timestamp));
        result.setResultStatus("PENDING");
        result.setNonConformityFlag(false);
        // sys_user_id for automated QC results (analyzer pipeline — no user session)
        result.setSystemUserId(SYSTEM_AUTOMATION_USER_ID);
        result.setSysUserId(String.valueOf(SYSTEM_AUTOMATION_USER_ID));

        // Persist result
        String id = resultDAO.insert(result);
        LogEvent.logInfo(this.getClass().getName(), "createQCResult", "Created QC result: " + id);

        // Retrieve persisted result
        Optional<QCResult> persistedResult = resultDAO.get(id);
        if (!persistedResult.isPresent()) {
            throw new LIMSRuntimeException("Failed to retrieve persisted QC result: " + id);
        }

        // During establishment, try to compute statistics now that we have a new
        // result.
        // If enough results have accumulated for the rolling window, compute stats
        // and transition the lot to ACTIVE.
        int sysUserId = result.getSystemUserId();
        if (isEstablishment) {
            tryBootstrapStatistics(controlLot, sysUserId);
        } else if ("ROLLING".equals(controlLot.getCalculationMethod())) {
            // For active rolling lots, recalculate statistics with each new result
            // so the window slides forward.
            recalculateRollingStatistics(controlLot, sysUserId);
        }

        // Only publish event for rule evaluation when the lot is ACTIVE and
        // the result has a z-score. During establishment there are no
        // meaningful statistics to evaluate rules against.
        if ("ACTIVE".equals(controlLot.getStatus()) && persistedResult.get().getZScore() != null) {
            eventPublisher.publishEvent(new QCResultCreatedEvent(this, persistedResult.get()));
        }

        return persistedResult.get();
    }

    /**
     * Resolve the unit of measure for a QC result. If the observation carried a
     * unit, use it. Otherwise, look up the test definition's unit — the profile's
     * {@code default_test_mappings} already stores the unit per test code, and the
     * {@code Test} entity's {@code unitOfMeasure} field reflects it.
     */
    private String resolveUnit(String observationUnit, String testId) {
        if (observationUnit != null && !observationUnit.isBlank()) {
            return observationUnit;
        }
        try {
            Test test = testService.get(testId);
            if (test != null && test.getUnitOfMeasure() != null) {
                String uom = test.getUnitOfMeasure().getUnitOfMeasureName();
                if (uom != null && !uom.isBlank()) {
                    return uom;
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "resolveUnit",
                    "Could not look up unit for testId=" + testId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Attempt to compute initial statistics for a lot in ESTABLISHMENT status.
     * Called after each new result is persisted. If enough results exist to fill
     * the calculator's window, statistics are computed, persisted, and the lot
     * transitions to ACTIVE.
     */
    private void tryBootstrapStatistics(QCControlLot controlLot, int sysUserId) {
        QCStatistics computed = computeStatistics(controlLot);
        if (computed == null) {
            return; // Not enough results yet
        }

        // Persist the computed statistics
        computed.setId(UUID.randomUUID().toString());
        computed.setValidityStart(new Timestamp(System.currentTimeMillis()));
        computed.setSystemUserId(sysUserId);
        computed.setSysUserId(String.valueOf(sysUserId));
        statisticsDAO.insert(computed);

        // Transition lot to ACTIVE
        controlLot.setStatus("ACTIVE");
        controlLot.setSysUserId(String.valueOf(sysUserId));
        controlLotDAO.update(controlLot);

        LogEvent.logInfo(this.getClass().getName(), "tryBootstrapStatistics", "Bootstrapped statistics for lot "
                + controlLot.getId() + " (method=" + controlLot.getCalculationMethod() + "), transitioned to ACTIVE");
    }

    /**
     * Recalculate rolling statistics for an ACTIVE lot after a new result. The
     * window slides forward to include the most recent N results.
     */
    private void recalculateRollingStatistics(QCControlLot controlLot, int sysUserId) {
        QCStatistics computed = computeStatistics(controlLot);
        if (computed == null) {
            return;
        }

        computed.setId(UUID.randomUUID().toString());
        computed.setValidityStart(new Timestamp(System.currentTimeMillis()));
        computed.setSystemUserId(sysUserId);
        computed.setSysUserId(String.valueOf(sysUserId));
        statisticsDAO.insert(computed);
    }

    /**
     * Compute statistics for a control lot using the appropriate calculator
     * (ROLLING, INITIAL_RUNS, etc.).
     *
     * @return computed statistics, or null if insufficient data or no calculator
     *         found
     */
    private QCStatistics computeStatistics(QCControlLot controlLot) {
        String calculationMethod = controlLot.getCalculationMethod();

        StatisticsCalculator calculator = statisticsCalculators.stream().filter(c -> c.supports(calculationMethod))
                .findFirst().orElse(null);
        if (calculator == null) {
            return null;
        }

        // Get results in DESC order (as expected by RollingCalculator)
        List<QCResult> results = resultDAO.findByControlLot(controlLot.getId());

        // calculate() returns null if insufficient data for the window size
        return calculator.calculate(controlLot, results);
    }
}
