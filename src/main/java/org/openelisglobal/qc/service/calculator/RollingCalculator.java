package org.openelisglobal.qc.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Calculator for rolling window method. Calculates mean and standard deviation
 * using most recent N results (moving window). Per US6: Should recalculate
 * statistics with each new result.
 */
@Component
public class RollingCalculator implements StatisticsCalculator {

    private static final int DEFAULT_WINDOW_SIZE = 20;

    @Override
    public boolean supports(String calculationMethod) {
        return "ROLLING".equals(calculationMethod);
    }

    @Override
    public QCStatistics calculate(QCControlLot controlLot, List<QCResult> results) {
        int windowSize = controlLot.getInitialRunsCount() != null ? controlLot.getInitialRunsCount()
                : DEFAULT_WINDOW_SIZE;

        // Check if we have enough results
        if (results == null || results.size() < windowSize) {
            return null; // Insufficient data
        }

        // Take most recent N results (assumes results are ordered by date DESC)
        List<QCResult> recentResults = results.subList(0, Math.min(windowSize, results.size()));

        // Calculate mean
        BigDecimal sum = BigDecimal.ZERO;
        for (QCResult result : recentResults) {
            sum = sum.add(result.getResultValue());
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(recentResults.size()), 4, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (QCResult result : recentResults) {
            BigDecimal diff = result.getResultValue().subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(recentResults.size()), 4, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);

        // Create statistics entity
        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLot.getId());
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(mean);
        statistics.setStandardDeviation(stdDev);
        statistics.setNumValues(recentResults.size());
        statistics.setCalculationMethod("ROLLING");

        return statistics;
    }
}
