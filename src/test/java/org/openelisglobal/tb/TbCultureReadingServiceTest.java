package org.openelisglobal.tb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for TB Culture Reading service. Tests ORM mappings and
 * basic CRUD operations.
 */
public class TbCultureReadingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TbCultureReadingService tbCultureReadingService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/tb-sample-registration.xml");
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
        assertEquals("Should have 3 readings", 3, readings.size());

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
        assertEquals("Latest should be week 3", Integer.valueOf(3), latestOpt.get().getWeekNumber());
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
}
