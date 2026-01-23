package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

public class ReadingIngestionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ReadingIngestionService readingIngestionService;

    @Autowired
    FreezerService freezerService;

    @Autowired
    FreezerReadingService freezerReadingService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/reading_ingestion.xml");
    }

    @Test
    public void ingest_shouldSaveNormalReadingWithValidData() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-78.5");
        BigDecimal humidity = new BigDecimal("45.0");

        readingIngestionService.ingest(freezer, recordedAt, temperature, humidity, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertNotNull("Reading should have ID", reading.getId());
        assertEquals("Temperature should match", 0, temperature.compareTo(reading.getTemperatureCelsius()));
        assertEquals("Humidity should match", 0, humidity.compareTo(reading.getHumidityPercentage()));
        assertTrue("Transmission should be ok", reading.getTransmissionOk());
    }

    @Test
    public void ingest_shouldSaveReadingWithNullHumidity() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-79.0");

        readingIngestionService.ingest(freezer, recordedAt, temperature, null, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertEquals("Temperature should match", 0, temperature.compareTo(reading.getTemperatureCelsius()));
        assertEquals("Humidity should be null", null, reading.getHumidityPercentage());
    }

    @Test
    public void ingest_shouldSaveReadingWithTransmissionError() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-80.0");
        String errorMessage = "Connection timeout";

        readingIngestionService.ingest(freezer, recordedAt, temperature, null, false, errorMessage);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertFalse("Transmission should not be ok", reading.getTransmissionOk());
        assertEquals("Error message should match", errorMessage, reading.getErrorMessage());
    }

    @Test
    public void ingest_shouldSaveMultipleReadingsForSameFreezer() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime time1 = OffsetDateTime.now().minusMinutes(10);
        OffsetDateTime time2 = OffsetDateTime.now().minusMinutes(5);
        OffsetDateTime time3 = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, time1, new BigDecimal("-78.0"), new BigDecimal("40.0"), true, null);
        readingIngestionService.ingest(freezer, time2, new BigDecimal("-79.0"), new BigDecimal("42.0"), true, null);
        readingIngestionService.ingest(freezer, time3, new BigDecimal("-80.0"), new BigDecimal("44.0"), true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        FreezerReading reading = latestReading.get();
        assertEquals("Latest temperature should be -80.0", 0,
                new BigDecimal("-80.0").compareTo(reading.getTemperatureCelsius()));
    }

    @Test
    public void ingest_shouldSaveReadingsForDifferentFreezers() {
        Long freezerId1 = 100L;
        Long freezerId2 = 101L;

        Freezer freezer1 = freezerService.findById(freezerId1).orElse(null);
        Freezer freezer2 = freezerService.findById(freezerId2).orElse(null);

        assertNotNull("Freezer 1 should exist", freezer1);
        assertNotNull("Freezer 2 should exist", freezer2);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temp1 = new BigDecimal("-78.0");
        BigDecimal temp2 = new BigDecimal("-5.0");

        readingIngestionService.ingest(freezer1, recordedAt, temp1, null, true, null);
        readingIngestionService.ingest(freezer2, recordedAt, temp2, null, true, null);

        Optional<FreezerReading> reading1 = freezerReadingService.getLatestReading(freezerId1);
        Optional<FreezerReading> reading2 = freezerReadingService.getLatestReading(freezerId2);

        assertTrue("Freezer 1 should have reading", reading1.isPresent());
        assertTrue("Freezer 2 should have reading", reading2.isPresent());

        assertEquals("Freezer 1 temperature should match", 0, temp1.compareTo(reading1.get().getTemperatureCelsius()));
        assertEquals("Freezer 2 temperature should match", 0, temp2.compareTo(reading2.get().getTemperatureCelsius()));
    }

    @Test
    public void ingest_shouldSaveReadingWithNegativeTemperature() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-196.0"); // Liquid nitrogen temperature

        readingIngestionService.ingest(freezer, recordedAt, temperature, null, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        assertEquals("Temperature should be -196.0", 0,
                temperature.compareTo(latestReading.get().getTemperatureCelsius()));
    }

    @Test
    public void ingest_shouldSaveReadingWithHighHumidity() {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime recordedAt = OffsetDateTime.now();
        BigDecimal temperature = new BigDecimal("-80.0");
        BigDecimal humidity = new BigDecimal("95.5");

        readingIngestionService.ingest(freezer, recordedAt, temperature, humidity, true, null);

        Optional<FreezerReading> latestReading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Latest reading should exist", latestReading.isPresent());

        assertEquals("Humidity should be 95.5", 0, humidity.compareTo(latestReading.get().getHumidityPercentage()));
    }
}
