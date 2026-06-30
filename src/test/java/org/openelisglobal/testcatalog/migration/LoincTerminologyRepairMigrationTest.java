package org.openelisglobal.testcatalog.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Repair test for the OGC-957 legacy-LOINC migration (changeset
 * 047-fix-loinc-terminology.xml).
 *
 * The 043 backfill copied test.loinc into test_terminology_mapping but with
 * relationship 'EQUIVALENT' (not an app relationship value, so the editor shows
 * the raw i18n key) and a NULL last_updated @Version (first edit would throw
 * StaleStateException). This test seeds rows mimicking that output and runs the
 * same repair statements the changeset runs.
 *
 * Invariants: 'EQUIVALENT' becomes 'SAME_AS'; legitimate user relationships are
 * untouched; every NULL @Version is seeded from lastupdated; the repair is
 * idempotent.
 */
public class LoincTerminologyRepairMigrationTest extends BaseWebContextSensitiveTest {

    private static final String FIX_RELATIONSHIP_SQL = "UPDATE clinlims.test_terminology_mapping"
            + " SET relationship = 'SAME_AS' WHERE relationship = 'EQUIVALENT'";

    private static final String FIX_VERSION_SQL = "UPDATE clinlims.test_terminology_mapping"
            + " SET last_updated = lastupdated WHERE last_updated IS NULL";

    private static final long BACKFILLED = 94101L;
    private static final long USER_ROW = 94102L;
    private static final long NON_LOINC = 94103L;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanSeededData();

        insertTest(BACKFILLED, "RepairBackfilled");
        insertTest(USER_ROW, "RepairUserRow");
        insertTest(NON_LOINC, "RepairNonLoinc");

        // Mimics the 043 backfill: EQUIVALENT relationship, NULL @Version.
        insertMapping(BACKFILLED, "LOINC", "1558-6", "EQUIVALENT", false);
        // A legitimate user-entered mapping: SAME_AS with a real @Version, must stay.
        insertMapping(USER_ROW, "LOINC", "4548-4", "SAME_AS", true);
        // A non-LOINC mapping with a NULL @Version: relationship kept, version
        // repaired.
        insertMapping(NON_LOINC, "SNOMED", "271649006", "BROADER_THAN", false);
    }

    @After
    public void tearDown() {
        cleanSeededData();
    }

    @Test
    public void repair_remapsEquivalentToSameAs_andSeedsNullVersion_idempotently() {
        runRepair();

        assertEquals("EQUIVALENT is re-mapped to SAME_AS", "SAME_AS", relationship(BACKFILLED));
        assertNotNull("backfilled row gets a non-null @Version", version(BACKFILLED));

        assertEquals("user SAME_AS relationship is untouched", "SAME_AS", relationship(USER_ROW));

        assertEquals("non-LOINC relationship is untouched", "BROADER_THAN", relationship(NON_LOINC));
        assertNotNull("non-LOINC NULL @Version is repaired", version(NON_LOINC));

        long equivalents = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_terminology_mapping WHERE relationship = 'EQUIVALENT'", Long.class);
        assertEquals("no EQUIVALENT rows remain", 0L, equivalents);

        long nullVersions = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_terminology_mapping WHERE test_id IN (94101, 94102, 94103)"
                        + " AND last_updated IS NULL",
                Long.class);
        assertEquals("no NULL @Version rows remain", 0L, nullVersions);

        // Idempotent: a second run changes nothing.
        runRepair();
        assertEquals("SAME_AS", relationship(BACKFILLED));
        assertEquals("BROADER_THAN", relationship(NON_LOINC));
    }

    private void runRepair() {
        jdbc.execute(FIX_RELATIONSHIP_SQL);
        jdbc.execute(FIX_VERSION_SQL);
    }

    private String relationship(long testId) {
        return jdbc.queryForObject("SELECT relationship FROM clinlims.test_terminology_mapping WHERE test_id = ?",
                String.class, testId);
    }

    private Timestamp version(long testId) {
        return jdbc.queryForObject("SELECT last_updated FROM clinlims.test_terminology_mapping WHERE test_id = ?",
                Timestamp.class, testId);
    }

    private void insertTest(long id, String name) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                id, name, name + " (loinc repair test)", UUID.randomUUID().toString());
    }

    private void insertMapping(long testId, String source, String code, String relationship, boolean withVersion) {
        if (withVersion) {
            jdbc.update(
                    "INSERT INTO clinlims.test_terminology_mapping"
                            + " (id, test_id, source, code, relationship, is_active, lastupdated, last_updated)"
                            + " VALUES (?, ?, ?, ?, ?, 'Y', NOW(), NOW())",
                    UUID.randomUUID().toString(), testId, source, code, relationship);
        } else {
            // last_updated left NULL — mirrors the raw-SQL 043 backfill.
            jdbc.update(
                    "INSERT INTO clinlims.test_terminology_mapping"
                            + " (id, test_id, source, code, relationship, is_active, lastupdated)"
                            + " VALUES (?, ?, ?, ?, ?, 'Y', NOW())",
                    UUID.randomUUID().toString(), testId, source, code, relationship);
        }
    }

    private void cleanSeededData() {
        jdbc.execute("DELETE FROM clinlims.test_terminology_mapping WHERE test_id IN (94101, 94102, 94103)");
        jdbc.execute("DELETE FROM clinlims.test WHERE id IN (94101, 94102, 94103)");
    }
}
