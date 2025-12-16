package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.dao.SampleRoutingDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.notebook.valueholder.StorageCondition;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Unit tests for Post-Analysis Storage Service. Per T110: Tests storage
 * assignment with retention period and conditions per US6 requirements.
 *
 * <p>
 * US6 Goal: Store processed samples under defined conditions with tracked
 * location and retention period using existing SampleStorageService.
 *
 * <p>
 * Independent Test: Select 10 samples, assign to storage location with
 * retention period (e.g., "Frozen, -80°C, 5 years"), verify
 * SampleStorageAssignment and SampleStorageMovement records created with
 * correct conditions.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PostAnalysisStorageServiceTest {

    @Mock
    private SampleRoutingDAO routingDAO;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private SampleStorageAssignmentDAO storageAssignmentDAO;

    @Mock
    private SampleStorageMovementDAO storageMovementDAO;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private SampleRoutingServiceImpl service;

    private NoteBook testNotebook;
    private SystemUser testUser;

    @Before
    public void setUp() {
        // Set up test notebook
        testNotebook = new NoteBook();
        testNotebook.setId(1);
        testNotebook.setTitle("Test Immunology Workflow");

        // Set up test user
        testUser = mock(SystemUser.class);
        when(testUser.getId()).thenReturn("1");
    }

    /**
     * T114a: Storage condition types enum. Verifies storage conditions are properly
     * defined.
     */
    @Test
    public void testStorageConditionTypes_AllTypesExist() {
        // Verify all required storage condition types exist
        StorageCondition[] conditions = StorageCondition.values();

        // Should have at least 4 basic types
        assertTrue("Should have multiple storage conditions", conditions.length >= 4);

        // Verify specific required types
        assertNotNull("REFRIGERATED should exist", StorageCondition.valueOf("REFRIGERATED"));
        assertNotNull("FROZEN_MINUS20 should exist", StorageCondition.valueOf("FROZEN_MINUS20"));
        assertNotNull("FROZEN_MINUS80 should exist", StorageCondition.valueOf("FROZEN_MINUS80"));
        assertNotNull("ROOM_TEMP should exist", StorageCondition.valueOf("ROOM_TEMP"));
    }

    @Test
    public void testStorageConditionTypes_DisplayNames() {
        assertEquals("Refrigerated (2-8°C)", StorageCondition.REFRIGERATED.getDisplayName());
        assertEquals("Frozen (-20°C)", StorageCondition.FROZEN_MINUS20.getDisplayName());
        assertEquals("Frozen (-80°C)", StorageCondition.FROZEN_MINUS80.getDisplayName());
        assertEquals("Room Temperature (15-25°C)", StorageCondition.ROOM_TEMP.getDisplayName());
    }

  /** T112: Route to storage with conditions and retention period. */
  @Test
  public void testRouteToStorage_ValidInputs_CreatesRoutingWithStorageAssignment() {
    // Arrange
    when(noteBookService.get(1)).thenReturn(testNotebook);
    when(systemUserService.get("1")).thenReturn(testUser);
    when(routingDAO.getByNotebookIdAndSampleItemId(1, 50)).thenReturn(null);
    when(routingDAO.insert(any(SampleRouting.class))).thenReturn(1);

    // Mock storage assignment that will be returned from DAO
    SampleStorageAssignment mockAssignment = new SampleStorageAssignment();
    mockAssignment.setId(100);
    when(storageAssignmentDAO.get(100)).thenReturn(java.util.Optional.of(mockAssignment));

    // Mock storage service response
    when(sampleStorageService.assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(
            Map.of(
                "assignmentId",
                "100",
                "hierarchicalPath",
                "Cold Room > Freezer 1 > Shelf A > Rack 1 > Box 1",
                "assignedDate",
                "2025-12-09 10:00:00"));

    // Act
    SampleRouting routing =
        service.routeToStorage(
            1, // notebookId
            50, // sampleItemId
            "1", // locationId (rack)
            "rack", // locationType
            "A1", // positionCoordinate
            StorageCondition.FROZEN_MINUS80, // condition
            5, // retentionYears
            "1" // userId
            );

    // Assert
    assertNotNull("Routing should be created", routing);
    assertEquals(
        "Destination should be STORAGE", DestinationType.STORAGE, routing.getDestinationType());
    assertNotNull("Storage assignment should be set", routing.getStorageAssignment());
  }

  @Test
  public void testRouteToStorage_WithRetentionPeriod_SetsExpiryDate() {
    // Arrange
    when(noteBookService.get(1)).thenReturn(testNotebook);
    when(systemUserService.get("1")).thenReturn(testUser);
    when(routingDAO.getByNotebookIdAndSampleItemId(1, 50)).thenReturn(null);
    when(routingDAO.insert(any(SampleRouting.class))).thenReturn(1);

    when(sampleStorageService.assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Map.of("assignmentId", "100", "hierarchicalPath", "Test Location"));

    // Act - 5 years retention
    SampleRouting routing =
        service.routeToStorage(1, 50, "1", "rack", "B2", StorageCondition.FROZEN_MINUS80, 5, "1");

    // Assert
    assertNotNull("Routing should be created", routing);

    // Verify notes contain retention info
    ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
    verify(sampleStorageService)
        .assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), anyString(), notesCaptor.capture());

    String notes = notesCaptor.getValue();
    assertTrue("Notes should contain storage condition", notes.contains("FROZEN_MINUS80"));
    assertTrue("Notes should contain retention period", notes.contains("5 years"));
  }

  /** T113: Bulk storage assignment with audit trail. */
  @Test
  public void testBulkRouteToStorage_10Samples_AllAssigned() {
    // Arrange - per US6 independent test: "Select 10 samples"
    when(noteBookService.get(1)).thenReturn(testNotebook);
    when(systemUserService.get("1")).thenReturn(testUser);
    when(routingDAO.getByNotebookIdAndSampleItemId(anyInt(), anyInt())).thenReturn(null);
    when(routingDAO.insert(any(SampleRouting.class))).thenReturn(1);

    // Note: bulkRouteToStorage passes null for positionCoordinate, so use any() matcher
    when(sampleStorageService.assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(Map.of("assignmentId", "100", "hierarchicalPath", "Test Location"));

    // Act - route 10 samples
    List<Integer> sampleIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    int routed =
        service.bulkRouteToStorage(
            1, // notebookId
            sampleIds,
            "1", // locationId (rack)
            "rack", // locationType
            StorageCondition.FROZEN_MINUS80,
            5, // retentionYears
            "1" // userId
            );

    // Assert
    assertEquals("All 10 samples should be routed", 10, routed);
    verify(routingDAO, times(10)).insert(any(SampleRouting.class));
    verify(sampleStorageService, times(10))
        .assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(), anyString());
  }

    /** T114b: Retention period calculation - verify expiry date. */
    @Test
    public void testCalculateExpiryDate_5Years_ReturnsCorrectDate() {
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = service.calculateExpiryDate(5);

        assertEquals("Expiry should be 5 years from today", today.plusYears(5), expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_2Years_ReturnsCorrectDate() {
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = service.calculateExpiryDate(2);

        assertEquals("Expiry should be 2 years from today", today.plusYears(2), expiryDate);
    }

  /** T113: Verify SampleStorageMovement audit trail is created. */
  @Test
  public void testRouteToStorage_CreatesAuditTrail() {
    // Arrange
    when(noteBookService.get(1)).thenReturn(testNotebook);
    when(systemUserService.get("1")).thenReturn(testUser);
    when(routingDAO.getByNotebookIdAndSampleItemId(1, 50)).thenReturn(null);
    when(routingDAO.insert(any(SampleRouting.class))).thenReturn(1);

    // SampleStorageService.assignSampleItemWithLocation creates movement internally
    when(sampleStorageService.assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Map.of("assignmentId", "100", "hierarchicalPath", "Test Location"));

    // Act
    SampleRouting routing =
        service.routeToStorage(1, 50, "1", "rack", "C3", StorageCondition.REFRIGERATED, 1, "1");

    // Assert - movement is created by SampleStorageService
    assertNotNull("Routing should be created", routing);
    verify(sampleStorageService)
        .assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), anyString(), anyString());
  }

    /** Test storage condition descriptions for UI display. */
    @Test
    public void testStorageCondition_TemperatureRanges() {
        assertEquals("2-8°C", StorageCondition.REFRIGERATED.getTemperatureRange());
        assertEquals("-20°C", StorageCondition.FROZEN_MINUS20.getTemperatureRange());
        assertEquals("-80°C", StorageCondition.FROZEN_MINUS80.getTemperatureRange());
        assertEquals("15-25°C", StorageCondition.ROOM_TEMP.getTemperatureRange());
    }

  /** Verify routing validates storage location exists. */
  @Test(expected = IllegalArgumentException.class)
  public void testRouteToStorage_InvalidLocation_ThrowsException() {
    // Arrange
    when(noteBookService.get(1)).thenReturn(testNotebook);
    when(systemUserService.get("1")).thenReturn(testUser);
    when(routingDAO.getByNotebookIdAndSampleItemId(1, 50)).thenReturn(null);

    // Storage service returns error - our implementation wraps RuntimeException in
    // IllegalArgumentException
    when(sampleStorageService.assignSampleItemWithLocation(
            anyString(), anyString(), anyString(), any(), anyString()))
        .thenThrow(new RuntimeException("Location not found"));

    // Act - should throw IllegalArgumentException (wrapped from RuntimeException)
    service.routeToStorage(
        1,
        50,
        "999", // invalid location
        "rack",
        null,
        StorageCondition.REFRIGERATED,
        1,
        "1");
  }
}
