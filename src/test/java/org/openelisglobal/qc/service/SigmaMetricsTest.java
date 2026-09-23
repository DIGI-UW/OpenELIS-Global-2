package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.qc.service.SigmaMetrics.SigmaResult;

/**
 * Exact-value checks for the Westgard sigma formula and its classification
 * bands. Pure function, so no Spring context. OGC-704.
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
    public void sigmaIsNotCalculableWithoutATea_butTheCvStillIs() {
        // CV is a property of the control statistics alone, so it is reported wherever
        // mean/SD are usable; only sigma needs the per-test total allowable error.
        for (Double tea : new Double[] { null, 0.0, -5.0 }) {
            SigmaResult r = SigmaMetrics.compute(new BigDecimal("100"), new BigDecimal("2"), tea);
            assertEquals("TEa " + tea, 2.0, r.cv(), 1e-9);
            assertNull("TEa " + tea, r.sigma());
            assertEquals("TEa " + tea, SigmaMetrics.NOT_CALCULABLE, r.category());
        }
        // A less round case, so the arithmetic is pinned rather than the shape alone.
        assertEquals(8.088, SigmaMetrics.compute(new BigDecimal("1250.31"), new BigDecimal("101.12"), null).cv(), 1e-3);
    }

    @Test
    public void nothingIsCalculableWhenThereIsNoUsableMeanOrSd() {
        // mean=0 is the divide-by-zero guard; a missing mean or SD has nothing to
        // divide at all.
        assertNotCalculable(SigmaMetrics.compute(BigDecimal.ZERO, new BigDecimal("2"), 10.0));
        assertNotCalculable(SigmaMetrics.compute(null, new BigDecimal("2"), 10.0));
        assertNotCalculable(SigmaMetrics.compute(new BigDecimal("100"), null, 10.0));
    }

    @Test
    public void zeroSdReportsNoVariationRatherThanNoCv() {
        // An SD of 0 means fewer than 2 usable points, so there is no sigma to
        // report — but 0/mean is a real CV of zero, not an absent one, and the
        // report prints it rather than a dash.
        SigmaResult r = SigmaMetrics.compute(new BigDecimal("100"), BigDecimal.ZERO, 10.0);
        assertEquals(0.0, r.cv(), 1e-9);
        assertNull(r.sigma());
        assertEquals(SigmaMetrics.NOT_CALCULABLE, r.category());
    }

    private static void assertNotCalculable(SigmaResult r) {
        assertNull(r.cv());
        assertNull(r.sigma());
        assertEquals(SigmaMetrics.NOT_CALCULABLE, r.category());
    }
}
