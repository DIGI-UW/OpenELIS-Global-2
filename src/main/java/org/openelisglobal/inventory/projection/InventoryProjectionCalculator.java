package org.openelisglobal.inventory.projection;

import java.time.LocalDate;
import java.util.Arrays;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjection.LeadTimeTier;

public final class InventoryProjectionCalculator {

    public static final int WINDOW_DAYS = 30;

    public static final int STALE_AFTER_DAYS = 14;

    public static final int DEFAULT_LEAD_TIME_DAYS = 30;

    private InventoryProjectionCalculator() {
    }

    public static LeadTime resolveLeadTime(Integer setDays, Integer observedDays) {
        if (setDays != null && setDays > 0) {
            return new LeadTime(setDays, LeadTimeTier.SET);
        }
        if (observedDays != null && observedDays > 0) {
            return new LeadTime(observedDays, LeadTimeTier.OBSERVED);
        }
        return new LeadTime(DEFAULT_LEAD_TIME_DAYS, LeadTimeTier.DEFAULT);
    }

    /** dailyUse: one entry per window day, oldest first, zero-filled. */
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

        // A null threshold counts as zero, so an empty item still reorders.
        double floor = threshold == null ? 0 : threshold;
        boolean atOrBelowThreshold = onHand <= floor;

        if (median <= 0) {
            // More than half the window saw no usage: too little to project from.
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

        projection.setOrderByDate(projection.getRunOutEarly().minusDays(leadTime.days()));
        projection.setTrendPercent(trendPercent(dailyUse));

        boolean pastOrderBy = projection.getOrderByDate().isBefore(today);
        if (atOrBelowThreshold || pastOrderBy) {
            projection.setStatus(BoardStatus.REORDER_NOW);
        } else if ((onHand - floor) / fastRate <= leadTime.days()) {
            // Reaches the threshold before a replacement order could arrive.
            projection.setStatus(BoardStatus.REORDER_SOON);
        } else {
            projection.setStatus(BoardStatus.ADEQUATE);
        }
        return projection;
    }

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

    /** 1.4826 scales the median absolute deviation to a std deviation. */
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

    public record LeadTime(int days, LeadTimeTier tier) {
    }
}
