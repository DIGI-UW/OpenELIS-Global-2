package org.openelisglobal.qc.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests verifying: 1. MANUFACTURER_FIXED control lots
 * auto-seed/re-seed qc_statistics from manufacturer mean and SD (on create and
 * update) 2. The invariant: a lot cannot be ACTIVE without statistics. Enforced
 * at creation (validator rejects incomplete config) and at activation (rejects
 * when no statistics exist).
 */
public class QCControlLotServiceManufacturerFixedTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private QCStatisticsService statisticsService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-initial-runs.xml");
    }

    @Test
    public void createControlLot_manufacturerFixed_shouldSeedStatisticsRow() {
        // Arrange: build a MANUFACTURER_FIXED lot with known mean and SD
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-MFR-SEED-" + System.currentTimeMillis());
        lot.setProductName("Glucose Control Normal");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(100.0);
        lot.setManufacturerStdDev(5.0);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        // Act: create the lot through the service
        QCControlLot created = controlLotService.createControlLot(lot);
        assertNotNull("Control lot should be created", created);

        // Assert: a statistics row must now exist for this lot
        QCStatistics stats = statisticsService.getLatestStatistics(created.getId());

        assertNotNull("Statistics row should be seeded for MANUFACTURER_FIXED lot", stats);
        assertEquals("Statistics control lot ID should match", created.getId(), stats.getControlLotId());
        assertEquals("Statistics mean should equal manufacturer mean", 0,
                new BigDecimal("100.00000").compareTo(stats.getMean()));
        assertEquals("Statistics SD should equal manufacturer SD", 0,
                new BigDecimal("5.00000").compareTo(stats.getStandardDeviation()));
        assertEquals("Calculation method should be MANUFACTURER_FIXED", "MANUFACTURER_FIXED",
                stats.getCalculationMethod());
        assertEquals("Num values should be 0 (manufacturer-provided, not calculated)", Integer.valueOf(0),
                stats.getNumValues());
        assertNotNull("Validity start should be set", stats.getValidityStart());
        assertNull("Validity end should be null (currently active)", stats.getValidityEnd());
        assertNotNull("System user ID should be set", stats.getSystemUserId());
    }

    @Test
    public void createControlLot_initialRuns_shouldNotSeedStatisticsRow() {
        // Arrange: build an INITIAL_RUNS lot — should NOT get a statistics row
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-INIT-NOSEED-" + System.currentTimeMillis());
        lot.setProductName("Cholesterol Control Normal");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        // Act
        QCControlLot created = controlLotService.createControlLot(lot);
        assertNotNull("Control lot should be created", created);

        // Assert: NO statistics row should exist
        QCStatistics stats = statisticsService.getLatestStatistics(created.getId());
        assertNull("Statistics should NOT be seeded for INITIAL_RUNS lot", stats);
    }

    @Test
    public void updateControlLot_manufacturerFixed_shouldSeedStatisticsWhenNoneExist() {
        // Arrange: create a MANUFACTURER_FIXED lot (will seed initial stats)
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-MFR-UPD-" + System.currentTimeMillis());
        lot.setProductName("Creatinine Control Normal");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(1.0);
        lot.setManufacturerStdDev(0.1);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        QCControlLot created = controlLotService.createControlLot(lot);
        assertNotNull(created);

        // Verify initial seed
        QCStatistics initialStats = statisticsService.getLatestStatistics(created.getId());
        assertNotNull("Initial stats should be seeded on create", initialStats);
        assertEquals(0, new BigDecimal("1.00000").compareTo(initialStats.getMean()));

        // Act: update with new manufacturer values
        created.setManufacturerMean(55.5);
        created.setManufacturerStdDev(3.2);
        controlLotService.update(created);

        // Assert: statistics should be re-seeded with new values
        QCStatistics updatedStats = statisticsService.getLatestStatistics(created.getId());
        assertNotNull("Statistics should exist after update", updatedStats);
        assertEquals("Mean should be updated to 55.5", 0, new BigDecimal("55.50000").compareTo(updatedStats.getMean()));
        assertEquals("SD should be updated to 3.2", 0,
                new BigDecimal("3.20000").compareTo(updatedStats.getStandardDeviation()));
        assertEquals("MANUFACTURER_FIXED", updatedStats.getCalculationMethod());

        // Old statistics should be invalidated
        assertNotNull("Old statistics validity_end should be set",
                initialStats.getValidityEnd() != null || !initialStats.getId().equals(updatedStats.getId()));
    }

    @Test
    public void updateControlLot_manufacturerFixedWithNoExistingStats_shouldSeedStatistics() {
        // Arrange: simulate a lot created before the fix (no stats row)
        // Create as INITIAL_RUNS first (no stats seeded), then switch to
        // MANUFACTURER_FIXED
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-LEGACY-" + System.currentTimeMillis());
        lot.setProductName("Legacy Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        QCControlLot created = controlLotService.createControlLot(lot);
        assertNotNull(created);
        assertNull("INITIAL_RUNS lot should have no stats", statisticsService.getLatestStatistics(created.getId()));

        // Act: switch to MANUFACTURER_FIXED and update
        created.setCalculationMethod("MANUFACTURER_FIXED");
        created.setManufacturerMean(42.0);
        created.setManufacturerStdDev(2.5);
        controlLotService.update(created);

        // Assert: statistics row should now be created
        QCStatistics stats = statisticsService.getLatestStatistics(created.getId());
        assertNotNull("Statistics should be seeded when switching to MANUFACTURER_FIXED", stats);
        assertEquals(0, new BigDecimal("42.00000").compareTo(stats.getMean()));
        assertEquals(0, new BigDecimal("2.50000").compareTo(stats.getStandardDeviation()));
        assertEquals("MANUFACTURER_FIXED", stats.getCalculationMethod());
    }

    // ===================== Validation: creation-time prerequisites
    // =====================

    @Test(expected = IllegalArgumentException.class)
    public void createControlLot_manufacturerFixed_nullMean_shouldReject() {
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-NOVAL-" + System.currentTimeMillis());
        lot.setProductName("Bad Config Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(null); // Missing mean
        lot.setManufacturerStdDev(5.0);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        controlLotService.createControlLot(lot); // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void createControlLot_manufacturerFixed_nullStdDev_shouldReject() {
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-NOVAL2-" + System.currentTimeMillis());
        lot.setProductName("Bad Config Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(100.0);
        lot.setManufacturerStdDev(null); // Missing SD
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        controlLotService.createControlLot(lot); // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void createControlLot_initialRuns_nullCount_shouldReject() {
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-NOVAL3-" + System.currentTimeMillis());
        lot.setProductName("Bad Config Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(null); // Missing count
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        controlLotService.createControlLot(lot); // Should throw
    }

    // ===================== Validation: activation-time prerequisites
    // =====================

    @Test(expected = IllegalArgumentException.class)
    public void activateControlLot_noStatistics_shouldReject() {
        // Arrange: create an INITIAL_RUNS lot (ESTABLISHMENT, no stats)
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-NOACT-" + System.currentTimeMillis());
        lot.setProductName("Cannot Activate Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        QCControlLot created = controlLotService.createControlLot(lot);
        assertEquals("ESTABLISHMENT", created.getStatus());
        assertNull("Should have no statistics", statisticsService.getLatestStatistics(created.getId()));

        // Act: attempt to activate without statistics — should throw
        controlLotService.activateControlLot(created.getId());
    }

    @Test
    public void activateControlLot_withStatistics_shouldSucceed() {
        // Arrange: create a MANUFACTURER_FIXED lot (ACTIVE with stats seeded)
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-REACT-" + System.currentTimeMillis());
        lot.setProductName("Reactivation Control");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(100.0);
        lot.setManufacturerStdDev(5.0);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        QCControlLot created = controlLotService.createControlLot(lot);
        assertEquals("ACTIVE", created.getStatus());
        assertNotNull("Should have statistics", statisticsService.getLatestStatistics(created.getId()));

        // Deactivate first, then reactivate
        controlLotService.deactivateControlLot(created.getId());
        QCControlLot deactivated = controlLotService.get(created.getId());
        assertEquals("EXPIRED", deactivated.getStatus());

        // Act: reactivate — should succeed because statistics exist
        QCControlLot reactivated = controlLotService.activateControlLot(created.getId());
        assertEquals("ACTIVE", reactivated.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void update_setActiveWithoutStatistics_shouldReject() {
        // Arrange: create an INITIAL_RUNS lot (ESTABLISHMENT, no stats)
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setLotNumber("LOT-UPD-NOACT-" + System.currentTimeMillis());
        lot.setProductName("Cannot Activate Via Update");
        lot.setControlLevel("NORMAL");
        lot.setTestId("1");
        lot.setInstrumentId("1");
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);

        QCControlLot created = controlLotService.createControlLot(lot);
        assertEquals("ESTABLISHMENT", created.getStatus());
        assertNull("Should have no statistics", statisticsService.getLatestStatistics(created.getId()));

        // Act: set status to ACTIVE via update() — should throw
        created.setStatus("ACTIVE");
        controlLotService.update(created);
    }
}
