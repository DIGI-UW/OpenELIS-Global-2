package org.openelisglobal.inventory.projection;

import java.time.LocalDate;
import java.util.Arrays;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjection.LeadTimeTier;

/**
 * The run-out arithmetic behind the items board, kept free of Spring and the
 * database so the edge cases that matter (no history, one busy day, perfectly
 * steady use, erratic use) can be tested directly.
 *
 * <p>
 * Two rules govern the output. The centre of the estimate is the
 * <em>median</em> daily use, not the mean, so a single outbreak day cannot pull
 * a run-out date forward. And the estimate is a window whose width is the
 * dispersion of that daily use, so steady consumption produces a tight window
 * and erratic consumption a visibly wide one. Where the data cannot support a
 * date at all, the methods here return no date rather than a confident-looking
 * guess.
 */
public final class InventoryProjectionCalculator {

    /** Trailing days of usage the estimate is built from. */
    public static final int WINDOW_DAYS = 30;

    /**
     * Usage older than this makes the estimate stale, and the board hedges rather
     * than asserts.
     */
    public static final int STALE_AFTER_DAYS = 14;

    /**
     * Placeholder lead time when the lab has set none and none has been observed.
     * Deliberately a constant: the value only becomes worth configuring once
     * mark-as-ordered starts producing observed lead times, which is when the tier
     * below it stops being reached.
     */
    public static final int DEFAULT_LEAD_TIME_DAYS = 30;

    private InventoryProjectionCalculator() {
    }

    /**
     * Resolves the lead time to use, preferring what the lab entered, then what its
     * own deliveries showed, then the placeholder. Returns the tier alongside the
     * number so the board can say which one it used rather than presenting a
     * placeholder as fact.
     *
     * @param setDays      the value entered for this item, or null
     * @param observedDays the median of this item's order-to-receipt cycles, or
     *                     null when too few cycles have completed
     */
    public static LeadTime resolveLeadTime(Integer setDays, Integer observedDays) {
        if (setDays != null && setDays > 0) {
            return new LeadTime(setDays, LeadTimeTier.SET);
        }
        if (observedDays != null && observedDays > 0) {
            return new LeadTime(observedDays, LeadTimeTier.OBSERVED);
        }
        return new LeadTime(DEFAULT_LEAD_TIME_DAYS, LeadTimeTier.DEFAULT);
    }

    /**
     * Builds the computed half of a board row. The caller fills in the item's
     * identity.
     *
     * @param onHand    usable quantity across the item's lots
     * @param dailyUse  one entry per day of the trailing window, oldest first,
     *                  zero-filled on days with no usage. Zero-filling matters:
     *                  dropping the quiet days would overstate the median and pull
     *                  every run-out date forward.
     * @param threshold the item's low stock threshold, or null when it has none
     * @param leadTime  the resolved lead time and its tier
     * @param basisDate the newest usage date seen, or null when the item has no
     *                  usage
     * @param today     the date to measure from
     */
    public static InventoryProjection project(double onHand, double[] dailyUse, Integer threshold, LeadTime leadTime,
            LocalDate basisDate, LocalDate today) {

        InventoryProjection projection = new InventoryProjection();
        projection.setOnHand(onHand);
        projection.setLowStockThreshold(threshold);
        projection.setLeadTimeDays(leadTime.days());
        projection.setLeadTimeTier(leadTime.tier());
        projection.setBasisDate(basisDate);
        projection.setStale(basisDate == null || basisDate.isBefore(today.minusDays(STALE_AFTER_DAYS)));

        double median = median(dailyUse);
        projection.setMedianDailyUse(median);

        // An item with no threshold entered is held to zero rather than left out of the
        // ladder. Reading a null threshold as "no floor at all" pinned such an item at
        // Adequate forever: the at-or-below test could not fire, the reorder test was
        // guarded on the threshold being set, and every row fell through to Adequate
        // even at nothing on hand. Out of stock is out of stock whether or not anyone
        // typed a number. The threshold itself is still reported as null, so the board
        // goes on saying the lab has not set one.
        double floor = threshold == null ? 0 : threshold;
        boolean atOrBelowThreshold = onHand <= floor;

        if (median <= 0) {
            // More than half the window saw no usage at all. Any run-out date derived from
            // this
            // would be arithmetic on noise, so the row falls back to the item's own
            // threshold and
            // says so. This covers both a brand new item and a genuinely low-volume one.
            projection.setStatus(atOrBelowThreshold ? BoardStatus.REORDER_NOW : BoardStatus.BUILDING_DATA);
            return projection;
        }

        double spread = medianAbsoluteDeviation(dailyUse, median);
        double fastRate = median + spread;
        double slowRate = median - spread;

        projection.setRunOutEarly(today.plusDays((long) Math.floor(onHand / fastRate)));
        if (slowRate > 0) {
            projection.setRunOutLate(today.plusDays((long) Math.ceil(onHand / slowRate)));
        }
        // When slowRate is not positive the slow end is open-ended; the late date stays
        // null rather
        // than being clamped to something that would read as a real estimate.

        projection.setOrderByDate(projection.getRunOutEarly().minusDays(leadTime.days()));
        projection.setTrendPercent(trendPercent(dailyUse));

        if (atOrBelowThreshold) {
            projection.setStatus(BoardStatus.REORDER_NOW);
        } else if ((onHand - floor) / fastRate <= leadTime.days()) {
            // Reaches the threshold before a replacement order could arrive, so a
            // slow-to-arrive
            // item flags earlier than a fast one. This is the same test reorder suggestions
            // use.
            projection.setStatus(BoardStatus.REORDER_SOON);
        } else {
            projection.setStatus(BoardStatus.ADEQUATE);
        }
        return projection;
    }

    /**
     * Change in median daily use between the older and newer half of the window, as
     * a percentage. Returns null when the older half saw no usage, because there is
     * no honest percentage to quote against zero.
     */
    static Double trendPercent(double[] dailyUse) {
        if (dailyUse.length < 4) {
            return null;
        }
        int midpoint = dailyUse.length / 2;
        double older = median(Arrays.copyOfRange(dailyUse, 0, midpoint));
        double newer = median(Arrays.copyOfRange(dailyUse, midpoint, dailyUse.length));
        if (older <= 0) {
            return null;
        }
        return (newer - older) / older * 100.0;
    }

    static double median(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int midpoint = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[midpoint] : (sorted[midpoint - 1] + sorted[midpoint]) / 2.0;
    }

    /**
     * How far daily use typically strays from its median, as a robust stand-in for
     * the standard deviation.
     *
     * <p>
     * A plain standard deviation would undo the point of choosing the median: one
     * outbreak day of 200 against a normal 2 leaves the median at 2 but sends the
     * deviation past 36, which would widen the window absurdly and flag a month of
     * stock as needing a reorder. The median absolute deviation ignores that day
     * the same way the median does. The 1.4826 factor is the usual consistency
     * constant, so the result is on the same scale as the standard deviation the
     * window is meant to express.
     */
    static double medianAbsoluteDeviation(double[] values, double centre) {
        if (values.length == 0) {
            return 0;
        }
        double[] distances = new double[values.length];
        for (int index = 0; index < values.length; index++) {
            distances[index] = Math.abs(values[index] - centre);
        }
        return median(distances) * 1.4826;
    }

    /** A resolved lead time and the tier it came from. */
    public record LeadTime(int days, LeadTimeTier tier) {
    }
}
