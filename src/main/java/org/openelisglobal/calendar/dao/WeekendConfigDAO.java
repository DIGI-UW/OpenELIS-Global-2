package org.openelisglobal.calendar.dao;

import java.util.List;
import org.openelisglobal.calendar.valueholder.WeekendConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface WeekendConfigDAO extends BaseDAO<WeekendConfig, Integer> {

    List<WeekendConfig> getAllConfigs();

    List<Integer> getWeekendDayNumbers();
}
