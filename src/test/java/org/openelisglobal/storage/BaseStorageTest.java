package org.openelisglobal.storage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base test class for storage-related tests that provides unified fixture loading
 * and cleanup helpers.
 * 
 * This class ensures test fixtures are loaded once per test class and provides
 * cleanup methods that preserve fixtures while removing test-created data.
 * 
 * Usage:
 * <pre>
 * public class MyStorageTest extends BaseStorageTest {
 *     @Before
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
 * Fixture Data Ranges (preserved during cleanup):
 * - Storage: IDs 1-999 (fixtures)
 * - Samples: E2E-* and TEST-* accession numbers
 * - Patients: E2E-PAT-* external IDs
 * - Sample items: IDs 10000-20000 (fixtures)
 * - Analyses: IDs 20000-30000 (fixtures)
 * - Results: IDs 30000-40000 (fixtures)
 */
public abstract class BaseStorageTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseStorageTest.class);
    
    // Static flag to ensure fixtures are only loaded once per test run
    private static boolean fixturesLoaded = false;
    private static final Object FIXTURE_LOCK = new Object();

    @Autowired
    protected DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Load fixtures once per test run (not per test class)
        loadFixturesIfNeeded();
        
        // Clean up test-created data before each test
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test-created data after each test (preserves fixtures)
        cleanStorageTestData();
    }

    /**
     * Load test fixtures if they haven't been loaded yet.
     * Uses a static flag to ensure fixtures are only loaded once per test run.
     */
    private void loadFixturesIfNeeded() {
        synchronized (FIXTURE_LOCK) {
            if (fixturesLoaded) {
                logger.debug("Fixtures already loaded, skipping");
                return;
            }

            try {
                logger.info("Loading test fixtures...");
                
                // Check if fixtures already exist
                Integer roomCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')",
                    Integer.class
                );
                
                if (roomCount != null && roomCount >= 3) {
                    logger.info("Fixtures already exist (found {} test rooms), skipping load", roomCount);
                    fixturesLoaded = true;
                    return;
                }

                // Load fixtures via SQL script
                Path sqlFile = Paths.get("src/test/resources/storage-test-data.sql");
                if (!Files.exists(sqlFile)) {
                    // Try relative to project root
                    sqlFile = Paths.get(System.getProperty("user.dir"), "src/test/resources/storage-test-data.sql");
                }
                
                if (!Files.exists(sqlFile)) {
                    logger.warn("Fixture SQL file not found at {}, skipping fixture load", sqlFile);
                    logger.warn("Tests may fail if fixtures are required");
                    fixturesLoaded = true; // Mark as loaded to avoid repeated warnings
                    return;
                }

                // Read and execute SQL file
                String sql = Files.readAllLines(sqlFile).stream()
                    .collect(Collectors.joining("\n"));
                
                // Execute SQL (split by semicolons for better error handling)
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("\\echo")) {
                        try {
                            jdbcTemplate.execute(trimmed);
                        } catch (Exception e) {
                            // Log but continue - some statements may fail if data already exists
                            logger.debug("SQL statement execution warning: {}", e.getMessage());
                        }
                    }
                }
                
                logger.info("Test fixtures loaded successfully");
                fixturesLoaded = true;
                
            } catch (Exception e) {
                logger.error("Failed to load test fixtures: {}", e.getMessage(), e);
                logger.warn("Tests may fail if fixtures are required");
                // Mark as loaded to avoid repeated failures
                fixturesLoaded = true;
            }
        }
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the database.
     * This method deletes test-created entities but preserves fixture data.
     * 
     * Fixture data ranges (preserved):
     * - Storage: IDs 1-999
     * - Samples: E2E-* and TEST-* accession numbers (fixtures)
     * - Patients: E2E-PAT-* external IDs
     * - Sample items: IDs 10000-20000
     * - Analyses: IDs 20000-30000
     * - Results: IDs 30000-40000
     * 
     * Test-created data (deleted):
     * - Storage: IDs >= 1000, codes/names starting with TEST-
     * - Samples: TEST-* accession numbers (if created by tests)
     * - Sample items: IDs >= 20000 (test-created)
     */
    protected void cleanStorageTestData() {
        try {
            // Delete test-created data (IDs >= 1000 or codes/names starting with TEST-)
            // This preserves fixture data loaded by fixtures (IDs 1-999, E2E-*)
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000 OR coordinate LIKE 'TEST-%'");
            jdbcTemplate.execute(
                "DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR short_code LIKE 'TEST-%'");
            jdbcTemplate.execute(
                "DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR short_code LIKE 'TEST-%'");
            jdbcTemplate.execute(
                "DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%' OR short_code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            
            // Clean up test-created samples (preserve E2E-* fixtures)
            jdbcTemplate.execute(
                "DELETE FROM sample_storage_assignment WHERE sample_item_id IN " +
                "(SELECT id FROM sample_item WHERE samp_id IN " +
                "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%'))");
            jdbcTemplate.execute(
                "DELETE FROM sample_item WHERE samp_id IN " +
                "(SELECT id FROM sample WHERE accession_number LIKE 'TEST-%')");
            jdbcTemplate.execute("DELETE FROM sample WHERE accession_number LIKE 'TEST-%'");
            
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            logger.warn("Failed to clean storage test data: {}", e.getMessage());
        }
    }

    /**
     * Reset the fixture loaded flag. Useful for tests that need to reload fixtures.
     * Should be used sparingly - typically only in test setup/teardown scenarios.
     */
    protected static void resetFixtureFlag() {
        synchronized (FIXTURE_LOCK) {
            fixturesLoaded = false;
        }
    }
}

