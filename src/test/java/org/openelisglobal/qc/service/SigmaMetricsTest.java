package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.qc.service.SigmaMetrics.SigmaResult;

/**
 * Exact-value checks for the Westgard sigma formula and its classification
 * bands. Pure function, so no Spring context. C.1 / OGC-704.
 */
public class SigmaMetricsTest {

    @Test
    public void computesCvAndSigmaAndClassifiesAcceptable() {
        // mean=100, sd=2 -> CV=2.0%; TEa=10 -> sigma=5.0 -> ACCEPTABLE (4..5.99)
        SigmaResult r = SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("2"), 10.0);
        assertEquals(2.0, r.cv(), 1e-9);
        assertEquals(5.0, r.sigma(), 1e-9);
        assertEquals(SigmaMetrics.ACCEPTABLE, r.category());
    }

    @Test
    public void classifiesBandBoundaries() {
        // CV fixed at 1.0% (mean=100, sd=1): sigma == TEa
        assertEquals(SigmaMetrics.WORLD_CLASS,
                SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("1"), 6.0).category());
        assertEquals(SigmaMetrics.ACCEPTABLE,
                SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("1"), 4.0).category());
        assertEquals(SigmaMetrics.MARGINAL,
                SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("1"), 3.0).category());
        assertEquals(SigmaMetrics.POOR,
                SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("1"), 2.99).category());
    }

    @Test
    public void notCalculableWhenTeaMissingOrNonPositive() {
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("2"), null));
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("2"), 0.0));
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("2"), -5.0));
    }

    @Test
    public void notCalculableWhenMeanOrSdNonPositiveOrNull() {
        // sd=0 (fewer than 2 usable points) and mean=0 (divide-by-zero guard)
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), BigDecimal.ZERO, 10.0));
        assertNotCalculable(SigmaMetrics.compute(BigDecimal.ZERO, new BigDecimal("2"), 10.0));
        assertNotCalculable(SigmaMetrics.compute(null, new BigDecimal("2"), 10.0));
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), null, 10.0));
    }

    private static void assertNotCalculable(SigmaResult r) {
        assertNull(r.cv());
        assertNull(r.sigma());
        assertEquals(SigmaMetrics.NOT_CALCULABLE, r.category());
    }
}
