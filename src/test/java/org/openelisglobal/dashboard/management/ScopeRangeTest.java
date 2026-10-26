package org.openelisglobal.dashboard.management;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;

public class ScopeRangeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Test
    public void rollingScopesEndTodayAndPriorPeriodEndsTheDayBefore() {
        assertEquals(
                new ScopeRange(LocalDate.of(2026, 9, 8), TODAY, LocalDate.of(2026, 8, 8), LocalDate.of(2026, 9, 7)),
                ScopeRange.of("30d", TODAY));
        assertEquals(LocalDate.of(2026, 10, 1), ScopeRange.of("7d", TODAY).from());
        assertEquals(LocalDate.of(2026, 7, 10), ScopeRange.of("90d", TODAY).from());
        assertEquals(LocalDate.of(2025, 10, 8), ScopeRange.of("year", TODAY).from());
    }

    @Test
    public void calendarScopesStartAtTheQuarterOrYear() {
        assertEquals(LocalDate.of(2026, 10, 1), ScopeRange.of("quarter", TODAY).from());
        assertEquals(LocalDate.of(2026, 7, 1), ScopeRange.of("quarter", LocalDate.of(2026, 9, 30)).from());
        assertEquals(LocalDate.of(2026, 1, 1), ScopeRange.of("ytd", TODAY).from());
    }

    @Test
    public void priorPeriodHasTheSameLength() {
        ScopeRange ytd = ScopeRange.of("ytd", LocalDate.of(2026, 1, 10));
        assertEquals(LocalDate.of(2025, 12, 31), ytd.priorTo());
        assertEquals(LocalDate.of(2025, 12, 22), ytd.priorFrom());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownScopeIsRejected() {
        ScopeRange.of("decade", TODAY);
    }
}
