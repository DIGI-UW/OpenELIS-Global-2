package org.openelisglobal.qc.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;

/**
 * Mean and standard deviation of a set of QC result values, rounded half-up to
 * the scale the caller persists.
 *
 * <p>
 * The standard deviation divides by N-1: control limits are established from a
 * sample of runs, not from the whole population (CLSI C24). Dividing by N
 * understated the deviation by about 2.6% at N=20 and inflated every z-score.
 */
public record SampleStatistics(BigDecimal mean, BigDecimal standardDeviation) {

    /**
     * Mean of the given results.
     *
     * @throws IllegalArgumentException if no results are given
     */
    public static BigDecimal mean(List<QCResult> results, int scale) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate mean: results list is empty");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (QCResult result : results) {
            sum = sum.add(result.getResultValue());
        }
        return sum.divide(BigDecimal.valueOf(results.size()), scale, RoundingMode.HALF_UP);
    }

    /**
     * Mean and sample standard deviation of the given results.
     *
     * @throws IllegalArgumentException if fewer than two results are given, since
     *                                  the sample standard deviation would divide
     *                                  by zero
     */
    public static SampleStatistics of(List<QCResult> results, int scale) {
        if (results == null || results.size() < 2) {
            throw new IllegalArgumentException("Cannot calculate standard deviation: need at least 2 values");
        }
        BigDecimal mean = mean(results, scale);

        // The sum of squared differences stays exact; only the square root, which
        // is irrational anyway, is taken in double precision.
        BigDecimal squaredDiffSum = BigDecimal.ZERO;
        for (QCResult result : results) {
            BigDecimal diff = result.getResultValue().subtract(mean);
            squaredDiffSum = squaredDiffSum.add(diff.multiply(diff));
        }
        double variance = squaredDiffSum.doubleValue() / (results.size() - 1);
        return new SampleStatistics(mean,
                BigDecimal.valueOf(Math.sqrt(variance)).setScale(scale, RoundingMode.HALF_UP));
    }
}
