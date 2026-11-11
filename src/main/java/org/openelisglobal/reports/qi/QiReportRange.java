package org.openelisglobal.reports.qi;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * The date window every quality-indicator endpoint takes, plus the paging
 * clamps that go with it. Bad input is rejected as 400 with the offending
 * reason, so a controller never has to spell the guard out itself. The shared
 * advice over the quality-assurance controllers turns the rejection into the
 * 400 body clients have always received.
 */
public record QiReportRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 200;
    public static final long MAX_DATE_RANGE_DAYS = 366;

    /** ISO dates, from at or before to, at most a year apart. */
    public static QiReportRange parse(String fromDate, String toDate) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid parameter: " + e.getMessage());
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed 1 year");
        }
        return new QiReportRange(from, to);
    }

    public static int clampPage(int page) {
        return Math.max(page, 0);
    }

    public static int clampPageSize(int pageSize) {
        return pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
