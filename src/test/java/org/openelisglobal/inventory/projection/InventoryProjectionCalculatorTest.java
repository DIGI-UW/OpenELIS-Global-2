package org.openelisglobal.inventory.projection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjection.LeadTimeTier;
import org.openelisglobal.inventory.projection.InventoryProjectionCalculator.LeadTime;

/**
 * The run-out arithmetic, exercised directly. These cases are the ones the
 * board is judged on: an item with no history must not show a date, a steady
 * item and an erratic item must not show the same confidence, and one busy day
 * must not drag every estimate forward.
 */
public class InventoryProjectionCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);
    private static final LeadTime SET_14_DAYS = new LeadTime(14, LeadTimeTier.SET);

    private static double[] flat(double perDay) {
        double[] daily = new double[InventoryProjectionCalculator.WINDOW_DAYS];
        Arrays.fill(daily, perDay);
        return daily;
    }

    @Test
    public void steadyUseProducesASingleDateWindowAndAnOrderByOneLeadTimeEarlier() {
        // 4 a day, never varying: the spread is zero, so both ends of the window land
        // on the same
        // day and the estimate is as tight as it can honestly be.
        InventoryProjection row = InventoryProjectionCalculator.project(40.0, flat(4.0), 5, SET_14_DAYS, TODAY, TODAY);

        assertEquals(4.0, row.getMedianDailyUse(), 0.0001);
        assertEquals(LocalDate.of(2026, 9, 26), row.getRunOutEarly());
        assertEquals(LocalDate.of(2026, 9, 26), row.getRunOutLate());
        assertEquals(LocalDate.of(2026, 9, 12), row.getOrderByDate());
        assertEquals(14, row.getLeadTimeDays().intValue());
        assertEquals(LeadTimeTier.SET, row.getLeadTimeTier());
        assertFalse(row.isStale());
    }

    @Test
    public void erraticUseWidensTheWindowWithoutMovingTheMedian() {
        double[] erratic = flat(4.0);
        for (int day = 0; day < erratic.length; day += 2) {
            erratic[day] = 0.0;
            erratic[day + 1] = 8.0;
        }
        InventoryProjection steady = InventoryProjectionCalculator.project(40.0, flat(4.0), 5, SET_14_DAYS, TODAY,
                TODAY);
        InventoryProjection swinging = InventoryProjectionCalculator.project(40.0, erratic, 5, SET_14_DAYS, TODAY,
                TODAY);

        // Same average consumption, same stock, but the swinging item cannot promise
        // the same date.
        assertEquals(4.0, swinging.getMedianDailyUse(), 0.0001);
        assertEquals(LocalDate.of(2026, 9, 20), swinging.getRunOutEarly());
        assertNull("a slow end that runs to zero rate is open-ended, not a date", swinging.getRunOutLate());
        assertTrue("the erratic item must run out no later than the steady one at its early end",
                swinging.getRunOutEarly().isBefore(steady.getRunOutEarly()));
        assertEquals(LocalDate.of(2026, 9, 6), swinging.getOrderByDate());
    }

    @Test
    public void oneBusyDayDoesNotDragTheEstimateForward() {
        double[] withSpike = flat(2.0);
        withSpike[withSpike.length - 1] = 200.0;

        InventoryProjection row = InventoryProjectionCalculator.project(60.0, withSpike, 5, SET_14_DAYS, TODAY, TODAY);

        // The mean of this series is above 8 a day; the median holds at 2, which is
        // what the item
        // actually consumes on a normal day. The spread has to ignore the spike too, or
        // the window
        // would widen far enough to call a month of stock urgent.
        assertEquals(2.0, row.getMedianDailyUse(), 0.0001);
        assertEquals(LocalDate.of(2026, 10, 16), row.getRunOutEarly());
        assertEquals(LocalDate.of(2026, 10, 16), row.getRunOutLate());
        assertEquals(BoardStatus.ADEQUATE, row.getStatus());
    }

    @Test
    public void theSpreadIgnoresAnOutlierTheSameWayTheMedianDoes() {
        double[] withSpike = flat(2.0);
        withSpike[withSpike.length - 1] = 200.0;

        assertEquals("one outlier day must not register as everyday variability", 0.0,
                InventoryProjectionCalculator.medianAbsoluteDeviation(withSpike, 2.0), 0.0001);
        assertEquals("genuine day-to-day swing must still register", 1.4826,
                InventoryProjectionCalculator.medianAbsoluteDeviation(new double[] { 1, 3, 1, 3 }, 2.0), 0.0001);
    }

    @Test
    public void anItemWithNoUsageShowsNoDatesAndSaysItIsStillBuildingData() {
        InventoryProjection row = InventoryProjectionCalculator.project(40.0, new double[30], 5,
                new LeadTime(30, LeadTimeTier.DEFAULT), null, TODAY);

        assertEquals(BoardStatus.BUILDING_DATA, row.getStatus());
        assertNull("no history must not produce a run-out date", row.getRunOutEarly());
        assertNull(row.getRunOutLate());
        assertNull("without a run-out date there is nothing to derive an order-by from", row.getOrderByDate());
        assertNull(row.getTrendPercent());
        assertTrue("an item with no usage at all is by definition stale", row.isStale());
    }

    @Test
    public void lowStockStillFlagsForReorderEvenWithNoHistoryToProjectFrom() {
        InventoryProjection row = InventoryProjectionCalculator.project(3.0, new double[30], 10,
                new LeadTime(30, LeadTimeTier.DEFAULT), null, TODAY);

        assertEquals("threshold alone is enough to call for a reorder", BoardStatus.REORDER_NOW, row.getStatus());
        assertNull(row.getRunOutEarly());
    }

    @Test
    public void anItemWithNoThresholdStillFlagsWhenItRunsDry() {
        InventoryProjection noHistory = InventoryProjectionCalculator.project(0.0, new double[30], null,
                new LeadTime(30, LeadTimeTier.DEFAULT), null, TODAY);
        InventoryProjection withHistory = InventoryProjectionCalculator.project(0.0, flat(4.0), null,
                new LeadTime(30, LeadTimeTier.DEFAULT), TODAY, TODAY);

        assertEquals("nothing on hand is a reorder whether or not a threshold was entered", BoardStatus.REORDER_NOW,
                noHistory.getStatus());
        assertEquals(BoardStatus.REORDER_NOW, withHistory.getStatus());
        assertNull("the board must still say the lab has set no threshold", noHistory.getLowStockThreshold());
    }

    @Test
    public void anItemWithNoThresholdIsAdequateWhileItHasStockToSpare() {
        // 2 a day, 400 on hand, 30 days to resupply: months of headroom and no
        // threshold. The fallback floor of zero must not make everything urgent.
        InventoryProjection row = InventoryProjectionCalculator.project(400.0, flat(2.0), null,
                new LeadTime(30, LeadTimeTier.DEFAULT), TODAY, TODAY);

        assertEquals(BoardStatus.ADEQUATE, row.getStatus());
    }

    @Test
    public void reorderSoonFiresWhenTheThresholdArrivesInsideTheLeadTime() {
        // 4 a day, 50 on hand, threshold 20: the threshold is about 7 days out.
        InventoryProjection shortLead = InventoryProjectionCalculator.project(50.0, flat(4.0), 20,
                new LeadTime(3, LeadTimeTier.SET), TODAY, TODAY);
        InventoryProjection longLead = InventoryProjectionCalculator.project(50.0, flat(4.0), 20,
                new LeadTime(21, LeadTimeTier.SET), TODAY, TODAY);

        assertEquals("3 days to resupply, 7 days of headroom: no rush", BoardStatus.ADEQUATE, shortLead.getStatus());
        assertEquals("21 days to resupply and 7 days of headroom: order now to arrive in time",
                BoardStatus.REORDER_SOON, longLead.getStatus());
    }

    @Test
    public void quietUsageMarksTheEstimateStaleWithoutSuppressingIt() {
        LocalDate lastSeen = TODAY.minusDays(InventoryProjectionCalculator.STALE_AFTER_DAYS + 1);

        InventoryProjection row = InventoryProjectionCalculator.project(40.0, flat(4.0), 5, SET_14_DAYS, lastSeen,
                TODAY);

        assertTrue(row.isStale());
        assertEquals(lastSeen, row.getBasisDate());
        assertEquals("a stale estimate is still shown, flagged, not withheld", LocalDate.of(2026, 9, 26),
                row.getRunOutEarly());
    }

    @Test
    public void trendComparesTheTwoHalvesOfTheWindow() {
        double[] risingUse = new double[30];
        Arrays.fill(risingUse, 0, 15, 2.0);
        Arrays.fill(risingUse, 15, 30, 3.0);

        assertEquals(50.0, InventoryProjectionCalculator.trendPercent(risingUse), 0.0001);
    }

    @Test
    public void trendIsSuppressedWhenTheEarlierHalfSawNoUsage() {
        double[] newlyActive = new double[30];
        Arrays.fill(newlyActive, 15, 30, 3.0);

        assertNull("there is no honest percentage to quote against zero",
                InventoryProjectionCalculator.trendPercent(newlyActive));
    }

    @Test
    public void leadTimeFallsBackThroughItsTiersAndReportsWhichOneItUsed() {
        assertEquals(new LeadTime(7, LeadTimeTier.SET), InventoryProjectionCalculator.resolveLeadTime(7, 18));
        assertEquals("with nothing entered, what the deliveries actually showed wins",
                new LeadTime(18, LeadTimeTier.OBSERVED), InventoryProjectionCalculator.resolveLeadTime(null, 18));
        assertEquals(new LeadTime(InventoryProjectionCalculator.DEFAULT_LEAD_TIME_DAYS, LeadTimeTier.DEFAULT),
                InventoryProjectionCalculator.resolveLeadTime(null, null));
        assertEquals("a zero is not a lead time anyone entered on purpose",
                new LeadTime(InventoryProjectionCalculator.DEFAULT_LEAD_TIME_DAYS, LeadTimeTier.DEFAULT),
                InventoryProjectionCalculator.resolveLeadTime(0, null));
    }

    @Test
    public void medianIgnoresOrderAndAveragesTheMiddlePairWhenTheCountIsEven() {
        assertEquals(3.0, InventoryProjectionCalculator.median(new double[] { 9, 1, 3, 2, 5 }), 0.0001);
        assertEquals(2.5, InventoryProjectionCalculator.median(new double[] { 1, 2, 3, 4 }), 0.0001);
        assertEquals(0.0, InventoryProjectionCalculator.median(new double[0]), 0.0001);
    }
}
