package org.openelisglobal.testsamplehandling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingHistoryService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 / OGC-766 — audit writes to test_sample_handling_history on storage
 * changes: insert logs a row, a real field change logs another, and a no-change
 * re-save is skipped.
 */
public class TestSampleHandlingAuditIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95441L;

    @Autowired
    private TestSampleHandlingService handlingService;

    @Autowired
    private TestSampleHandlingHistoryService historyService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AuditIT", "AuditIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_sample_handling_history WHERE test_sample_handling_id IN"
                + " (SELECT id FROM clinlims.test_sample_handling WHERE test_id = ?)", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_sample_handling WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    private TestSampleHandling config(String condition, Integer duration) {
        TestSampleHandling h = new TestSampleHandling();
        h.setStorageCondition(condition);
        h.setStorageDuration(duration);
        return h;
    }

    private String handlingId() {
        return handlingService.getByTestId(String.valueOf(TEST_ID)).getId();
    }

    @Test
    public void insertThenChange_logsTwoRows_noChangeSkips() {
        // 1) first save → INSERT audit row
        handlingService.saveForTest(String.valueOf(TEST_ID), config("REFRIGERATED", 7), "1");
        List<TestSampleHandlingHistory> afterInsert = historyService.getByHandlingId(handlingId());
        assertEquals(1, afterInsert.size());
        assertEquals("INSERT", afterInsert.get(0).getChangeType());

        // 2) change a field → UPDATE audit row (now 2, newest first)
        handlingService.saveForTest(String.valueOf(TEST_ID), config("FROZEN", 7), "1");
        List<TestSampleHandlingHistory> afterUpdate = historyService.getByHandlingId(handlingId());
        assertEquals(2, afterUpdate.size());
        assertEquals("UPDATE", afterUpdate.get(0).getChangeType());
        assertTrue(afterUpdate.get(0).getNewValues().contains("FROZEN"));
        assertTrue(afterUpdate.get(0).getPreviousValues().contains("REFRIGERATED"));

        // 3) re-save identical state → no new audit row
        handlingService.saveForTest(String.valueOf(TEST_ID), config("FROZEN", 7), "1");
        assertEquals(2, historyService.getByHandlingId(handlingId()).size());
    }
}
