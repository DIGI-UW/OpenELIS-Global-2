package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stops someone narrowing freezer_reading's device columns, which no decoder
 * bounds.
 */
public class FreezerReadingPrecisionTest extends BaseWebContextSensitiveTest {

    /**
     * A DS18B20/ESP32 "no data" sentinel decoded as a raw signed 16-bit register at
     * the default scale of 1.0, which is what an unconfigured probe reports.
     */
    private static final BigDecimal SENTINEL = new BigDecimal("32767.00");

    @Autowired
    ReadingIngestionService readingIngestionService;

    @Autowired
    FreezerService freezerService;

    @Autowired
    FreezerReadingService freezerReadingService;

    @Autowired
    javax.sql.DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer_second_probe.xml");
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void deviceReportedReadingColumnsShouldHaveNoMagnitudeCeiling() {
        for (String column : List.of("temperature_celsius", "humidity_percentage", "temperature_celsius_2")) {
            assertNull(
                    "freezer_reading." + column + " must stay unbounded DECIMAL: it holds whatever a device"
                            + " reports, and a value the column cannot represent costs the whole poll row",
                    numericPrecisionOf("freezer_reading", column));
        }
    }

    @Test
    public void adminEnteredConfigurationColumnsShouldKeepTheirNarrowedPrecision() {
        assertEquals("freezer.target_temperature is hand-typed, so 089 narrows it", Integer.valueOf(6),
                numericPrecisionOf("freezer", "target_temperature"));
        assertEquals("threshold_profile.humidity_critical_max is hand-typed, so 089 narrows it", Integer.valueOf(6),
                numericPrecisionOf("threshold_profile", "humidity_critical_max"));
    }

    /**
     * A reading the schema cannot store rolls the poll back, and the offline switch
     * counts stored rows, so the device goes unmonitored rather than wrong.
     */
    @Test
    public void ingestShouldStoreASentinelReadingSoTheOfflineDetectorStillSeesThePoll() {
        Freezer freezer = freezerService.findById(100L).orElse(null);
        assertNotNull("Dual-probe freezer should exist", freezer);

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), SENTINEL, SENTINEL, SENTINEL, true, null);

        List<FreezerReading> recent = freezerReadingService.getRecentReadings(100L, 1);
        assertEquals("the poll must leave a row for the offline detector to count", 1, recent.size());
        FreezerReading stored = recent.get(0);
        assertEquals("temperature sentinel should round-trip", 0, SENTINEL.compareTo(stored.getTemperatureCelsius()));
        assertEquals("humidity sentinel should round-trip", 0, SENTINEL.compareTo(stored.getHumidityPercentage()));
        assertEquals("second-probe sentinel should round-trip", 0, SENTINEL.compareTo(stored.getTemperatureCelsius2()));
        assertTrue("the row must be marked transmitted, not a failure",
                Boolean.TRUE.equals(stored.getTransmissionOk()));
    }

    private Integer numericPrecisionOf(String table, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT numeric_precision FROM information_schema.columns"
                        + " WHERE table_schema = 'clinlims' AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
    }
}
