package org.openelisglobal.reports.qi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import org.openelisglobal.referencetables.service.ReferenceTablesService;

/**
 * Helpers shared by the quality-indicator computes (turnaround time, rejection,
 * amendment, critical callback): window bounds, period bucketing, rate rounding
 * and in-memory paging. One copy is what keeps the four reports numerically
 * identical — same rounding mode, same bucket edges, same half-open window.
 */
public final class QiReportSupport {

    private QiReportSupport() {
    }

    /** Inclusive start of the window. */
    public static Timestamp startOf(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    /**
     * Exclusive end of the window: midnight starting the day after {@code date}.
     */
    public static Timestamp endOf(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }

    /**
     * The {@code date_trunc} unit for a reporting interval. Unknown and null
     * intervals fall back to daily, matching the lenient parsing every QI surface
     * applies to the interval parameter.
     */
    public static String truncUnit(String interval) {
        if (interval == null) {
            return "day";
        }
        return switch (interval.toUpperCase()) {
        case "WEEKLY" -> "week";
        case "MONTHLY" -> "month";
        default -> "day";
        };
    }

    /** Period label for a {@code date_trunc} bucket coming back from SQL. */
    public static String periodKey(Object truncatedBucket, String interval) {
        return periodKey(((Timestamp) truncatedBucket).toLocalDateTime().toLocalDate(), interval);
    }

    /** Period label: ISO date, {@code yyyy-Www} (ISO week) or {@code yyyy-MM}. */
    public static String periodKey(LocalDate date, String interval) {
        String resolved = interval == null ? "DAILY" : interval.toUpperCase();
        return switch (resolved) {
        case "WEEKLY" -> date.getYear() + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        case "MONTHLY" -> date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        default -> date.toString();
        };
    }

    /** Percent to two decimals (HALF_UP), or null when the denominator is zero. */
    public static Double ratePercent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return round2(part * 100.0 / whole);
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * One page of an already-ordered in-memory list; pages past the end come back
     * empty rather than throwing.
     */
    public static <T> List<T> page(List<T> all, int page, int pageSize) {
        int fromIndex = Math.min(page * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        return all.subList(fromIndex, toIndex);
    }

    /**
     * Numeric reference_tables id, for native queries matching note/history rows.
     */
    public static Long refTableId(ReferenceTablesService referenceTablesService, String name) {
        return Long.valueOf(referenceTablesService.getReferenceTableByName(name).getId());
    }
}
