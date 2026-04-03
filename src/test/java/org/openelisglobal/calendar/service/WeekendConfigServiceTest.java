package org.openelisglobal.calendar.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.calendar.dao.WeekendConfigDAO;
import org.openelisglobal.calendar.valueholder.WeekendConfig;

@RunWith(MockitoJUnitRunner.class)
public class WeekendConfigServiceTest {

    @InjectMocks
    private WeekendConfigServiceImpl weekendConfigService;

    @Mock
    private WeekendConfigDAO weekendConfigDAO;

    private List<WeekendConfig> allConfigs;

    @Before
    public void setUp() {
        allConfigs = Arrays.asList(makeConfig(0, true), // Sunday - weekend
                makeConfig(1, false), // Monday
                makeConfig(2, false), // Tuesday
                makeConfig(3, false), // Wednesday
                makeConfig(4, false), // Thursday
                makeConfig(5, false), // Friday
                makeConfig(6, true) // Saturday - weekend
        );
    }

    @Test
    public void getWeekendDayNumbers_returnsSatAndSun() {
        when(weekendConfigDAO.getWeekendDayNumbers()).thenReturn(Arrays.asList(0, 6));

        List<Integer> result = weekendConfigService.getWeekendDayNumbers();

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(0)); // Sunday
        Assert.assertTrue(result.contains(6)); // Saturday
    }

    @Test
    public void updateWeekendDays_changesToFridaySaturday() {
        when(weekendConfigDAO.getAllConfigs()).thenReturn(allConfigs);

        // Change from Sat+Sun to Fri+Sat
        weekendConfigService.updateWeekendDays(Arrays.asList(5, 6), 1);

        // Sunday should be turned off (was true, should be false)
        verify(weekendConfigDAO).update(allConfigs.get(0)); // Sun: true -> false
        // Friday should be turned on (was false, should be true)
        verify(weekendConfigDAO).update(allConfigs.get(5)); // Fri: false -> true
        // Saturday stays true - no update needed
        // Total updates: 2 (Sun off, Fri on)
    }

    @Test
    public void updateWeekendDays_noChangesSkipsUpdate() {
        when(weekendConfigDAO.getAllConfigs()).thenReturn(allConfigs);

        // Same as current: Sat(6) + Sun(0)
        weekendConfigService.updateWeekendDays(Arrays.asList(0, 6), 1);

        // No changes needed
        verify(weekendConfigDAO, never()).update(any());
    }

    private WeekendConfig makeConfig(int dayOfWeek, boolean isWeekend) {
        WeekendConfig config = new WeekendConfig();
        config.setId(dayOfWeek + 1);
        config.setDayOfWeek(dayOfWeek);
        config.setIsWeekend(isWeekend);
        config.setSystemUserId(1);
        return config;
    }
}
