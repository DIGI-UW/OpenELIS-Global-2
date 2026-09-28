package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * Stored display dates are written in the site's own date order, so
 * re-formatting one must read it in that order. Reading a day-first date
 * month-first swapped day and month whenever the day was 12 or less, and the
 * edit form saved the swapped date back.
 */
public class DateUtilFormatStringDateTest extends BaseWebContextSensitiveTest {

    private static final Locale DAY_FIRST = Locale.forLanguageTag("fr-FR");
    private static final Locale MONTH_FIRST = Locale.forLanguageTag("en-US");

    @Test
    public void dayFirstSite_readsAnAmbiguousDateDayFirst() {
        assertEquals("03/11/1990", DateUtil.formatStringDate("03/11/1990", "dd/MM/yyyy", DAY_FIRST));
        assertEquals("10/02/1986", DateUtil.formatStringDate("10/02/1986", "dd/MM/yyyy", DAY_FIRST));
    }

    @Test
    public void dayFirstSite_convertsToMonthFirstWithoutSwappingTheDate() {
        assertEquals("11/03/1990", DateUtil.formatStringDate("03/11/1990", "MM/dd/yyyy", DAY_FIRST));
    }

    @Test
    public void monthFirstSite_readsAnAmbiguousDateMonthFirst() {
        assertEquals("02/10/1986", DateUtil.formatStringDate("02/10/1986", "MM/dd/yyyy", MONTH_FIRST));
        assertEquals("10/02/1986", DateUtil.formatStringDate("02/10/1986", "dd/MM/yyyy", MONTH_FIRST));
    }

    @Test
    public void anUnambiguousDateInTheOtherOrderIsStillRead() {
        assertEquals("20/06/2021", DateUtil.formatStringDate("20/06/2021", "dd/MM/yyyy", MONTH_FIRST));
        assertEquals("06/20/2021", DateUtil.formatStringDate("06/20/2021", "MM/dd/yyyy", DAY_FIRST));
    }

    @Test
    public void aDateWithUnknownDayAndMonthIsReturnedAsStored() {
        assertEquals("XX/XX/1950", DateUtil.formatStringDate("XX/XX/1950", "dd/MM/yyyy", DAY_FIRST));
    }

    @Test
    public void blankIsEmpty() {
        assertEquals("", DateUtil.formatStringDate(" ", "dd/MM/yyyy", DAY_FIRST));
        assertEquals("", DateUtil.formatStringDate(null, "dd/MM/yyyy", DAY_FIRST));
    }
}
