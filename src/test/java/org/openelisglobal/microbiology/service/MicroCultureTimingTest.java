package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.Test;

public class MicroCultureTimingTest {
    private final Timestamp start = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    public void extensionPreservesOriginalClock() {
        assertEquals(start.getTime() + 60 * 3600000L,
                MicroCultureTiming.ends(start, new BigDecimal("48"), "HOURS", 12 * 3600000L).getTime());
    }

    @Test
    public void checkScheduleRemainsAnchoredAfterLateReading() {
        assertEquals(start.getTime() + 16 * 3600000L, MicroCultureTiming
                .nextCheck(start, new Timestamp(start.getTime() + 10 * 3600000L), new BigDecimal("8")).getTime());
    }

    @Test
    public void exactDeadlineIsDueAndPositivityUsesElapsedTime() {
        assertTrue(MicroCultureTiming.due(start, start.toInstant()));
        assertFalse(MicroCultureTiming.due(start, start.toInstant().minusMillis(1)));
        assertEquals(new BigDecimal("1.50"),
                MicroCultureTiming.hours(start, new Timestamp(start.getTime() + 90 * 60000L)));
    }

    @Test
    public void invalidUnitsAmountsAndPrecisionAreRejected() {
        for (String unit : new String[] { null, "MINUTES" })
            try {
                MicroCultureTiming.milliseconds(BigDecimal.ONE, unit);
                fail();
            } catch (IllegalArgumentException expected) {
            }
        for (BigDecimal value : new BigDecimal[] { null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("0.001"),
                new BigDecimal("87601") })
            try {
                MicroCultureTiming.milliseconds(value, "HOURS");
                fail();
            } catch (IllegalArgumentException expected) {
            }
        assertEquals(4428000L, MicroCultureTiming.milliseconds(new BigDecimal("1.2300"), "HOURS"));
    }
}
