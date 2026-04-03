package org.openelisglobal.calendar.dao;

import java.util.List;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.dao.BaseDAO;

public interface PublicHolidayDAO extends BaseDAO<PublicHoliday, Integer> {

    List<PublicHoliday> getHolidaysForYear(int year, boolean includeInactive);

    boolean existsByDateInYear(java.sql.Date date, int year, Integer excludeId);
}
