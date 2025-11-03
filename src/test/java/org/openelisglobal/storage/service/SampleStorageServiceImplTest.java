package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.dao.StoragePositionDAO;
import org.openelisglobal.storage.valueholder.*;

/**
 * Unit tests for SampleStorageService - Sample Assignment Logic
 * Following TDD: Write tests BEFORE implementation
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceImplTest {

    @Mock
    private SampleDAO sampleDAO;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    private Sample testSample;
    private StoragePosition testPosition;
    private StorageRack testRack;
    private StorageShelf testShelf;
    private StorageDevice testDevice;
    private StorageRoom testRoom;

    @Before
    public void setUp() {
        // Create test hierarchy
        testRoom = new StorageRoom();
        testRoom.setId("room-1");
        testRoom.setCode("MAIN");
        testRoom.setName("Main Laboratory");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId("device-1");
        testDevice.setCode("FRZ01");
        testDevice.setName("Freezer Unit 1");
        testDevice.setType(StorageDevice.DeviceType.FREEZER);
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId("shelf-1");
        testShelf.setLabel("Shelf-A");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId("rack-1");
        testRack.setLabel("Rack R1");
        testRack.setRows(8);
        testRack.setColumns(12);
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);

        testPosition = new StoragePosition();
        testPosition.setId("position-1");
        testPosition.setCoordinate("A5");
        testPosition.setOccupied(false);
        testPosition.setParentRack(testRack);

        testSample = new Sample();
        testSample.setId("sample-123");
    }

    /**
     * T043: Test assigning sample to valid position sets occupied flag
     * Business logic: position.occupied should be set to true
     */
    @Test
    public void testAssignSample_ValidPosition_SetsOccupied() {
        // Given: Unoccupied position
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(true);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class)))
            .thenReturn("assignment-id");

        // When: Assign sample
        String assignmentId = sampleStorageService.assignSample("sample-123", "position-1", "Test assignment");

        // Then: Position should be marked as occupied
        assertTrue("Position should be marked occupied", testPosition.getOccupied());
        verify(storageLocationService, times(1)).update(testPosition);
    }

    /**
     * T043: Test assigning sample creates audit log entry
     * Business logic: SampleStorageMovement audit record should be created
     */
    @Test
    public void testAssignSample_CreatesAuditLog() {
        // Given: Valid assignment
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(true);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class)))
            .thenReturn("assignment-id");

        // When: Assign sample
        sampleStorageService.assignSample("sample-123", "position-1", "Test assignment");

        // Then: Audit log entry should be created
        verify(sampleStorageMovementDAO, times(1)).insert(any(SampleStorageMovement.class));
    }

    /**
     * T043: Test assigning sample to occupied position throws exception
     * Validation: Prevent double-occupancy
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSample_OccupiedPosition_ThrowsException() {
        // Given: Position is already occupied
        testPosition.setOccupied(true);
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));

        // When: Attempt to assign to occupied position
        // Then: Expect exception
        sampleStorageService.assignSample("sample-123", "position-1", "Should fail");
    }

    /**
     * T043: Test assigning sample to inactive location throws exception
     * Validation: Cannot assign to inactive hierarchy
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSample_InactiveLocation_ThrowsException() {
        // Given: Position in inactive hierarchy
        testDevice.setActive(false); // Inactive device
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(false);

        // When: Attempt to assign to inactive location
        // Then: Expect exception
        sampleStorageService.assignSample("sample-123", "position-1", "Should fail");
    }

    /**
     * T043: Test assigning sample calculates capacity warnings
     * Business logic: Return warnings at 80%, 90%, 100% capacity
     */
    @Test
    public void testAssignSample_CalculatesCapacityWarnings() {
        // Given: Rack at 85% capacity (82 of 96 positions occupied)
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(true);
        when(storageLocationService.countOccupied(testRack.getId())).thenReturn(82);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class)))
            .thenReturn("assignment-id");

        // When: Assign sample
        String warning = sampleStorageService.assignSampleWithCapacityCheck("sample-123", "position-1", "Test");

        // Then: Expect capacity warning
        assertNotNull("Warning should be returned", warning);
        assertTrue("Warning should mention capacity", warning.contains("85%") || warning.contains("full"));
    }

    /**
     * T043: Test concurrent assignment to same position throws exception
     * Validation: Optimistic locking should prevent double-assignment
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSample_ConcurrentAccess_ThrowsException() {
        // Given: Position that will be modified concurrently
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(true);
        
        // Simulate optimistic locking exception
        doThrow(new org.hibernate.StaleObjectStateException("StoragePosition", "position-1"))
            .when(storageLocationService).update(testPosition);

        // When: Attempt to assign (concurrent modification)
        // Then: Expect exception
        sampleStorageService.assignSample("sample-123", "position-1", "Concurrent assignment");
    }

    /**
     * T043: Test assignment triggers position FHIR sync
     * Integration: @PostUpdate hook should fire on position update
     * Note: This test verifies service updates position, FHIR sync happens via JPA hooks
     */
    @Test
    public void testAssignSample_TriggersPositionUpdate() {
        // Given: Valid assignment
        when(storageLocationService.get(eq("position-1"), eq(StoragePosition.class))).thenReturn(testPosition);
        when(sampleDAO.get("sample-123")).thenReturn(Optional.of(testSample));
        when(storageLocationService.validateLocationActive(testPosition)).thenReturn(true);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class)))
            .thenReturn("assignment-id");

        // When: Assign sample
        sampleStorageService.assignSample("sample-123", "position-1", "Test");

        // Then: Position should be updated (which triggers @PostUpdate FHIR sync)
        verify(storageLocationService, times(1)).update(testPosition);
        assertTrue("Position update should trigger FHIR sync via @PostUpdate hook", 
            testPosition.getOccupied());
    }
}

