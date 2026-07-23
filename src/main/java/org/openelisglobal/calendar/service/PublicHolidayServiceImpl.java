package org.openelisglobal.calendar.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.calendar.dao.PublicHolidayDAO;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublicHolidayServiceImpl implements PublicHolidayService {

    private static final Logger logger = LoggerFactory.getLogger(PublicHolidayServiceImpl.class);

    @Autowired
    private PublicHolidayDAO publicHolidayDAO;

    @Override
    @Transactional(readOnly = true)
    public List<PublicHoliday> getHolidaysForYear(int year, boolean includeInactive) {
        return publicHolidayDAO.getHolidaysForYear(year, includeInactive);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicHoliday getById(Integer id) {
        return publicHolidayDAO.get(id).orElse(null);
    }

    @Override
    public PublicHoliday create(PublicHoliday holiday, Integer sysUserId) {
        validateNoDuplicate(holiday.getHolidayDate(), null);
        holiday.setSystemUserId(sysUserId);
        Integer id = publicHolidayDAO.insert(holiday);
        return publicHolidayDAO.get(id).orElse(holiday);
    }

    @Override
    public PublicHoliday update(PublicHoliday holiday, Integer sysUserId) {
        validateNoDuplicate(holiday.getHolidayDate(), holiday.getId());
        holiday.setSystemUserId(sysUserId);
        return publicHolidayDAO.update(holiday);
    }

    @Override
    public void delete(Integer id) {
        PublicHoliday holiday = publicHolidayDAO.get(id).orElse(null);
        if (holiday == null) {
            throw new LIMSRuntimeException("Holiday not found: " + id);
        }
        publicHolidayDAO.delete(holiday);
    }

    @Override
    public ImportResult importHolidays(List<PublicHoliday> holidays, int targetYear, Integer sysUserId) {
        int imported = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        // Phase 1: Validate all rows first (no DB mutations)
        List<PublicHoliday> validHolidays = new ArrayList<>();
        Set<String> seenDates = new HashSet<>();
        for (int i = 0; i < holidays.size(); i++) {
            PublicHoliday holiday = holidays.get(i);
            int row = i + 1; // 1-based for user display
            if (holiday.getHolidayDate() == null || holiday.getHolidayName() == null
                    || holiday.getHolidayName().isBlank()) {
                errors.add(new ImportError(row, "Missing required fields (date and name)"));
                skipped++;
                continue;
            }
            if (targetYear > 0 && holiday.getHolidayDate().toLocalDate().getYear() != targetYear) {
                errors.add(new ImportError(row,
                        "Date " + holiday.getHolidayDate() + " is not in target year " + targetYear));
                skipped++;
                continue;
            }
            String dateKey = holiday.getHolidayDate().toString();
            if (!seenDates.add(dateKey)) {
                errors.add(new ImportError(row, "Duplicate date within batch: " + dateKey));
                skipped++;
                continue;
            }
            if (publicHolidayDAO.existsByDateInYear(holiday.getHolidayDate(), null)) {
                errors.add(new ImportError(row, "Duplicate date: " + holiday.getHolidayDate().toString()));
                skipped++;
                continue;
            }
            holiday.setSystemUserId(sysUserId);
            if (holiday.getIsActive() == null) {
                holiday.setIsActive(true);
            }
            if (holiday.getIsRecurring() == null) {
                holiday.setIsRecurring(false);
            }
            validHolidays.add(holiday);
        }

        // Phase 2: Insert all valid rows (no try/catch — let @Transactional handle
        // failures atomically)
        for (PublicHoliday holiday : validHolidays) {
            publicHolidayDAO.insert(holiday);
            imported++;
        }

        return new ImportResult(imported, skipped, errors);
    }

    private void validateNoDuplicate(Date holidayDate, Integer excludeId) {
        if (publicHolidayDAO.existsByDateInYear(holidayDate, excludeId)) {
            throw new LIMSRuntimeException("A holiday already exists for this date (including recurring holidays)");
        }
    }
}
