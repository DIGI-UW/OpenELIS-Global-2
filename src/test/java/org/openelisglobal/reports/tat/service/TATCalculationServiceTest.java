package org.openelisglobal.reports.tat.service;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.calendar.service.PublicHolidayService;
import org.openelisglobal.calendar.service.WeekendConfigService;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;

@RunWith(MockitoJUnitRunner.class)
public class TATCalculationServiceTest {

    @InjectMocks
    private TATCalculationServiceImpl tatCalculationService;

    @Mock
    private WeekendConfigService weekendConfigService;

    @Mock
    private PublicHolidayService publicHolidayService;

    @Before
    public void setUp() {
        // Default: Saturday(6) and Sunday(0) are weekends
        when(weekendConfigService.getWeekendDayNumbers()).thenReturn(Arrays.asList(0, 6));
        // Default: no holidays
        when(publicHolidayService.getHolidaysForYear(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(Collections.emptyList());
    }

    // ========== Calendar Time Tests ==========

    @Test
    public void calendarTime_sameDaySixHours() {
        Timestamp start = Timestamp.valueOf("2026-03-15 09:00:00");
        Timestamp end = Timestamp.valueOf("2026-03-15 15:00:00");

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.CALENDAR);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, new BigDecimal("6.00").compareTo(hours));
    }

    @Test
    public void calendarTime_weekendSpan65Hours() {
        // Friday 4 PM to Monday 9 AM = 65 hours
        Timestamp start = Timestamp.valueOf("2026-03-13 16:00:00"); // Friday
        Timestamp end = Timestamp.valueOf("2026-03-16 09:00:00"); // Monday

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.CALENDAR);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, new BigDecimal("65.00").compareTo(hours));
    }

    @Test
    public void calendarTime_nullStartReturnsNull() {
        BigDecimal hours = tatCalculationService.calculateTatHours(null, Timestamp.valueOf("2026-03-15 09:00:00"),
                TATCalculationMode.CALENDAR);
        Assert.assertNull(hours);
    }

    @Test
    public void calendarTime_nullEndReturnsNull() {
        BigDecimal hours = tatCalculationService.calculateTatHours(Timestamp.valueOf("2026-03-15 09:00:00"), null,
                TATCalculationMode.CALENDAR);
        Assert.assertNull(hours);
    }

    @Test
    public void calendarTime_sameTimestampReturnsZero() {
        Timestamp ts = Timestamp.valueOf("2026-03-15 09:00:00");
        BigDecimal hours = tatCalculationService.calculateTatHours(ts, ts, TATCalculationMode.CALENDAR);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(hours));
    }

    // ========== Working Time Tests ==========

    @Test
    public void workingTime_sameDayWorkingDay() {
        // Monday 9 AM to 3 PM = 6 hours working time
        Timestamp start = Timestamp.valueOf("2026-03-16 09:00:00"); // Monday
        Timestamp end = Timestamp.valueOf("2026-03-16 15:00:00");

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.WORKING_TIME);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, new BigDecimal("6.00").compareTo(hours));
    }

    @Test
    public void workingTime_sameDayWeekendReturnsZero() {
        // Saturday to Saturday = 0 working hours
        Timestamp start = Timestamp.valueOf("2026-03-14 09:00:00"); // Saturday
        Timestamp end = Timestamp.valueOf("2026-03-14 15:00:00");

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.WORKING_TIME);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(hours));
    }

    @Test
    public void workingTime_fridayToMondayExcludesWeekend() {
        // Friday 4 PM to Monday 9 AM
        // Working time: Friday 4 PM to midnight = 8h, Mon midnight to 9 AM = 9h = 17h
        Timestamp start = Timestamp.valueOf("2026-03-13 16:00:00"); // Friday
        Timestamp end = Timestamp.valueOf("2026-03-16 09:00:00"); // Monday

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.WORKING_TIME);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, new BigDecimal("17.00").compareTo(hours));
    }

    @Test
    public void workingTime_allExcludedDaysReturnsZero() {
        // Saturday to Sunday = both excluded
        Timestamp start = Timestamp.valueOf("2026-03-14 09:00:00"); // Saturday
        Timestamp end = Timestamp.valueOf("2026-03-15 17:00:00"); // Sunday

        BigDecimal hours = tatCalculationService.calculateTatHours(start, end, TATCalculationMode.WORKING_TIME);

        Assert.assertNotNull(hours);
        Assert.assertEquals(0, BigDecimal.ZERO.compareTo(hours));
    }

    // ========== Excluded Days Count ==========

    @Test
    public void countExcludedDays_oneWeek() {
        // A full week has 2 weekend days (Sat + Sun)
        int count = tatCalculationService.countExcludedDays(LocalDate.of(2026, 3, 9), // Monday
                LocalDate.of(2026, 3, 15)); // Sunday

        Assert.assertEquals(2, count);
    }

    @Test
    public void countExcludedDays_weekdaysOnlyReturnsZero() {
        // Mon-Fri = 0 excluded days
        int count = tatCalculationService.countExcludedDays(LocalDate.of(2026, 3, 9), // Monday
                LocalDate.of(2026, 3, 13)); // Friday

        Assert.assertEquals(0, count);
    }
}
