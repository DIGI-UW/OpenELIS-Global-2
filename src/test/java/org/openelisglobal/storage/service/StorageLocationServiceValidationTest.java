package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Unit tests for the validation, uniqueness and error-handling branches of
 * {@link StorageLocationServiceImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceValidationTest {

    @Mock
    private StorageRoomService storageRoomService;

    @Mock
    private StorageDeviceService storageDeviceService;

    @Mock
    private StorageShelfService storageShelfService;

    @Mock
    private StorageRackService storageRackService;

    @Mock
    private StorageBoxService storageBoxService;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @InjectMocks
    private StorageLocationServiceImpl storageLocationService;

    // ------------------------------------------------------------------
    // validateLocationActive
    // ------------------------------------------------------------------

    private StorageBox boxWithHierarchy(Boolean roomActive, Boolean deviceActive, Boolean shelfActive,
            Boolean rackActive) {
        StorageRoom room = new StorageRoom();
        room.setId(1);
        room.setActive(roomActive);

        StorageDevice device = new StorageDevice();
        device.setId(2);
        device.setActive(deviceActive);
        device.setParentRoom(room);

        StorageShelf shelf = new StorageShelf();
        shelf.setId(3);
        shelf.setActive(shelfActive);
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setId(4);
        rack.setActive(rackActive);
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setId(5);
        box.setParentRack(rack);
        return box;
    }

    @Test
    public void testValidateLocationActive_NullBox_ReturnsFalse() {
        assertFalse("A null box cannot be active", storageLocationService.validateLocationActive(null));
    }

    @Test
    public void testValidateLocationActive_NullParentRack_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, true);
        box.setParentRack(null);

        assertFalse("A box without a parent rack cannot be active", storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullShelf_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, true);
        box.getParentRack().setParentShelf(null);

        assertFalse("A box whose rack has no shelf cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullDevice_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, true);
        box.getParentRack().getParentShelf().setParentDevice(null);

        assertFalse("A box whose shelf has no device cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullRoom_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, true);
        box.getParentRack().getParentShelf().getParentDevice().setParentRoom(null);

        assertFalse("A box whose device has no room cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_InactiveRoom_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(false, true, true, true);

        assertFalse("A box in an inactive room cannot be active", storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullRoomActive_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(null, true, true, true);

        assertFalse("A room with a null active flag cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_InactiveDevice_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, false, true, true);

        assertFalse("A box in an inactive device cannot be active", storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullDeviceActive_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, null, true, true);

        assertFalse("A device with a null active flag cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_InactiveShelf_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, false, true);

        assertFalse("A box on an inactive shelf cannot be active", storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullShelfActive_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, null, true);

        assertFalse("A shelf with a null active flag cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_InactiveRack_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, false);

        assertFalse("A box on an inactive rack cannot be active", storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_NullRackActive_ReturnsFalse() {
        StorageBox box = boxWithHierarchy(true, true, true, null);

        assertFalse("A rack with a null active flag cannot be active",
                storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_AllAncestorsActive_ReturnsTrue() {
        StorageBox box = boxWithHierarchy(true, true, true, true);

        assertTrue("A box with an active room, device, shelf and rack is active",
                storageLocationService.validateLocationActive(box));
    }

    // ------------------------------------------------------------------
    // isCodeUniqueForRoom
    // ------------------------------------------------------------------

    @Test
    public void testIsCodeUniqueForRoom_NullCode_ReturnsTrue() {
        assertTrue("Null codes are allowed", storageLocationService.isCodeUniqueForRoom(null, null));
    }

    @Test
    public void testIsCodeUniqueForRoom_EmptyCode_ReturnsTrue() {
        assertTrue("Empty codes are allowed", storageLocationService.isCodeUniqueForRoom("", null));
    }

    @Test
    public void testIsCodeUniqueForRoom_WhitespaceCode_ReturnsTrue() {
        assertTrue("Whitespace-only codes are allowed", storageLocationService.isCodeUniqueForRoom("   ", null));
    }

    @Test
    public void testIsCodeUniqueForRoom_TrimsCodeBeforeLookup() {
        when(storageRoomService.findByCode("ABC")).thenReturn(null);

        assertTrue(storageLocationService.isCodeUniqueForRoom("  ABC  ", null));
        verify(storageRoomService).findByCode("ABC");
    }

    @Test
    public void testIsCodeUniqueForRoom_DatabaseMiss_ReturnsTrue() {
        when(storageRoomService.findByCode("NEW")).thenReturn(null);

        assertTrue("A code not present in the database is unique",
                storageLocationService.isCodeUniqueForRoom("NEW", null));
    }

    @Test
    public void testIsCodeUniqueForRoom_ExistingDifferentRoom_ReturnsFalse() {
        StorageRoom existing = new StorageRoom();
        existing.setId(5);
        when(storageRoomService.findByCode("DUP")).thenReturn(existing);

        assertFalse("A code used by another room is not unique", storageLocationService.isCodeUniqueForRoom("DUP", 6));
    }

    @Test
    public void testIsCodeUniqueForRoom_SelfMatch_ReturnsTrue() {
        StorageRoom existing = new StorageRoom();
        existing.setId(5);
        when(storageRoomService.findByCode("SELF")).thenReturn(existing);

        assertTrue("A code matching the excluded room is unique",
                storageLocationService.isCodeUniqueForRoom("SELF", 5));
    }

    @Test
    public void testIsCodeUniqueForRoom_ExistingWithNullExcludeId_ReturnsFalse() {
        StorageRoom existing = new StorageRoom();
        existing.setId(5);
        when(storageRoomService.findByCode("DUP")).thenReturn(existing);

        assertFalse("An existing code with no exclude id is not unique",
                storageLocationService.isCodeUniqueForRoom("DUP", null));
    }

    @Test
    public void testIsCodeUniqueForDevice_DatabaseMiss_ReturnsTrue() {
        when(storageDeviceService.findByCode("DEV")).thenReturn(null);

        assertTrue(storageLocationService.isCodeUniqueForDevice("DEV", null));
    }

    @Test
    public void testIsCodeUniqueForDevice_ExistingDifferentId_ReturnsFalse() {
        StorageDevice existing = new StorageDevice();
        existing.setId(10);
        when(storageDeviceService.findByCode("DEV")).thenReturn(existing);

        assertFalse(storageLocationService.isCodeUniqueForDevice("DEV", 11));
    }

    @Test
    public void testIsCodeUniqueForDevice_SelfMatch_ReturnsTrue() {
        StorageDevice existing = new StorageDevice();
        existing.setId(10);
        when(storageDeviceService.findByCode("DEV")).thenReturn(existing);

        assertTrue(storageLocationService.isCodeUniqueForDevice("DEV", 10));
    }

    @Test
    public void testIsCodeUniqueForShelf_DatabaseMiss_ReturnsTrue() {
        when(storageShelfService.findByCode("SHF")).thenReturn(null);

        assertTrue(storageLocationService.isCodeUniqueForShelf("SHF", null));
    }

    @Test
    public void testIsCodeUniqueForShelf_ExistingDifferentId_ReturnsFalse() {
        StorageShelf existing = new StorageShelf();
        existing.setId(20);
        when(storageShelfService.findByCode("SHF")).thenReturn(existing);

        assertFalse(storageLocationService.isCodeUniqueForShelf("SHF", 21));
    }

    @Test
    public void testIsCodeUniqueForShelf_SelfMatch_ReturnsTrue() {
        StorageShelf existing = new StorageShelf();
        existing.setId(20);
        when(storageShelfService.findByCode("SHF")).thenReturn(existing);

        assertTrue(storageLocationService.isCodeUniqueForShelf("SHF", 20));
    }

    @Test
    public void testIsCodeUniqueForRack_DatabaseMiss_ReturnsTrue() {
        when(storageRackService.findByCode("RCK")).thenReturn(null);

        assertTrue(storageLocationService.isCodeUniqueForRack("RCK", null));
    }

    @Test
    public void testIsCodeUniqueForRack_ExistingDifferentId_ReturnsFalse() {
        StorageRack existing = new StorageRack();
        existing.setId(30);
        when(storageRackService.findByCode("RCK")).thenReturn(existing);

        assertFalse(storageLocationService.isCodeUniqueForRack("RCK", 31));
    }

    @Test
    public void testIsCodeUniqueForRack_SelfMatch_ReturnsTrue() {
        StorageRack existing = new StorageRack();
        existing.setId(30);
        when(storageRackService.findByCode("RCK")).thenReturn(existing);

        assertTrue(storageLocationService.isCodeUniqueForRack("RCK", 30));
    }

    // ------------------------------------------------------------------
    // insert
    // ------------------------------------------------------------------

    @Test
    public void testInsert_UnsupportedEntityType_Throws() {
        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.insert(new Object()));

        assertTrue(ex.getMessage().contains("Unsupported entity type for insert"));
    }

    @Test
    public void testInsert_RoomWithDuplicateCode_Throws() {
        StorageRoom existing = new StorageRoom();
        existing.setId(1);
        when(storageRoomService.findByCode("R1")).thenReturn(existing);

        StorageRoom room = new StorageRoom();
        room.setCode("R1");

        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class, () -> storageLocationService.insert(room));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    public void testInsert_BoxWithNegativeDimensions_Throws() {
        StorageBox box = new StorageBox();
        box.setRows(-1);
        box.setColumns(10);

        assertThrows(IllegalArgumentException.class, () -> storageLocationService.insert(box));
    }

    @Test
    public void testInsert_BoxWithNullDimensions_Throws() {
        StorageBox box = new StorageBox();

        assertThrows(IllegalArgumentException.class, () -> storageLocationService.insert(box));
    }

    // ------------------------------------------------------------------
    // update
    // ------------------------------------------------------------------

    @Test
    public void testUpdate_UnsupportedEntityType_Throws() {
        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.update(new Object()));

        assertTrue(ex.getMessage().contains("Unsupported entity type for update"));
    }

    @Test
    public void testUpdate_RoomNotFound_Throws() {
        when(storageRoomService.getRoom(99)).thenReturn(Optional.empty());

        StorageRoom room = new StorageRoom();
        room.setId(99);

        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.update(room));
        assertTrue(ex.getMessage().contains("Room not found"));
    }

    @Test
    public void testUpdate_RoomCopiesEditableFieldsAndPersists() {
        StorageRoom existing = new StorageRoom();
        existing.setId(1);
        existing.setName("Old Name");
        when(storageRoomService.getRoom(1)).thenReturn(Optional.of(existing));

        StorageRoom incoming = new StorageRoom();
        incoming.setId(1);
        incoming.setName("New Name");
        incoming.setDescription("New Description");
        incoming.setActive(true);

        assertNull("update returns null", storageLocationService.update(incoming));

        assertEquals("Name should be updated", "New Name", existing.getName());
        assertEquals("Description should be updated", "New Description", existing.getDescription());
        assertTrue("Active flag should be updated", existing.getActive());
        verify(storageRoomService).update(existing);
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    public void testDelete_UnsupportedEntityType_Throws() {
        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.delete(new Object()));

        assertTrue(ex.getMessage().contains("Unsupported entity type for delete"));
    }

    @Test
    public void testDelete_RoomNotFound_Throws() {
        when(storageRoomService.getRoom(7)).thenReturn(Optional.empty());

        StorageRoom room = new StorageRoom();
        room.setId(7);

        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.delete(room));
        assertTrue(ex.getMessage().contains("Room not found"));
    }

    // ------------------------------------------------------------------
    // get
    // ------------------------------------------------------------------

    @Test
    public void testGet_UnsupportedClass_Throws() {
        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> storageLocationService.get(1, String.class));

        assertTrue(ex.getMessage().contains("Unsupported entity class"));
    }

    @Test
    public void testGet_Room_ReturnsRoom() {
        StorageRoom room = new StorageRoom();
        room.setId(1);
        when(storageRoomService.getRoom(1)).thenReturn(Optional.of(room));

        assertSame(room, storageLocationService.get(1, StorageRoom.class));
    }

    @Test
    public void testGet_RoomNotFound_ReturnsNull() {
        when(storageRoomService.getRoom(1)).thenReturn(Optional.empty());

        assertNull(storageLocationService.get(1, StorageRoom.class));
    }
}
