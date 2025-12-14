package org.openelisglobal.pharmaceutical.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.pharmaceutical.dao.EnvironmentalExcursionEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent.AlertType;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent.ExcursionStatus;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;

/**
 * Unit tests for EnvironmentalExcursionService. Tests excursion recording,
 * acknowledgement, resolution, and escalation workflows.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentalExcursionServiceTest {

    @Mock
    private EnvironmentalExcursionEventDAO environmentalExcursionEventDAO;

    @Mock
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @InjectMocks
    private EnvironmentalExcursionServiceImpl excursionService;

    private EnvironmentalExcursionEvent activeExcursion;
    private EnvironmentalExcursionEvent acknowledgedExcursion;

    @Before
    public void setUp() {
        activeExcursion = new EnvironmentalExcursionEvent();
        activeExcursion.setId(1);
        activeExcursion.setDeviceId(100);
        activeExcursion.setAlertType(AlertType.TEMPERATURE_HIGH);
        activeExcursion.setRecordedValue(8.5);
        activeExcursion.setThresholdValue(8.0);
        activeExcursion.setStatus(ExcursionStatus.ACTIVE);
        activeExcursion.setDetectedAt(new Timestamp(System.currentTimeMillis()));
        activeExcursion.setSysUserId("user1");

        acknowledgedExcursion = new EnvironmentalExcursionEvent();
        acknowledgedExcursion.setId(2);
        acknowledgedExcursion.setDeviceId(100);
        acknowledgedExcursion.setAlertType(AlertType.TEMPERATURE_HIGH);
        acknowledgedExcursion.setStatus(ExcursionStatus.ACKNOWLEDGED);
        acknowledgedExcursion.setDetectedAt(new Timestamp(System.currentTimeMillis()));
        acknowledgedExcursion.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        acknowledgedExcursion.setAcknowledgedBy("user2");
        acknowledgedExcursion.setSysUserId("user1");
    }

    // ==================== Recording Excursion Tests ====================

    @Test
    public void testRecordExcursion_CreatesNewExcursionWithActiveStatus() {
        // Arrange
        when(environmentalExcursionEventDAO.insert(any(EnvironmentalExcursionEvent.class))).thenReturn(1);

        // Act
        EnvironmentalExcursionEvent result = excursionService.recordExcursion(
                100,
                AlertType.TEMPERATURE_HIGH,
                8.5,
                8.0,
                "Freezer Room A",
                "user1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Device ID should match", Integer.valueOf(100), result.getDeviceId());
        assertEquals("Alert type should match", AlertType.TEMPERATURE_HIGH, result.getAlertType());
        assertEquals("Recorded value should match", Double.valueOf(8.5), result.getRecordedValue());
        assertEquals("Threshold value should match", Double.valueOf(8.0), result.getThresholdValue());
        assertEquals("Status should be ACTIVE", ExcursionStatus.ACTIVE, result.getStatus());
        assertNotNull("Detected at should be set", result.getDetectedAt());
    }

    @Test
    public void testRecordExcursion_SetsCorrectDeviceLocation() {
        // Arrange
        when(environmentalExcursionEventDAO.insert(any(EnvironmentalExcursionEvent.class))).thenReturn(1);

        // Act
        EnvironmentalExcursionEvent result = excursionService.recordExcursion(
                100,
                AlertType.POWER_FAILURE,
                0.0,
                0.0,
                "Cold Storage Building B, Room 3",
                "user1");

        // Assert
        assertEquals("Device location should match", "Cold Storage Building B, Room 3", result.getDeviceLocation());
    }

    // ==================== Acknowledgement Tests ====================

    @Test
    public void testAcknowledgeExcursion_UpdatesStatusToAcknowledged() {
        // Arrange
        when(environmentalExcursionEventDAO.get(1)).thenReturn(Optional.of(activeExcursion));

        // Act
        EnvironmentalExcursionEvent result = excursionService.acknowledgeExcursion(
                1, "Investigating root cause", "user2");

        // Assert
        assertEquals("Status should be ACKNOWLEDGED", ExcursionStatus.ACKNOWLEDGED, result.getStatus());
        assertNotNull("Acknowledged at should be set", result.getAcknowledgedAt());
        assertEquals("Acknowledged by should match", "user2", result.getAcknowledgedBy());
        verify(environmentalExcursionEventDAO).update(any(EnvironmentalExcursionEvent.class));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testAcknowledgeExcursion_ThrowsExceptionForNonActiveExcursion() {
        // Arrange
        acknowledgedExcursion.setStatus(ExcursionStatus.RESOLVED);
        when(environmentalExcursionEventDAO.get(2)).thenReturn(Optional.of(acknowledgedExcursion));

        // Act - should throw exception
        excursionService.acknowledgeExcursion(2, "Notes", "user2");
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testAcknowledgeExcursion_ThrowsExceptionForNonExistentExcursion() {
        // Arrange
        when(environmentalExcursionEventDAO.get(999)).thenReturn(Optional.empty());

        // Act - should throw exception
        excursionService.acknowledgeExcursion(999, "Notes", "user2");
    }

    // ==================== Resolution Tests ====================

    @Test
    public void testResolveExcursion_UpdatesStatusToResolved() {
        // Arrange
        when(environmentalExcursionEventDAO.get(2)).thenReturn(Optional.of(acknowledgedExcursion));

        // Act
        EnvironmentalExcursionEvent result = excursionService.resolveExcursion(
                2, "Issue resolved by replacing faulty sensor", "user3");

        // Assert
        assertEquals("Status should be RESOLVED", ExcursionStatus.RESOLVED, result.getStatus());
        assertNotNull("Resolved at should be set", result.getResolvedAt());
        assertEquals("Resolved by should match", "user3", result.getResolvedBy());
        verify(environmentalExcursionEventDAO).update(any(EnvironmentalExcursionEvent.class));
    }

    @Test
    public void testResolveExcursion_CanResolveActiveExcursionDirectly() {
        // Arrange
        when(environmentalExcursionEventDAO.get(1)).thenReturn(Optional.of(activeExcursion));

        // Act
        EnvironmentalExcursionEvent result = excursionService.resolveExcursion(
                1, "Quick resolution - false alarm", "user2");

        // Assert
        assertEquals("Status should be RESOLVED", ExcursionStatus.RESOLVED, result.getStatus());
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testResolveExcursion_ThrowsExceptionForAlreadyResolvedExcursion() {
        // Arrange
        acknowledgedExcursion.setStatus(ExcursionStatus.RESOLVED);
        when(environmentalExcursionEventDAO.get(2)).thenReturn(Optional.of(acknowledgedExcursion));

        // Act - should throw exception
        excursionService.resolveExcursion(2, "Notes", "user3");
    }

    // ==================== Escalation Tests ====================

    @Test
    public void testEscalateExcursion_UpdatesStatusToEscalated() {
        // Arrange
        when(environmentalExcursionEventDAO.get(1)).thenReturn(Optional.of(activeExcursion));

        // Act
        EnvironmentalExcursionEvent result = excursionService.escalateExcursion(
                1, "Temperature continues to rise - need immediate management attention", "user2");

        // Assert
        assertEquals("Status should be ESCALATED", ExcursionStatus.ESCALATED, result.getStatus());
        assertTrue("Notes should contain escalation reason",
                result.getNotes().contains("Temperature continues to rise"));
        verify(environmentalExcursionEventDAO).update(any(EnvironmentalExcursionEvent.class));
    }

    // ==================== Query Tests ====================

    @Test
    public void testFindActiveExcursions_ReturnsOnlyActiveExcursions() {
        // Arrange
        List<EnvironmentalExcursionEvent> activeList = Arrays.asList(activeExcursion);
        when(environmentalExcursionEventDAO.findActiveExcursions()).thenReturn(activeList);

        // Act
        List<EnvironmentalExcursionEvent> result = excursionService.findActiveExcursions();

        // Assert
        assertEquals("Should return 1 active excursion", 1, result.size());
        assertEquals("Status should be ACTIVE", ExcursionStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    public void testGetUnacknowledgedExcursions_ReturnsActiveExcursions() {
        // Arrange
        List<EnvironmentalExcursionEvent> activeList = Arrays.asList(activeExcursion);
        when(environmentalExcursionEventDAO.findByStatus(ExcursionStatus.ACTIVE)).thenReturn(activeList);

        // Act
        List<EnvironmentalExcursionEvent> result = excursionService.getUnacknowledgedExcursions();

        // Assert
        assertEquals("Should return 1 unacknowledged excursion", 1, result.size());
    }

    @Test
    public void testHasActiveExcursions_ReturnsTrueWhenActiveExcursionsExist() {
        // Arrange
        List<EnvironmentalExcursionEvent> deviceExcursions = Arrays.asList(activeExcursion);
        when(environmentalExcursionEventDAO.findByDeviceId(100)).thenReturn(deviceExcursions);

        // Act
        boolean result = excursionService.hasActiveExcursions(100);

        // Assert
        assertTrue("Should return true when active excursions exist", result);
    }

    @Test
    public void testHasActiveExcursions_ReturnsTrueForAcknowledgedExcursions() {
        // Arrange
        List<EnvironmentalExcursionEvent> deviceExcursions = Arrays.asList(acknowledgedExcursion);
        when(environmentalExcursionEventDAO.findByDeviceId(100)).thenReturn(deviceExcursions);

        // Act
        boolean result = excursionService.hasActiveExcursions(100);

        // Assert
        assertTrue("Should return true when acknowledged excursions exist", result);
    }

    @Test
    public void testHasActiveExcursions_ReturnsFalseWhenNoActiveExcursions() {
        // Arrange
        EnvironmentalExcursionEvent resolvedExcursion = new EnvironmentalExcursionEvent();
        resolvedExcursion.setStatus(ExcursionStatus.RESOLVED);
        List<EnvironmentalExcursionEvent> deviceExcursions = Arrays.asList(resolvedExcursion);
        when(environmentalExcursionEventDAO.findByDeviceId(100)).thenReturn(deviceExcursions);

        // Act
        boolean result = excursionService.hasActiveExcursions(100);

        // Assert
        assertFalse("Should return false when no active excursions exist", result);
    }

    // ==================== Affected Samples Tests ====================

    @Test
    public void testGetAffectedSamples_ReturnsEmptyListWhenNoSampleIds() {
        // Arrange
        activeExcursion.setAffectedSampleIds(null);
        when(environmentalExcursionEventDAO.get(1)).thenReturn(Optional.of(activeExcursion));

        // Act
        List<Map<String, Object>> result = excursionService.getAffectedSamples(1);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testGetAffectedSamples_ReturnsSampleInfoForValidIds() {
        // Arrange
        activeExcursion.setAffectedSampleIds("10, 20, 30");
        when(environmentalExcursionEventDAO.get(1)).thenReturn(Optional.of(activeExcursion));

        PharmaceuticalSample sample1 = new PharmaceuticalSample();
        sample1.setId(10);
        sample1.setSampleName("Sample A");
        sample1.setLotNumber("LOT001");
        sample1.setStatus(PharmaceuticalSample.SampleStatus.REGISTERED);

        PharmaceuticalSample sample2 = new PharmaceuticalSample();
        sample2.setId(20);
        sample2.setSampleName("Sample B");
        sample2.setLotNumber("LOT002");
        sample2.setStatus(PharmaceuticalSample.SampleStatus.IN_TESTING);

        when(pharmaceuticalSampleDAO.get(10)).thenReturn(Optional.of(sample1));
        when(pharmaceuticalSampleDAO.get(20)).thenReturn(Optional.of(sample2));
        when(pharmaceuticalSampleDAO.get(30)).thenReturn(Optional.empty());

        // Act
        List<Map<String, Object>> result = excursionService.getAffectedSamples(1);

        // Assert
        assertEquals("Should return 2 samples (third not found)", 2, result.size());
        assertEquals("First sample ID should match", 10, result.get(0).get("sampleId"));
        assertEquals("First sample name should match", "Sample A", result.get(0).get("sampleName"));
        assertEquals("Second sample ID should match", 20, result.get(1).get("sampleId"));
    }

    @Test
    public void testGetAffectedSamples_ReturnsEmptyForNonExistentExcursion() {
        // Arrange
        when(environmentalExcursionEventDAO.get(999)).thenReturn(Optional.empty());

        // Act
        List<Map<String, Object>> result = excursionService.getAffectedSamples(999);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }
}
