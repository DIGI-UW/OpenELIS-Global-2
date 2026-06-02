package org.openelisglobal.calendar.service;

import java.util.List;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PublicHolidayService {

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_VIEW')")
    List<PublicHoliday> getHolidaysForYear(int year, boolean includeInactive);

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_VIEW')")
    PublicHoliday getById(Integer id);

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_MANAGE')")
    PublicHoliday create(PublicHoliday holiday, Integer sysUserId);

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_MANAGE')")
    PublicHoliday update(PublicHoliday holiday, Integer sysUserId);

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_MANAGE')")
    void delete(Integer id);

    /**
     * Import holidays from parsed CSV rows.
     *
     * @return result with counts: imported, skipped, errors
     */
    @PreAuthorize("hasAuthority('PRIV_CALENDAR_MANAGE')")
    ImportResult importHolidays(List<PublicHoliday> holidays, int targetYear, Integer sysUserId);

    record ImportResult(int imported, int skipped, List<ImportError> errors) {
    }

    record ImportError(int row, String reason) {
    }
}
