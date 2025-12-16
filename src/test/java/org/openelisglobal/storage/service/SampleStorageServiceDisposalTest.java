package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.valueholder.*;

/**
 * Unit tests for SampleStorageService disposal functionality (OGC-73) Tests
 * that disposal correctly sets numeric status ID and clears storage assignment
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceDisposalTest {

    @Mock
    private SampleItemDAO sampleItemDAO;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Mock
    private StorageLocationService storageLocationService;

    @Mock
    private IStatusService statusService;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    private SampleItem testSampleItem;
    private SampleStorageAssignment testAssignment;
    private StorageDevice testDevice;
    private StorageRoom testRoom;
    private static final String DISPOSED_STATUS_ID = "24";

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

        testSampleItem = new SampleItem();
        testSampleItem.setId("sample-item-123");
        testSampleItem.setStatusId("1"); // Active status

        testAssignment = new SampleStorageAssignment();
        testAssignment.setId(1);
        testAssignment.setSampleItem(testSampleItem);
        testAssignment.setLocationId(10);
        testAssignment.setLocationType("device");
        testAssignment.setPositionCoordinate("A1");

        // Mock StatusService - use lenient for common stubs that may not be used in all
        // tests
        lenient().when(statusService.getStatusID(SampleStatus.Disposed)).thenReturn(DISPOSED_STATUS_ID);
        // Default: non-disposed statuses don't match Disposed status
        lenient().when(statusService.matches(anyString(), eq(SampleStatus.Disposed))).thenReturn(false);
    }

    @Test
    public void testDisposeSampleItem_SetsCorrectNumericStatusId() {
        // Arrange
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(sampleStorageAssignmentDAO.findBySampleItemId("sample-item-123")).thenReturn(null);
        // No movement record created when there's no previous assignment (no previous location)

        // Act
        Map<String, Object> result = sampleStorageService.disposeSampleItem("sample-item-123", "expired", "autoclave",
                "Test disposal");

        // Assert
        verify(sampleItemDAO).update(testSampleItem);
        assertEquals("Status ID should be set to disposed status ID", DISPOSED_STATUS_ID, testSampleItem.getStatusId());
        assertEquals("sample-item-123", result.get("sampleItemId"));
        assertEquals("disposed", result.get("status"));
        // Verify no movement record was created (no previous location to track)
        verify(sampleStorageMovementDAO, never()).insert(any(SampleStorageMovement.class));
    }

    @Test
    public void testDisposeSampleItem_ClearsStorageAssignment() {
        // Arrange
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(sampleStorageAssignmentDAO.findBySampleItemId("sample-item-123")).thenReturn(testAssignment);
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(1);

        // Act
        sampleStorageService.disposeSampleItem("sample-item-123", "expired", "autoclave", null);

        // Assert
        verify(sampleStorageAssignmentDAO).delete(testAssignment);
        verify(sampleItemDAO).update(testSampleItem);
    }

    @Test
    public void testDisposeSampleItem_CreatesMovementAuditRecord() {
        // Arrange
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        when(sampleStorageAssignmentDAO.findBySampleItemId("sample-item-123")).thenReturn(testAssignment);
        when(storageLocationService.get(10, StorageDevice.class)).thenReturn(testDevice);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(1);

        // Act
        sampleStorageService.disposeSampleItem("sample-item-123", "expired", "autoclave", "Test notes");

        // Assert
        verify(sampleStorageMovementDAO).insert(any(SampleStorageMovement.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testDisposeSampleItem_AlreadyDisposed_ThrowsException() {
        // Arrange
        testSampleItem.setStatusId(DISPOSED_STATUS_ID);
        when(sampleItemDAO.get("sample-item-123")).thenReturn(Optional.of(testSampleItem));
        // Override lenient stub for this specific test - disposed status should match
        when(statusService.matches(eq(DISPOSED_STATUS_ID), eq(SampleStatus.Disposed))).thenReturn(true);

        // Act
        sampleStorageService.disposeSampleItem("sample-item-123", "expired", "autoclave", null);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testDisposeSampleItem_InvalidSampleId_ThrowsException() {
        // Arrange
        when(sampleItemDAO.get("invalid-id")).thenReturn(Optional.empty());

        // Act
        sampleStorageService.disposeSampleItem("invalid-id", "expired", "autoclave", null);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testDisposeSampleItem_MissingReason_ThrowsException() {
        // Act
        sampleStorageService.disposeSampleItem("sample-item-123", null, "autoclave", null);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testDisposeSampleItem_MissingMethod_ThrowsException() {
        // Act
        sampleStorageService.disposeSampleItem("sample-item-123", "expired", null, null);
    }
}
