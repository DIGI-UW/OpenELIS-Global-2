package org.openelisglobal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixture replacement belongs to the test transaction, including cascading
 * deletes.
 */
@Transactional
public class FixtureTransactionIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private LocalizationService localizations;
    private Map<String, Object> originalTranslations;
    private Map<String, Object> translationsWithProbe;
    private String probeDescription;

    @BeforeTransaction
    public void snapshotOriginalTranslations() {
        originalTranslations = translationSnapshot();
        // An owned sentinel makes this test meaningful even after a legacy suite
        // has already replaced some of the original seed rows.
        probeDescription = "Fixture rollback " + UUID.randomUUID();
        Localization probe = new Localization();
        probe.setDescription(probeDescription);
        probe.setLocalizedValue("en", probeDescription);
        probe.setSysUserId(TEST_SYS_USER_ID);
        localizations.insert(probe);
        translationsWithProbe = translationSnapshot();
    }

    @Test
    public void fixtureIsVisibleToRealServicesWithoutCommittingOverTheOriginalRecords() throws Exception {
        executeDataSetWithStateManagement("testdata/localization.xml");
        assertEquals("Hello", localizations.get("1").getEnglish());
        assertEquals("Bonjour", localizations.get("1").getFrench());
        assertEquals(Integer.valueOf(3),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM localization", Integer.class));
    }

    @Test
    public void cleanupAndSubsequentServiceWritesShareTheFixtureTransaction() throws Exception {
        executeDataSetWithStateManagement("testdata/localization.xml");
        cleanRowsInCurrentConnection(new String[] { "localization" });
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM localization", Integer.class));

        Localization replacement = new Localization();
        replacement.setDescription("Replacement owned by the rollback transaction");
        replacement.setEnglish("Replacement");
        replacement.setSysUserId(TEST_SYS_USER_ID);
        String id = localizations.insert(replacement);
        assertEquals("Replacement", localizations.get(id).getEnglish());
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM localization", Integer.class));
        // The common @AfterTransaction assertion verifies that cleanup and the
        // replacement both rolled back, preserving the pre-test records exactly.
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void standaloneCleanupCommitsItsOwnTransaction() throws Exception {
        String table = createOwnedCleanupTable();
        try {
            cleanRowsInCurrentConnection(new String[] { table });
            // No test transaction: this independent read observes the commit.
            assertEquals(Integer.valueOf(0),
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        } finally {
            jdbcTemplate.execute("DROP TABLE " + table);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void standaloneCleanupRollsBackAllTablesIfAnyCleanupFails() throws Exception {
        String table = createOwnedCleanupTable();
        try {
            try {
                cleanRowsInCurrentConnection(new String[] { table, table + "_missing" });
                fail("The absent second table must fail cleanup");
            } catch (SQLException expected) {
                assertEquals("42P01", expected.getSQLState());
            }
            assertEquals("The successful first cleanup must also roll back", Integer.valueOf(3),
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        } finally {
            jdbcTemplate.execute("DROP TABLE " + table);
        }
    }

    private String createOwnedCleanupTable() {
        // Own the entire table so committed cleanup never destroys application seeds
        // or another suite's data. PostgreSQL DDL here belongs only to the test DB.
        String table = "fixture_cleanup_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE TABLE " + table + " (id INTEGER PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO " + table + " (id) VALUES (1), (2), (3)");
        return table;
    }

    @AfterTransaction
    public void originalTranslationsSurviveRollback() {
        try {
            assertEquals("Fixture replacement must roll back with the test, including dependent translations",
                    translationsWithProbe, translationSnapshot());
        } finally {
            localizations.getAllMatching("description", probeDescription).forEach(row -> {
                row.setSysUserId(TEST_SYS_USER_ID);
                localizations.delete(row);
            });
        }
        assertEquals("Remove only the sentinel owned by this check", originalTranslations, translationSnapshot());
    }

    private Map<String, Object> translationSnapshot() {
        return jdbcTemplate.queryForMap("SELECT " + "(SELECT COUNT(*) FROM localization) AS localizations, "
                + "(SELECT md5(string_agg(id::text || ':' || locale || ':' || value, '|' ORDER BY id)) "
                + "FROM localization_value) AS translations, " + "(SELECT COUNT(*) FROM test_section) AS lab_units, "
                + "(SELECT md5(string_agg(id::text || ':' || value, '|' ORDER BY id)) "
                + "FROM site_information WHERE tag = 'localization') AS configuration_references");
    }
}
