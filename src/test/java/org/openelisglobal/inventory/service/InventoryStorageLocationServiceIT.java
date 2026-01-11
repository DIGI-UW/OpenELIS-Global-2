package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryStorageLocationServiceIT extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryStorageLocationService storageLocationService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void testGetStorageLocation_LoadedFromDataset() {
        InventoryStorageLocation location = storageLocationService.get(1L);

        assertNotNull("Location should be loaded", location);
        assertEquals("Test Refrigerator 1", location.getName());
        assertEquals("TEST-FRIDGE-01", location.getLocationCode());
        assertTrue("Should be active", location.getIsActive());
    }

    @Test
    public void testGetAllActive_ReturnsDatasetLocations() {
        List<InventoryStorageLocation> locations = storageLocationService.getAllActive();

        assertNotNull("Locations should not be null", locations);
        assertTrue("Should have at least 2 locations", locations.size() >= 2);
    }

    @Test
    public void testGetByLocationCode_FindsDatasetLocation() {
        InventoryStorageLocation location = storageLocationService.getByLocationCode("TEST-ROOM-01");

        assertNotNull("Should find location by code", location);
        assertEquals(Long.valueOf(2L), location.getId());
        assertEquals("Test Room Temperature", location.getName());
    }

    @Test
    public void get_shouldReturnStorageLocationWhenExists() {
        InventoryStorageLocation location = storageLocationService.get(1L);

        assertNotNull("Location should be loaded from dataset", location);
        assertEquals("Test Refrigerator 1", location.getName());
        assertEquals("TEST-FRIDGE-01", location.getLocationCode());
        assertTrue("Should be active", location.getIsActive());
    }

    @Test
    public void getByLocationCode_shouldFindLocationByCode() {
        InventoryStorageLocation location = storageLocationService.getByLocationCode("TEST-ROOM-01");

        assertNotNull("Should find location by code", location);
        assertEquals(Long.valueOf(2L), location.getId());
        assertEquals("Test Room Temperature", location.getName());
    }

    @Test
    public void getAllActive_shouldReturnActiveLocations() {
        List<InventoryStorageLocation> locations = storageLocationService.getAllActive();

        assertNotNull("Locations should not be null", locations);
        assertTrue("Should have at least 2 active locations", locations.size() >= 2);
    }

    @Test
    public void getByLocationType_shouldFilterByType() {
        List<InventoryStorageLocation> refrigerators = storageLocationService
                .getByLocationType(LocationType.REFRIGERATOR);

        assertNotNull("Refrigerators list should not be null", refrigerators);
        assertFalse("Should have at least 1 refrigerator", refrigerators.isEmpty());

        // Verify it's actually a refrigerator
        InventoryStorageLocation fridge = refrigerators.stream().filter(loc -> Long.valueOf(1L).equals(loc.getId()))
                .findFirst().orElse(null);

        assertNotNull("Should find test refrigerator", fridge);
        assertEquals(LocationType.REFRIGERATOR, fridge.getLocationType());
    }

    @Test
    public void update_shouldUpdateStorageLocation() {
        InventoryStorageLocation location = storageLocationService.get(1L);
        location.setDescription("Updated description for testing");

        InventoryStorageLocation updatedLocation = storageLocationService.update(location);

        assertNotNull("Updated location should not be null", updatedLocation);
        assertEquals("Updated description for testing", storageLocationService.get(1L).getDescription());
    }

    @Test
    public void createStorageLocation_shouldInsertNewLocation() {
        InventoryStorageLocation newLocation = new InventoryStorageLocation();
        newLocation.setName("Test Freezer Created");
        newLocation.setLocationCode("TEST-FREEZER-99");
        newLocation.setLocationType(LocationType.FREEZER);
        newLocation.setIsActive(true);
        newLocation.setSysUserId("1");

        Long insertedId = storageLocationService.insert(newLocation);

        assertNotNull("Inserted ID should not be null", insertedId);

        InventoryStorageLocation savedLocation = storageLocationService.get(insertedId);
        assertNotNull("Saved location should be retrievable", savedLocation);
        assertEquals("Test Freezer Created", savedLocation.getName());
    }
}
