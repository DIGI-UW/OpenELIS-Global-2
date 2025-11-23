package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.valueholder.*;

/**
 * Unit tests for flexible assignment - Simplified approach: - locationType:
 * 'device', 'shelf', or 'rack' only (no 'position' entity) -
 * positionCoordinate: optional text field for any location_type - No backward
 * compatibility with storage_position_id Following TDD: Write tests BEFORE
 * implementation
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceFlexibleAssignmentTest {

    @Mock
    private SampleItemDAO sampleItemDAO;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    private SampleItem testSampleItem;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack;
    private StorageRoom testRoom;

    @Before
    public void setUp() {
        // Create test hierarchy
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setCode("MAIN");
        testRoom.setName("Main Laboratory");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(10);
        testDevice.setCode("FRZ01");
        testDevice.setName("Freezer Unit 1");
        testDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId(20);
        testShelf.setLabel("Shelf-A");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId(30);
        testRack.setLabel("Rack R1");
        testRack.setRows(8);
        testRack.setColumns(12);
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);

        testSampleItem = new SampleItem();
        testSampleItem.setId("sample-item-123");
    }

    @Test
    public void testAssignSampleItemWithLocation_DeviceLevel_Valid() {
        // Setup
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class))).thenReturn(100);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(200);

        // Execute
        Map<String, Object> result = sampleStorageService.assignSampleItemWithLocation(
                "sample-item-123", "10", "device", null, "Test notes");

        // Verify
        assertNotNull(result);
        assertEquals("100", result.get("assignmentId"));
        assertNotNull(result.get("hierarchicalPath"));
        assertTrue(result.get("hierarchicalPath").toString().contains("Main Laboratory"));
        assertTrue(result.get("hierarchicalPath").toString().contains("Freezer Unit 1"));

        // Verify assignment was created with correct fields
        verify(sampleStorageAssignmentDAO).insert(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(10) && 
                   a.getLocationType().equals("device") &&
                   a.getPositionCoordinate() == null;
        }));
    }

    @Test
    public void testAssignSampleItemWithLocation_DeviceLevel_WithCoordinate_Valid() {
        // Setup
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class))).thenReturn(100);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(200);

        // Execute
        Map<String, Object> result = sampleStorageService.assignSampleItemWithLocation(
                "sample-item-123", "10", "device", "A5", "Test notes");

        // Verify
        assertNotNull(result);
        assertEquals("100", result.get("assignmentId"));
        assertTrue(result.get("hierarchicalPath").toString().contains("A5"));

        // Verify assignment includes position coordinate
        verify(sampleStorageAssignmentDAO).insert(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(10) && 
                   a.getLocationType().equals("device") &&
                   "A5".equals(a.getPositionCoordinate());
        }));
    }

    @Test
    public void testAssignSampleItemWithLocation_ShelfLevel_Valid() {
        // Setup
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(20, StorageShelf.class)).thenReturn(testShelf);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class))).thenReturn(100);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(200);

        // Execute
        Map<String, Object> result = sampleStorageService.assignSampleItemWithLocation(
                "sample-item-123", "20", "shelf", null, "Test notes");

        // Verify
        assertNotNull(result);
        assertEquals("100", result.get("assignmentId"));
        assertTrue(result.get("hierarchicalPath").toString().contains("Shelf-A"));

        // Verify assignment was created with correct fields
        verify(sampleStorageAssignmentDAO).insert(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(20) && 
                   a.getLocationType().equals("shelf");
        }));
    }

    @Test
    public void testAssignSampleItemWithLocation_RackLevel_Valid() {
        // Setup
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(30, StorageRack.class)).thenReturn(testRack);
        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class))).thenReturn(100);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(200);

        // Execute
        Map<String, Object> result = sampleStorageService.assignSampleItemWithLocation(
                "sample-item-123", "30", "rack", "B3", "Test notes");

        // Verify
        assertNotNull(result);
        assertEquals("100", result.get("assignmentId"));
        assertTrue(result.get("hierarchicalPath").toString().contains("Rack R1"));
        assertTrue(result.get("hierarchicalPath").toString().contains("B3"));

        // Verify assignment was created with correct fields
        verify(sampleStorageAssignmentDAO).insert(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(30) && 
                   a.getLocationType().equals("rack") &&
                   "B3".equals(a.getPositionCoordinate());
        }));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_MissingLocationId_ThrowsException() {
        // Execute - should throw exception
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", null, "device", null, "Test notes");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_MissingLocationType_ThrowsException() {
        // Execute - should throw exception
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", "10", null, null, "Test notes");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_InvalidLocationType_ThrowsException() {
        // Execute - should throw exception for invalid type
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", "10", "invalid", null, "Test notes");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_PositionType_ThrowsException() {
        // Execute - 'position' is not a valid locationType (position is just text
        // coordinate)
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", "10", "position", null, "Test notes");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_InactiveLocation_ThrowsException() {
        // Setup - inactive device
        testDevice.setActive(false);
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);

        // Execute - should throw exception
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", "10", "device", null, "Test notes");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testAssignSampleItemWithLocation_DeviceWithoutRoom_ThrowsException() {
        // Setup - device without parent room
        testDevice.setParentRoom(null);
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);

        // Execute - should throw exception (minimum 2 levels: room + device)
        sampleStorageService.assignSampleItemWithLocation("sample-item-123", "10", "device", null, "Test notes");
    }

    @Test
    public void testMoveSampleItemWithLocation_DeviceToShelf_Valid() {
        // Setup - existing assignment
        SampleStorageAssignment existingAssignment = new SampleStorageAssignment();
        existingAssignment.setId(50);
        existingAssignment.setSampleItem(testSampleItem);
        existingAssignment.setLocationId(10);
        existingAssignment.setLocationType("device");

        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(sampleStorageAssignmentDAO.findBySampleItemId("sample-item-123")).thenReturn(existingAssignment);
        when(storageLocationService.get(20, StorageShelf.class)).thenReturn(testShelf);
        when(sampleStorageAssignmentDAO.update(any(SampleStorageAssignment.class))).thenReturn(existingAssignment);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(300);

        // Execute
        String movementId = sampleStorageService.moveSampleItemWithLocation("sample-item-123", "20", "shelf", null,
                "Moving to shelf");

        // Verify
        assertNotNull(movementId);
        assertEquals("300", movementId);

        // Verify assignment was updated
        verify(sampleStorageAssignmentDAO).update(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(20) && a.getLocationType().equals("shelf");
        }));
    }

    @Test
    public void testMoveSampleItemWithLocation_DeviceToRack_WithCoordinate_Valid() {
        // Setup - existing assignment
        SampleStorageAssignment existingAssignment = new SampleStorageAssignment();
        existingAssignment.setId(50);
        existingAssignment.setSampleItem(testSampleItem);
        existingAssignment.setLocationId(10);
        existingAssignment.setLocationType("device");

        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(sampleStorageAssignmentDAO.findBySampleItemId("sample-item-123")).thenReturn(existingAssignment);
        when(storageLocationService.get(30, StorageRack.class)).thenReturn(testRack);
        when(sampleStorageAssignmentDAO.update(any(SampleStorageAssignment.class))).thenReturn(existingAssignment);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(300);

        // Execute
        String movementId = sampleStorageService.moveSampleItemWithLocation("sample-item-123", "30", "rack", "C7",
                "Moving to rack");

        // Verify
        assertNotNull(movementId);
        assertEquals("300", movementId);

        // Verify assignment was updated with coordinate
        verify(sampleStorageAssignmentDAO).update(argThat(assignment -> {
            SampleStorageAssignment a = (SampleStorageAssignment) assignment;
            return a.getLocationId().equals(30) && a.getLocationType().equals("rack")
                    && "C7".equals(a.getPositionCoordinate());
        }));
    }
}
