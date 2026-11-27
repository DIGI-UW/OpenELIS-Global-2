package org.openelisglobal;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.Test;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/** Remaining legacy fixture helpers must respect their caller's transaction. */
@Transactional
public class FixtureHelperTransactionIntegrationTest extends BaseWebContextSensitiveTest {
    private String referenceName;
    private String configurationName;

    @BeforeTransaction
    public void assignOwnedNames() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        referenceName = "fxref-" + suffix;
        configurationName = "fxcfg-" + suffix;
    }

    @Test
    public void helperRecordsAreVisibleAndIdempotentWithinTheTestTransaction() {
        String id = ensureReferenceTable(referenceName);
        assertEquals(id, ensureReferenceTable(referenceName));
        assertEquals(referenceName, jdbcTemplate.queryForObject("SELECT name FROM reference_tables WHERE id = ?",
                String.class, Integer.valueOf(id)));

        ensureSiteInformation(configurationName, "original");
        ensureSiteInformation(configurationName, "must not overwrite");
        assertEquals("original", jdbcTemplate.queryForObject("SELECT value FROM site_information WHERE name = ?",
                String.class, configurationName));
    }

    @Test
    public void explicitSequenceSynchronizationSeesUncommittedRowsAndNeverReusesIds() {
        String table = "fixture_sequence_" + UUID.randomUUID().toString().replace("-", "");
        String sequence = table + "_seq";
        // PostgreSQL rolls this owned schema back with the test. A separate
        // connection cannot see it, making the old helper fail without a lock wait.
        jdbcTemplate.execute("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY)");
        jdbcTemplate.execute("CREATE SEQUENCE " + sequence);
        jdbcTemplate.update("INSERT INTO " + table + " (id) VALUES (42)");
        resyncSequence(sequence, table);
        assertEquals(Long.valueOf(43), jdbcTemplate.queryForObject("SELECT nextval('" + sequence + "')", Long.class));
        jdbcTemplate.update("DELETE FROM " + table);
        resyncSequence(sequence, table);
        assertEquals(Long.valueOf(44), jdbcTemplate.queryForObject("SELECT nextval('" + sequence + "')", Long.class));
    }

    @AfterTransaction
    public void helperRecordsDoNotEscapeRollback() {
        try {
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reference_tables WHERE name = ?", Integer.class, referenceName));
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM site_information WHERE name = ?", Integer.class, configurationName));
        } finally {
            // Own these exact names; also clean the red regression's leaked rows.
            jdbcTemplate.update("DELETE FROM reference_tables WHERE name = ?", referenceName);
            jdbcTemplate.update("DELETE FROM site_information WHERE name = ?", configurationName);
        }
    }
}
