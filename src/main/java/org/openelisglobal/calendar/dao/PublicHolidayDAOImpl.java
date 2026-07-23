package org.openelisglobal.calendar.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicHolidayDAOImpl extends BaseDAOImpl<PublicHoliday, Integer> implements PublicHolidayDAO {

    private static final Logger logger = LoggerFactory.getLogger(PublicHolidayDAOImpl.class);

    public PublicHolidayDAOImpl() {
        super(PublicHoliday.class);
    }

    @Override
    public List<PublicHoliday> getHolidaysForYear(int year, boolean includeInactive) {
        try {
            // Get non-recurring holidays for the target year + all recurring holidays.
            // Use date range instead of EXTRACT (EXTRACT in HQL causes SQL syntax
            // errors with Hibernate 5.x on PostgreSQL).
            String hql = "FROM PublicHoliday h WHERE "
                    + "(h.holidayDate BETWEEN :yearStart AND :yearEnd OR h.isRecurring = true)";
            if (!includeInactive) {
                hql += " AND h.isActive = true";
            }
            hql += " ORDER BY h.holidayDate ASC";

            Query<PublicHoliday> query = entityManager.unwrap(Session.class).createQuery(hql, PublicHoliday.class);
            query.setParameter("yearStart", java.sql.Date.valueOf(year + "-01-01"));
            query.setParameter("yearEnd", java.sql.Date.valueOf(year + "-12-31"));
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting holidays for year {}", year, e);
            throw new LIMSRuntimeException("Error getting holidays for year", e);
        }
    }

    @Override
    public boolean existsByDateInYear(java.sql.Date date, Integer excludeId) {
        try {
            // Check for exact date match OR recurring holiday on same month/day.
            // Use native SQL for EXTRACT since Hibernate 5.x HQL doesn't support it.
            String sql = "SELECT COUNT(*) FROM clinlims.public_holiday h WHERE " + "((h.holiday_date = :date) OR "
                    + " (h.is_recurring = true AND EXTRACT(MONTH FROM h.holiday_date) = EXTRACT(MONTH FROM CAST(:date AS DATE))"
                    + "  AND EXTRACT(DAY FROM h.holiday_date) = EXTRACT(DAY FROM CAST(:date AS DATE))))";
            if (excludeId != null) {
                sql += " AND h.id != :excludeId";
            }

            @SuppressWarnings("unchecked")
            Query<Number> query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("date", date);
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            Number count = query.uniqueResult();
            return count != null && count.longValue() > 0;
        } catch (Exception e) {
            logger.error("Error checking holiday exists by date", e);
            throw new LIMSRuntimeException("Error checking holiday exists by date", e);
        }
    }
}
