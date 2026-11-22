package org.openelisglobal.qc.service.calculator;

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
    private static final int SCALE = 4;

    @Override
    public boolean supports(String calculationMethod) {
        return "ROLLING".equals(calculationMethod);
    }

    @Override
    public QCStatistics calculate(QCControlLot controlLot, List<QCResult> results) {
        int windowSize = controlLot.getInitialRunsCount() != null ? controlLot.getInitialRunsCount()
                : DEFAULT_WINDOW_SIZE;

        // Check if we have enough results. Sample SD divides by N−1, so the
        // effective window (capped at windowSize below) needs at least 2 values —
        // a window of 1 must not divide by 0.
        if (results == null || results.size() < windowSize || windowSize < 2) {
            return null; // Insufficient data
        }

        // Take most recent N results (results are ordered by run date DESC)
        List<QCResult> recentResults = results.subList(0, windowSize);
        SampleStatistics stats = SampleStatistics.of(recentResults, SCALE);

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLot.getId());
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(stats.mean());
        statistics.setStandardDeviation(stats.standardDeviation());
        statistics.setNumValues(recentResults.size());
        statistics.setCalculationMethod("ROLLING");

        return statistics;
    }
}
