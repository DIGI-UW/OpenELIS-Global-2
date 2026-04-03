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
            // Get non-recurring holidays for the target year + all recurring holidays
            String hql = "FROM PublicHoliday h WHERE "
                    + "(EXTRACT(YEAR FROM h.holidayDate) = :year OR h.isRecurring = true)";
            if (!includeInactive) {
                hql += " AND h.isActive = true";
            }
            hql += " ORDER BY h.holidayDate ASC";

            Query<PublicHoliday> query = entityManager.unwrap(Session.class).createQuery(hql, PublicHoliday.class);
            query.setParameter("year", year);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting holidays for year {}", year, e);
            throw new LIMSRuntimeException("Error getting holidays for year", e);
        }
    }

    @Override
    public boolean existsByDateInYear(java.sql.Date date, int year, Integer excludeId) {
        try {
            // Check for exact date match OR recurring holiday on same month/day
            String hql = "SELECT COUNT(*) FROM PublicHoliday h WHERE " + "((h.holidayDate = :date) OR "
                    + " (h.isRecurring = true AND EXTRACT(MONTH FROM h.holidayDate) = EXTRACT(MONTH FROM :date)"
                    + "  AND EXTRACT(DAY FROM h.holidayDate) = EXTRACT(DAY FROM :date)))";
            if (excludeId != null) {
                hql += " AND h.id != :excludeId";
            }

            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("date", date);
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking holiday exists by date", e);
            throw new LIMSRuntimeException("Error checking holiday exists by date", e);
        }
    }
}
