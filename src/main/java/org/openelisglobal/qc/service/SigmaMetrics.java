package org.openelisglobal.qc.service;

import java.math.BigDecimal;

/**
 * Westgard sigma metric: {@code sigma = (TEa - bias) / CV}, with
 * {@code CV = SD / mean * 100}. Bias is fixed at 0 (no peer-comparison service
 * exists yet), so a "no peer data" qualifier applies wherever this is shown.
 *
 * <p>
 * Pure function of the stored control statistics plus the per-test TEa; no
 * state, so no service/bean. OGC-704.
 */
public final class SigmaMetrics {

    public static final String WORLD_CLASS = "WORLD_CLASS";
    public static final String ACCEPTABLE = "ACCEPTABLE";
    public static final String MARGINAL = "MARGINAL";
    public static final String POOR = "POOR";
    public static final String NOT_CALCULABLE = "NOT_CALCULABLE";

    private SigmaMetrics() {
    }

    /**
     * Immutable result. CV depends only on mean/SD, so it is present whenever those
     * are usable, including when sigma is NOT_CALCULABLE for want of a TEa; sigma
     * is null unless the category is calculable.
     */
    public record SigmaResult(Double cv, Double sigma, String category) {
    }

    /**
     * Coefficient of variation as a percent: {@code SD / mean * 100}. Returns null
     * when mean/SD are missing or mean is non-positive (divide-by-zero guard).
     */
    private static Double cv(BigDecimal mean, BigDecimal sd) {
        if (mean == null || sd == null || mean.signum() <= 0 || sd.signum() < 0) {
            return null;
        }
        return sd.doubleValue() / mean.doubleValue() * 100.0;
    }

    /**
     * @param mean control mean (from qc_statistics)
     * @param sd   control standard deviation (from qc_statistics)
     * @param tea  per-test total allowable error, percent (null when unset)
     * @return cv/sigma/category; NOT_CALCULABLE with a null sigma when TEa is
     *         missing or non-positive, and additionally a null cv when mean/SD are
     *         missing or non-positive (an SD of 0 already implies < 2 usable
     *         points, so no separate N guard is needed)
     */
    public static SigmaResult compute(BigDecimal mean, BigDecimal sd, Double tea) {
        Double coefficientOfVariation = cv(mean, sd);
        // Sigma additionally requires TEa and a non-zero CV; without them the sigma
        // metric is NOT_CALCULABLE, but the CV is still carried through when it could
        // be computed at all.
        if (tea == null || tea <= 0 || coefficientOfVariation == null || coefficientOfVariation <= 0) {
            return new SigmaResult(coefficientOfVariation, null, NOT_CALCULABLE);
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
