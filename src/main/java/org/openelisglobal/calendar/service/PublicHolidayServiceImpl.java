package org.openelisglobal.calendar.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
        validateNoDuplicate(holiday.getHolidayDate(), getYear(holiday.getHolidayDate()), null);
        holiday.setSystemUserId(sysUserId);
        Integer id = publicHolidayDAO.insert(holiday);
        return publicHolidayDAO.get(id).orElse(holiday);
    }

    @Override
    public PublicHoliday update(PublicHoliday holiday, Integer sysUserId) {
        validateNoDuplicate(holiday.getHolidayDate(), getYear(holiday.getHolidayDate()), holiday.getId());
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

        for (int i = 0; i < holidays.size(); i++) {
            PublicHoliday holiday = holidays.get(i);
            int row = i + 1; // 1-based for user display
            try {
                if (holiday.getHolidayDate() == null || holiday.getHolidayName() == null
                        || holiday.getHolidayName().isBlank()) {
                    errors.add(new ImportError(row, "Missing required fields (date and name)"));
                    skipped++;
                    continue;
                }
                if (publicHolidayDAO.existsByDateInYear(holiday.getHolidayDate(), targetYear, null)) {
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
                publicHolidayDAO.insert(holiday);
                imported++;
            } catch (Exception e) {
                logger.warn("Error importing holiday row {}: {}", row, e.getMessage());
                errors.add(new ImportError(row, "Import error: " + e.getMessage()));
                skipped++;
            }
        }
        return new ImportResult(imported, skipped, errors);
    }

    private void validateNoDuplicate(Date holidayDate, int year, Integer excludeId) {
        if (publicHolidayDAO.existsByDateInYear(holidayDate, year, excludeId)) {
            throw new LIMSRuntimeException("A holiday already exists for this date (including recurring holidays)");
        }
    }

    private int getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }
}
