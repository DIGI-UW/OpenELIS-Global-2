package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.beans.factory.annotation.Autowired;

public class FreezerSecondProbeIngestionTest extends BaseWebContextSensitiveTest {

    @Autowired
    ReadingIngestionService readingIngestionService;

    @Autowired
    FreezerService freezerService;

    @Autowired
    FreezerReadingService freezerReadingService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer_second_probe.xml");
    }

    @Test
    public void dualProbeFreezer_shouldLoadSecondProbeRegisterConfig() {
        Freezer freezer = freezerService.findById(100L).orElse(null);
        assertNotNull("Dual-probe freezer should exist", freezer);

        assertEquals("Second probe register should match fixture", Integer.valueOf(2),
                freezer.getTemperatureRegister2());
        assertEquals("Second probe scale should match fixture", 0,
                new BigDecimal("1.0").compareTo(freezer.getTemperatureScale2()));
        assertEquals("Second probe offset should match fixture", 0,
                new BigDecimal("-80.0").compareTo(freezer.getTemperatureOffset2()));
    }

    @Test
    public void singleProbeFreezer_shouldHaveNoSecondProbeConfig() {
        Freezer freezer = freezerService.findById(101L).orElse(null);
        assertNotNull("Single-probe freezer should exist", freezer);

        assertNull("Legacy single-probe device should have no second register configured",
                freezer.getTemperatureRegister2());
    }

    @Test
    public void ingest_shouldStoreSecondProbeTemperatureForDualProbeFreezer() {
        Freezer freezer = freezerService.findById(100L).orElse(null);
        assertNotNull("Dual-probe freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-78.5");
        BigDecimal temperature2 = new BigDecimal("-76.2");

        readingIngestionService.ingest(freezer, recordedAt, temperature, null, temperature2, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(100L);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertEquals("Probe 1 temperature should match", 0, temperature.compareTo(reading.getTemperatureCelsius()));
        assertEquals("Probe 2 temperature should match", 0, temperature2.compareTo(reading.getTemperatureCelsius2()));
    }

    @Test
    public void ingest_shouldLeaveSecondProbeTemperatureNullForSingleProbeFreezer() {
        Freezer freezer = freezerService.findById(101L).orElse(null);
        assertNotNull("Single-probe freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-20.0");

        readingIngestionService.ingest(freezer, recordedAt, temperature, null, null, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(101L);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertEquals("Probe 1 temperature should still be recorded", 0,
                temperature.compareTo(reading.getTemperatureCelsius()));
        assertNull("Probe 2 temperature should be null for a legacy single-probe device",
                reading.getTemperatureCelsius2());
    }
}
