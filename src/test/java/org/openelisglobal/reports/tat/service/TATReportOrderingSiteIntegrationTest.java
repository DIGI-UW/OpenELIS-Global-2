package org.openelisglobal.reports.tat.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TATReportOrderingSiteIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate DAY = LocalDate.of(2099, 3, 10);

    @Autowired
    private TATReportService tatReportService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void eachRowCarriesItsOwnSamplesOrderingSite() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long north = organization("North clinic " + run);
        long south = organization("South clinic " + run);
        long east = organization("East clinic " + run);
        sample("N" + run, north);
        sample("S" + run, south);
        sample("O" + run);
        sample("D" + run, north, east);

        Map<String, String> sites = new HashMap<>();
        allResults().stream().filter(r -> r.getLabNumber().endsWith(run))
                .forEach(r -> sites.put(r.getLabNumber(), r.getOrderingSite()));

        assertEquals(Set.of("N" + run, "S" + run, "O" + run, "D" + run), sites.keySet());
        assertEquals("North clinic " + run, sites.get("N" + run));
        assertEquals("South clinic " + run, sites.get("S" + run));
        assertNull(sites.get("O" + run));
        assertEquals("East clinic " + run, sites.get("D" + run));
    }

    @Test
    public void moreRowsRunNoMoreQueries() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long site = organization("Clinic " + run);
        sample("A" + run, site);
        sample("B" + run, site);
        long twoRows = queriesForOneDay();
        for (int i = 0; i < 6; i++) {
            sample("C" + i + run, site);
        }
        assertEquals(twoRows, queriesForOneDay());
    }

    private long queriesForOneDay() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            allResults();
            return statistics.getQueryExecutionCount();
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    private List<TATResult> allResults() {
        return tatReportService.getAllResults(DAY, DAY, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                null, null, null, null, null, null, false);
    }

    private long organization(String name) {
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.organization (id, name)"
                + " VALUES (nextval('clinlims.organization_seq'), ?) RETURNING id", Long.class, name);
    }

    private void sample(String accession, long... organizationIds) {
        long sampleId = jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                        + " VALUES (nextval('clinlims.sample_seq'), ?, ?, ?) RETURNING id",
                Long.class, accession, DAY.atTime(8, 0), DAY.atTime(9, 0));
        long itemId = jdbcTemplate
                .queryForObject(
                        "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id)"
                                + " VALUES (nextval('clinlims.sample_item_seq'), ?, 1,"
                                + " (SELECT min(id) FROM clinlims.status_of_sample)) RETURNING id",
                        Long.class, sampleId);
        jdbcTemplate.update("INSERT INTO clinlims.analysis"
                + " (id, sampitem_id, analysis_type, status_id, started_date, completed_date, released_date)"
                + " VALUES (nextval('clinlims.analysis_seq'), ?, 'MANUAL', (SELECT min(id) FROM clinlims.status_of_sample"
                + " WHERE status_type = 'ANALYSIS' AND name <> 'Test Canceled'), ?, ?, ?)", itemId, DAY.atTime(10, 0),
                DAY.atTime(11, 0), DAY.atTime(12, 0));
        for (long organizationId : organizationIds) {
            jdbcTemplate.update("INSERT INTO clinlims.sample_organization (id, samp_id, org_id)"
                    + " VALUES (nextval('clinlims.sample_org_seq'), ?, ?)", sampleId, organizationId);
        }
    }
}
