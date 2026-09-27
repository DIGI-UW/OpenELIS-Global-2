package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.ObjectNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The pathology case view rework retired three {@code PathologyStatus} names
 * (FR-2.1). The test database is migrated by the full changelog before any test
 * runs, so by the time this class executes, the changeset under test has
 * already run against an empty pathology_sample table and has nothing left to
 * prove. The SQL equivalent of the changeset's three update elements is
 * therefore run again here, against rows this test inserts itself on the
 * retired names, to show each one lands on its bench stage, that nothing is
 * left on a retired name (AC-5), that an unrelated value is untouched, and that
 * a migrated row loads again through the service, which a row on a retired name
 * did not.
 */
public class PathologyStatusMigrationTest extends BaseWebContextSensitiveTest {

    /**
     * The SQL equivalent of the changeset's three update elements
     * (001-pathology-status-bench-stages).
     */
    private static final String MAP_CUTTING_TO_GROSSING = "UPDATE clinlims.pathology_sample"
            + " SET status = 'GROSSING' WHERE status = 'CUTTING'";
    private static final String MAP_SLICING_TO_MICROTOMY = "UPDATE clinlims.pathology_sample"
            + " SET status = 'MICROTOMY' WHERE status = 'SLICING'";
    private static final String MAP_ADDITIONAL_REQUEST_TO_READY_PATHOLOGIST = "UPDATE clinlims.pathology_sample"
            + " SET status = 'READY_PATHOLOGIST' WHERE status = 'ADDITIONAL_REQUEST'";

    private static final int FORMERLY_CUTTING = 9101;
    private static final int FORMERLY_SLICING = 9102;
    private static final int FORMERLY_ADDITIONAL_REQUEST = 9103;
    private static final int STILL_PROCESSING = 9104;

    @Autowired
    private PathologySampleService pathologySampleService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        authenticateAs("technician1");
        cleanup();

        insertPathologySample(FORMERLY_CUTTING, "CUTTING");
        insertPathologySample(FORMERLY_SLICING, "SLICING");
        insertPathologySample(FORMERLY_ADDITIONAL_REQUEST, "ADDITIONAL_REQUEST");
        insertPathologySample(STILL_PROCESSING, "PROCESSING");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void migration_mapsEachRetiredNameOntoItsBenchStage() {
        runMigration();

        assertEquals("CUTTING becomes the stage it was already doing", "GROSSING", statusOf(FORMERLY_CUTTING));
        assertEquals("SLICING becomes the bench stage that absorbs it", "MICROTOMY", statusOf(FORMERLY_SLICING));
        assertEquals("a request raised mid-bench is read from its stage, not a status of its own", "READY_PATHOLOGIST",
                statusOf(FORMERLY_ADDITIONAL_REQUEST));
    }

    @Test
    public void migration_leavesNoRowOnARetiredName() {
        runMigration();

        Integer remaining = jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_sample"
                + " WHERE status IN ('CUTTING', 'SLICING', 'ADDITIONAL_REQUEST')", Integer.class);

        assertEquals("no row is left on a name the enum no longer has (AC-5)", Integer.valueOf(0), remaining);
    }

    @Test
    public void migration_leavesAnUnchangedValueAlone() {
        runMigration();

        assertEquals("a stage the migration does not touch is not touched", "PROCESSING", statusOf(STILL_PROCESSING));
    }

    @Test
    public void migration_isSafeToRunTwice() {
        runMigration();

        runMigration();

        assertEquals("a second run leaves the already-migrated row unchanged", "GROSSING", statusOf(FORMERLY_CUTTING));
        assertEquals("a second run leaves the already-migrated row unchanged", "MICROTOMY", statusOf(FORMERLY_SLICING));
        assertEquals("a second run leaves the already-migrated row unchanged", "READY_PATHOLOGIST",
                statusOf(FORMERLY_ADDITIONAL_REQUEST));
        assertEquals("a second run leaves an unrelated value unchanged", "PROCESSING", statusOf(STILL_PROCESSING));
    }

    @Test
    public void aMigratedRow_loadsThroughTheService() {
        runMigration();

        assertEquals("a row migrated off SLICING hydrates as its bench stage", PathologyStatus.MICROTOMY,
                pathologySampleService.get(FORMERLY_SLICING).getStatus());
        assertEquals("a row migrated off ADDITIONAL_REQUEST hydrates as its bench stage",
                PathologyStatus.READY_PATHOLOGIST, pathologySampleService.get(FORMERLY_ADDITIONAL_REQUEST).getStatus());
    }

    @Test
    public void aRowStillOnARetiredName_cannotBeLoaded() {
        assertEquals("the row exists and still holds the retired name, so the load below fails on"
                + " hydration and not because the row is missing", "CUTTING", statusOf(FORMERLY_CUTTING));

        try {
            pathologySampleService.get(FORMERLY_CUTTING);
            fail("a row on a retired name must not hydrate");
        } catch (RuntimeException expected) {
            assertFalse("the row is present, so BaseObjectServiceImpl.get's ObjectNotFoundException would mean"
                    + " the wrong thing failed", expected instanceof ObjectNotFoundException);
            // Hibernate's EnumType.STRING mapping resolves a stored name through
            // Enum.valueOf, whose failure message names both the constant and the enum
            // class it could not find it on.
            assertTrue(
                    "Hibernate cannot map the stored name to a PathologyStatus constant, which is the"
                            + " hazard the migration exists to remove",
                    causeChainNamesAll(expected, "CUTTING", "PathologyStatus"));
        }
    }

    private void runMigration() {
        jdbcTemplate.execute(MAP_CUTTING_TO_GROSSING);
        jdbcTemplate.execute(MAP_SLICING_TO_MICROTOMY);
        jdbcTemplate.execute(MAP_ADDITIONAL_REQUEST_TO_READY_PATHOLOGIST);
    }

    private String statusOf(int id) {
        return jdbcTemplate.queryForObject("SELECT status FROM clinlims.pathology_sample WHERE id = ?", String.class,
                id);
    }

    private void insertPathologySample(int id, String status) {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_sample (id, program_id, sample_id, status,"
                + " last_updated) VALUES (?, 1, 1, ?, now())", id, status);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_sample WHERE id IN (?, ?, ?, ?)", FORMERLY_CUTTING,
                FORMERLY_SLICING, FORMERLY_ADDITIONAL_REQUEST, STILL_PROCESSING);
    }

    /** Walks getCause() for one message that names every given word. */
    private boolean causeChainNamesAll(Throwable throwable, String... allOf) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null) {
                boolean namesAll = true;
                for (String candidate : allOf) {
                    if (!message.contains(candidate)) {
                        namesAll = false;
                        break;
                    }
                }
                if (namesAll) {
                    return true;
                }
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }
}
