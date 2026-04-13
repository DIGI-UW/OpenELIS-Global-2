package org.openelisglobal.reports.tat.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.calendar.service.PublicHolidayService;
import org.openelisglobal.calendar.service.WeekendConfigService;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TATCalculationServiceImpl implements TATCalculationService {

    @Autowired
    private WeekendConfigService weekendConfigService;

    @Autowired
    private PublicHolidayService publicHolidayService;

    @Override
    public BigDecimal calculateTatHours(Timestamp start, Timestamp end, TATCalculationMode mode) {
        if (start == null || end == null) {
            return null;
        }

        // Reject negative durations (data entry errors where end < start)
        Duration elapsed = Duration.between(start.toInstant(), end.toInstant());
        if (elapsed.isNegative()) {
            return null;
        }

        if (mode == TATCalculationMode.CALENDAR) {
            return BigDecimal.valueOf(elapsed.toMillis()).divide(BigDecimal.valueOf(3_600_000), 2,
                    RoundingMode.HALF_UP);
        }

        // WORKING_TIME: exclude hours falling on weekends and holidays
        return calculateWorkingTimeHours(start.toLocalDateTime(), end.toLocalDateTime());
    }

    @Override
    public List<LocalDate> getExcludedDates(LocalDate from, LocalDate to) {
        Set<LocalDate> excluded = new HashSet<>();
        List<Integer> weekendDayNums = weekendConfigService.getWeekendDayNumbers();
        Set<DayOfWeek> weekendDays = new HashSet<>();
        for (Integer dayNum : weekendDayNums) {
            weekendDays.add(dayOfWeekFromNumber(dayNum));
        }

        // Add all weekend days in range
        LocalDate current = from;
        while (!current.isAfter(to)) {
            if (weekendDays.contains(current.getDayOfWeek())) {
                excluded.add(current);
            }
            current = current.plusDays(1);
        }

        // Add active holidays in range (consider recurring)
        int fromYear = from.getYear();
        int toYear = to.getYear();
        for (int year = fromYear; year <= toYear; year++) {
            List<PublicHoliday> holidays = publicHolidayService.getHolidaysForYear(year, false);
            for (PublicHoliday h : holidays) {
                LocalDate holidayDate = h.getHolidayDate().toLocalDate();
                if (h.getIsRecurring()) {
                    // For recurring, check the same month/day in the target year
                    try {
                        holidayDate = LocalDate.of(year, holidayDate.getMonthValue(), holidayDate.getDayOfMonth());
                    } catch (java.time.DateTimeException e) {
                        // Feb 29 in non-leap year — skip this holiday for this year
                        continue;
                    }
                }
                if (!holidayDate.isBefore(from) && !holidayDate.isAfter(to)) {
                    excluded.add(holidayDate);
                }
            }
        }

        List<LocalDate> sortedExcluded = new ArrayList<>(excluded);
        sortedExcluded.sort(null);
        return sortedExcluded;
    }

    @Override
    public int countExcludedDays(LocalDate from, LocalDate to) {
        return getExcludedDates(from, to).size();
    }

    private BigDecimal calculateWorkingTimeHours(LocalDateTime start, LocalDateTime end) {
        List<LocalDate> excludedDates = getExcludedDates(start.toLocalDate(), end.toLocalDate());
        Set<LocalDate> excludedSet = new HashSet<>(excludedDates);

        if (start.toLocalDate().equals(end.toLocalDate())) {
            // Same day
            if (excludedSet.contains(start.toLocalDate())) {
                return BigDecimal.ZERO;
            }
            long minutes = Duration.between(start, end).toMinutes();
            return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalMinutes = BigDecimal.ZERO;

        // First day: from start time to end of day (midnight)
        if (!excludedSet.contains(start.toLocalDate())) {
            LocalDateTime endOfStartDay = start.toLocalDate().plusDays(1).atStartOfDay();
            long mins = Duration.between(start, endOfStartDay).toMinutes();
            totalMinutes = totalMinutes.add(BigDecimal.valueOf(mins));
        }

        // Full middle days: 24 hours each if not excluded
        LocalDate day = start.toLocalDate().plusDays(1);
        while (day.isBefore(end.toLocalDate())) {
            if (!excludedSet.contains(day)) {
                totalMinutes = totalMinutes.add(BigDecimal.valueOf(24 * 60));
            }
            day = day.plusDays(1);
        }

        // Last day: from midnight to end time
        if (!excludedSet.contains(end.toLocalDate())) {
            LocalDateTime startOfEndDay = end.toLocalDate().atStartOfDay();
            long mins = Duration.between(startOfEndDay, end).toMinutes();
            totalMinutes = totalMinutes.add(BigDecimal.valueOf(mins));
        }

        return totalMinutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Convert 0=Sunday..6=Saturday to Java DayOfWeek */
    private DayOfWeek dayOfWeekFromNumber(int num) {
        // 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
        // Java: MONDAY=1, TUESDAY=2, ..., SUNDAY=7
        if (num == 0)
            return DayOfWeek.SUNDAY;
        return DayOfWeek.of(num);
    }
}
