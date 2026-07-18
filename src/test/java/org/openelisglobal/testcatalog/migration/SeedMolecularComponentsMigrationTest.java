package org.openelisglobal.testcatalog.migration;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Idempotency + targeting test for the OGC-1125 molecular-component seed
 * (changeset OGC-1125-seed-molecular-components in
 * 055-seed-molecular-components.xml).
 *
 * The seed adds the SARS-CoV-2 target components (N2, E) to the universal
 * COVIDPCR(Respiratory Swab) test, keyed by its unique test.description (LOINC
 * 94500-6 is reused across many tests, so it would over-match). This test
 * creates that SARS test plus an unrelated test that SHARES LOINC 94500-6 but
 * not the description, then runs the same INSERT the changeset runs (kept in
 * sync with its inline SQL) and asserts:
 *
 * - N2/E land on the description-matched SARS test only; - the test that merely
 * shares LOINC 94500-6 gets nothing (no over-match); - seeded components are
 * non-primary with non-null codes; - a second run adds nothing (idempotent).
 */
public class SeedMolecularComponentsMigrationTest extends BaseWebContextSensitiveTest {

    private static final String SARS_DESC = "COVIDPCR(Respiratory Swab)";
    private static final String SARS_LOINC = "94500-6";
    private static final String UNRELATED_DESC = "ZZZ Molecular Seed Unrelated (test)";

    private static final String SEEDED_CODES = "'N2','E'";

    // Kept in sync with the inline SQL in changeset
    // OGC-1125-seed-molecular-components.
    private static final String SEED_SQL = "INSERT INTO clinlims.test_result_component"
            + " (id, test_id, code, label, display_order, result_type, uom_id, is_active, lastupdated, last_updated)"
            + " SELECT gen_random_uuid()::varchar, t.id, v.code, v.label, v.ord, 'N',"
            + " (SELECT id FROM clinlims.unit_of_measure WHERE name = 'Ct' LIMIT 1), 'Y', now(), now()"
            + " FROM clinlims.test t CROSS JOIN (VALUES"
            + " ('N2','N2 (Ct)',1),('E','E (Ct)',2)) AS v(code, label, ord)"
            + " WHERE t.description = 'COVIDPCR(Respiratory Swab)' AND NOT EXISTS (SELECT 1"
            + " FROM clinlims.test_result_component c WHERE c.test_id = t.id AND c.code = v.code);"
            + " INSERT INTO clinlims.test_result"
            + " (id, test_id, tst_rslt_type, sort_order, is_active, component_id, lastupdated)"
            + " SELECT nextval('clinlims.test_result_seq'), c.test_id, 'N', c.display_order + 1, true, c.id, now()"
            + " FROM clinlims.test_result_component c WHERE c.code IN ('N2','E')"
            + " AND c.test_id IN (SELECT id FROM clinlims.test WHERE description = 'COVIDPCR(Respiratory Swab)')"
            + " AND NOT EXISTS (SELECT 1 FROM clinlims.test_result tr"
            + " WHERE tr.component_id = c.id AND tr.is_active = true);";

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private final Set<Long> createdTestIds = new HashSet<>();
    private long nextTestId = 94011L;
    private long sarsTestId;
    private long unrelatedTestId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        createdTestIds.clear();
        // The seeded codes (N2/E) are unique to this seed, so it is safe to clear
        // every such row up front — including any the startup migration created.
        deleteAllSeededComponents();
        jdbcTemplate.update("DELETE FROM clinlims.test WHERE description = ?", UNRELATED_DESC);

        // The seed references a "Ct" unit; ensure one exists.
        Long ctCount = jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.unit_of_measure WHERE name = 'Ct'",
                Long.class);
        if (ctCount == 0) {
            jdbcTemplate.update("INSERT INTO clinlims.unit_of_measure (id, name, description, lastupdated)"
                    + " VALUES (nextval('clinlims.unit_of_measure_seq'), 'Ct', 'Cycle threshold', NOW())");
        }

        // The universal SARS test matched by description, and an unrelated test that
        // shares the SARS LOINC 94500-6 but not the description.
        sarsTestId = ensureTest(SARS_DESC, SARS_LOINC);
        unrelatedTestId = insertTest(UNRELATED_DESC, SARS_LOINC);
    }

    @After
    public void tearDown() {
        deleteAllSeededComponents();
        for (Long id : createdTestIds) {
            jdbcTemplate.update("DELETE FROM clinlims.test WHERE id = ?", id);
        }
    }

    @Test
    public void seed_addsSarsComponentsByDescriptionOnly_andIsIdempotent() {
        runSeed();

        assertEquals("N2 + E seeded on COVIDPCR(Respiratory Swab)", Long.valueOf(2L), seededComponentCount(sarsTestId));
        assertEquals("a test that only shares SARS LOINC 94500-6 gets nothing (no over-match)", Long.valueOf(0L),
                seededComponentCount(unrelatedTestId));

        // Each numeric component must get its own test_result row (component_id-linked)
        // so result entry binds one Result per component instead of falling back to the
        // primary — the root cause of the duplicate-row bug.
        assertEquals("each numeric component has its own test_result row", Long.valueOf(2L),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.test_result tr"
                                + " JOIN clinlims.test_result_component c ON c.id = tr.component_id"
                                + " WHERE c.test_id = ? AND c.code IN (" + SEEDED_CODES + ") AND tr.is_active = true",
                        Long.class, sarsTestId));

        assertEquals("seeded components are non-primary", Long.valueOf(0L),
                jdbcTemplate
                        .queryForObject(
                                "SELECT count(*) FROM clinlims.test_result_component"
                                        + " WHERE test_id = ? AND code IN (" + SEEDED_CODES + ") AND is_primary = true",
                                Long.class, sarsTestId));
        assertEquals("no seeded component has a null/blank code", Long.valueOf(0L),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.test_result_component"
                                + " WHERE test_id = ? AND (code IS NULL OR length(trim(code)) = 0)",
                        Long.class, sarsTestId));
        // Regression guard: last_updated is Hibernate's @Version. A NULL here makes a
        // later edit fail with StaleStateException (0 rows updated), so the seed must
        // populate it.
        assertEquals("seeded components must have a non-null @Version (last_updated)", Long.valueOf(0L),
                jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.test_result_component"
                        + " WHERE test_id = ? AND code IN (" + SEEDED_CODES + ") AND last_updated IS NULL", Long.class,
                        sarsTestId));

        // Idempotency: a second run adds nothing.
        long before = totalSeededComponents();
        runSeed();
        assertEquals("seed must be idempotent (seeded component count stable)", before, totalSeededComponents());
    }

    private void runSeed() {
        for (String statement : SEED_SQL.split(";")) {
            if (!statement.trim().isEmpty()) {
                jdbcTemplate.execute(statement);
            }
        }
    }

    private long ensureTest(String description, String loinc) {
        Long existing = jdbcTemplate.query("SELECT id FROM clinlims.test WHERE description = ?",
                rs -> rs.next() ? rs.getLong(1) : null, description);
        return existing != null ? existing : insertTest(description, loinc);
    }

    private long insertTest(String description, String loinc) {
        long id = nextTestId++;
        jdbcTemplate.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, loinc, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, ?, NOW())",
                id, description, description, UUID.randomUUID().toString(), loinc);
        createdTestIds.add(id);
        return id;
    }

    private Long seededComponentCount(long testId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.test_result_component"
                + " WHERE test_id = ? AND code IN (" + SEEDED_CODES + ")", Long.class, testId);
    }

    private long totalSeededComponents() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.test_result_component"
                + " WHERE test_id IN (?, ?) AND code IN (" + SEEDED_CODES + ")", Long.class, sarsTestId,
                unrelatedTestId);
    }

    private void deleteAllSeededComponents() {
        // Delete the per-component test_result rows first (they FK to the component).
        jdbcTemplate.update("DELETE FROM clinlims.test_result WHERE component_id IN"
                + " (SELECT id FROM clinlims.test_result_component WHERE code IN (" + SEEDED_CODES + "))");
        jdbcTemplate.update("DELETE FROM clinlims.test_result_component WHERE code IN (" + SEEDED_CODES + ")");
    }
}
