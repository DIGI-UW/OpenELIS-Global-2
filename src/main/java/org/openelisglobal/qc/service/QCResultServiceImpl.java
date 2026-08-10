package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.event.BenchControlFailedEvent;
import org.openelisglobal.qc.event.QCResultCreatedEvent;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.calculator.StatisticsCalculator;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
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

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> findLatestAcceptedBefore(String instrumentId, String testId, Timestamp before) {
        return resultDAO.findLatestAcceptedBefore(instrumentId, testId, before);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> findLatestAcceptedBenchResultBefore(String testSectionId, String testId, Timestamp before) {
        return resultDAO.findLatestAcceptedBenchResultBefore(testSectionId, testId, before);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> findBenchResults(Timestamp startDate, Timestamp endDate, QCSource source, int maxRows) {
        return resultDAO.findBenchResults(startDate, endDate, source, maxRows);
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
        BigDecimal zScore = statistics != null ? computeZScore(resultValue, statistics) : null;

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
     * Record a bench control run (OGC-1147). See
     * {@link QCResultService#createBenchQCResult(BenchQCCaptureForm, int)} for the
     * contract; this method deliberately mirrors the ASTM path above rather than
     * refactoring it, so the shipped analyzer flow is untouched (NFR-1).
     */
    @Override
    @Transactional
    public QCResult createBenchQCResult(BenchQCCaptureForm capture, int sysUserId) throws IllegalArgumentException {

        QCSource source = capture.getSource();
        if (source == null || !source.isBenchEntered()) {
            throw new IllegalArgumentException("Bench capture requires source MANUAL or RDT, got: " + source);
        }
        QCQualitativeOutcome outcome = capture.getQualitativeOutcome();
        if (outcome == null) {
            throw new IllegalArgumentException("Bench capture requires a qualitative outcome");
        }
        // The DB CHECK guards value-vs-outcome presence; it deliberately does not pair
        // outcome vocabularies to sources, so that OGC-427/428 can add sources without
        // rewriting the constraint. That pairing is enforced here instead.
        if (!outcome.isValidFor(source)) {
            throw new IllegalArgumentException("Outcome " + outcome + " is not valid for source " + source);
        }

        BigDecimal resultValue = capture.getResultValue();
        if (source.isQuantitative() && resultValue == null) {
            throw new IllegalArgumentException("A " + source + " control requires a measured value");
        }
        if (!source.isQuantitative() && resultValue != null) {
            // FR-A3: never store a synthetic number for a qualitative outcome.
            throw new IllegalArgumentException("An " + source + " control must not carry a measured value");
        }

        // A control lot is optional for RDT (the cassette is named by controlLabel) but
        // is
        // what makes a manual quantitative run plottable, so validate it when present.
        QCControlLot controlLot = null;
        if (capture.getControlLotId() != null) {
            controlLot = controlLotDAO.get(capture.getControlLotId()).orElseThrow(
                    () -> new IllegalArgumentException("Control lot not found: " + capture.getControlLotId()));
            String status = controlLot.getStatus();
            if (!"ACTIVE".equals(status) && !"ESTABLISHMENT".equals(status)) {
                throw new IllegalArgumentException(
                        "Control lot is not usable: " + controlLot.getId() + " (status: " + status + ")");
            }
            // The UI only offers bench lots, but the API must refuse too: a bench
            // point recorded against an analyzer's lot enters that analyzer's
            // history and its multi-result Westgard windows.
            if (StringUtils.isNotBlank(controlLot.getInstrumentId())) {
                throw new IllegalArgumentException("Control lot " + controlLot.getId()
                        + " belongs to an analyzer; a bench capture requires a bench (no-analyzer) lot");
            }
        } else if (source.isQuantitative()) {
            throw new IllegalArgumentException("A " + source + " control requires a control lot");
        }

        // Same z-score arithmetic as the analyzer path. Null when the lot has no
        // statistics yet (establishment) or when there is no number at all (RDT) — and
        // a
        // null z-score is what keeps RDT runs out of Westgard evaluation, per D4.
        BigDecimal zScore = null;
        if (controlLot != null && resultValue != null) {
            QCStatistics statistics = statisticsDAO.findLatestByControlLot(controlLot.getId());
            if (statistics != null) {
                zScore = computeZScore(resultValue, statistics);
            }
        }

        LocalDateTime runAt = capture.getRunDateTime() != null ? capture.getRunDateTime() : LocalDateTime.now();

        QCResult result = new QCResult();
        result.setId(UUID.randomUUID().toString());
        result.setSource(source);
        result.setQualitativeOutcome(outcome);
        result.setTestId(capture.getTestId());
        result.setTestSectionId(capture.getTestSectionId());
        result.setControlLotId(capture.getControlLotId());
        result.setControlLabel(capture.getControlLabel());
        result.setResultValue(resultValue);
        result.setUnitOfMeasure(resolveUnit(capture.getUnitOfMeasure(), capture.getTestId()));
        result.setExpectedValue(capture.getExpectedValue());
        result.setUncertainty(capture.getUncertainty());
        result.setZScore(zScore);
        result.setRunDateTime(Timestamp.valueOf(runAt));
        // The tech has already judged this run, so it is not PENDING evaluation the way
        // an
        // analyzer result is: record their verdict directly.
        result.setResultStatus(outcome.isFailing() ? "REJECTED" : "ACCEPTED");
        result.setNonConformityFlag(outcome.isFailing());
        result.setExternalNotes(capture.getNotes());
        // The acting technician, not SYSTEM_AUTOMATION_USER_ID — this is the seam that
        // made
        // a bench path impossible before OGC-1147.
        result.setSystemUserId(sysUserId);
        result.setSysUserId(String.valueOf(sysUserId));
        result.setTechnicianId(sysUserId);

        String id = resultDAO.insert(result);
        LogEvent.logInfo(this.getClass().getName(), "createBenchQCResult",
                "Recorded " + source + " QC result " + id + " outcome=" + outcome);

        QCResult persisted = resultDAO.get(id)
                .orElseThrow(() -> new LIMSRuntimeException("Failed to retrieve persisted QC result: " + id));

        // Identical gate to the analyzer path: only a lot with real statistics and a
        // computed z-score reaches rule evaluation.
        if (controlLot != null && "ACTIVE".equals(controlLot.getStatus()) && persisted.getZScore() != null) {
            eventPublisher.publishEvent(new QCResultCreatedEvent(this, persisted));
        }

        // Raised as an after-commit event, never inline. Anything that throws while
        // raising the signal would mark THIS transaction rollback-only, and catching it
        // here would not undo that — the control result would be silently discarded by
        // the very code meant to react to it. See BenchControlFailedEventListener.
        if (outcome.isFailing()) {
            eventPublisher.publishEvent(new BenchControlFailedEvent(this, persisted));
        }

        return persisted;
    }

    /**
     * Z-score of a measured value against a lot's established statistics. Extracted
     * so the analyzer and bench paths cannot drift apart; behaviour is unchanged
     * from the inline form it replaces, including returning zero rather than
     * dividing by a zero SD.
     */
    private BigDecimal computeZScore(BigDecimal resultValue, QCStatistics statistics) {
        BigDecimal stdDev = statistics.getStandardDeviation();
        if (stdDev == null || stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return resultValue.subtract(statistics.getMean()).divide(stdDev, 4, RoundingMode.HALF_UP);
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
