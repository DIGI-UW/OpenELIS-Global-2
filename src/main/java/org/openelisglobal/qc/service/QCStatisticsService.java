package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Service interface for QC Statistics calculation and management. Supports User
 * Story 6: Statistical calculations for control lots
 */
public interface QCStatisticsService extends BaseObjectService<QCStatistics, String> {

    /**
     * Calculate statistics from initial runs method. Collects first N results and
     * calculates mean/SD.
     *
     * @param controlLotId The control lot ID
     * @param requiredRuns Number of runs required
     * @return Calculated statistics
     * @throws IllegalArgumentException if insufficient data
     */
    QCStatistics calculateInitialRunsStatistics(String controlLotId, Integer requiredRuns)
            throws IllegalArgumentException;

    /**
     * Calculate rolling window statistics. Uses most recent N results to calculate
     * mean/SD.
     *
     * @param controlLotId The control lot ID
     * @param windowSize   Number of recent results to include
     * @return Calculated statistics
     * @throws IllegalArgumentException if insufficient data
     */
    QCStatistics calculateRollingStatistics(String controlLotId, Integer windowSize) throws IllegalArgumentException;

    /**
     * Get the latest (current) statistics for a control lot.
     *
     * @param controlLotId The control lot ID
     * @return Latest statistics, or null if none exist
     */
    QCStatistics getLatestStatistics(String controlLotId);

    /**
     * Calculate mean from list of QC results.
     *
     * @param results List of QC results
     * @return Calculated mean
     * @throws IllegalArgumentException if results is empty
     */
    BigDecimal calculateMean(List<QCResult> results) throws IllegalArgumentException;

    /**
     * Calculate standard deviation from list of QC results. Uses sample standard
     * deviation (n-1).
     *
     * @param results List of QC results
     * @return Calculated standard deviation
     * @throws IllegalArgumentException if results has fewer than 2 values
     */
    BigDecimal calculateStandardDeviation(List<QCResult> results) throws IllegalArgumentException;

    /**
     * Get statistics filtered by calculation method.
     *
     * @param controlLotId      The control lot ID
     * @param calculationMethod The calculation method (INITIAL_RUNS, ROLLING, etc.)
     * @return List of matching statistics
     */
    List<QCStatistics> getStatisticsByMethod(String controlLotId, String calculationMethod);

    /**
     * Update existing statistics record.
     *
     * @param statistics The statistics to update
     * @return Updated statistics
     */
    QCStatistics updateStatistics(QCStatistics statistics);

    /**
     * Invalidate old statistics by setting validity end date.
     *
     * @param controlLotId The control lot ID
     */
    void invalidateOldStatistics(String controlLotId);
}
