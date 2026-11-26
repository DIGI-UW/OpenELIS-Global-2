package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class DateUtilConvertDateToLocalDateTest extends BaseWebContextSensitiveTest {

    private TimeZone runnersTimeZone;

    @Before
    public void setEastOfUtc() {
        runnersTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Nairobi"));
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(runnersTimeZone);
    }

    @Test
    public void convertDateToLocalDate_null_isNull() {
        assertNull(DateUtil.convertDateToLocalDate(null));
    }

    @Test
    public void convertDateToLocalDate_sqlDate_doesNotThrowAndKeepsTheDay() {
        // A column read through Hibernate arrives as java.sql.Date, whose
        // toInstant() throws.
        java.sql.Date entered = java.sql.Date.valueOf("2024-06-03");

        assertEquals(LocalDate.of(2024, 6, 3), DateUtil.convertDateToLocalDate(entered));
    }

    @Test
    public void convertDateToLocalDate_localMidnightEastOfUtc_isThatDayNotTheDayBefore() {
        // In UTC this instant is 21:00 on 2024-06-02.
        Date localMidnight = new GregorianCalendar(2024, 5, 3).getTime();

        assertEquals(LocalDate.of(2024, 6, 3), DateUtil.convertDateToLocalDate(localMidnight));
    }
}
