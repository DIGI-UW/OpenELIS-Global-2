package org.openelisglobal.reports.tat.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface TATCalculationService {

    /**
     * Calculate TAT in hours between two timestamps.
     *
     * @param start start timestamp
     * @param end   end timestamp
     * @param mode  CALENDAR (all hours) or WORKING_TIME (exclude weekends/holidays)
     * @return hours as decimal, or null if either timestamp is null
     */
    BigDecimal calculateTatHours(Timestamp start, Timestamp end, TATCalculationMode mode);

    /**
     * Get all dates that are excluded from Working Time in a range. Combines
     * weekend days + active public holidays.
     */
    List<LocalDate> getExcludedDates(LocalDate from, LocalDate to);

    /**
     * Count excluded days in a date range (for the info bar display).
     */
    int countExcludedDays(LocalDate from, LocalDate to);
}
