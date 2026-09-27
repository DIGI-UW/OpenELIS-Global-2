package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * OGC-1179 #5 — what the bench is told about a test's critical zone.
 *
 * <p>
 * An unset bound carries an infinity: POSITIVE for a high one and NEGATIVE for
 * a low one, the same convention the normal and valid ranges use. Testing only
 * against POSITIVE meant every unset low bound read as authored, and the
 * worklist reported a critical zone of "&lt; -Infinity" — in red, beside the
 * reference range, on 36 of 51 analyses across four lab units, including coded
 * tests that can have no numeric bound at all. Red text that means nothing,
 * where flags are read, teaches the reader to disregard the one place a real
 * critical value appears.
 */
public class CriticalRangeDisplayTest {

    private static final String DIGITS = "1";

    private ResultLimit authoredLimit(Double low, Double high) {
        ResultLimit limit = new ResultLimit();
        limit.setId("77");
        if (low != null) {
            limit.setLowCritical(low);
        }
        if (high != null) {
            limit.setHighCritical(high);
        }
        return limit;
    }

    @Test
    public void reportsBothBoundsWhenBothAreAuthored() {
        assertEquals("< 50.0 or > 400.0", CriticalRangeFormat.display(authoredLimit(50d, 400d), "N", DIGITS));
    }

    @Test
    public void reportsOneBoundWhenOnlyOneIsAuthored() {
        assertEquals("< 50.0", CriticalRangeFormat.display(authoredLimit(50d, null), "N", DIGITS));
        assertEquals("> 400.0", CriticalRangeFormat.display(authoredLimit(null, 400d), "N", DIGITS));
    }

    /** The reported defect, in both spellings of "unset". */
    @Test
    public void saysNothingWhenABoundIsAnUnsetInfinity() {
        assertEquals("", CriticalRangeFormat.display(authoredLimit(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                "N", DIGITS));
        assertEquals("", CriticalRangeFormat.display(authoredLimit(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
                "N", DIGITS));
        assertEquals("a NaN bound is not a bound either", "",
                CriticalRangeFormat.display(authoredLimit(Double.NaN, Double.NaN), "N", DIGITS));
    }

    @Test
    public void anUnsetLowBoundDoesNotSuppressAnAuthoredHighOne() {
        assertEquals("> 400.0",
                CriticalRangeFormat.display(authoredLimit(Double.NEGATIVE_INFINITY, 400d), "N", DIGITS));
    }

    /** A coded result has no numeric zone it could be critical in. */
    @Test
    public void saysNothingForANonNumericResult() {
        ResultLimit limit = authoredLimit(50d, 400d);

        assertEquals("", CriticalRangeFormat.display(limit, "D", DIGITS));
        assertEquals("", CriticalRangeFormat.display(limit, "A", DIGITS));
        assertEquals("", CriticalRangeFormat.display(limit, null, DIGITS));
    }

    /**
     * A limit with no id is the selector's synthetic empty one, standing for "no
     * authored range matched this patient" — the same reading the result flag
     * already applies to it.
     */
    @Test
    public void saysNothingForTheSyntheticEmptyLimit() {
        ResultLimit synthetic = new ResultLimit();
        synthetic.setLowCritical(50d);
        synthetic.setHighCritical(400d);

        assertEquals("", CriticalRangeFormat.display(synthetic, "N", DIGITS));
        assertEquals("", CriticalRangeFormat.display(null, "N", DIGITS));
    }
}
