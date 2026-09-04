package org.openelisglobal.calendar.service;

import java.util.List;
import org.openelisglobal.calendar.valueholder.WeekendConfig;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WeekendConfigService {

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_VIEW')")
    List<Integer> getWeekendDayNumbers();

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_VIEW')")
    List<WeekendConfig> getAllConfigs();

    @PreAuthorize("hasAuthority('PRIV_CALENDAR_MANAGE')")
    void updateWeekendDays(List<Integer> weekendDays, Integer sysUserId);
}
