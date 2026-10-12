package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.junit.Test;

public class WeightsViewTest {

    private static WeightsView weights(String qc, String eqa, String nce, String amendment) {
        return new WeightsView(qc == null ? null : new BigDecimal(qc), new BigDecimal(eqa), new BigDecimal(nce),
                new BigDecimal(amendment));
    }

    @Test
    public void sumWithinOneHundredthOfOneIsValid() {
        assertTrue(weights("0.30", "0.25", "0.25", "0.20").isValid());
        assertTrue(weights("0.34", "0.33", "0.33", "0.01").isValid());
        assertTrue(weights("1.00", "0", "0", "0").isValid());
    }

    @Test
    public void sumOffByMoreThanOneHundredthIsRejected() {
        assertFalse(weights("0.50", "0.50", "0.50", "0.50").isValid());
        assertFalse(weights("0.30", "0.25", "0.25", "0.18").isValid());
    }

    @Test
    public void missingOrNegativeWeightIsRejected() {
        assertFalse(weights(null, "0.40", "0.30", "0.30").isValid());
        assertFalse(weights("-0.10", "0.50", "0.30", "0.30").isValid());
    }
}
