package org.openelisglobal.storage.fhir;

import static org.junit.Assert.*;

import java.util.UUID;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.storage.valueholder.*;

public class StorageLocationFhirTransformTest {

    private StorageLocationFhirTransform transformer;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack;
    private StoragePosition testPosition;

    @Before
    public void setup() {
        // Initialize transformer
        transformer = new StorageLocationFhirTransform();

        // Setup test data
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setFhirUuid(UUID.randomUUID());
        testRoom.setCode("MAIN");
        testRoom.setName("Main Laboratory");
        testRoom.setDescription("Primary laboratory storage facility");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(2);
        testDevice.setFhirUuid(UUID.randomUUID());
        testDevice.setCode("FRZ01");
        testDevice.setName("Freezer Unit 1");
        testDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        testDevice.setTemperatureSetting(java.math.BigDecimal.valueOf(-80.0));
        testDevice.setActive(true);
        testDevice.setParentRoom(testRoom);

        testShelf = new StorageShelf();
        testShelf.setId(3);
        testShelf.setFhirUuid(UUID.randomUUID());
        testShelf.setLabel("SHA");
        testShelf.setActive(true);
        testShelf.setParentDevice(testDevice);

        testRack = new StorageRack();
        testRack.setId(4);
        testRack.setFhirUuid(UUID.randomUUID());
        testRack.setLabel("RKR1");
        testRack.setRows(9);
        testRack.setColumns(9);
        testRack.setPositionSchemaHint("A1");
        testRack.setActive(true);
        testRack.setParentShelf(testShelf);

        testPosition = new StoragePosition();
        testPosition.setId(5);
        testPosition.setFhirUuid(UUID.randomUUID());
        testPosition.setCoordinate("A5");
        testPosition.setRowIndex(1);
        testPosition.setColumnIndex(5);
        // Occupancy is now calculated dynamically from SampleStorageAssignment records
        testPosition.setParentRack(testRack);
        testPosition.setParentShelf(testShelf);
        testPosition.setParentDevice(testDevice); // Required for hierarchical code generation
    }

    @Test
    public void testTransformStorageRoomToFhirLocation_ValidRoom_ReturnsLocationWithRoomType() {
        // When: Transform Room to FHIR Location
        Location fhirLocation = transformer.transformToFhirLocation(testRoom);

        // Then: Verify FHIR Location structure per fhir-mappings.md
        assertNotNull("FHIR Location should not be null", fhirLocation);
        assertEquals("Location.id should match entity fhir_uuid", testRoom.getFhirUuid().toString(),
                fhirLocation.getId());
        assertEquals("Location.identifier should match room code", "MAIN",
                fhirLocation.getIdentifierFirstRep().getValue());
        assertEquals("Location.name should match room name", testRoom.getName(), fhirLocation.getName());
        assertEquals("Location.status should be active", LocationStatus.ACTIVE, fhirLocation.getStatus());
        assertEquals("physicalType should be 'ro' (room)", "ro",
                fhirLocation.getPhysicalType().getCodingFirstRep().getCode());
        assertEquals("Location.mode should be 'instance'", "instance", fhirLocation.getMode().toCode());
        assertFalse("Room Location should NOT have partOf (top-level)", fhirLocation.hasPartOf());
    }

    @Test
    public void testTransformStorageDeviceToFhirLocation_ValidDevice_ReturnsLocationWithHierarchy() {
        // When: Transform Device to FHIR Location
        Location fhirLocation = transformer.transformToFhirLocation(testDevice);

        // Then: Verify FHIR Location structure
        assertNotNull(fhirLocation);
        assertEquals(testDevice.getFhirUuid().toString(), fhirLocation.getId());
        assertEquals("Identifier should be hierarchical: room-device", "MAIN-FRZ01",
                fhirLocation.getIdentifierFirstRep().getValue());
        assertEquals("physicalType should be 've' (vehicle/equipment)", "ve",
                fhirLocation.getPhysicalType().getCodingFirstRep().getCode());
        assertTrue("Device Location should have partOf reference to Room", fhirLocation.hasPartOf());
        assertTrue("partOf should reference parent Room Location",
                fhirLocation.getPartOf().getReference().contains(testRoom.getFhirUuid().toString()));
        assertEquals("Location.type should match device type", "freezer",
                fhirLocation.getTypeFirstRep().getCodingFirstRep().getCode());
    }

    @Test
    public void testTransformStorageShelfToFhirLocation_ValidShelf_ReturnsLocationWithParent() {
        // When: Transform Shelf to FHIR Location
        Location fhirLocation = transformer.transformToFhirLocation(testShelf);

        // Then: Verify FHIR Location structure
        assertNotNull(fhirLocation);
        assertEquals(testShelf.getFhirUuid().toString(), fhirLocation.getId());
        assertEquals("Identifier should be hierarchical: room-device-shelf", "MAIN-FRZ01-SHA",
                fhirLocation.getIdentifierFirstRep().getValue());
        assertEquals("physicalType should be 'co' (container)", "co",
                fhirLocation.getPhysicalType().getCodingFirstRep().getCode());
        assertTrue(fhirLocation.hasPartOf());
        assertTrue("partOf should reference parent Device Location",
                fhirLocation.getPartOf().getReference().contains(testDevice.getFhirUuid().toString()));
    }

    @Test
    public void testTransformStorageRackToFhirLocation_ValidRack_ReturnsLocationWithGridExtensions() {
        // When: Transform Rack to FHIR Location
        Location fhirLocation = transformer.transformToFhirLocation(testRack);

        // Then: Verify FHIR Location structure
        assertNotNull(fhirLocation);
        assertEquals(testRack.getFhirUuid().toString(), fhirLocation.getId());
        assertEquals("Identifier should be full hierarchical: room-device-shelf-rack", "MAIN-FRZ01-SHA-RKR1",
                fhirLocation.getIdentifierFirstRep().getValue());
        assertEquals("co", fhirLocation.getPhysicalType().getCodingFirstRep().getCode());
        assertTrue(fhirLocation.hasPartOf());
        assertTrue(fhirLocation.getPartOf().getReference().contains(testShelf.getFhirUuid().toString()));

        // Verify grid dimensions extension
        boolean hasGridExtension = fhirLocation.getExtension().stream()
                .anyMatch(ext -> ext.getUrl().contains("rack-grid-dimensions"));
        assertTrue("Rack should have grid-dimensions extension", hasGridExtension);
    }

    @Test
    public void testTransformStoragePositionToFhirLocation_ValidPosition_ReturnsLocationWithOccupancyExtension() {
        // When: Transform Position to FHIR Location
        Location fhirLocation = transformer.transformToFhirLocation(testPosition);

        // Then: Verify FHIR Location structure
        assertNotNull(fhirLocation);
        assertEquals(testPosition.getFhirUuid().toString(), fhirLocation.getId());
        assertEquals("Identifier should include position coordinate", "MAIN-FRZ01-SHA-RKR1-A5",
                fhirLocation.getIdentifierFirstRep().getValue());
        assertEquals("Location.name should be the position coordinate", "A5", fhirLocation.getName());
        assertEquals("co", fhirLocation.getPhysicalType().getCodingFirstRep().getCode());
        assertTrue(fhirLocation.hasPartOf());
        assertTrue("partOf should reference parent Rack Location",
                fhirLocation.getPartOf().getReference().contains(testRack.getFhirUuid().toString()));

        // Verify occupancy extension
        boolean hasOccupancyExtension = fhirLocation.getExtension().stream()
                .anyMatch(ext -> ext.getUrl().contains("position-occupancy"));
        assertTrue("Position should have position-occupancy extension", hasOccupancyExtension);
    }

    @Test
    public void testTransformInactiveRoom_ReturnsInactiveStatus() {
        // Given: Inactive room
        testRoom.setActive(false);

        // When: Transform to FHIR
        Location fhirLocation = transformer.transformToFhirLocation(testRoom);

        // Then: Status should be inactive
        assertEquals("Inactive entity should map to inactive FHIR status", LocationStatus.INACTIVE,
                fhirLocation.getStatus());
    }

    @Test
    public void testHierarchicalCodeGeneration_BuildsCorrectPath() {
        // When: Transform entities at different levels
        Location roomLocation = transformer.transformToFhirLocation(testRoom);
        Location deviceLocation = transformer.transformToFhirLocation(testDevice);
        Location shelfLocation = transformer.transformToFhirLocation(testShelf);
        Location rackLocation = transformer.transformToFhirLocation(testRack);
        Location positionLocation = transformer.transformToFhirLocation(testPosition);

        // Then: Verify hierarchical identifier codes
        assertEquals("MAIN", roomLocation.getIdentifierFirstRep().getValue());
        assertEquals("MAIN-FRZ01", deviceLocation.getIdentifierFirstRep().getValue());
        assertEquals("MAIN-FRZ01-SHA", shelfLocation.getIdentifierFirstRep().getValue());
        assertEquals("MAIN-FRZ01-SHA-RKR1", rackLocation.getIdentifierFirstRep().getValue());
        assertEquals("MAIN-FRZ01-SHA-RKR1-A5", positionLocation.getIdentifierFirstRep().getValue());
    }
}
