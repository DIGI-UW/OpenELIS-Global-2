package org.openelisglobal.testcatalog.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

/**
 * Losslessness test for the OGC-937 result-component backfill (OGC-949 M1,
 * changeset 041-result-components.xml).
 *
 * The Liquibase changelog runs against an empty Testcontainers DB at context
 * startup, so the changeset's backfill sees no rows. To test the backfill
 * against legacy-shaped data, this test seeds tests/TEST_RESULT/RESULT_LIMITS
 * rows with NULL component_id and then executes the SAME SQL the changeset runs
 * (041-component-backfill.sql via sqlFile — single source of truth).
 *
 * Invariants asserted (spec FR-003 / US1): - row counts of TEST, TEST_RESULT,
 * RESULT_LIMITS are unchanged by the backfill; - exactly one PRIMARY
 * test_result_component per test; - every seeded TEST_RESULT and RESULT_LIMITS
 * row is repointed to its test's PRIMARY component; - the PRIMARY component
 * copies the legacy per-test shape (TEST.UOM_ID, the test's TEST_RESULT type /
 * significant digits); - the backfill is idempotent (second run changes
 * nothing).
 */
public class ComponentBackfillMigrationTest extends BaseWebContextSensitiveTest {

    private static final String BACKFILL_SQL_PATH = "liquibase/3.5.x.x/041-component-backfill.sql";

    // High ids to avoid colliding with seed/fixture data; cleaned up in @After.
    private static final long NUMERIC_TEST_ID = 94001L;
    private static final long DICT_TEST_ID = 94002L;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private long uomId;
    private long testResultTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanSeededData();

        uomId = jdbcTemplate.queryForObject("SELECT min(id) FROM clinlims.unit_of_measure", Long.class);
        testResultTypeId = jdbcTemplate.queryForObject("SELECT min(id) FROM clinlims.type_of_test_result", Long.class);

        // Legacy-shaped numeric test: one TEST_RESULT row (type N, sig digits), two
        // range rows.
        insertTest(NUMERIC_TEST_ID, "BackfillNumeric", uomId);
        insertTestResult(948001L, NUMERIC_TEST_ID, "N", null, 2L);
        insertResultLimit(948101L, NUMERIC_TEST_ID, 0.0, 5.0);
        insertResultLimit(948102L, NUMERIC_TEST_ID, 5.0, 120.0);

        // Legacy-shaped dictionary test: two select-list option rows, no ranges.
        insertTest(DICT_TEST_ID, "BackfillDict", null);
        insertTestResult(948002L, DICT_TEST_ID, "D", "1001", null);
        insertTestResult(948003L, DICT_TEST_ID, "D", "1002", null);
    }

    @After
    public void tearDown() {
        cleanSeededData();
    }

    @Test
    public void backfill_isLosslessAndRepointsAllRowsToPrimary() throws Exception {
        long testCountBefore = count("clinlims.test");
        long testResultCountBefore = count("clinlims.test_result");
        long resultLimitCountBefore = count("clinlims.result_limits");

        runBackfill();

        // Losslessness: no rows created or destroyed in the legacy tables.
        assertEquals("TEST row count must be unchanged", testCountBefore, count("clinlims.test"));
        assertEquals("TEST_RESULT row count must be unchanged", testResultCountBefore, count("clinlims.test_result"));
        assertEquals("RESULT_LIMITS row count must be unchanged", resultLimitCountBefore,
                count("clinlims.result_limits"));

        // Exactly one PRIMARY component per seeded test.
        for (long testId : new long[] { NUMERIC_TEST_ID, DICT_TEST_ID }) {
            assertEquals("exactly one PRIMARY component for test " + testId, Long.valueOf(1L),
                    jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM clinlims.test_result_component WHERE test_id = ? AND code = 'PRIMARY'",
                            Long.class, testId));
        }

        // Every seeded TEST_RESULT / RESULT_LIMITS row repointed to its test's PRIMARY.
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.test_result tr WHERE tr.test_id IN (?, ?) AND (tr.component_id IS NULL"
                        + " OR tr.component_id <> (SELECT c.id FROM clinlims.test_result_component c"
                        + " WHERE c.test_id = tr.test_id AND c.code = 'PRIMARY'))",
                Long.class, NUMERIC_TEST_ID, DICT_TEST_ID));
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.result_limits rl WHERE rl.test_id = ? AND (rl.component_id IS NULL"
                        + " OR rl.component_id <> (SELECT c.id FROM clinlims.test_result_component c"
                        + " WHERE c.test_id = rl.test_id AND c.code = 'PRIMARY'))",
                Long.class, NUMERIC_TEST_ID));

        // PRIMARY copies the legacy shape: uom from TEST.UOM_ID, type + sig digits from
        // TEST_RESULT.
        List<java.util.Map<String, Object>> primary = jdbcTemplate
                .queryForList("SELECT uom_id, result_type, significant_digits FROM clinlims.test_result_component"
                        + " WHERE test_id = ? AND code = 'PRIMARY'", NUMERIC_TEST_ID);
        assertEquals(1, primary.size());
        assertNotNull("PRIMARY must copy TEST.UOM_ID", primary.get(0).get("uom_id"));
        assertEquals("N", primary.get(0).get("result_type"));
        assertEquals(2L, ((Number) primary.get(0).get("significant_digits")).longValue());

        java.util.Map<String, Object> dictPrimary = jdbcTemplate.queryForMap(
                "SELECT result_type, uom_id FROM clinlims.test_result_component WHERE test_id = ? AND code = 'PRIMARY'",
                DICT_TEST_ID);
        assertEquals("D", dictPrimary.get("result_type"));

        // Idempotency: running the backfill again changes nothing.
        long componentCount = count("clinlims.test_result_component");
        runBackfill();
        assertEquals("backfill must be idempotent (component count stable)", componentCount,
                count("clinlims.test_result_component"));
        assertEquals("backfill must be idempotent (TEST_RESULT count stable)", testResultCountBefore,
                count("clinlims.test_result"));
    }

    private void runBackfill() throws IOException {
        String sql = FileCopyUtils.copyToString(new java.io.InputStreamReader(
                new ClassPathResource(BACKFILL_SQL_PATH).getInputStream(), StandardCharsets.UTF_8));
        assertTrue("backfill SQL must not be empty", sql.trim().length() > 0);
        jdbcTemplate.execute(sql);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private void insertTest(long id, String name, Long uom) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, uom_id, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, ?, NOW())",
                id, name, name + " (component backfill test)", UUID.randomUUID().toString(), uom);
    }

    private void insertTestResult(long id, long testId, String type, String value, Long significantDigits) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, significant_digits, lastupdated)"
                        + " VALUES (?, ?, ?, ?, ?, NOW())",
                id, testId, type, value, significantDigits);
    }

    private void insertResultLimit(long id, long testId, double minAge, double maxAge) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age, lastupdated)"
                        + " VALUES (?, ?, ?, ?, ?, NOW())",
                id, testId, testResultTypeId, minAge, maxAge);
    }

    private void cleanSeededData() {
        jdbcTemplate.execute("DELETE FROM clinlims.result_limits WHERE test_id IN (94001, 94002)");
        jdbcTemplate.execute("DELETE FROM clinlims.test_result WHERE test_id IN (94001, 94002)");
        try {
            jdbcTemplate.execute("DELETE FROM clinlims.test_result_component WHERE test_id IN (94001, 94002)");
        } catch (Exception ignored) {
            // table does not exist until changeset 041 lands (TDD red phase)
        }
        jdbcTemplate.execute("DELETE FROM clinlims.test WHERE id IN (94001, 94002)");
    }
}
