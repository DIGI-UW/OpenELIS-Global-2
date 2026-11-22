package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.service.calculator.SampleStatistics;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Statistics calculation and management.
 * Implements statistical calculations per US6.
 */
@Service
public class QCStatisticsServiceImpl extends AuditableBaseObjectServiceImpl<QCStatistics, String>
        implements QCStatisticsService {

    private static final int SCALE = 5;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private QCResultDAO resultDAO;

    public QCStatisticsServiceImpl() {
        super(QCStatistics.class);
    }

    @Override
    protected QCStatisticsDAO getBaseObjectDAO() {
        return statisticsDAO;
    }

    @Override
    @Transactional
    public QCStatistics calculateInitialRunsStatistics(String controlLotId, Integer requiredRuns)
            throws IllegalArgumentException {
        List<QCResult> results = resultDAO.findByControlLot(controlLotId);

        if (results.size() < requiredRuns) {
            throw new IllegalArgumentException(
                    String.format("Insufficient data: %d results found, %d required", results.size(), requiredRuns));
        }

        // Use only the first N results for initial runs
        SampleStatistics stats = SampleStatistics.of(results.subList(0, requiredRuns), SCALE);

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLotId);
        statistics.setMean(stats.mean());
        statistics.setStandardDeviation(stats.standardDeviation());
        statistics.setNumValues(requiredRuns);
        statistics.setCalculationMethod("INITIAL_RUNS");
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setValidityStart(new Timestamp(System.currentTimeMillis()));

        String id = statisticsDAO.insert(statistics);
        return statisticsDAO.get(id).orElse(null);
    }

    @Override
    @Transactional
    public QCStatistics calculateRollingStatistics(String controlLotId, Integer windowSize)
            throws IllegalArgumentException {
        List<QCResult> results = resultDAO.findByControlLot(controlLotId);

        if (results.size() < windowSize) {
            throw new IllegalArgumentException(
                    String.format("Insufficient data: %d results found, %d required", results.size(), windowSize));
        }

        // Use the most recent N results for rolling window. findByControlLot orders
        // by run date DESC, so the newest runs are at the head of the list.
        SampleStatistics stats = SampleStatistics.of(results.subList(0, windowSize), SCALE);

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLotId);
        statistics.setMean(stats.mean());
        statistics.setStandardDeviation(stats.standardDeviation());
        statistics.setNumValues(windowSize);
        statistics.setCalculationMethod("ROLLING");
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setValidityStart(new Timestamp(System.currentTimeMillis()));

        String id = statisticsDAO.insert(statistics);
        return statisticsDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public QCStatistics getLatestStatistics(String controlLotId) {
        return statisticsDAO.findLatestByControlLot(controlLotId);
    }

    @Override
    public BigDecimal calculateMean(List<QCResult> results) throws IllegalArgumentException {
        return SampleStatistics.mean(results, SCALE);
    }

    @Override
    public BigDecimal calculateStandardDeviation(List<QCResult> results) throws IllegalArgumentException {
        return SampleStatistics.of(results, SCALE).standardDeviation();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCStatistics> getStatisticsByMethod(String controlLotId, String calculationMethod) {
        return statisticsDAO.findByCalculationMethod(controlLotId, calculationMethod);
    }

    @Override
    @Transactional
    public QCStatistics updateStatistics(QCStatistics statistics) {
        return statisticsDAO.update(statistics);
    }

    @Override
    @Transactional
    public void invalidateOldStatistics(String controlLotId) {
        QCStatistics latest = statisticsDAO.findLatestByControlLot(controlLotId);
        if (latest != null && latest.getValidityEnd() == null) {
            latest.setValidityEnd(new Timestamp(System.currentTimeMillis()));
            statisticsDAO.update(latest);
        }
    }
}
