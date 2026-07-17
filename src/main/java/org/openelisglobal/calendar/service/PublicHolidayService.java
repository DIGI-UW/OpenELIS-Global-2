package org.openelisglobal.calendar.service;

import java.util.List;
import org.openelisglobal.calendar.valueholder.PublicHoliday;

public interface PublicHolidayService {

    List<PublicHoliday> getHolidaysForYear(int year, boolean includeInactive);

    PublicHoliday getById(Integer id);

    PublicHoliday create(PublicHoliday holiday, Integer sysUserId);

    PublicHoliday update(PublicHoliday holiday, Integer sysUserId);

    void delete(Integer id);

    /**
     * Import holidays from parsed CSV rows.
     *
     * @return result with counts: imported, skipped, errors
     */
    ImportResult importHolidays(List<PublicHoliday> holidays, int targetYear, Integer sysUserId);

    record ImportResult(int imported, int skipped, List<ImportError> errors) {
    }

    record ImportError(int row, String reason) {
    }
}
