package org.openelisglobal.calendar.service;

import java.util.List;
import org.openelisglobal.calendar.valueholder.WeekendConfig;

public interface WeekendConfigService {

    List<Integer> getWeekendDayNumbers();

    List<WeekendConfig> getAllConfigs();

    void updateWeekendDays(List<Integer> weekendDays, Integer sysUserId);
}
