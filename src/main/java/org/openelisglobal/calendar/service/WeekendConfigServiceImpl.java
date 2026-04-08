package org.openelisglobal.calendar.service;

import java.util.List;
import org.openelisglobal.calendar.dao.WeekendConfigDAO;
import org.openelisglobal.calendar.valueholder.WeekendConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WeekendConfigServiceImpl implements WeekendConfigService {

    @Autowired
    private WeekendConfigDAO weekendConfigDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getWeekendDayNumbers() {
        return weekendConfigDAO.getWeekendDayNumbers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeekendConfig> getAllConfigs() {
        return weekendConfigDAO.getAllConfigs();
    }

    @Override
    public void updateWeekendDays(List<Integer> weekendDays, Integer sysUserId) {
        List<WeekendConfig> allConfigs = weekendConfigDAO.getAllConfigs();
        for (WeekendConfig config : allConfigs) {
            boolean shouldBeWeekend = weekendDays.contains(config.getDayOfWeek());
            if (shouldBeWeekend != Boolean.TRUE.equals(config.getIsWeekend())) {
                config.setIsWeekend(shouldBeWeekend);
                config.setSystemUserId(sysUserId);
                weekendConfigDAO.update(config);
            }
        }
    }
}
