package org.openelisglobal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Locks the base-test invariant:
 * {@link BaseWebContextSensitiveTest#executeDataSetWithStateManagement} must
 * NOT truncate or replace static-seed tables (currently
 * {@code reference_tables}) even when a fixture XML declares rows in those
 * tables.
 *
 * <p>
 * Background. Pre-fix, the DBUnit loader called {@code TRUNCATE ...
 * RESTART IDENTITY CASCADE} on every table named in the fixture before
 * REFRESHing the rows. {@code reference_tables} is populated by Liquibase at DB
 * init with ~246 rows that every audit-emitting service (post PR #3591) looks
 * up by name in {@code AuditTrailServiceImpl.saveNewHistory}. When a fixture
 * incidentally declared a {@code <reference_tables>} row (10+ fixtures had
 * this), the truncate wiped the seed, leaving every downstream audit-emitting
 * test surefire-order-dependently red with "Reference Table is null".
 *
 * <p>
 * This test loads a fixture that ASSERTIVELY declares a rogue
 * {@code <reference_tables>} row and verifies the SQL seed survives. Regression
 * of the filter would make this test go red.
 */
public class FixtureLoaderProtectedSeedsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private DataSource dataSource;

    /**
     * Seed tables that should survive any fixture load. Mirrors the constant on
     * {@link BaseWebContextSensitiveTest} but is duplicated here so a future change
     * that drops a name from PROTECTED_SEED_TABLES still gets caught by an explicit
     * assertion.
     */
    private static final String[] CANARY_SEED_NAMES = { "PATIENT", "PERSON", "DICTIONARY", "SAMPLE", "ANALYSIS",
            "site_information", "BARCODE_LABEL_INFO" };

    @Test
    public void loadingFixtureWithRogueReferenceTablesRow_doesNotWipeSeed() throws Exception {
        int seedSizeBefore = countReferenceTables();
        assertTrue("seed should be populated before test (got " + seedSizeBefore + ")", seedSizeBefore >= 50);

        for (String name : CANARY_SEED_NAMES) {
            ReferenceTables row = referenceTablesService.getReferenceTableByName(name);
            assertNotNull("Canary seed row missing before fixture load: " + name, row);
        }

        // Load any existing fixture that declares <reference_tables> rows. Use
        // result.xml because it's a small fixture with id=9999 (no PK conflict
        // with seed) so the test focuses purely on the truncate-survival
        // invariant, not on REFRESH side-effects.
        executeDataSetWithStateManagement("testdata/result.xml");

        for (String name : CANARY_SEED_NAMES) {
            ReferenceTables row = referenceTablesService.getReferenceTableByName(name);
            assertNotNull("Canary seed row wiped by fixture load: " + name, row);
        }

        int seedSizeAfter = countReferenceTables();
        assertTrue("Seed shrank from " + seedSizeBefore + " to " + seedSizeAfter + " — fixture loader truncated "
                + "reference_tables despite the PROTECTED_SEED_TABLES filter. Did someone drop reference_tables "
                + "from BaseWebContextSensitiveTest.PROTECTED_SEED_TABLES?", seedSizeAfter >= seedSizeBefore);
    }

    private int countReferenceTables() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM clinlims.reference_tables");
                ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
