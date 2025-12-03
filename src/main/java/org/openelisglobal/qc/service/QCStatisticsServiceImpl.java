package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
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

    private static final MathContext MATH_CONTEXT = new MathContext(15, RoundingMode.HALF_UP);
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
        List<QCResult> initialResults = results.subList(0, requiredRuns);

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLotId);
        statistics.setMean(calculateMean(initialResults));
        statistics.setStandardDeviation(calculateStandardDeviation(initialResults));
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

        // Use the most recent N results for rolling window
        List<QCResult> recentResults = results.subList(Math.max(0, results.size() - windowSize), results.size());

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLotId);
        statistics.setMean(calculateMean(recentResults));
        statistics.setStandardDeviation(calculateStandardDeviation(recentResults));
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
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate mean: results list is empty");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (QCResult result : results) {
            sum = sum.add(result.getResultValue());
        }

        return sum.divide(new BigDecimal(results.size()), SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateStandardDeviation(List<QCResult> results) throws IllegalArgumentException {
        if (results == null || results.size() < 2) {
            throw new IllegalArgumentException("Cannot calculate standard deviation: need at least 2 values");
        }

        BigDecimal mean = calculateMean(results);

        // Calculate sum of squared differences: sum((x - mean)^2)
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (QCResult result : results) {
            BigDecimal diff = result.getResultValue().subtract(mean);
            BigDecimal squaredDiff = diff.multiply(diff, MATH_CONTEXT);
            sumSquaredDiff = sumSquaredDiff.add(squaredDiff);
        }

        // Sample standard deviation: sqrt(sum / (n-1))
        BigDecimal variance = sumSquaredDiff.divide(new BigDecimal(results.size() - 1), MATH_CONTEXT);

        // Calculate square root using Newton's method
        return sqrt(variance, SCALE);
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

    /**
     * Calculate square root using Newton's method (Babylonian method). BigDecimal
     * does not have a built-in sqrt, so we implement it.
     *
     * @param value The value to find square root of
     * @param scale The decimal scale
     * @return Square root
     */
    private BigDecimal sqrt(BigDecimal value, int scale) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal two = new BigDecimal(2);
        BigDecimal x = value.divide(two, scale, RoundingMode.HALF_UP);
        BigDecimal lastX = BigDecimal.ZERO;

        // Newton's method: x_new = (x + value/x) / 2
        while (!x.equals(lastX)) {
            lastX = x;
            x = value.divide(x, scale, RoundingMode.HALF_UP);
            x = x.add(lastX);
            x = x.divide(two, scale, RoundingMode.HALF_UP);
        }

        return x;
    }
}
