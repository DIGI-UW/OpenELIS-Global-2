package org.openelisglobal.resultlimit;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 M7 / OGC-937 — ORM validation for the
 * {@code ResultLimit.component_id} mapping (the M1 column, now mapped on the
 * legacy entity). Reference ranges are scoped to a result component; this
 * proves the new property round-trips and the component-scoped query works,
 * against a real Testcontainers Postgres.
 */
public class ResultLimitComponentIdIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95301L;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String componentId;
    private String resultTypeId;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "RangeORM", "range ORM test", UUID.randomUUID().toString());
        resultTypeId = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_test_result", String.class);

        TestResultComponent c = new TestResultComponent();
        c.setTestId(String.valueOf(TEST_ID));
        c.setCode("PRIMARY");
        c.setLabel("Primary");
        c.setDisplayOrder(0);
        c.setSysUserId("1");
        componentId = componentService.insert(c);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // FK order: result_limits (-> component, -> test) before component before test.
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
        try {
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // table absent before changeset 041
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void componentScopedRange_persistsAndReadsBack() {
        ResultLimit limit = new ResultLimit();
        limit.setTestId(String.valueOf(TEST_ID));
        limit.setResultTypeId(resultTypeId);
        limit.setComponentId(componentId);
        limit.setGender("M");
        limit.setMinAge(0d);
        limit.setMaxAge(5d);
        limit.setLowNormal(10d);
        limit.setHighNormal(20d);
        limit.setSysUserId("1");
        resultLimitService.insert(limit);

        List<ResultLimit> byComponent = resultLimitService.getResultLimitsByComponentId(componentId);
        assertEquals(1, byComponent.size());
        ResultLimit loaded = byComponent.get(0);
        assertEquals(componentId, loaded.getComponentId());
        assertEquals("M", loaded.getGender());
        assertEquals(0d, loaded.getMinAge(), 0.0001);
        assertEquals(5d, loaded.getMaxAge(), 0.0001);
        assertEquals(10d, loaded.getLowNormal(), 0.0001);
        assertEquals(20d, loaded.getHighNormal(), 0.0001);
    }
}
