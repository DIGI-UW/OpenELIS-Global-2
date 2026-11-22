package org.openelisglobal.qc.service.calculator;

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

    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final int SCALE = 4;

    @Override
    public boolean supports(String calculationMethod) {
        return "INITIAL_RUNS".equals(calculationMethod);
    }

    @Override
    public QCStatistics calculate(QCControlLot controlLot, List<QCResult> results) {
        Integer initialRunsCount = controlLot.getInitialRunsCount();
        if (initialRunsCount == null || initialRunsCount <= 0) {
            initialRunsCount = DEFAULT_RUNS_COUNT;
        }

        // Check if we have enough results. Sample SD divides by N−1, so the
        // effective sample (capped at initialRunsCount below) needs at least 2
        // values — a lot configured with initialRunsCount=1 must not divide by 0.
        if (results == null || results.size() < initialRunsCount || initialRunsCount < 2) {
            return null; // Insufficient data
        }

        List<QCResult> initialResults = results.subList(0, initialRunsCount);
        SampleStatistics stats = SampleStatistics.of(initialResults, SCALE);

        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLot.getId());
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(stats.mean());
        statistics.setStandardDeviation(stats.standardDeviation());
        statistics.setNumValues(initialResults.size());
        statistics.setCalculationMethod("INITIAL_RUNS");

        return statistics;
    }
}
