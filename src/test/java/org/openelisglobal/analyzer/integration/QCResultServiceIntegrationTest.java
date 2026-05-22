package org.openelisglobal.analyzer.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for QCResultService.createQCResult().
 *
 * <p>
 * Verifies: control lot lookup, z-score calculation, persistence, and error
 * handling.
 *
 * <p>
 * Test data loaded via DBUnit from testdata/qc-result.xml:
 * <ul>
 * <li>lot-001: ACTIVE control lot with statistics (mean=100.0, SD=5.0)</li>
 * <li>lot-expired: EXPIRED control lot (for error path testing)</li>
 * <li>lot-no-stats: ACTIVE lot without statistics (for error path testing)</li>
 * </ul>
 */
public class QCResultServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCResultService qcResultService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-result.xml");
    }

    /**
     * Input: value=110, lot-001 (mean=100, SD=5) -> z-score = (110-100)/5 = 2.0
     */
    @Test
    public void testCreateQCResult_WithValidData_PersistsWithZScore() {
        QCResult result = qcResultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("110.0"),
                "mg/dL", LocalDateTime.now());

        assertNotNull("Should return persisted QCResult", result);
        assertNotNull("Result ID should be generated", result.getId());
        assertEquals("Result value should be 110.0", 0, new BigDecimal("110.0").compareTo(result.getResultValue()));
        assertEquals("Z-score should be 2.0000", 0, new BigDecimal("2.0000").compareTo(result.getZScore()));
        assertEquals("Control lot ID should match", "lot-001", result.getControlLotId());
        assertEquals("1", result.getTestId());
        assertEquals("1", result.getInstrumentId());
        assertEquals("Unit should be mg/dL", "mg/dL", result.getUnitOfMeasure());
        assertEquals("Status should be PENDING", "PENDING", result.getResultStatus());
    }

    /**
     * Input: value=90, lot-001 (mean=100, SD=5) -> z-score = (90-100)/5 = -2.0
     */
    @Test
    public void testCreateQCResult_WithValueBelowMean_CalculatesNegativeZScore() {
        QCResult result = qcResultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("90.0"), "mg/dL",
                LocalDateTime.now());

        assertEquals("Z-score should be -2.0000", 0, new BigDecimal("-2.0000").compareTo(result.getZScore()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithExpiredControlLot_ThrowsException() {
        qcResultService.createQCResult("1", "1", "lot-expired", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithNullResultValue_ThrowsException() {
        qcResultService.createQCResult("1", "1", "lot-001", "NORMAL", null, "mg/dL", LocalDateTime.now());
    }
}
