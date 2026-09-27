package org.openelisglobal.reports.dataexport.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Inclusive laboratory dates translated into a half-open query interval. */
public record ExportDateRange(Instant start, Instant endExclusive) {
    public static ExportDateRange of(String from, String to, ZoneId zone, int maximumDays) {
        if (from == null || to == null || zone == null || maximumDays < 1) {
            throw new IllegalArgumentException("reporting.dates.invalid");
        }
        try {
            LocalDate first = LocalDate.parse(from);
            LocalDate last = LocalDate.parse(to);
            long days = ChronoUnit.DAYS.between(first, last) + 1;
            if (days < 1 || days > maximumDays) {
                throw new IllegalArgumentException("reporting.dates.range");
            }
            return new ExportDateRange(first.atStartOfDay(zone).toInstant(),
                    last.plusDays(1).atStartOfDay(zone).toInstant());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("reporting.dates.invalid", exception);
        }
    }
}
