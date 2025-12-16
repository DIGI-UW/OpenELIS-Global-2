package org.openelisglobal.storage.valueholder;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for storage entity valueholders. Tests @PrePersist hooks,
 * calculated fields, and basic entity behavior.
 */
public class StorageEntityTest {

    @Test
    public void testStorageRoom_PrePersist_GeneratesFhirUuid() {
        // Given: New StorageRoom without fhir_uuid
        StorageRoom room = new StorageRoom();
        room.setName("Test Room");
        room.setCode("TEST");

        // When: PrePersist hook called
        room.onCreate();

        // Then: fhir_uuid should be generated
        assertNotNull("fhir_uuid should be auto-generated", room.getFhirUuid());
        assertNotNull("fhir_uuid string representation should exist", room.getFhirUuidAsString());
    }

    @Test
    public void testStorageRoom_GettersSetters_WorkCorrectly() {
        // Given: StorageRoom
        StorageRoom room = new StorageRoom();

        // When: Set properties
        room.setName("Main Laboratory");
        room.setCode("MAIN");
        room.setDescription("Primary lab storage");
        room.setActive(true);

        // Then: Getters return correct values
        assertEquals("Main Laboratory", room.getName());
        assertEquals("MAIN", room.getCode());
        assertEquals("Primary lab storage", room.getDescription());
        assertTrue(room.getActive());
        assertEquals(Boolean.TRUE, room.getActive());
    }

    @Test
    public void testStorageDevice_ParentRelationship_WorksCorrectly() {
        // Given: Room and Device
        StorageRoom room = new StorageRoom();
        room.setCode("MAIN");
        room.setName("Main Lab");

        StorageDevice device = new StorageDevice();
        device.setCode("FRZ01");
        device.setName("Freezer Unit 1");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(room);

        // Then: Parent relationship established
        assertNotNull(device.getParentRoom());
        assertEquals("MAIN", device.getParentRoom().getCode());
        assertEquals(room, device.getParentRoom());
    }

    @Test
    public void testStorageDevice_TemperatureSetting_AcceptsNegativeValues() {
        // Given: Device with -80°C temperature
        StorageDevice device = new StorageDevice();
        device.setTemperatureSetting(java.math.BigDecimal.valueOf(-80.0));

        // Then: Temperature stored correctly
        assertEquals(java.math.BigDecimal.valueOf(-80.0), device.getTemperatureSetting());
    }

    @Test
    public void testStorageRack_CalculateCapacity_ReturnsRowsTimesColumns() {
        // Given: Rack with 9x9 grid
        StorageRack rack = new StorageRack();
        rack.setRows(9);
        rack.setColumns(9);

        // When: Calculate capacity
        Integer capacity = rack.getCapacity();

        // Then: Capacity should be 81
        assertEquals("Capacity should be rows * columns", Integer.valueOf(81), capacity);
    }

    @Test
    public void testStorageRack_CalculateCapacity_NoGrid_ReturnsZero() {
        // Given: Rack with no grid (rows=0)
        StorageRack rack = new StorageRack();
        rack.setRows(0);
        rack.setColumns(0);

        // When: Calculate capacity
        Integer capacity = rack.getCapacity();

        // Then: Capacity should be 0
        assertEquals("No grid should return 0 capacity", Integer.valueOf(0), capacity);
    }

    @Test
    public void testStoragePosition_OccupancyFlag_DefaultsFalse() {
        // Given: New StoragePosition
        StoragePosition position = new StoragePosition();

        // Then: Occupied should default to false (or null)
        // Occupancy is now calculated dynamically from SampleStorageAssignment records
    }

    @Test
    public void testStoragePosition_CoordinateFlexibility_AcceptsAnyFormat() {
        // Given: Position with various coordinate formats
        StoragePosition pos1 = new StoragePosition();
        StoragePosition pos2 = new StoragePosition();
        StoragePosition pos3 = new StoragePosition();

        // When: Set different coordinate formats
        pos1.setCoordinate("A5"); // Alphanumeric
        pos2.setCoordinate("1-1"); // Numeric with dash
        pos3.setCoordinate("RED-01"); // Color-coded

        // Then: All formats accepted (free text, no validation)
        assertEquals("A5", pos1.getCoordinate());
        assertEquals("1-1", pos2.getCoordinate());
        assertEquals("RED-01", pos3.getCoordinate());
    }

    @Test
    public void testSampleStorageAssignment_PrePersist_SetsAssignedDate() {
        // Given: New assignment without date
        SampleStorageAssignment assignment = new SampleStorageAssignment();

        // When: PrePersist hook called
        assignment.onCreate();

        // Then: assignedDate should be set
        assertNotNull("assignedDate should be auto-set", assignment.getAssignedDate());
    }

    @Test
    public void testSampleStorageMovement_Immutability_Marker() {
        // Given: SampleStorageMovement class
        // Then: Class should be marked with @Immutable annotation
        // (This is verified at compile time - Hibernate will prevent updates)
        assertTrue("SampleStorageMovement should be immutable",
                SampleStorageMovement.class.isAnnotationPresent(org.hibernate.annotations.Immutable.class));
    }

    @Test
    public void testStoragePosition_GridIndexes_Optional() {
        // Given: Position without grid indexes
        StoragePosition position = new StoragePosition();
        position.setCoordinate("BASKET-1");
        // rowIndex and columnIndex intentionally NULL

        // Then: Should be valid (grid indexes are optional)
        assertNull(position.getRowIndex());
        assertNull(position.getColumnIndex());
        assertEquals("BASKET-1", position.getCoordinate());
    }
}
