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
 * Calculator for initial establishment method. Calculates mean and standard
 * deviation from first N runs (default 20). Per US6: Should enter ESTABLISHMENT
 * status until sufficient results collected.
 */
@Component
public class InitialRunsCalculator implements StatisticsCalculator {

    @Override
    public boolean supports(String calculationMethod) {
        return "INITIAL_RUNS".equals(calculationMethod);
    }

    @Override
    public QCStatistics calculate(QCControlLot controlLot, List<QCResult> results) {
        Integer initialRunsCount = controlLot.getInitialRunsCount();
        if (initialRunsCount == null || initialRunsCount <= 0) {
            initialRunsCount = 20; // Default
        }

        // Check if we have enough results
        if (results == null || results.size() < initialRunsCount) {
            return null; // Insufficient data
        }

        // Take first N results
        List<QCResult> initialResults = results.subList(0, Math.min(initialRunsCount, results.size()));

        // Calculate mean
        BigDecimal sum = BigDecimal.ZERO;
        for (QCResult result : initialResults) {
            sum = sum.add(result.getResultValue());
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(initialResults.size()), 4, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (QCResult result : initialResults) {
            BigDecimal diff = result.getResultValue().subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(initialResults.size()), 4, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);

        // Create statistics entity
        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLot.getId());
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(mean);
        statistics.setStandardDeviation(stdDev);
        statistics.setNumValues(initialResults.size());
        statistics.setCalculationMethod("INITIAL_RUNS");

        return statistics;
    }
}
