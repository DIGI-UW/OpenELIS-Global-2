package org.openelisglobal.dashboard.management;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.springframework.beans.factory.annotation.Autowired;

public class ManagementDashboardServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ManagementDashboardService managementDashboardService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/management-dashboard.xml");
    }

    @Test
    public void countsSamplesReceivedOnEveryDayOfTheScopeAndNoOthers() {
        ManagementSummary summary = managementDashboardService.getSummary(new ScopeRange(LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 28)));

        assertEquals(2, summary.samplesReceived());
    }

    @Test
    public void reportsEveryOptionalModule() {
        ManagementSummary summary = managementDashboardService.getSummary(ScopeRange.of("30d", LocalDate.now()));

        assertEquals(Set.of("inventory", "eqa", "analyzers", "ast", "qc"), summary.features().keySet());
    }
}
