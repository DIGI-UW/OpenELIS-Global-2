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

    private static ResultLimit componentRange(double lowNormal, double highNormal) {
        ResultLimit limit = new ResultLimit();
        limit.setLowNormal(lowNormal);
        limit.setHighNormal(highNormal);
        limit.setLowCritical(Double.NEGATIVE_INFINITY);
        limit.setHighCritical(Double.POSITIVE_INFINITY);
        return limit;
    }

    // OGC-1266: a multi-component result (COVID PCR N2 Ct 33 against 22 - 32)
    // printed no Alert letter on the patient report.
    @Test
    public void componentLetter_numericOutsideItsOwnRange_isBOrE() {
        assertEquals("E", ResultAlertFlags.componentLetter(componentRange(22d, 32d), "N", "33"));
        assertEquals("B", ResultAlertFlags.componentLetter(componentRange(22d, 32d), "N", "21.9"));
        assertEquals("", ResultAlertFlags.componentLetter(componentRange(22d, 32d), "N", "32"));
    }

    @Test
    public void componentLetter_criticalBeatsNormal() {
        ResultLimit range = componentRange(22d, 32d);
        range.setHighCritical(40d);
        assertEquals("EE", ResultAlertFlags.componentLetter(range, "N", "41"));
    }

    @Test
    public void componentLetter_selectListAnswerOtherThanTheNormalChoice_isStar() {
        ResultLimit range = new ResultLimit();
        range.setDictionaryNormalId("1334");
        assertEquals("*", ResultAlertFlags.componentLetter(range, "D", "1335"));
        assertEquals("", ResultAlertFlags.componentLetter(range, "D", "1334"));
        assertEquals("", ResultAlertFlags.componentLetter(new ResultLimit(), "D", "1335"));
    }

    @Test
    public void componentLetter_nothingToJudge_isEmpty() {
        assertEquals("", ResultAlertFlags.componentLetter(null, "N", "33"));
        assertEquals("", ResultAlertFlags.componentLetter(componentRange(22d, 32d), "N", ""));
        assertEquals("", ResultAlertFlags.componentLetter(componentRange(22d, 32d), "A", "33"));
    }
}
