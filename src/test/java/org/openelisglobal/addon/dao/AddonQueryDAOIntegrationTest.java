package org.openelisglobal.addon.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.addon.dao.AddonQueryDAO.StatusIds;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The laboratory-wide queries of the add-on API and the access log, on a real
 * database. Two orders (12345 received 3 June 2024, section TB; 13333 received
 * 4 June, TestSection2); the fixture's own status ids stand for the states.
 */
public class AddonQueryDAOIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate JUNE_3 = LocalDate.of(2024, 6, 3);
    private static final LocalDate JUNE_4 = LocalDate.of(2024, 6, 4);
    /** analysis 1 is "pending" (status 1), analysis 2 "finalized" (status 2) */
    private static final StatusIds IDS = new StatusIds(List.of(1), 3, 2, List.of(-1));

    @Autowired
    private AddonQueryDAO queries;
    @Autowired
    private AddonAccessLogDAO accessLog;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = '2024-06-04 02:00:00' WHERE id = 2");
        jdbcTemplate.update("DELETE FROM clinlims.addon_access_log");
    }

    @Test
    public void ordersAreListedNewestFirstWithTheirStates() {
        AddonQueryDAO.Page<AddonQueryDAO.OrderRow> page = queries.orders(JUNE_3, JUNE_4, "any", null, IDS, 50, 0);
        assertEquals(2, page.total());
        assertEquals("13333", page.rows().get(0).accessionNumber());
        assertEquals("12345", page.rows().get(1).accessionNumber());
        assertEquals(1, page.rows().get(1).pending());
        assertEquals(1, page.rows().get(0).finalized());
        assertEquals(List.of("TestSection2"), page.rows().get(0).sections());
    }

    @Test
    public void ordersAreFilteredByStateSectionAndDays() {
        assertEquals(List.of("12345"), accessions(queries.orders(JUNE_3, JUNE_4, "pending", null, IDS, 50, 0)));
        assertEquals(List.of("13333"), accessions(queries.orders(JUNE_3, JUNE_4, "finalized", null, IDS, 50, 0)));
        assertEquals(List.of("13333"), accessions(queries.orders(JUNE_3, JUNE_4, "any", "TestSection2", IDS, 50, 0)));
        assertEquals(List.of("12345"), accessions(queries.orders(JUNE_3, JUNE_3, "any", null, IDS, 50, 0)));
        assertEquals(List.of("12345"), accessions(queries.orders(JUNE_3, JUNE_4, "any", null, IDS, 1, 1)));
        assertTrue(queries.orders(JUNE_4.plusDays(1), JUNE_4.plusDays(2), "any", null, IDS, 50, 0).rows().isEmpty());
    }

    @Test
    public void theWorklistHoldsTestsInTheGivenStatesOldestFirst() {
        AddonQueryDAO.Page<AddonQueryDAO.WorkItem> page = queries.worklist(List.of(1, 2), null, 50);
        assertEquals(2, page.total());
        assertEquals("12345", page.rows().get(0).accessionNumber());
        assertEquals("CBC", page.rows().get(0).testCode());
        assertEquals("TB", page.rows().get(0).section());
        assertEquals(1, queries.worklist(List.of(1), null, 50).rows().size());
        assertEquals(0, queries.worklist(List.of(1), "TestSection2", 50).rows().size());
    }

    @Test
    public void countsAreAggregatesOnly() {
        List<AddonQueryDAO.CountRow> byDay = queries.counts(JUNE_3, JUNE_4, "day", IDS);
        assertEquals(2, byDay.size());
        assertEquals("2024-06-03", byDay.get(0).key());
        assertEquals(1, byDay.get(0).orders());
        assertEquals(0, byDay.get(0).finalized());
        assertEquals(1, byDay.get(1).finalized());
        List<AddonQueryDAO.CountRow> bySection = queries.counts(JUNE_3, JUNE_4, "section", IDS);
        assertEquals(List.of("TB", "TestSection2"), bySection.stream().map(AddonQueryDAO.CountRow::key).toList());
    }

    @Test
    public void turnaroundRunsFromReceivedToReleased() {
        List<AddonQueryDAO.TatRow> rows = queries.turnaround(JUNE_4, JUNE_4, "section", 2);
        assertEquals(1, rows.size());
        assertEquals("TestSection2", rows.get(0).key());
        assertEquals(1, rows.get(0).count());
        assertEquals(120.0, rows.get(0).medianMinutes(), 0.01);
        assertEquals("UA", queries.turnaround(JUNE_4, JUNE_4, "test", 2).get(0).key());
        // released before it was received (bad data) is left out
        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = '2024-06-03 23:00:00' WHERE id = 2");
        assertTrue(queries.turnaround(JUNE_3, JUNE_4, "section", 2).isEmpty());
    }

    @Test
    public void theCatalogListsTests() {
        List<AddonQueryDAO.TestRow> tests = queries.tests(null, null);
        assertTrue(tests.stream().anyMatch(t -> "CBC".equals(t.code()) && "123456".equals(t.loinc())
                && "TB".equals(t.section()) && "mg/dL".equals(t.unit())));
        assertTrue(queries.tests("TB", null).stream().noneMatch(t -> "UA".equals(t.code())));
    }

    @Test
    public void theAccessLogKeepsEachRequestAndForgetsOldOnes() {
        accessLog.insert(new AddonAccessLogDAO.Entry(Instant.now(), "mcp", "service", null, "agent:a1 Claude",
                "get_order", "GET", "/orders/{accession}", "order", "12345", 200, null));
        accessLog.insert(new AddonAccessLogDAO.Entry(Instant.now().minusSeconds(400L * 86400), "mcp", "service", null,
                null, null, "GET", "/patients", "query", "patients Nguyen", 403, 0));
        assertEquals(2, accessLog.find("mcp", null, null, null, null, 10).size());
        assertEquals(1, accessLog.find("mcp", null, null, "agent:a1 Claude", null, 10).size());
        assertEquals("get_order", accessLog.find("mcp", null, null, null, "12345", 10).get(0).purpose());
        assertEquals(0, accessLog.find("other", null, null, null, null, 10).size());
        assertEquals(1, accessLog.purgeOlderThan(180));
        assertEquals(1, accessLog.find("mcp", null, null, null, null, 10).size());
    }

    private static List<String> accessions(AddonQueryDAO.Page<AddonQueryDAO.OrderRow> page) {
        return page.rows().stream().map(AddonQueryDAO.OrderRow::accessionNumber).toList();
    }
}
