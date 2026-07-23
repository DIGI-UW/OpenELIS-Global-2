package org.openelisglobal.qc.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for QCResultService (T139).
 *
 * Tests QC result creation with real database (Testcontainers PostgreSQL). Test
 * data loaded via DBUnit from testdata/qc-result.xml: - lot-001: ACTIVE control
 * lot with statistics (mean=100.0, SD=5.0) - lot-expired: EXPIRED control lot -
 * lot-no-stats: ACTIVE control lot without statistics
 */
public class QCResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCResultService resultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-result.xml");
    }

    @Test
    public void createQCResult_withValidData_shouldCalculateZScoreAndPersist() {
        // Act: value=110, mean=100, SD=5 → z-score = (110-100)/5 = 2.0
        QCResult result = resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("110.0"), "mg/dL",
                LocalDateTime.of(2025, 6, 15, 10, 30));

        // Assert: result persisted with correct z-score
        assertNotNull("Result should be persisted", result);
        assertNotNull("Result ID should be generated", result.getId());
        assertEquals("Result value should be 110.0", 0, new BigDecimal("110.0").compareTo(result.getResultValue()));
        assertEquals("Z-score should be 2.0000", 0, new BigDecimal("2.0000").compareTo(result.getZScore()));
        assertEquals("Control lot ID should match", "lot-001", result.getControlLotId());
        assertEquals("1", result.getTestId());
        assertEquals("1", result.getInstrumentId());
        assertEquals("Unit should be mg/dL", "mg/dL", result.getUnitOfMeasure());
        assertEquals("Status should be PENDING", "PENDING", result.getResultStatus());
        assertFalse("Non-conformity flag should be false", result.getNonConformityFlag());
        assertEquals("Run date/time should match input",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 6, 15, 10, 30)), result.getRunDateTime());
    }

    @Test
    public void createQCResult_withValueBelowMean_shouldCalculateNegativeZScore() {
        // Act: value=95, mean=100, SD=5 → z-score = (95-100)/5 = -1.0
        QCResult result = resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("95.0"), "mg/dL",
                LocalDateTime.now());

        // Assert
        assertEquals("Z-score should be -1.0000", 0, new BigDecimal("-1.0000").compareTo(result.getZScore()));
    }

    @Test
    public void createQCResult_shouldCalculateHighPrecisionZScore() {
        // Act: value=103.3, mean=100, SD=5 → z-score = 3.3/5 = 0.66
        QCResult result = resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("103.3"), "mg/dL",
                LocalDateTime.now());

        // Assert: 4 decimal places
        assertEquals("Z-score should be 0.6600", 0, new BigDecimal("0.6600").compareTo(result.getZScore()));
    }

    @Test
    public void createQCResult_shouldBeRetrievableAfterCreation() {
        // Act
        QCResult created = resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("105.0"), "mg/dL",
                LocalDateTime.of(2025, 12, 1, 10, 30));

        // Assert: re-fetch from database to verify persistence
        QCResult fetched = resultService.get(created.getId());
        assertNotNull("Should be retrievable by ID", fetched);
        assertEquals("IDs should match", created.getId(), fetched.getId());
        assertEquals("Result values should match", 0, new BigDecimal("105.0").compareTo(fetched.getResultValue()));
        // z-score = (105 - 100) / 5 = 1.0000
        assertEquals("Z-score should be persisted correctly", 0,
                new BigDecimal("1.0000").compareTo(fetched.getZScore()));
        assertEquals("Control lot ID should be persisted", "lot-001", fetched.getControlLotId());
        assertEquals("Run date/time should be persisted",
                java.sql.Timestamp.valueOf(LocalDateTime.of(2025, 12, 1, 10, 30)), fetched.getRunDateTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createQCResult_withInvalidControlLot_shouldThrowException() {
        resultService.createQCResult("1", "1", "nonexistent-lot", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createQCResult_withExpiredControlLot_shouldThrowException() {
        resultService.createQCResult("1", "1", "lot-expired", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createQCResult_withNullResultValue_shouldThrowException() {
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", null, "mg/dL", LocalDateTime.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createQCResult_withMissingStatistics_shouldThrowException() {
        // lot-no-stats is ACTIVE but has no qc_statistics row
        resultService.createQCResult("1", "1", "lot-no-stats", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }
}
