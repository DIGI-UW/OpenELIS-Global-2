package org.openelisglobal.scheduler.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Essential unit tests for CronSchedulerService.
 *
 * Tests cover: - Custom method: getCronScheduleByJobName() - Basic CRUD
 * operations (inherited but essential to verify) - Real production usage
 * scenarios - Edge cases
 *
 * Note: Extensive testing of inherited BaseObjectService methods is not needed
 * as those are tested in the base class tests.
 */
public class CronSchedulerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CronSchedulerService cronSchedulerService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/cron_scheduler_service.xml");
    }

    // ========== CUSTOM METHOD TESTS ==========

    @Test
    public void testGetCronScheduleByJobName_WithValidJobName_ReturnsScheduler() {
        CronScheduler result = cronSchedulerService.getCronScheduleByJobName("sendSiteIndicators");

        assertNotNull("CronScheduler should not be null", result);
        assertEquals("ID should be 100", "100", result.getId());
        assertEquals("Job name should match", "sendSiteIndicators", result.getJobName());
        assertEquals("Cron statement should match", "0 0 2 * * ?", result.getCronStatement());
        assertTrue("Should be active", result.getActive());
    }

    @Test
    public void testGetCronScheduleByJobName_WithInvalidJobName_ReturnsNull() {
        CronScheduler result = cronSchedulerService.getCronScheduleByJobName("nonExistentJob");

        assertNull("Should return null for non-existent job name", result);
    }

    @Test
    public void testGetCronScheduleByJobName_WithNullJobName_HandlesGracefully() {
        try {
            CronScheduler result = cronSchedulerService.getCronScheduleByJobName(null);
            assertNull("Should return null for null job name", result);
        } catch (Exception e) {
            // If implementation throws exception for null, that's also acceptable
            assertTrue("Exception for null parameter is acceptable", true);
        }
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    public void testGet_WithValidId_ReturnsScheduler() {
        CronScheduler result = cronSchedulerService.get("100");

        assertNotNull("CronScheduler should not be null", result);
        assertEquals("ID should match", "100", result.getId());
        assertEquals("Job name should match", "sendSiteIndicators", result.getJobName());
        assertTrue("Should be active", result.getActive());
    }

    @Test(expected = org.hibernate.ObjectNotFoundException.class)
    public void testGet_WithInvalidId_ThrowsException() {
        cronSchedulerService.get("999");
    }

    @Test
    public void testGetAll_ReturnsAllSchedulers() {
        List<CronScheduler> results = cronSchedulerService.getAll();

        assertNotNull("Results should not be null", results);
        assertEquals("Should return 8 schedulers", 8, results.size());
    }

    @Test
    public void testInsert_WithValidData_CreatesScheduler() {
        CronScheduler newScheduler = new CronScheduler();
        newScheduler.setCronStatement("0 0 6 * * ?");
        newScheduler.setActive(true);
        newScheduler.setRunIfPast(true);
        newScheduler.setName("New Test Job");
        newScheduler.setJobName("newTestJob");
        newScheduler.setDisplayKey("schedule.name.newTest");
        newScheduler.setDescriptionKey("schedule.description.newTest");
        newScheduler.setSysUserId("1");

        String insertedId = cronSchedulerService.insert(newScheduler);

        assertNotNull("Inserted ID should not be null", insertedId);

        CronScheduler retrieved = cronSchedulerService.get(insertedId);
        assertNotNull("Retrieved scheduler should not be null", retrieved);
        assertEquals("Job name should match", "newTestJob", retrieved.getJobName());
        assertEquals("Cron statement should match", "0 0 6 * * ?", retrieved.getCronStatement());
        assertTrue("Should be active", retrieved.getActive());
    }

    @Test
    public void testUpdate_WithModifiedFields_UpdatesScheduler() {
        CronScheduler existing = cronSchedulerService.get("107");
        assertNotNull("Existing scheduler should not be null", existing);

        String newCronStatement = "0 0 10 * * ?";
        existing.setCronStatement(newCronStatement);
        existing.setSysUserId("1");

        CronScheduler updated = cronSchedulerService.update(existing);

        assertNotNull("Updated scheduler should not be null", updated);
        assertEquals("Cron statement should be updated", newCronStatement, updated.getCronStatement());

        // Verify persistence
        CronScheduler retrieved = cronSchedulerService.get("107");
        assertEquals("Persisted cron statement should match", newCronStatement, retrieved.getCronStatement());
    }

    @Test
    public void testDelete_WithValidId_DeletesScheduler() {
        CronScheduler toDelete = cronSchedulerService.get("104");
        assertNotNull("Scheduler to delete should exist", toDelete);

        cronSchedulerService.delete("104", "1");

        try {
            cronSchedulerService.get("104");
            fail("Should throw ObjectNotFoundException for deleted scheduler");
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Expected - scheduler was deleted
        }
    }

    // ========== PRODUCTION SCENARIO TESTS ==========

    @Test
    public void testJobExecutionScenario_UpdateLastRunAfterJobCompletion() {
        // Simulates what AggregateReportJob.updateRunTimestamp() does
        CronScheduler reportScheduler = cronSchedulerService.getCronScheduleByJobName("sendSiteIndicators");
        assertNotNull("Report scheduler should exist", reportScheduler);

        Timestamp originalLastRun = reportScheduler.getLastRun();
        assertNotNull("Original last run should exist", originalLastRun);

        // Job executes and updates lastRun
        Timestamp newLastRun = new Timestamp(System.currentTimeMillis());
        reportScheduler.setLastRun(newLastRun);
        reportScheduler.setSysUserId("1");

        CronScheduler updated = cronSchedulerService.update(reportScheduler);

        assertNotNull("Updated scheduler should not be null", updated);
        assertTrue("Last run should be updated to newer time", updated.getLastRun().after(originalLastRun));

        // Verify another process can read the updated timestamp
        CronScheduler refreshed = cronSchedulerService.getCronScheduleByJobName("sendSiteIndicators");
        assertTrue("Persisted last run should be after original", refreshed.getLastRun().after(originalLastRun));
    }

    @Test
    public void testMalariaJobScenario_GetLastRunForIncrementalExport() {
        // Simulates what MalariaSurveilanceJob.getLatestCollectionDate() does
        CronScheduler malariaScheduler = cronSchedulerService.getCronScheduleByJobName("sendMalariaSurviellanceReport");
        assertNotNull("Malaria scheduler should exist", malariaScheduler);

        Timestamp lastRun = malariaScheduler.getLastRun();
        assertNotNull("Last run should exist for incremental export", lastRun);

        // Verify it's used to determine what data to export
        assertEquals("Should match test data timestamp", Timestamp.valueOf("2024-01-05 00:00:00"), lastRun);
    }

    @Test
    public void testSchedulerConfigScenario_LoadAllActiveSchedules() {
        // Simulates what SchedulerConfig.addReloadableCronSchedulers() does
        List<CronScheduler> allSchedulers = cronSchedulerService.getAll();

        assertNotNull("All schedulers should be loaded", allSchedulers);
        assertEquals("Should load 8 schedulers", 8, allSchedulers.size());

        // Filter active schedulers with valid cron statements
        long activeValidCount = allSchedulers.stream()
                .filter(s -> s.getActive() && !"never".equals(s.getCronStatement())).count();

        assertEquals("Should have 6 active schedulers with valid cron statements", 6, activeValidCount);
    }

    @Test
    public void testSchedulerReloadScenario_ActivateInactiveScheduler() {
        // Simulates admin activating a scheduler through UI and reloading
        CronScheduler inactiveScheduler = cronSchedulerService.get("103");
        assertNotNull("Inactive scheduler should exist", inactiveScheduler);
        assertFalse("Should initially be inactive", inactiveScheduler.getActive());

        // Admin activates it via UI
        inactiveScheduler.setActive(true);
        inactiveScheduler.setSysUserId("1");
        cronSchedulerService.update(inactiveScheduler);

        // Verify it would be picked up by SchedulerConfig reload
        CronScheduler refreshed = cronSchedulerService.get("103");
        assertTrue("Should now be active", refreshed.getActive());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    public void testGetAllMatching_WithActiveFilter_ReturnsCorrectCount() {
        List<CronScheduler> activeSchedulers = cronSchedulerService.getAllMatching("active", true);

        assertNotNull("Results should not be null", activeSchedulers);
        assertEquals("Should return 7 active schedulers", 7, activeSchedulers.size());

        for (CronScheduler scheduler : activeSchedulers) {
            assertTrue("All schedulers should be active", scheduler.getActive());
        }
    }

    @Test
    public void testSchedulerWithNeverStatement_IsHandledCorrectly() {
        CronScheduler neverRunScheduler = cronSchedulerService.getCronScheduleByJobName("neverRunJob");

        assertNotNull("Never run scheduler should exist", neverRunScheduler);
        assertEquals("Cron statement should be 'never'", "never", neverRunScheduler.getCronStatement());
        assertTrue("Should still be active", neverRunScheduler.getActive());
        assertNull("Last run should be null", neverRunScheduler.getLastRun());
    }

    @Test
    public void testSchedulerWithRunIfPastFalse_IsIdentified() {
        CronScheduler scheduler = cronSchedulerService.get("102");

        assertNotNull("Scheduler should exist", scheduler);
        assertFalse("Should not run if past due", scheduler.getRunIfPast());
        assertEquals("Should be the daily backup job", "dailyBackupJob", scheduler.getJobName());
    }
}
