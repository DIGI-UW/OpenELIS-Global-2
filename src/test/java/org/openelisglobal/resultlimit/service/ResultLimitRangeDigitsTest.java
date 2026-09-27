package org.openelisglobal.resultlimit.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A displayed reference range must never misstate its configured limits: a test
 * set to whole numbers with a range of 0.7 to 1.1 showed "1 - 1".
 */
public class ResultLimitRangeDigitsTest {

    @Test
    public void limitsWithMoreDecimalsThanTheTestWidenTheDisplay() {
        assertEquals("1", ResultLimitServiceImpl.rangeDigits("0", 0.7, 1.1));
        assertEquals("2", ResultLimitServiceImpl.rangeDigits("0", 2.03, 150));
    }

    @Test
    public void limitsThatFitTheTestKeepItsDecimals() {
        assertEquals("2", ResultLimitServiceImpl.rangeDigits("2", 6, 13));
        assertEquals("0", ResultLimitServiceImpl.rangeDigits("0", 1, 486));
        assertEquals("1", ResultLimitServiceImpl.rangeDigits("1", 12, 16));
    }

    @Test
    public void openEndedAndUnsetDigitsAreLeftAlone() {
        assertEquals("0", ResultLimitServiceImpl.rangeDigits("0", Double.NEGATIVE_INFINITY, 5));
        assertEquals("-1", ResultLimitServiceImpl.rangeDigits("-1", 0.7));
        assertEquals("4", ResultLimitServiceImpl.rangeDigits("0", 0.1 + 0.2));
    }
}
