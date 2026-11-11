package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Abstract base class for Westgard rule evaluators.
 *
 * Provides common functionality for z-score calculations and data validation.
 */
public abstract class AbstractWestgardRuleEvaluator implements WestgardRuleEvaluator {

    @Override
    public boolean canEvaluate(WestgardRuleConfig config) {
        return config != null && config.getEnabled() && getRuleCode().equals(config.getRuleCode());
    }

    /**
     * Calculate z-score for a result value.
     *
     * @param value  Result value
     * @param mean   Mean from statistics
     * @param stdDev Standard deviation from statistics
     * @return Z-score as BigDecimal
     */
    protected BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal stdDev) {
        if (stdDev == null || stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.subtract(mean).divide(stdDev, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get z-score from a QC result (use pre-calculated if available).
     *
     * @param result     QC result
     * @param statistics Statistics for calculation fallback
     * @return Z-score value
     */
    protected BigDecimal getZScore(QCResult result, QCStatistics statistics) {
        if (result.getZScore() != null) {
            return result.getZScore();
        }
        return calculateZScore(result.getResultValue(), statistics.getMean(), statistics.getStandardDeviation());
    }

    /**
     * Check if there's sufficient historical data for evaluation.
     *
     * @param historicalResults List of historical results
     * @return true if sufficient data exists
     */
    protected boolean hasSufficientData(List<QCResult> historicalResults) {
        int required = getRequiredResultCount();
        // We need (required - 1) historical results since current result counts as 1
        return historicalResults != null && historicalResults.size() >= (required - 1);
    }

    /**
     * Check if a z-score exceeds a threshold (absolute value comparison).
     *
     * @param zScore    Z-score to check
     * @param threshold Threshold value (positive)
     * @return true if |zScore| >= threshold
     */
    protected boolean exceedsThreshold(BigDecimal zScore, BigDecimal threshold) {
        return zScore.abs().compareTo(threshold) >= 0;
    }

    /**
     * Check if a z-score is on the positive side of the mean.
     *
     * @param zScore Z-score to check
     * @return true if zScore > 0
     */
    protected boolean isPositiveSide(BigDecimal zScore) {
        return zScore.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if two z-scores are on the same side of the mean.
     *
     * @param zScore1 First z-score
     * @param zScore2 Second z-score
     * @return true if both are positive or both are negative
     */
    protected boolean sameSide(BigDecimal zScore1, BigDecimal zScore2) {
        return (zScore1.compareTo(BigDecimal.ZERO) > 0 && zScore2.compareTo(BigDecimal.ZERO) > 0)
                || (zScore1.compareTo(BigDecimal.ZERO) < 0 && zScore2.compareTo(BigDecimal.ZERO) < 0);
    }
}
