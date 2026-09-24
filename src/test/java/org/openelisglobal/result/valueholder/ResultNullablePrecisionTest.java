package org.openelisglobal.result.valueholder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * OGC-1170 — a result that records no precision.
 *
 * <p>
 * {@code significant_digits} and {@code grouping} are nullable columns, and a
 * row carrying neither is ordinary: results written before those columns were
 * populated, and imports that supply no precision. Both were held as
 * primitives, so loading such a row threw
 * {@code PropertyAccessException: Null value was
 * assigned to a property of primitive type} and took the surrounding query down
 * with it — one row without a precision made every result in its lab unit
 * unreachable, and the worklist returned HTTP 500 for that unit alone.
 *
 * <p>
 * The accessors still answer with an {@code int}, so callers are unchanged.
 * What matters is which int: zero is not a neutral stand-in for an unrecorded
 * precision, it is an instruction to cut the value at the decimal point.
 */
public class ResultNullablePrecisionTest {

    @Test
    public void anUnrecordedPrecisionReadsAsReportExactlyAsStored() {
        // -1 is the value StringUtil.doubleWithSignificantDigits and
        // ResultServiceImpl.getResultValue already read as "do not reformat".
        assertEquals(-1, new Result().getSignificantDigits());
    }

    @Test
    public void anUnrecordedPrecisionIsNotReadAsZeroDecimalPlaces() {
        assertEquals("zero would truncate the value at the decimal point", -1, new Result().getSignificantDigits());
    }

    @Test
    public void arecordedPrecisionIsAnsweredAsGiven() {
        Result result = new Result();

        result.setSignificantDigits(0);
        assertEquals(0, result.getSignificantDigits());

        result.setSignificantDigits(3);
        assertEquals(3, result.getSignificantDigits());
    }

    @Test
    public void anUnrecordedGroupingReadsAsTheOnlyGroup() {
        // Which is where a single-valued result has always sat.
        assertEquals(0, new Result().getGrouping());
    }

    @Test
    public void arecordedGroupingIsAnsweredAsGiven() {
        Result result = new Result();

        result.setGrouping(2);
        assertEquals(2, result.getGrouping());
    }
}
