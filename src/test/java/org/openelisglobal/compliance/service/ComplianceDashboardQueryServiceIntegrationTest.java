package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.MonthDataPointDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Environmental Compliance Dashboard's order counts (OGC-1192). "Total
 * Orders" counted result rows, so an order counted once per result and an order
 * with no result yet did not count at all: a freshly entered environmental
 * order left the dashboard at zero. The compliance rate also counted results no
 * threshold applies to as failures. The fixture has six results on four June
 * orders over three sites; see compliance-dashboard.xml.
 */
public class ComplianceDashboardQueryServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate JUNE_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate JUNE_END = LocalDate.of(2026, 6, 30);

    @Autowired
    private ComplianceDashboardQueryService dashboardService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/compliance-dashboard.xml");
        jdbcTemplate.update("INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                + " SELECT 9101, 'envSamplingSiteId', 'Sampling site id', now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.observation_history_type WHERE type_name = 'envSamplingSiteId')");
        jdbcTemplate.update("DELETE FROM clinlims.observation_history WHERE id BETWEEN 9101 AND 9106"
                + " OR sample_id BETWEEN 9101 AND 9106");
        String[][] siteBySample = { { "9101", "9101" }, { "9102", "9101" }, { "9103", "9102" }, { "9105", "9101" },
                { "9106", "9103" } };
        for (String[] row : siteBySample) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.observation_history"
                            + " (id, sample_id, observation_history_type_id, value_type, value, lastupdated)"
                            + " SELECT ?, ?, id, 'L', ?, now() FROM clinlims.observation_history_type"
                            + " WHERE type_name = 'envSamplingSiteId'",
                    Integer.parseInt(row[0]), Integer.parseInt(row[0]), row[1]);
        }
    }

    @Test
    public void totalOrdersCountsEachOrderOnceIncludingOrdersWithoutResults() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, null, JUNE_START, JUNE_END);

        assertEquals(4, summary.getTotalOrders());
    }

    @Test
    public void sitesMonitoredCountsSitesWithAnOrderInThePeriod() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, null, JUNE_START, JUNE_END);

        assertEquals(3, summary.getSitesMonitored());
    }

    @Test
    public void complianceRateCountsOnlyResultsJudgedAgainstAThreshold() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, null, JUNE_START, JUNE_END);

        assertEquals(80.0, summary.getComplianceRate(), 0.0001);
        assertEquals(1, summary.getTotalExceedances());
    }

    @Test
    public void siteComparisonRateLeavesOutResultsWithNoThreshold() {
        Map<String, SiteComparisonDTO> bySite = dashboardService.getSiteComparison(null, JUNE_START, JUNE_END).stream()
                .collect(Collectors.toMap(SiteComparisonDTO::getSiteId, site -> site));

        assertEquals(100.0, bySite.get("9101").getComplianceRate(), 0.0001);
        assertEquals(0.0, bySite.get("9102").getComplianceRate(), 0.0001);
    }

    @Test
    public void aSiteWithNoJudgedResultHasNoComplianceRate() {
        Map<String, SiteComparisonDTO> bySite = dashboardService.getSiteComparison(null, JUNE_START, JUNE_END).stream()
                .collect(Collectors.toMap(SiteComparisonDTO::getSiteId, site -> site));

        assertNull(bySite.get("9103").getComplianceRate());
        assertNull(bySite.get("9103").getColorBand());
        assertEquals(1, bySite.get("9103").getTotalOrders());
    }

    @Test
    public void aFilterWithNoJudgedResultHasNoSummaryComplianceRate() {
        DashboardSummaryDTO summary = dashboardService.getSummary(List.of("9103"), null, JUNE_START, JUNE_END);

        assertNull(summary.getComplianceRate());
        assertNull(summary.getTrend().getComplianceRate());
        assertEquals(1, summary.getTotalOrders());
    }

    @Test
    public void theRateTrendIsEmptyWhenThePriorPeriodHasNoJudgedResult() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, null, JUNE_START, JUNE_END);

        assertEquals(80.0, summary.getComplianceRate(), 0.0001);
        assertNull(summary.getTrend().getComplianceRate());
    }

    @Test
    public void theTrendHasNoPointForASiteWithNoJudgedResult() {
        assertTrue(dashboardService.getTrend(null, null, JUNE_START, JUNE_END).getSeries().stream()
                .noneMatch(series -> "9103".equals(series.getSiteId())));
    }

    @Test
    public void trendRateLeavesOutResultsWithNoThreshold() {
        MonthDataPointDTO siteAJune10 = dashboardService.getTrend(null, null, JUNE_START, JUNE_END).getSeries().stream()
                .filter(series -> "9101".equals(series.getSiteId())).findFirst().orElseThrow().getDataPoints().get(0);

        assertEquals(5, siteAJune10.getTotalResults());
        assertEquals(100.0, siteAJune10.getComplianceRate(), 0.0001);
    }

    @Test
    public void aSiteFilterCountsOnlyThatSitesOrders() {
        DashboardSummaryDTO summary = dashboardService.getSummary(List.of("9101"), null, JUNE_START, JUNE_END);

        assertEquals(2, summary.getTotalOrders());
        assertEquals(1, summary.getSitesMonitored());
    }

    @Test
    public void aStandardFilterCountsOnlyOrdersUnderThatStandard() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, "9101", JUNE_START, JUNE_END);

        assertEquals(2, summary.getTotalOrders());
    }

    @Test
    public void anOrderCollectedOutsideThePeriodIsNotCounted() {
        DashboardSummaryDTO summary = dashboardService.getSummary(null, null, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31));

        assertEquals(1, summary.getTotalOrders());
    }

    @Test
    public void siteComparisonCountsOrdersNotResults() {
        Map<String, Integer> ordersBySite = dashboardService.getSiteComparison(null, JUNE_START, JUNE_END).stream()
                .collect(Collectors.toMap(SiteComparisonDTO::getSiteId, SiteComparisonDTO::getTotalOrders));

        assertEquals(Integer.valueOf(2), ordersBySite.get("9101"));
        assertEquals(Integer.valueOf(1), ordersBySite.get("9102"));
    }
}
