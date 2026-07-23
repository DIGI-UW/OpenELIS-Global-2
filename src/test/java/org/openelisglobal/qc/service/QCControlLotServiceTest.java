package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for QCControlLotService (TDD - RED phase) Tests control lot
 * management per US6 (Manage QC Control Lots)
 *
 * Following Constitution V (TDD): Write tests FIRST, ensure they FAIL before
 * implementation
 */
@RunWith(MockitoJUnitRunner.class)
public class QCControlLotServiceTest {

    @Mock
    private QCControlLotDAO controlLotDAO;

    @Mock
    private QCStatisticsDAO statisticsDAO;

    @Spy
    private QCControlLotValidator validator = new QCControlLotValidator();

    @Mock
    private WestgardRuleConfigService ruleConfigService;

    @InjectMocks
    private QCControlLotServiceImpl controlLotService;

    private QCControlLot testControlLot;

    @Before
    public void setUp() {
        // Inject the mocked statisticsDAO into the real validator
        ReflectionTestUtils.setField(validator, "statisticsDAO", statisticsDAO);

        testControlLot = QCControlLotBuilder.create().withId("test-lot-1").withProductName("Hematology Control Level 1")
                .withLotNumber("LOT-2025-001").withTestId("1").withInstrumentId("1").build();
    }

    /**
     * Test creating a control lot with manufacturer fixed values Per US6: Should be
     * immediately ACTIVE and ready for evaluation
     */
    @Test
    public void testCreateControlLotWithManufacturerValues_ShouldBeActive() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withId("test-lot-1").withCalculationMethod("MANUFACTURER_FIXED")
                .withManufacturerValues(100.0, 5.0).build();
        when(controlLotDAO.insert(any(QCControlLot.class))).thenReturn("test-lot-1");
        when(controlLotDAO.get("test-lot-1")).thenReturn(Optional.of(lot));
        // After seedManufacturerStatistics inserts stats, validator must find them
        when(statisticsDAO.findLatestByControlLot("test-lot-1"))
                .thenReturn(new org.openelisglobal.qc.valueholder.QCStatistics());

        // Act
        QCControlLot result = controlLotService.createControlLot(lot);

        // Assert
        assertNotNull("Created lot should not be null", result);
        assertEquals("Status should be ACTIVE for manufacturer fixed values", "ACTIVE", result.getStatus());
        assertNotNull("Manufacturer mean should be set", result.getManufacturerMean());
        assertNotNull("Manufacturer std dev should be set", result.getManufacturerStdDev());
        verify(controlLotDAO, times(1)).insert(any(QCControlLot.class));
        verify(controlLotDAO, times(1)).get("test-lot-1");
        // Verify statistics were actually seeded (not just pre-stubbed)
        verify(statisticsDAO).insert(any(org.openelisglobal.qc.valueholder.QCStatistics.class));
    }

    /**
     * Test creating a control lot with initial establishment method Per US6: Should
     * enter ESTABLISHMENT status until sufficient results collected
     */
    @Test
    public void testCreateControlLotWithInitialEstablishment_ShouldEnterEstablishmentStatus() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withId("test-lot-2").withCalculationMethod("INITIAL_RUNS")
                .withInitialRunsCount(20).build();
        when(controlLotDAO.insert(any(QCControlLot.class))).thenReturn("test-lot-2");
        when(controlLotDAO.get("test-lot-2")).thenReturn(Optional.of(lot));

        // Act
        QCControlLot result = controlLotService.createControlLot(lot);

        // Assert
        assertNotNull("Created lot should not be null", result);
        assertEquals("Status should be ESTABLISHMENT for initial runs", "ESTABLISHMENT", result.getStatus());
        assertEquals("Initial runs count should be 20", Integer.valueOf(20), result.getInitialRunsCount());
        verify(controlLotDAO, times(1)).insert(any(QCControlLot.class));
        verify(controlLotDAO, times(1)).get("test-lot-2");
        // Verify rule config seeding was attempted for this lot's test+instrument
        verify(ruleConfigService).findByTestAndInstrument(lot.getTestId(), lot.getInstrumentId());
    }

    /**
     * Test creating a control lot with rolling window method Per US6: Should enter
     * ESTABLISHMENT status initially
     */
    @Test
    public void testCreateControlLotWithRollingWindow_ShouldEnterEstablishmentStatus() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withId("test-lot-3").withCalculationMethod("ROLLING").build();
        when(controlLotDAO.insert(any(QCControlLot.class))).thenReturn("test-lot-3");
        when(controlLotDAO.get("test-lot-3")).thenReturn(Optional.of(lot));

        // Act
        QCControlLot result = controlLotService.createControlLot(lot);

        // Assert
        assertNotNull("Created lot should not be null", result);
        assertEquals("Status should be ESTABLISHMENT for rolling window", "ESTABLISHMENT", result.getStatus());
        verify(controlLotDAO, times(1)).insert(any(QCControlLot.class));
        verify(controlLotDAO, times(1)).get("test-lot-3");
        // Verify rule config seeding was attempted for this lot's test+instrument
        verify(ruleConfigService).findByTestAndInstrument(lot.getTestId(), lot.getInstrumentId());
    }

    /**
     * Test activating a control lot in establishment phase Per US6: Should
     * transition from ESTABLISHMENT to ACTIVE
     */
    @Test
    public void testActivateControlLot_ShouldTransitionToActive() {
        // Arrange
        testControlLot.setStatus("ESTABLISHMENT");
        when(controlLotDAO.get(anyString())).thenReturn(Optional.of(testControlLot));
        when(controlLotDAO.update(any(QCControlLot.class))).thenReturn(testControlLot);
        // Validator checks that statistics exist for ACTIVE lots
        when(statisticsDAO.findLatestByControlLot("test-lot-1"))
                .thenReturn(new org.openelisglobal.qc.valueholder.QCStatistics());

        // Act
        QCControlLot result = controlLotService.activateControlLot("test-lot-1");

        // Assert
        assertNotNull("Activated lot should not be null", result);
        assertEquals("Status should be ACTIVE after activation", "ACTIVE", result.getStatus());
        verify(controlLotDAO, times(1)).get("test-lot-1");
        verify(controlLotDAO, times(1)).update(any(QCControlLot.class));
    }

    /**
     * Test deactivating a control lot Per US6: Should handle lot expiration or
     * manual deactivation
     */
    @Test
    public void testDeactivateControlLot_ShouldMarkAsExpired() {
        // Arrange
        testControlLot.setStatus("ACTIVE");
        when(controlLotDAO.get(anyString())).thenReturn(Optional.of(testControlLot));
        when(controlLotDAO.update(any(QCControlLot.class))).thenReturn(testControlLot);

        // Act
        QCControlLot result = controlLotService.deactivateControlLot("test-lot-1");

        // Assert
        assertNotNull("Deactivated lot should not be null", result);
        assertEquals("Status should be EXPIRED after deactivation", "EXPIRED", result.getStatus());
        verify(controlLotDAO, times(1)).get("test-lot-1");
        verify(controlLotDAO, times(1)).update(any(QCControlLot.class));
    }

    /**
     * Test retrieving active control lots delegates to DAO with correct parameters.
     * Per US6: Filtering is done by the DAO query, not the service.
     */
    @Test
    public void testGetActiveControlLots_ShouldDelegateToDAOWithCorrectParams() {
        // Arrange
        QCControlLot activeLot1 = QCControlLotBuilder.create().withId("lot-1").asActive().build();
        QCControlLot activeLot2 = QCControlLotBuilder.create().withId("lot-2").asActive().build();
        when(controlLotDAO.getActiveByTestAndInstrument("1", "1")).thenReturn(Arrays.asList(activeLot1, activeLot2));

        // Act
        List<QCControlLot> results = controlLotService.getActiveControlLots("1", "1");

        // Assert — service returns exactly what DAO returns
        assertEquals("Should return 2 lots from DAO", 2, results.size());
        assertEquals("lot-1", results.get(0).getId());
        assertEquals("lot-2", results.get(1).getId());
        verify(controlLotDAO).getActiveByTestAndInstrument("1", "1");
    }

    /**
     * Test retrieving control lot by lot number
     * Per US6: Should support lookup by unique lot number
     */
    @Test
    public void testGetControlLotByLotNumber_ShouldReturnMatchingLot() {
        // Arrange
        when(controlLotDAO.getByLotNumber("LOT-2025-001")).thenReturn(testControlLot);

        // Act
        QCControlLot result = controlLotService.getControlLotByLotNumber("LOT-2025-001");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Lot number should match", "LOT-2025-001", result.getLotNumber());
        verify(controlLotDAO, times(1)).getByLotNumber("LOT-2025-001");
    }

    /**
     * Test validation: manufacturer fixed values must have mean and stdDev Per US6:
     * Should reject invalid configuration
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateControlLot_ManufacturerFixedWithoutValues_ShouldThrowException() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withCalculationMethod("MANUFACTURER_FIXED")
                .withManufacturerMean(null).withManufacturerStdDev(null).build();

        // Act
        controlLotService.createControlLot(lot); // Should throw IllegalArgumentException
    }

    /**
     * Test validation: initial runs count must be positive Per US6: Should validate
     * configuration parameters
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateControlLot_InitialRunsWithZeroCount_ShouldThrowException() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withCalculationMethod("INITIAL_RUNS").withInitialRunsCount(0)
                .build();

        // Act
        controlLotService.createControlLot(lot); // Should throw IllegalArgumentException
    }

    /**
     * Test automatic expiration check Per US6: Should automatically mark expired
     * lots
     */
    @Test
    public void testCheckExpiration_ShouldMarkExpiredLots() {
        // Arrange
        Timestamp pastDate = new Timestamp(System.currentTimeMillis() - 86400000L); // Yesterday
        testControlLot.setStatus("ACTIVE");
        testControlLot.setExpirationDate(pastDate);
        when(controlLotDAO.getActiveByTestAndInstrument("1", "1")).thenReturn(Arrays.asList(testControlLot));
        when(controlLotDAO.update(any(QCControlLot.class))).thenReturn(testControlLot);

        // Act
        controlLotService.checkAndExpireLots("1", "1");

        // Assert
        verify(controlLotDAO, times(1)).getActiveByTestAndInstrument("1", "1");
        verify(controlLotDAO, times(1)).update(argThat(lot -> "EXPIRED".equals(lot.getStatus())));
    }

    // ===================== Rule config seeding tests =====================

    /**
     * Test: Creating a control lot seeds default Westgard rule configs when none
     * exist for the test+instrument combo.
     */
    @Test
    public void testCreateControlLot_ShouldSeedRuleConfigsWhenNoneExist() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withId("lot-seed-1").withTestId("5").withInstrumentId("10")
                .withCalculationMethod("INITIAL_RUNS").withInitialRunsCount(20).build();
        when(controlLotDAO.insert(any(QCControlLot.class))).thenReturn("lot-seed-1");
        when(controlLotDAO.get("lot-seed-1")).thenReturn(Optional.of(lot));
        when(ruleConfigService.findByTestAndInstrument("5", "10")).thenReturn(Arrays.asList());

        // Act
        QCControlLot result = controlLotService.createControlLot(lot);

        // Assert — verify return value
        assertNotNull("Created lot should not be null", result);
        assertEquals("lot-seed-1", result.getId());

        // Assert — should check for existing configs and create defaults
        verify(ruleConfigService).findByTestAndInstrument("5", "10");
        verify(ruleConfigService).createDefaultConfig("5", "10");
    }

    /**
     * Test: Creating a control lot does NOT re-seed rule configs when they already
     * exist for the test+instrument combo.
     */
    @Test
    public void testCreateControlLot_ShouldNotReseedRuleConfigsWhenAlreadyExist() {
        // Arrange
        QCControlLot lot = QCControlLotBuilder.create().withId("lot-seed-2").withTestId("5").withInstrumentId("10")
                .withCalculationMethod("INITIAL_RUNS").withInitialRunsCount(20).build();
        when(controlLotDAO.insert(any(QCControlLot.class))).thenReturn("lot-seed-2");
        when(controlLotDAO.get("lot-seed-2")).thenReturn(Optional.of(lot));

        // Existing configs already present
        WestgardRuleConfig existingConfig = new WestgardRuleConfig();
        existingConfig.setRuleCode("1₃ₛ");
        when(ruleConfigService.findByTestAndInstrument("5", "10")).thenReturn(Arrays.asList(existingConfig));

        // Act
        controlLotService.createControlLot(lot);

        // Assert — should check but NOT create
        verify(ruleConfigService).findByTestAndInstrument("5", "10");
        verify(ruleConfigService, never()).createDefaultConfig(anyString(), anyString());
    }

}
