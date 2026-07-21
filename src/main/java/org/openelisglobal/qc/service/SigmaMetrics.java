package org.openelisglobal.qc.service;

import java.math.BigDecimal;

/**
 * Westgard sigma metric: {@code sigma = (TEa - bias) / CV}, with
 * {@code CV = SD / mean * 100}. Bias is fixed at 0 (no peer-comparison service
 * exists yet), so a "no peer data" qualifier applies wherever this is shown.
 *
 * <p>
 * Pure function of the stored control statistics plus the per-test TEa; no
 * state, so no service/bean. C.1 / OGC-704.
 */
public final class SigmaMetrics {

    public static final String WORLD_CLASS = "WORLD_CLASS";
    public static final String ACCEPTABLE = "ACCEPTABLE";
    public static final String MARGINAL = "MARGINAL";
    public static final String POOR = "POOR";
    public static final String NOT_CALCULABLE = "NOT_CALCULABLE";

    private SigmaMetrics() {
    }

    /** Immutable result; cv/sigma are null when not calculable. */
    public record SigmaResult(Double cv, Double sigma, String category) {
    }

    /**
     * @param mean control mean (from qc_statistics)
     * @param sd   control standard deviation (from qc_statistics)
     * @param tea  per-test total allowable error, percent (null when unset)
     * @return cv/sigma/category; NOT_CALCULABLE with null cv/sigma when any input
     *         is missing or non-positive (an SD of 0 already implies < 2 usable
     *         points, so no separate N guard is needed)
     */
    /**
     * Coefficient of variation as a percent: {@code SD / mean * 100}. Depends only
     * on the control mean/SD (independent of TEa), so it is available even when the
     * full sigma metric is NOT_CALCULABLE. Returns null when mean/SD are missing or
     * mean is non-positive (divide-by-zero guard).
     */
    public static Double cv(BigDecimal mean, BigDecimal sd) {
        if (mean == null || sd == null || mean.signum() <= 0 || sd.signum() < 0) {
            return null;
        }
        return sd.doubleValue() / mean.doubleValue() * 100.0;
    }

    public static SigmaResult compute(BigDecimal mean, BigDecimal sd, Double tea) {
        Double coefficientOfVariation = cv(mean, sd);
        // Sigma additionally requires TEa and a non-zero CV; without them the sigma
        // metric is NOT_CALCULABLE (CV itself may still be computable — see cv()).
        if (tea == null || tea <= 0 || coefficientOfVariation == null || coefficientOfVariation <= 0) {
            return new SigmaResult(null, null, NOT_CALCULABLE);
        }
        double sigma = tea / coefficientOfVariation; // bias = 0
        return new SigmaResult(coefficientOfVariation, sigma, classify(sigma));
    }

    private static String classify(double sigma) {
        if (sigma >= 6.0) {
            return WORLD_CLASS;
        }
        if (sigma >= 4.0) {
            return ACCEPTABLE;
        }
        if (sigma >= 3.0) {
            return MARGINAL;
        }
        return POOR;
    }
}
