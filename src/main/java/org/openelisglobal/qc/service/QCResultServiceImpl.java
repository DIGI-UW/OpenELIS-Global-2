package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
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

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
     * This method is called by Feature 004 after parsing ASTM Q-segments
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

        // Verify control lot is ACTIVE
        if (!"ACTIVE".equals(controlLot.getStatus())) {
            throw new IllegalArgumentException(
                    "Control lot is not active: " + controlLotId + " (status: " + controlLot.getStatus() + ")");
        }

        // Retrieve latest statistics for z-score calculation
        QCStatistics statistics = statisticsDAO.findLatestByControlLot(controlLotId);
        if (statistics == null) {
            throw new IllegalArgumentException(
                    "No statistics found for control lot: " + controlLotId + ". Cannot calculate z-score.");
        }

        // Calculate z-score: (value - mean) / standard deviation
        BigDecimal mean = statistics.getMean();
        BigDecimal stdDev = statistics.getStandardDeviation();
        BigDecimal zScore = resultValue.subtract(mean).divide(stdDev, 4, RoundingMode.HALF_UP);

        // Create QC Result entity
        QCResult result = new QCResult();
        result.setId(UUID.randomUUID().toString());
        result.setControlLotId(controlLotId);
        result.setTestId(Integer.valueOf(testId));
        result.setInstrumentId(Integer.valueOf(analyzerId));
        result.setResultValue(resultValue);
        result.setUnitOfMeasure(unit);
        result.setZScore(zScore);
        result.setRunDateTime(Timestamp.valueOf(timestamp));
        result.setResultStatus("PENDING");
        result.setNonConformityFlag(false);

        // Persist result
        String id = resultDAO.insert(result);
        LogEvent.logInfo(this.getClass().getName(), "createQCResult", "Created QC result: " + id);

        // Publish event for async rule evaluation (Constitution pattern)
        // QCResultCreatedEvent will trigger WestgardRuleEvaluationService
        eventPublisher.publishEvent(new QCResultCreatedEvent(this, id));

        // Retrieve and return persisted result
        Optional<QCResult> persistedResult = resultDAO.get(id);
        if (!persistedResult.isPresent()) {
            throw new LIMSRuntimeException("Failed to retrieve persisted QC result: " + id);
        }

        return persistedResult.get();
    }

    /**
     * Event published when QC result is created Triggers async Westgard rule
     * evaluation
     */
    public static class QCResultCreatedEvent {
        private final Object source;
        private final String resultId;

        public QCResultCreatedEvent(Object source, String resultId) {
            this.source = source;
            this.resultId = resultId;
        }

        public Object getSource() {
            return source;
        }

        public String getResultId() {
            return resultId;
        }
    }
}
