package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.service.ExportDateRange;

public class ExportDateRangeTest {
    @Test
    public void includesWholeLocalDaysAcrossDaylightSavingChange() {
        ExportDateRange range = ExportDateRange.of("2026-03-07", "2026-03-09", ZoneId.of("America/Los_Angeles"), 90);
        assertEquals("2026-03-07T08:00:00Z", range.start().toString());
        assertEquals("2026-03-10T07:00:00Z", range.endExclusive().toString());
        assertEquals(71, Duration.between(range.start(), range.endExclusive()).toHours());
    }

    @Test
    public void maximumCountsInclusiveCalendarDays() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        ExportDateRange.of(start.toString(), start.plusDays(89).toString(), ZoneId.of("UTC"), 90);
        assertThrows(IllegalArgumentException.class,
                () -> ExportDateRange.of(start.toString(), start.plusDays(90).toString(), ZoneId.of("UTC"), 90));
    }

    @Test
    public void rejectsMissingInvalidAndReversedDates() {
        for (String[] dates : new String[][] { { null, "2026-01-01" }, { "2026-01-01", "" },
                { "2026-02-30", "2026-03-01" }, { "2026-01-02", "2026-01-01" } }) {
            assertThrows(IllegalArgumentException.class,
                    () -> ExportDateRange.of(dates[0], dates[1], ZoneId.of("UTC"), 90));
        }
    }
}
