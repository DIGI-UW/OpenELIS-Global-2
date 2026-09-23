package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * OGC-1121 — the patient report's Alert column gains a critical tier: BB / EE
 * beyond an authored critical bound, nothing otherwise, so the legacy B / E
 * letters keep meaning "outside normal".
 */
public class ResultAlertFlagsTest {

    private static ResultLimit criticalBounds(double low, double high) {
        ResultLimit limit = new ResultLimit();
        limit.setLowCritical(low);
        limit.setHighCritical(high);
        return limit;
    }

    @Test
    public void beyondTheHighCriticalBound_isEE() {
        assertEquals("EE", ResultAlertFlags.criticalLetter(criticalBounds(2d, 150d), "200"));
    }

    @Test
    public void belowTheLowCriticalBound_isBB() {
        assertEquals("BB", ResultAlertFlags.criticalLetter(criticalBounds(2d, 150d), "1"));
    }

    @Test
    public void merelyAbnormal_getsNoCriticalLetter() {
        assertEquals("", ResultAlertFlags.criticalLetter(criticalBounds(2d, 150d), "120"));
    }

    @Test
    public void unauthoredBounds_neverFire() {
        ResultLimit unauthored = criticalBounds(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals("", ResultAlertFlags.criticalLetter(unauthored, "200"));
        assertEquals("", ResultAlertFlags.criticalLetter(unauthored, "-5"));
    }

    @Test
    public void nothingToJudge_isEmpty() {
        assertEquals("", ResultAlertFlags.criticalLetter(null, "200"));
        assertEquals("", ResultAlertFlags.criticalLetter(criticalBounds(2d, 150d), ""));
        assertEquals("", ResultAlertFlags.criticalLetter(criticalBounds(2d, 150d), "n/a"));
    }
}
