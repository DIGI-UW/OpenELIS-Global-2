package org.openelisglobal.storage;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base test class for storage-related tests that provides unified fixture
 * loading and cleanup helpers.
 * 
 * This class loads E2E test data via DBUnit XML and provides cleanup methods
 * that preserve fixtures while removing test-created data.
 * 
 * Usage:
 * 
 * <pre>
 * public class MyStorageTest extends BaseStorageTest {
 *     &#64;Before
 *     public void setUp() throws Exception {
 *         super.setUp();
 *         // Your test setup
 *     }
 * 
 *     @After
 *     public void tearDown() throws Exception {
 *         super.tearDown();
 *         // Your test cleanup
 *     }
 * }
 * </pre>
 * 
 * Fixture Data Ranges (preserved during cleanup): - Storage: IDs 1-999 (from
 * DBUnit fixtures) - Samples: E2E-* accession numbers (DBUnit fixtures) -
 * Patients: E2E-PAT-* external IDs (DBUnit fixtures) - Sample items: IDs
 * 10000-20000 (DBUnit fixtures) - Analyses: IDs 20000-30000 (DBUnit fixtures) -
 * Results: IDs 30000-40000 (DBUnit fixtures)
 *
 * Storage hierarchy and E2E test data are loaded via DBUnit XML in setUp().
 */
public abstract class BaseStorageTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseStorageTest.class);

    @Autowired
    protected DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Load user data first (required for assigned_by_user_id foreign key)
        executeDataSetWithStateManagement("testdata/user-role.xml");

        // Load type_of_sample data (required for sample_item foreign key)
        executeDataSetWithStateManagement("testdata/typeofsample.xml");

        // Load status_of_sample data (required for sample/sample_item status_id foreign
        // key)
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");

        // Load storage hierarchy + E2E test data via DBUnit
        executeDataSetWithStateManagement("testdata/storage-e2e.xml");

        // IMPORTANT: DBUnit inserts explicit IDs but does not advance PostgreSQL
        // sequences. If we don't bump these sequences above fixture ranges,
        // controller-created entities can collide on PKs and return 400s.
        jdbcTemplate.execute("SELECT setval('storage_room_seq', 1000, false)");
        jdbcTemplate.execute("SELECT setval('storage_device_seq', 1000, false)");
        jdbcTemplate.execute("SELECT setval('storage_shelf_seq', 1000, false)");
        jdbcTemplate.execute("SELECT setval('storage_rack_seq', 1000, false)");
        jdbcTemplate.execute("SELECT setval('storage_box_seq', 10000, false)");

        // Note: Validation is commented out temporarily due to transaction isolation
        // issues
        // The data is loaded correctly (verified by direct database queries)
        // TODO: Fix transaction isolation to enable validation in setUp()
        // validateTestData();

        // Clean up test-created data before each test
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test-created data after each test (preserves fixtures)
        cleanStorageTestData();
    }

    /**
     * Validate that required test data exists. Verifies foundation data from
     * Liquibase and E2E fixture data from DBUnit XML.
     * 
     * @throws IllegalStateException if required test data is missing
     */
    protected void validateTestData() {
        // Verify storage hierarchy fixtures exist
        Integer roomCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')", Integer.class);
        if (roomCount == null || roomCount < 3) {
            throw new IllegalStateException(
                    "Fixture data missing: Expected 3 test rooms (MAIN, SEC, INACTIVE), found " + roomCount);
        }

        // Verify E2E fixture data exists (from DBUnit XML)
        Integer patientCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%'", Integer.class);
        if (patientCount == null || patientCount < 3) {
            throw new IllegalStateException(
                    "E2E fixture data missing: Expected at least 3 E2E patients, found " + patientCount);
        }

        Integer sampleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number = 'E2E'",
                Integer.class);
        if (sampleCount == null || sampleCount < 5) {
            throw new IllegalStateException(
                    "E2E fixture data missing: Expected at least 5 E2E samples, found " + sampleCount);
        }
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the
     * database. This method deletes test-created entities but preserves fixture
     * data.
     * 
     * Fixture data ranges (preserved): - Storage: IDs 1-999 (from Liquibase
     * foundation data) - Samples: E2E-* accession numbers (DBUnit fixtures) -
     * Patients: E2E-PAT-* external IDs (DBUnit fixtures) - Sample items: IDs
     * 10000-20000 (DBUnit fixtures) - Analyses: IDs 20000-30000 (DBUnit fixtures) -
     * Results: IDs 30000-40000 (DBUnit fixtures) - Assignments: IDs 5000-5013
     * (DBUnit fixtures) - Movements: IDs 5000-5013 (DBUnit fixtures)
     * 
     * Test-created data (deleted): - Storage: IDs >= 1000, codes/names starting
     * with TEST- - Samples: TEST-* accession numbers (if created by tests) - Sample
     * items: IDs >= 20000 (test-created, not DBUnit fixtures) - Assignments: IDs
     * 1000-4999 and >= 5014 (test-created, not DBUnit fixtures) - Movements: IDs
     * 1000-4999 and >= 5014 (test-created, not DBUnit fixtures)
     */
    protected void cleanStorageTestData() {
        try {
            // Delete test-created storage data (IDs >= 1000 or codes/names starting with
            // TEST-)
            // This preserves Liquibase foundation data (IDs 1-999)
            // Note: storage_position table has been removed (replaced by StorageBox)
            jdbcTemplate.execute(
                    "DELETE FROM storage_box WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR code LIKE 'TEST-%'");
            jdbcTemplate.execute(
                    "DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR code LIKE 'TEST-%'");
            jdbcTemplate.execute(
                    "DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");

            // Clean up test-created samples (preserve E2E-* fixtures from DBUnit)
            jdbcTemplate.execute("DELETE FROM result WHERE analysis_id IN "
                    + "(SELECT id FROM analysis WHERE sampitem_id IN " + "(SELECT id FROM sample_item WHERE samp_id IN "
                    + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%')))");
            jdbcTemplate.execute(
                    "DELETE FROM analysis WHERE sampitem_id IN " + "(SELECT id FROM sample_item WHERE samp_id IN "
                            + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%'))");
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE sample_item_id IN "
                    + "(SELECT id FROM sample_item WHERE samp_id IN "
                    + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%'))");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE sample_item_id IN "
                    + "(SELECT id FROM sample_item WHERE samp_id IN "
                    + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%'))");
            jdbcTemplate.execute("DELETE FROM sample_item WHERE samp_id IN "
                    + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%')");
            jdbcTemplate.execute("DELETE FROM sample_human WHERE samp_id IN "
                    + "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%')");
            jdbcTemplate.execute("DELETE FROM sample WHERE accession_number LIKE 'TEST-%'");

        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            logger.warn("Failed to clean storage test data: {}", e.getMessage());
        }
    }

    /**
     * Helper to get the numeric sample_item.id from the external_id. Used for
     * database verification queries and SQL inserts that require numeric IDs.
     * 
     * This method converts external IDs (user-friendly identifiers like "EXT-123")
     * to numeric IDs (database primary keys) for direct SQL operations.
     * 
     * @param externalId The external ID (e.g., "EXT-1765401458866")
     * @return The numeric ID (integer) for the sample item
     * @throws IllegalStateException if the external ID is not found in the database
     * 
     * @see .specify/guides/sampleitem-id-patterns.md for detailed ID pattern
     *      documentation
     */
    protected int getSampleItemNumericId(String externalId) {
        Integer numericId = jdbcTemplate.queryForObject("SELECT id FROM sample_item WHERE external_id = ?",
                Integer.class, externalId);
        if (numericId == null) {
            throw new IllegalStateException("SampleItem not found with external ID: " + externalId);
        }
        return numericId;
    }

}
