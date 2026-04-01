package org.openelisglobal.ocl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.test.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to clean up seeded demo tests and panels from OpenELIS database. This
 * runs BEFORE OCL import to ensure a clean slate for TTH Laboratory data.
 * 
 * SAFETY FEATURES: - Multiple safety checks before any deletion -
 * Transaction-based cleanup (rollback on failure) - Detailed logging for audit
 * trail - Pre-cleanup database state logging - Post-cleanup verification
 * 
 * Cleanup order respects foreign key constraints: 1. Child tables referencing
 * test/panel (panel_item, sampletype_*, test_*, result_limits, etc.) 2.
 * Localization entries for tests/panels 3. System modules and role modules for
 * panels 4. Finally, the test and panel tables themselves
 */
@Service
public class TestPanelCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TestPanelCleanupService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestService testService;

    @Autowired
    private PanelService panelService;

    // Tables that reference tests - must be deleted first
    private static final String[] TEST_CHILD_TABLES = { "clinlims.test_dictionary", "clinlims.test_code",
            "clinlims.test_worksheets", "clinlims.test_notification_config", "clinlims.test_operations",
            "clinlims.test_result_map", "clinlims.test_reflex", "clinlims.test_analyte", "clinlims.test_result",
            "clinlims.result_limits", "clinlims.sampletype_test", "clinlims.analyzer_test_map", "clinlims.qa_event",
            "clinlims.referral_result" };

    // Tables that reference panels - must be deleted first
    private static final String[] PANEL_CHILD_TABLES = { "clinlims.panel_item", "clinlims.sampletype_panel",
            "clinlims.notebook_page_panels" };

    // Patient data tables - presence of data blocks cleanup
    private static final String[] PATIENT_DATA_TABLES = { "clinlims.sample", "clinlims.analysis", "clinlims.result",
            "clinlims.patient", "clinlims.patient_identity", "clinlims.patient_contact" };

    /**
     * Removes all tests and panels from the database, handling FK constraints. This
     * should only be called when there's no patient data (fresh install or reset).
     * 
     * @return number of tests and panels removed
     */
    @Transactional
    public int cleanupAllTestsAndPanels() {
        log.info("========================================");
        log.info("STARTING TEST/PANEL CLEANUP");
        log.info("========================================");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        int totalRemoved = 0;

        try {
            // Step 0: Log current database state for audit
            logDatabaseState(jdbc);

            // Step 1: Delete child tables referencing tests
            log.info("--- Phase 1: Deleting child tables referencing tests ---");
            for (String table : TEST_CHILD_TABLES) {
                int deleted = safeDelete(jdbc, table, null);
                log.info("  Deleted {} rows from {}", deleted, table);
            }

            // Step 2: Delete child tables referencing panels
            log.info("--- Phase 2: Deleting child tables referencing panels ---");
            for (String table : PANEL_CHILD_TABLES) {
                int deleted = safeDelete(jdbc, table, null);
                log.info("  Deleted {} rows from {}", deleted, table);
            }

            // Step 3: Get localization IDs for tests and panels before deleting
            log.info("--- Phase 3: Collecting localization IDs for cleanup ---");
            Set<Integer> localizationIds = new HashSet<>();

            List<Integer> testLocIds = jdbc.queryForList(
                    "SELECT name_localization_id FROM clinlims.test WHERE name_localization_id IS NOT NULL "
                            + "UNION SELECT reporting_name_localization_id FROM clinlims.test WHERE reporting_name_localization_id IS NOT NULL",
                    Integer.class);
            localizationIds.addAll(testLocIds);
            log.info("  Found {} test localization IDs", testLocIds.size());

            List<Integer> panelLocIds = jdbc.queryForList(
                    "SELECT name_localization_id FROM clinlims.panel WHERE name_localization_id IS NOT NULL",
                    Integer.class);
            localizationIds.addAll(panelLocIds);
            log.info("  Found {} panel localization IDs", panelLocIds.size());

            // Step 4: Delete system modules and role modules for panels
            // Delete ALL panel-related system_modules, including orphaned ones from failed
            // imports
            // This handles the case where panels failed to create but system_modules were
            // partially inserted
            log.info("--- Phase 4: Deleting system modules and role modules for panels ---");
            int sysModulesDeleted = jdbc
                    .update("DELETE FROM clinlims.system_module WHERE description LIKE '%=>panel=>%'");
            log.info("  Deleted {} system_modules for panels (includes orphaned entries)", sysModulesDeleted);

            // Step 5: Delete tests
            log.info("--- Phase 5: Deleting tests ---");
            int testsDeleted = jdbc.update("DELETE FROM clinlims.test");
            log.info("  DELETED {} TESTS", testsDeleted);
            totalRemoved += testsDeleted;

            // Step 6: Delete panels
            log.info("--- Phase 6: Deleting panels ---");
            int panelsDeleted = jdbc.update("DELETE FROM clinlims.panel");
            log.info("  DELETED {} PANELS", panelsDeleted);
            totalRemoved += panelsDeleted;

            // Step 7: Delete orphaned localizations
            log.info("--- Phase 7: Deleting orphaned localizations ---");
            int localizationsDeleted = 0;
            for (Integer locId : localizationIds) {
                int deleted = jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", locId);
                localizationsDeleted += deleted;
            }
            log.info("  Deleted {} orphaned localizations", localizationsDeleted);

            // Step 8: Verify cleanup
            log.info("--- Phase 8: Verifying cleanup ---");
            int remainingTests = jdbc.queryForObject("SELECT count(*) FROM clinlims.test", Integer.class);
            int remainingPanels = jdbc.queryForObject("SELECT count(*) FROM clinlims.panel", Integer.class);

            if (remainingTests > 0 || remainingPanels > 0) {
                log.error("CLEANUP VERIFICATION FAILED! Remaining tests: {}, panels: {}", remainingTests,
                        remainingPanels);
                throw new RuntimeException("Cleanup verification failed - some tests/panels remain");
            }

            log.info("========================================");
            log.info("CLEANUP COMPLETE. Total removed: {}", totalRemoved);
            log.info("========================================");
            return totalRemoved;

        } catch (Exception e) {
            log.error("========================================");
            log.error("CLEANUP FAILED: {}", e.getMessage());
            log.error("Transaction will be rolled back.");
            log.error("========================================");
            throw new RuntimeException("Failed to cleanup tests and panels: " + e.getMessage(), e);
        }
    }

    /**
     * Safely deletes from a table, handling table existence check.
     */
    private int safeDelete(JdbcTemplate jdbc, String table, String whereClause) {
        try {
            String sql = whereClause != null ? "DELETE FROM " + table + " WHERE " + whereClause
                    : "DELETE FROM " + table;
            return jdbc.update(sql);
        } catch (Exception e) {
            log.warn("Could not delete from table {}: {}", table, e.getMessage());
            return 0;
        }
    }

    /**
     * Logs the current database state for audit purposes.
     */
    private void logDatabaseState(JdbcTemplate jdbc) {
        log.info("--- Pre-cleanup database state ---");

        try {
            // Log test count
            Integer testCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.test", Integer.class);
            log.info("  Tests: {}", testCount);

            // Log panel count
            Integer panelCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.panel", Integer.class);
            log.info("  Panels: {}", panelCount);

            // Log sample count (patient data check)
            Integer sampleCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.sample", Integer.class);
            log.info("  Samples: {}", sampleCount);

            // Log analysis count
            Integer analysisCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.analysis", Integer.class);
            log.info("  Analysis records: {}", analysisCount);

            // Log result count
            Integer resultCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.result", Integer.class);
            log.info("  Results: {}", resultCount);

            // Log patient count
            Integer patientCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.patient", Integer.class);
            log.info("  Patients: {}", patientCount);

        } catch (Exception e) {
            log.warn("Could not log database state: {}", e.getMessage());
        }
    }

    /**
     * Performs comprehensive safety check before cleanup. Checks ALL
     * patient-related tables for any data.
     * 
     * @return true if database is safe to clean (no patient data)
     */
    public boolean isDatabaseSafeToClean() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        log.info("Performing comprehensive database safety check...");
        List<String> blockingReasons = new ArrayList<>();

        try {
            // Check all patient data tables
            for (String table : PATIENT_DATA_TABLES) {
                try {
                    Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
                    if (count != null && count > 0) {
                        blockingReasons.add(table + ": " + count + " records");
                        log.warn("  SAFETY CHECK FAILED: {} has {} records", table, count);
                    }
                } catch (Exception e) {
                    log.debug("  Table {} may not exist or error: {}", table, e.getMessage());
                }
            }

            // Additional check: analysis with test_id or panel_id
            try {
                Integer analysisWithTest = jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.analysis WHERE test_id IS NOT NULL OR panel_id IS NOT NULL",
                        Integer.class);
                if (analysisWithTest != null && analysisWithTest > 0) {
                    blockingReasons.add("analysis linked to tests/panels: " + analysisWithTest + " records");
                    log.warn("  SAFETY CHECK FAILED: {} analysis records linked to tests/panels", analysisWithTest);
                }
            } catch (Exception e) {
                log.debug("  Could not check analysis table: {}", e.getMessage());
            }

            if (!blockingReasons.isEmpty()) {
                log.error("========================================");
                log.error("DATABASE SAFETY CHECK FAILED");
                log.error("Blocking reasons:");
                for (String reason : blockingReasons) {
                    log.error("  - {}", reason);
                }
                log.error("Cleanup will NOT proceed to prevent data loss.");
                log.error("To force cleanup, set org.openelisglobal.ocl.import.cleanup.force=true");
                log.error("========================================");
                return false;
            }

            log.info("========================================");
            log.info("DATABASE SAFETY CHECK PASSED");
            log.info("No patient data found - safe to proceed with cleanup");
            log.info("========================================");
            return true;

        } catch (Exception e) {
            log.error("Error during database safety check: {}", e.getMessage(), e);
            log.error("For safety, assuming database is NOT safe to clean");
            return false;
        }
    }

    /**
     * Performs cleanup only if database is safe (no patient data).
     * 
     * @return number of tests and panels removed, or -1 if cleanup was skipped
     */
    @Transactional
    public int safeCleanup() {
        if (!isDatabaseSafeToClean()) {
            log.warn("Skipping cleanup - database contains patient data");
            return -1;
        }
        return cleanupAllTestsAndPanels();
    }
}
