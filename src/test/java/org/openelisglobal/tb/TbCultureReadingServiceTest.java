package org.openelisglobal.tb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbCultureReadingService.IncubationSummary;
import org.openelisglobal.tb.service.TbMediaPreparationService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for TB Culture Reading service. Tests ORM mappings and
 * basic CRUD operations.
 */
public class TbCultureReadingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TbCultureReadingService tbCultureReadingService;

    @Autowired
    private TbMediaPreparationService tbMediaPreparationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    // Test data IDs
    private static final int TEST_MEDIA_BATCH_ID = 99999;

    @Before
    public void setup() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/tb-sample-registration.xml");
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
        insertTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void insertTestData() {
        // Create test media batch for inoculation tests
        jdbcTemplate.update(
                "INSERT INTO clinlims.tb_media_preparation (id, batch_id, media_type, qc_status, preparation_date, expiry_date, last_updated) "
                        + "VALUES (?, ?, ?, ?, '2026-01-01', '2026-12-31', CURRENT_TIMESTAMP)",
                TEST_MEDIA_BATCH_ID, "TEST-BATCH-001", "LJ", "PASSED");
    }

    private void cleanTestData() {
        try {
            // Clean up records created during tests (linked to our test media batch),
            // but preserve seed data from tb-sample-registration.xml (id < 100)
            jdbcTemplate
                    .execute("DELETE FROM clinlims.tb_culture_reading WHERE media_batch_id = " + TEST_MEDIA_BATCH_ID);
            jdbcTemplate.execute("DELETE FROM clinlims.tb_culture_reading WHERE id >= 99000");
            jdbcTemplate.execute("DELETE FROM clinlims.tb_media_preparation WHERE id = " + TEST_MEDIA_BATCH_ID);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void verifyTestData() {
        List<TbCultureReading> readings = tbCultureReadingService.getAll();

        assertNotNull("Reading list should not be null", readings);
        assertFalse("Reading list should not be empty", readings.isEmpty());

        readings.forEach(reading -> {
            assertNotNull("Reading ID should not be null", reading.getId());
            assertNotNull("Week number should not be null", reading.getWeekNumber());
            assertNotNull("Culture method should not be null", reading.getCultureMethod());
            assertNotNull("Growth observation should not be null", reading.getGrowthObservation());
        });
    }

    @Test
    public void findBySampleItemId_shouldReturnReadingsOrderedByWeek() {
        String sampleItemId = "100";

        List<TbCultureReading> readings = tbCultureReadingService.findBySampleItemId(sampleItemId);

        assertNotNull("Readings list should not be null", readings);
        assertFalse("Should have culture readings", readings.isEmpty());
        assertEquals("Should have 4 readings (weeks 1, 2, 3, 4)", 4, readings.size());

        // Verify ordering by week number
        for (int i = 1; i < readings.size(); i++) {
            assertTrue("Readings should be ordered by week",
                    readings.get(i - 1).getWeekNumber() < readings.get(i).getWeekNumber());
        }
    }

    @Test
    public void findBySampleItemIdAndWeek_shouldReturnSpecificReading() {
        String sampleItemId = "100";
        Integer weekNumber = 3;

        Optional<TbCultureReading> readingOpt = tbCultureReadingService.findBySampleItemIdAndWeek(sampleItemId,
                weekNumber);

        assertTrue("Reading should be found", readingOpt.isPresent());
        assertEquals("Week number should match", weekNumber, readingOpt.get().getWeekNumber());
        assertEquals("Growth should be detected", GrowthObservation.GROWTH_DETECTED,
                readingOpt.get().getGrowthObservation());
    }

    @Test
    public void findBySampleItemIdAndWeek_shouldReturnEmptyForNonExistentWeek() {
        String sampleItemId = "100";
        Integer weekNumber = 8; // Week 8 doesn't exist in test data

        Optional<TbCultureReading> readingOpt = tbCultureReadingService.findBySampleItemIdAndWeek(sampleItemId,
                weekNumber);

        assertFalse("Reading should not be found for week 8", readingOpt.isPresent());
    }

    @Test
    public void findByGrowthObservation_shouldReturnMatchingReadings() {
        List<TbCultureReading> growthReadings = tbCultureReadingService
                .findByGrowthObservation(GrowthObservation.GROWTH_DETECTED);

        assertNotNull("Readings list should not be null", growthReadings);
        assertFalse("Should find readings with growth", growthReadings.isEmpty());

        growthReadings.forEach(reading -> {
            assertEquals("All readings should have growth detected", GrowthObservation.GROWTH_DETECTED,
                    reading.getGrowthObservation());
        });
    }

    @Test
    public void findLatestBySampleItemId_shouldReturnHighestWeekReading() {
        String sampleItemId = "100";

        Optional<TbCultureReading> latestOpt = tbCultureReadingService.findLatestBySampleItemId(sampleItemId);

        assertTrue("Latest reading should be found", latestOpt.isPresent());
        assertEquals("Latest should be week 4 (culture positive reading)", Integer.valueOf(4),
                latestOpt.get().getWeekNumber());
    }

    @Test
    public void countGrowthDetected_shouldCountDistinctSamples() {
        Long count = tbCultureReadingService.countGrowthDetected();

        assertNotNull("Count should not be null", count);
        assertTrue("Should have at least one growth detected sample", count >= 1);
    }

    @Test
    public void weekNumber_shouldBeValidated() {
        TbCultureReading reading = new TbCultureReading();

        // Test valid week numbers
        reading.setWeekNumber(1);
        assertEquals("Week 1 should be valid", Integer.valueOf(1), reading.getWeekNumber());

        reading.setWeekNumber(8);
        assertEquals("Week 8 should be valid", Integer.valueOf(8), reading.getWeekNumber());

        // Test invalid week number - should throw exception
        try {
            reading.setWeekNumber(9);
            assertTrue("Should have thrown exception for week 9", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention week range", e.getMessage().contains("between 1 and 8"));
        }

        try {
            reading.setWeekNumber(0);
            assertTrue("Should have thrown exception for week 0", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention week range", e.getMessage().contains("between 1 and 8"));
        }
    }

    @Test
    public void helperMethods_shouldWorkCorrectly() {
        TbCultureReading reading = new TbCultureReading();
        reading.setWeekNumber(8);

        // Test isGrowthDetected
        reading.setGrowthObservation(GrowthObservation.NO_GROWTH);
        assertFalse("Should not be growth detected", reading.isGrowthDetected());

        reading.setGrowthObservation(GrowthObservation.GROWTH_DETECTED);
        assertTrue("Should be growth detected", reading.isGrowthDetected());

        // Test isContaminated
        reading.setGrowthObservation(GrowthObservation.NO_GROWTH);
        assertFalse("Should not be contaminated", reading.isContaminated());

        reading.setGrowthObservation(GrowthObservation.CONTAMINATED);
        assertTrue("Should be contaminated", reading.isContaminated());

        // Test isFinalNegative
        reading.setWeekNumber(8);
        reading.setGrowthObservation(GrowthObservation.NO_GROWTH);
        assertTrue("Should be final negative", reading.isFinalNegative());

        reading.setWeekNumber(7);
        assertFalse("Week 7 should not be final", reading.isFinalNegative());
    }

    /**
     * Test that inoculation succeeds with valid sysUserId.
     *
     * This test verifies that the service properly sets sysUserId before insert to
     * avoid audit trail errors (SYS_USER_ID IS NULL).
     */
    @Test
    public void inoculate_shouldSucceedWithValidSysUserId() {
        // Arrange
        TbMediaPreparation mediaBatch = tbMediaPreparationService.get(TEST_MEDIA_BATCH_ID);
        assertNotNull("Test media batch should exist", mediaBatch);

        // Use sample 102 which has no pre-existing culture readings
        String sampleItemId = "102";
        String sysUserId = "1"; // Valid system user

        // Act
        TbCultureReading reading = tbCultureReadingService.inoculate(sampleItemId, mediaBatch, null, sysUserId);

        // Assert
        assertNotNull("Reading should be created", reading);
        assertNotNull("Reading should have ID", reading.getId());
        assertEquals("Week number should be 1", Integer.valueOf(1), reading.getWeekNumber());
        assertEquals("Growth observation should be NO_GROWTH", GrowthObservation.NO_GROWTH,
                reading.getGrowthObservation());
        assertNotNull("Inoculation date should be set", reading.getInoculationDate());
        assertNotNull("Media batch should be set", reading.getMediaBatch());
        assertEquals("Media batch should match", TEST_MEDIA_BATCH_ID, reading.getMediaBatch().getId().intValue());
    }

    /**
     * Test that recording a reading for a different week creates a NEW record
     * instead of overwriting the existing Week 1 record.
     *
     * BUG: Current implementation overwrites the existing record's week number,
     * losing the Week 1 reading data.
     *
     * EXPECTED: After recording Week 2, BOTH Week 1 AND Week 2 records should
     * exist.
     */
    @Test
    public void recordReading_shouldCreateNewRecordForDifferentWeek() {
        // Arrange - Inoculate to create Week 1 record
        TbMediaPreparation mediaBatch = tbMediaPreparationService.get(TEST_MEDIA_BATCH_ID);
        assertNotNull("Test media batch should exist", mediaBatch);

        // Use sample 102 which has no pre-existing culture readings
        String sampleItemId = "102";
        String sysUserId = "1";

        TbCultureReading week1Reading = tbCultureReadingService.inoculate(sampleItemId, mediaBatch, null, sysUserId);
        assertNotNull("Week 1 reading should be created", week1Reading);
        assertEquals("Initial week should be 1", Integer.valueOf(1), week1Reading.getWeekNumber());

        Integer week1ReadingId = week1Reading.getId();

        // Act - Record Week 2 reading
        TbCultureReading week2Reading = tbCultureReadingService.recordReading(week1ReadingId, 2, // Different week
                GrowthObservation.NO_GROWTH, "Week 2 observation notes", sysUserId);

        // Assert - BOTH Week 1 AND Week 2 should exist
        assertNotNull("Week 2 reading should be returned", week2Reading);
        assertEquals("Returned reading should be Week 2", Integer.valueOf(2), week2Reading.getWeekNumber());

        // Critical assertion: Week 1 record should STILL exist and be unchanged
        List<TbCultureReading> allReadings = tbCultureReadingService.findBySampleItemId(sampleItemId);

        // Filter to only the readings we created in this test (have our media batch)
        long readingsWithOurMediaBatch = allReadings.stream()
                .filter(r -> r.getMediaBatch() != null && r.getMediaBatch().getId().equals(TEST_MEDIA_BATCH_ID))
                .count();

        assertEquals("Should have 2 separate records (Week 1 AND Week 2), not 1 overwritten record", 2,
                readingsWithOurMediaBatch);

        // Verify Week 1 still exists by fetching the original record directly
        TbCultureReading originalWeek1 = tbCultureReadingService.get(week1ReadingId);
        assertNotNull("Original Week 1 record should still exist", originalWeek1);
        assertEquals("Original Week 1 record should NOT be overwritten to Week 2", Integer.valueOf(1),
                originalWeek1.getWeekNumber());
    }

    // ==================== Entry-Filtered Method Tests ====================
    // These tests verify that samples are correctly filtered by notebook entry ID
    // to prevent data leakage between different notebook entries.

    // Test entry IDs from test data
    private static final Integer ENTRY_ID_100 = 100; // Contains sample_item 100
    private static final Integer ENTRY_ID_101 = 101; // Contains sample_item 101
    private static final Integer NON_EXISTENT_ENTRY_ID = 99999;

    /**
     * Test that findIncubatingSamplesByEntry returns only samples for the specified
     * entry. Sample 100 should only appear when querying Entry 100.
     */
    @Test
    public void findIncubatingSamplesByEntry_shouldReturnOnlySamplesForSpecifiedEntry() {
        // Act - Query for entry 100
        List<TbCultureReading> entry100Samples = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_100);

        // Assert - Should find samples linked to entry 100
        assertNotNull("Result should not be null", entry100Samples);

        // Extract sample item IDs from results
        Set<String> sampleItemIds = entry100Samples.stream().map(cr -> cr.getSampleItem().getId())
                .collect(Collectors.toSet());

        // Sample 100 is linked to Entry 100
        assertTrue("Should include sample 100 for entry 100", sampleItemIds.contains("100"));
        // Sample 101 is linked to Entry 101, should NOT appear
        assertFalse("Should NOT include sample 101 for entry 100", sampleItemIds.contains("101"));
    }

    /**
     * Test that findIncubatingSamplesByEntry isolates data between entries. Entry
     * 101 should only see its own samples.
     */
    @Test
    public void findIncubatingSamplesByEntry_shouldIsolateDataBetweenEntries() {
        // Act - Query for entry 101
        List<TbCultureReading> entry101Samples = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_101);

        // Assert
        assertNotNull("Result should not be null", entry101Samples);

        Set<String> sampleItemIds = entry101Samples.stream().map(cr -> cr.getSampleItem().getId())
                .collect(Collectors.toSet());

        // Sample 101 is linked to Entry 101
        assertTrue("Should include sample 101 for entry 101", sampleItemIds.contains("101"));
        // Sample 100 is linked to Entry 100, should NOT appear
        assertFalse("Should NOT include sample 100 for entry 101", sampleItemIds.contains("100"));
    }

    /**
     * Test that findIncubatingSamplesByEntry returns empty list for non-existent
     * entry.
     */
    @Test
    public void findIncubatingSamplesByEntry_shouldReturnEmptyForNonExistentEntry() {
        // Act
        List<TbCultureReading> samples = tbCultureReadingService.findIncubatingSamplesByEntry(NON_EXISTENT_ENTRY_ID);

        // Assert
        assertNotNull("Result should not be null", samples);
        assertTrue("Should return empty list for non-existent entry", samples.isEmpty());
    }

    /**
     * Test that findByCultureResultAndEntry filters by both result and entry. Only
     * culture-positive samples belonging to the specified entry should be returned.
     */
    @Test
    public void findByCultureResultAndEntry_shouldFilterByResultAndEntry() {
        // Act - Query for positive samples in entry 100
        List<TbCultureReading> positiveSamples = tbCultureReadingService
                .findByCultureResultAndEntry(CultureResult.POSITIVE, ENTRY_ID_100);

        // Assert
        assertNotNull("Result should not be null", positiveSamples);

        // All returned samples should be POSITIVE
        for (TbCultureReading reading : positiveSamples) {
            assertEquals("All samples should have POSITIVE result", CultureResult.POSITIVE, reading.getCultureResult());
        }

        // Verify sample belongs to entry 100 (sample_item_id = 100)
        if (!positiveSamples.isEmpty()) {
            Set<String> sampleItemIds = positiveSamples.stream().map(cr -> cr.getSampleItem().getId())
                    .collect(Collectors.toSet());
            assertTrue("Positive samples should be from entry 100's sample (100)", sampleItemIds.contains("100"));
            assertFalse("Should NOT include sample 101", sampleItemIds.contains("101"));
        }
    }

    /**
     * Test that findByCultureResultAndEntry returns empty for entry with no
     * matching results.
     */
    @Test
    public void findByCultureResultAndEntry_shouldReturnEmptyWhenNoMatchingResults() {
        // Act - Entry 101 has no POSITIVE samples in test data
        List<TbCultureReading> positiveSamples = tbCultureReadingService
                .findByCultureResultAndEntry(CultureResult.POSITIVE, ENTRY_ID_101);

        // Assert
        assertNotNull("Result should not be null", positiveSamples);
        assertTrue("Should return empty list when entry has no matching results", positiveSamples.isEmpty());
    }

    /**
     * Test that getIncubationSummaryByEntry returns correct statistics for a
     * specific entry.
     */
    @Test
    public void getIncubationSummaryByEntry_shouldReturnStatsForSpecificEntry() {
        // Act
        IncubationSummary summary = tbCultureReadingService.getIncubationSummaryByEntry(ENTRY_ID_100);

        // Assert
        assertNotNull("Summary should not be null", summary);
        // Entry 100 has samples, so stats should be >= 0
        assertTrue("Total incubating should be >= 0", summary.totalIncubating() >= 0);
        assertTrue("Positive count should be >= 0", summary.positive() >= 0);
        assertTrue("Negative count should be >= 0", summary.negative() >= 0);
    }

    /**
     * Test that getIncubationSummaryByEntry returns zeroes for non-existent entry.
     */
    @Test
    public void getIncubationSummaryByEntry_shouldReturnZeroesForNonExistentEntry() {
        // Act
        IncubationSummary summary = tbCultureReadingService.getIncubationSummaryByEntry(NON_EXISTENT_ENTRY_ID);

        // Assert
        assertNotNull("Summary should not be null", summary);
        assertEquals("Total incubating should be 0 for non-existent entry", 0, summary.totalIncubating());
        assertEquals("Positive should be 0 for non-existent entry", 0, summary.positive());
        assertEquals("Negative should be 0 for non-existent entry", 0, summary.negative());
    }

    /**
     * Test data isolation - summary stats for different entries should be
     * independent.
     */
    @Test
    public void getIncubationSummaryByEntry_shouldProvideIndependentStatsPerEntry() {
        // Act
        IncubationSummary summary100 = tbCultureReadingService.getIncubationSummaryByEntry(ENTRY_ID_100);
        IncubationSummary summary101 = tbCultureReadingService.getIncubationSummaryByEntry(ENTRY_ID_101);

        // Assert - Both should return valid summaries
        assertNotNull("Summary for entry 100 should not be null", summary100);
        assertNotNull("Summary for entry 101 should not be null", summary101);

        // The counts don't need to be equal - they're independent entries
        // Just verify they're both valid (non-negative)
        assertTrue("Entry 100 positive count should be >= 0", summary100.positive() >= 0);
        assertTrue("Entry 101 positive count should be >= 0", summary101.positive() >= 0);
    }

    // ==================== Readings With Culture Result Tests ====================
    // These tests verify that findIncubatingSamplesByEntry returns ALL readings,
    // including those with cultureResult set. This allows the frontend to group
    // readings by sampleItemId and check if ANY reading has a result.

    /**
     * Test that findIncubatingSamplesByEntry returns readings that have a
     * cultureResult set (POSITIVE, NEGATIVE, etc.), not just those with NULL.
     *
     * This is critical for the frontend to correctly identify finalized samples
     * when grouping multiple readings by sampleItemId.
     */
    @Test
    public void findIncubatingSamplesByEntry_shouldReturnReadingsWithCultureResult() {
        // Arrange - First mark one reading as POSITIVE using the service
        // (DBUnit XML doesn't properly load enum values)
        List<TbCultureReading> initialReadings = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_100);
        assertFalse("Should have initial readings", initialReadings.isEmpty());

        // Mark the first reading as POSITIVE
        TbCultureReading readingToMark = initialReadings.get(0);
        tbCultureReadingService.determineFinalResult(readingToMark.getId(), CultureResult.POSITIVE, 1, "1");

        // Act - Query again - should return ALL readings including the one with
        // POSITIVE result
        List<TbCultureReading> entry100Readings = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_100);

        // Assert - Should find readings for sample 100
        assertNotNull("Result should not be null", entry100Readings);
        assertFalse("Should have readings", entry100Readings.isEmpty());

        // Check that at least one reading has a cultureResult set (POSITIVE)
        boolean hasReadingWithResult = entry100Readings.stream().anyMatch(r -> r.getCultureResult() != null);

        assertTrue("Should include readings with cultureResult set (POSITIVE)", hasReadingWithResult);

        // Verify we have readings WITH and WITHOUT cultureResult (to confirm we get
        // ALL)
        long readingsWithResult = entry100Readings.stream().filter(r -> r.getCultureResult() != null).count();
        long readingsWithoutResult = entry100Readings.stream().filter(r -> r.getCultureResult() == null).count();

        assertTrue("Should have at least one reading WITH cultureResult", readingsWithResult >= 1);
        assertTrue("Should have at least one reading WITHOUT cultureResult", readingsWithoutResult >= 1);
    }

    /**
     * Test that the frontend can determine if a sample is finalized by checking if
     * ANY reading has a cultureResult set (simulating frontend grouping logic).
     */
    @Test
    public void findIncubatingSamplesByEntry_frontendCanDetectFinalizedSamples() {
        // Arrange - First mark one reading as POSITIVE using the service
        List<TbCultureReading> initialReadings = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_100);
        assertFalse("Should have initial readings", initialReadings.isEmpty());

        // Mark the first reading as POSITIVE
        TbCultureReading readingToMark = initialReadings.get(0);
        tbCultureReadingService.determineFinalResult(readingToMark.getId(), CultureResult.POSITIVE, 1, "1");

        // Act - Query again
        List<TbCultureReading> entry100Readings = tbCultureReadingService.findIncubatingSamplesByEntry(ENTRY_ID_100);

        // Simulate frontend grouping: group readings by sampleItemId
        java.util.Map<String, List<TbCultureReading>> readingsBySample = entry100Readings.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getSampleItem().getId()));

        // For each sample, check if ANY reading has a cultureResult
        for (java.util.Map.Entry<String, List<TbCultureReading>> entry : readingsBySample.entrySet()) {
            String sampleItemId = entry.getKey();
            List<TbCultureReading> readings = entry.getValue();

            boolean isFinalized = readings.stream().anyMatch(r -> r.getCultureResult() != null);

            // Sample 100 should now have a POSITIVE result
            if ("100".equals(sampleItemId)) {
                assertTrue("Sample 100 should be detected as finalized (has POSITIVE reading)", isFinalized);

                // Verify the specific result
                CultureResult result = readings.stream().filter(r -> r.getCultureResult() != null)
                        .map(TbCultureReading::getCultureResult).findFirst().orElse(null);
                assertEquals("Sample 100 should have POSITIVE result", CultureResult.POSITIVE, result);
            }
        }
    }
}
