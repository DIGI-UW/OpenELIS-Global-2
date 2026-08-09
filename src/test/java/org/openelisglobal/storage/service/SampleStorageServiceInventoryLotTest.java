package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.storage.valueholder.StorageRoom;

/**
 * OGC-657: SampleStorageServiceImpl generalized to support InventoryLot
 * occupants alongside SampleItem, reusing the same assignment/movement audit
 * tables (discriminated by occupantType).
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceInventoryLotTest {

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Mock
    private StorageLocationService storageLocationService;

    @Mock
    private InventoryLotService inventoryLotService;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    private InventoryLot lot;
    private StorageRoom room;

    @Before
    public void setUp() {
        lot = new InventoryLot();
        lot.setId(42L);
        when(inventoryLotService.get(42L)).thenReturn(lot);

        room = new StorageRoom();
        room.setId(7);
        room.setName("Main Lab");
        room.setActive(true);
        when(storageLocationService.get(7, StorageRoom.class)).thenReturn(room);

        when(sampleStorageAssignmentDAO.insert(any(SampleStorageAssignment.class))).thenReturn(1);
        when(sampleStorageMovementDAO.insert(any(SampleStorageMovement.class))).thenReturn(1);
    }

    @Test
    public void assignInventoryLotWithLocation_createsAssignmentWithInventoryLotOccupant() {
        Map<String, Object> response = sampleStorageService.assignInventoryLotWithLocation("42", "7", "room", null,
                "initial receipt", "5");

        assertNotNull(response);
        assertEquals("Main Lab", response.get("hierarchicalPath"));

        ArgumentCaptor<SampleStorageAssignment> captor = ArgumentCaptor.forClass(SampleStorageAssignment.class);
        verify(sampleStorageAssignmentDAO).insert(captor.capture());
        SampleStorageAssignment saved = captor.getValue();
        assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, saved.getOccupantType());
        assertEquals(Long.valueOf(42L), saved.getInventoryLotId());
        assertEquals(null, saved.getSampleItemId());
        assertEquals(Integer.valueOf(7), saved.getLocationId());
        assertEquals("room", saved.getLocationType());
        assertEquals(Integer.valueOf(5), saved.getAssignedByUserId());

        ArgumentCaptor<SampleStorageMovement> movementCaptor = ArgumentCaptor.forClass(SampleStorageMovement.class);
        verify(sampleStorageMovementDAO).insert(movementCaptor.capture());
        assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, movementCaptor.getValue().getOccupantType());
        assertEquals(Long.valueOf(42L), movementCaptor.getValue().getInventoryLotId());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenAlreadyAssigned() {
        when(sampleStorageAssignmentDAO.findByInventoryLotId(42L))
                .thenReturn(existingAssignment(7, "room", null));

        sampleStorageService.assignInventoryLotWithLocation("42", "7", "room", null, null, "5");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLotNotFound() {
        // Matches the real InventoryLotService.get() contract: throws rather than
        // returning null when the id doesn't exist.
        when(inventoryLotService.get(999L)).thenThrow(new org.hibernate.ObjectNotFoundException(999L,
                "org.openelisglobal.inventory.valueholder.InventoryLot"));

        sampleStorageService.assignInventoryLotWithLocation("999", "7", "room", null, null, "5");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationInactive() {
        room.setActive(false);

        sampleStorageService.assignInventoryLotWithLocation("42", "7", "room", null, null, "5");
    }

    @Test
    public void moveInventoryLotWithLocation_updatesExistingAssignmentAndRecordsPreviousLocation() {
        SampleStorageAssignment existing = existingAssignment(3, "room", "A1");
        when(sampleStorageAssignmentDAO.findByInventoryLotId(42L)).thenReturn(existing);

        String movementId = sampleStorageService.moveInventoryLotWithLocation("42", "7", "room", "B2",
                "freezer failure", null, "5");

        assertNotNull(movementId);
        verify(sampleStorageAssignmentDAO).update(existing);
        assertEquals(Integer.valueOf(7), existing.getLocationId());
        assertEquals("B2", existing.getPositionCoordinate());

        ArgumentCaptor<SampleStorageMovement> movementCaptor = ArgumentCaptor.forClass(SampleStorageMovement.class);
        verify(sampleStorageMovementDAO).insert(movementCaptor.capture());
        SampleStorageMovement movement = movementCaptor.getValue();
        assertEquals(Integer.valueOf(3), movement.getPreviousLocationId());
        assertEquals("room", movement.getPreviousLocationType());
        assertEquals("A1", movement.getPreviousPositionCoordinate());
        assertEquals(Integer.valueOf(7), movement.getNewLocationId());
        assertEquals("B2", movement.getNewPositionCoordinate());
        assertEquals("freezer failure", movement.getReason());
    }

    @Test
    public void moveInventoryLotWithLocation_createsAssignmentWhenNoneExisted() {
        when(sampleStorageAssignmentDAO.findByInventoryLotId(42L)).thenReturn(null);

        sampleStorageService.moveInventoryLotWithLocation("42", "7", "room", null, "first move", null, "5");

        verify(sampleStorageAssignmentDAO, never()).update(any());
        verify(sampleStorageAssignmentDAO, times(1)).insert(any(SampleStorageAssignment.class));
    }

    @Test
    public void getInventoryLotLocation_returnsEmptyMapWhenUnassigned() {
        when(sampleStorageAssignmentDAO.findByInventoryLotId(42L)).thenReturn(null);

        Map<String, Object> location = sampleStorageService.getInventoryLotLocation("42");

        assertTrue(location.isEmpty());
    }

    @Test
    public void getInventoryLotLocation_returnsHierarchicalPathWhenAssigned() {
        when(sampleStorageAssignmentDAO.findByInventoryLotId(42L))
                .thenReturn(existingAssignment(7, "room", null));

        Map<String, Object> location = sampleStorageService.getInventoryLotLocation("42");

        assertEquals("Main Lab", location.get("hierarchicalPath"));
        assertEquals("42", location.get("inventoryLotId"));
    }

    @Test
    public void getLocationsForInventoryLots_returnsEmptyMapForEmptyInput() {
        Map<String, Map<String, Object>> result = sampleStorageService
                .getLocationsForInventoryLots(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void getLocationsForInventoryLots_keysResultByLotId() {
        SampleStorageAssignment assignmentA = existingAssignment(7, "room", null);
        assignmentA.setInventoryLotId(42L);
        SampleStorageAssignment assignmentB = existingAssignment(7, "room", null);
        assignmentB.setInventoryLotId(99L);
        when(sampleStorageAssignmentDAO.findByInventoryLotIds(Arrays.asList(42L, 99L)))
                .thenReturn(Arrays.asList(assignmentA, assignmentB));

        Map<String, Map<String, Object>> result = sampleStorageService
                .getLocationsForInventoryLots(Arrays.asList(42L, 99L));

        assertEquals(2, result.size());
        assertEquals("Main Lab", result.get("42").get("hierarchicalPath"));
        assertEquals("Main Lab", result.get("99").get("hierarchicalPath"));
    }

    private SampleStorageAssignment existingAssignment(Integer locationId, String locationType,
            String positionCoordinate) {
        SampleStorageAssignment assignment = new SampleStorageAssignment();
        assignment.setId(1);
        assignment.setOccupantType(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT);
        assignment.setInventoryLotId(42L);
        assignment.setLocationId(locationId);
        assignment.setLocationType(locationType);
        assignment.setPositionCoordinate(positionCoordinate);
        assignment.setAssignedByUserId(5);
        return assignment;
    }
}
