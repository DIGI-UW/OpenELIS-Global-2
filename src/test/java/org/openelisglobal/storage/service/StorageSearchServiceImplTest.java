package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for StorageSearchService - Search logic per FR-064 and FR-064a
 * (Phase 3.1 in plan.md). Following TDD: Write tests BEFORE implementation.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageSearchServiceImplTest {

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private StorageSearchServiceImpl searchService;

    private List<Map<String, Object>> mockSamples;
    private List<Map<String, Object>> mockRoomsForAPI;
    private List<Map<String, Object>> mockDevicesForAPI;
    private List<Map<String, Object>> mockShelvesForAPI;
    private List<Map<String, Object>> mockRacksForAPI;

    @Before
    public void setUp() {
        searchService = new StorageSearchServiceImpl();
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field sampleServiceField = StorageSearchServiceImpl.class
                    .getDeclaredField("sampleStorageService");
            sampleServiceField.setAccessible(true);
            sampleServiceField.set(searchService, sampleStorageService);

            java.lang.reflect.Field locationServiceField = StorageSearchServiceImpl.class
                    .getDeclaredField("storageLocationService");
            locationServiceField.setAccessible(true);
            locationServiceField.set(searchService, storageLocationService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
        setupMockData();
    }

    private void setupMockData() {
        // Mock SampleItems with different IDs, external IDs, parent Sample accession
        // numbers, and locations
        mockSamples = new ArrayList<>();

        Map<String, Object> sampleItem1 = new HashMap<>();
        sampleItem1.put("id", "1001");
        sampleItem1.put("sampleItemId", "1001");
        sampleItem1.put("sampleItemExternalId", "SI-1001-EXT");
        sampleItem1.put("sampleAccessionNumber", "TEST-SAMPLE-001");
        sampleItem1.put("type", "Blood");
        sampleItem1.put("status", "active");
        sampleItem1.put("location", "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5");
        mockSamples.add(sampleItem1);

        Map<String, Object> sampleItem2 = new HashMap<>();
        sampleItem2.put("id", "1002");
        sampleItem2.put("sampleItemId", "1002");
        sampleItem2.put("sampleItemExternalId", "SI-1002-EXT");
        sampleItem2.put("sampleAccessionNumber", "TB-2025-001");
        sampleItem2.put("type", "Serum");
        sampleItem2.put("status", "active");
        sampleItem2.put("location", "Main Laboratory > Refrigerator Unit 1 > Shelf-1 > Rack R2 > Position B3");
        mockSamples.add(sampleItem2);

        Map<String, Object> sampleItem3 = new HashMap<>();
        sampleItem3.put("id", "1003");
        sampleItem3.put("sampleItemId", "1003");
        sampleItem3.put("sampleItemExternalId", "SI-1003-EXT");
        sampleItem3.put("sampleAccessionNumber", "S-2025-002");
        sampleItem3.put("type", "Urine");
        sampleItem3.put("status", "active");
        sampleItem3.put("location", "Secondary Laboratory > Freezer Unit 2 > Shelf-B > Rack R3 > Position C1");
        mockSamples.add(sampleItem3);

        // Mock rooms as Maps (API format)
        mockRoomsForAPI = new ArrayList<>();
        Map<String, Object> room1 = new HashMap<>();
        room1.put("id", 1);
        room1.put("name", "Main Laboratory");
        room1.put("code", "MAIN-LAB");
        room1.put("active", true);
        mockRoomsForAPI.add(room1);

        Map<String, Object> room2 = new HashMap<>();
        room2.put("id", 2);
        room2.put("name", "Secondary Laboratory");
        room2.put("code", "SECOND-LAB");
        room2.put("active", true);
        mockRoomsForAPI.add(room2);

        // Mock devices as Maps (API format)
        mockDevicesForAPI = new ArrayList<>();
        Map<String, Object> device1 = new HashMap<>();
        device1.put("id", 10);
        device1.put("name", "Freezer Unit 1");
        device1.put("code", "FRZ01");
        device1.put("type", "freezer");
        device1.put("active", true);
        mockDevicesForAPI.add(device1);

        Map<String, Object> device2 = new HashMap<>();
        device2.put("id", 11);
        device2.put("name", "Refrigerator Unit 1");
        device2.put("code", "REFRIG01");
        device2.put("type", "refrigerator");
        device2.put("active", true);
        mockDevicesForAPI.add(device2);

        // Mock shelves as Maps (API format)
        mockShelvesForAPI = new ArrayList<>();
        Map<String, Object> shelf1 = new HashMap<>();
        shelf1.put("id", 20);
        shelf1.put("label", "Shelf-A");
        shelf1.put("active", true);
        mockShelvesForAPI.add(shelf1);

        Map<String, Object> shelf2 = new HashMap<>();
        shelf2.put("id", 21);
        shelf2.put("label", "Shelf-1");
        shelf2.put("active", true);
        mockShelvesForAPI.add(shelf2);

        // Mock racks as Maps (API format)
        mockRacksForAPI = new ArrayList<>();
        Map<String, Object> rack1 = new HashMap<>();
        rack1.put("id", 30);
        rack1.put("label", "Rack R1");
        rack1.put("active", true);
        mockRacksForAPI.add(rack1);

        Map<String, Object> rack2 = new HashMap<>();
        rack2.put("id", 31);
        rack2.put("label", "Rack R2");
        rack2.put("active", true);
        mockRacksForAPI.add(rack2);
    }

    // ========== Sample Search Service Tests ==========

    @Test
    public void testSearchSamples_FiltersBySampleId() throws Exception {
        // Filter SampleItems by ID substring
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        List<Map<String, Object>> results = searchService.searchSamples("1001");

        assertNotNull("Results should not be null", results);
        assertEquals("Should return one matching SampleItem", 1, results.size());
        assertEquals("Should return SampleItem with ID 1001", "1001", String.valueOf(results.get(0).get("id")));
    }

    @Test
    public void testSearchSamples_FiltersByAccessionPrefix() throws Exception {
        // Filter by parent Sample accession number prefix
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        List<Map<String, Object>> results = searchService.searchSamples("TB-2025");

        assertNotNull("Results should not be null", results);
        assertEquals("Should return one matching sample", 1, results.size());
        assertEquals("Should return SampleItem with TB-2025 prefix in parent Sample accession", "TB-2025-001", results.get(0).get("sampleAccessionNumber"));
    }

    @Test
    public void testSearchSamples_FiltersByLocationPath() throws Exception {
        // Filter by location path substring
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        List<Map<String, Object>> results = searchService.searchSamples("Freezer");

        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one matching sample", results.size() >= 1);
        
        // Verify all results contain "Freezer" in location
        for (Map<String, Object> sample : results) {
            String location = (String) sample.get("location");
            assertNotNull("Location should not be null", location);
            assertTrue("Location should contain 'Freezer' (case-insensitive)", 
                    location.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void testSearchSamples_OR_Logic() throws Exception {
        // Matches if ANY field matches (SampleItem ID, External ID, parent Sample accession, or location path)
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        // Query "1001" should match by SampleItem ID
        List<Map<String, Object>> resultsById = searchService.searchSamples("1001");
        assertEquals("Should match by SampleItem ID", 1, resultsById.size());

        // Query "TB-2025" should match by parent Sample accession number
        List<Map<String, Object>> resultsByPrefix = searchService.searchSamples("TB-2025");
        assertEquals("Should match by parent Sample accession number", 1, resultsByPrefix.size());

        // Query "SI-1001" should match by SampleItem External ID
        List<Map<String, Object>> resultsByExternalId = searchService.searchSamples("SI-1001");
        assertEquals("Should match by SampleItem External ID", 1, resultsByExternalId.size());

        // Query "Freezer" should match by location path
        List<Map<String, Object>> resultsByLocation = searchService.searchSamples("Freezer");
        assertTrue("Should match by location path", resultsByLocation.size() >= 1);
    }

    @Test
    public void testSearchSamples_CaseInsensitive() throws Exception {
        // Case-insensitive matching
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        // Lowercase query should match uppercase location
        List<Map<String, Object>> results = searchService.searchSamples("freezer");

        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one matching sample (case-insensitive)", results.size() >= 1);
    }

    @Test
    public void testSearchSamples_EmptyQuery_ReturnsAll() throws Exception {
        // Empty query returns all
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        List<Map<String, Object>> results = searchService.searchSamples("");

        assertNotNull("Results should not be null", results);
        assertEquals("Should return all samples when query is empty", mockSamples.size(), results.size());
    }

    @Test
    public void testSearchSamples_NullQuery_ReturnsAll() throws Exception {
        // Null query returns all
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        List<Map<String, Object>> results = searchService.searchSamples(null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return all samples when query is null", mockSamples.size(), results.size());
    }

    // ========== Room Search Service Tests ==========

    @Test
    public void testSearchRooms_FiltersByNameOrCode() throws Exception {
        // Matches name OR code
        when(storageLocationService.getRoomsForAPI()).thenReturn(mockRoomsForAPI);

        // Search by name
        List<Map<String, Object>> resultsByName = searchService.searchRooms("Main");
        assertNotNull("Results should not be null", resultsByName);
        assertTrue("Should return at least one matching room by name", resultsByName.size() >= 1);

        // Search by code
        List<Map<String, Object>> resultsByCode = searchService.searchRooms("MAIN-LAB");
        assertNotNull("Results should not be null", resultsByCode);
        assertTrue("Should return at least one matching room by code", resultsByCode.size() >= 1);
    }

    // ========== Device Search Service Tests ==========

    @Test
    public void testSearchDevices_FiltersByNameCodeOrType() throws Exception {
        // Matches name OR code OR type
        when(storageLocationService.getDevicesForAPI(null)).thenReturn(mockDevicesForAPI);

        // Search by name
        List<Map<String, Object>> resultsByName = searchService.searchDevices("Freezer Unit");
        assertNotNull("Results should not be null", resultsByName);
        assertTrue("Should return at least one matching device by name", resultsByName.size() >= 1);

        // Search by code
        List<Map<String, Object>> resultsByCode = searchService.searchDevices("FRZ01");
        assertNotNull("Results should not be null", resultsByCode);
        assertTrue("Should return at least one matching device by code", resultsByCode.size() >= 1);

        // Search by type
        List<Map<String, Object>> resultsByType = searchService.searchDevices("freezer");
        assertNotNull("Results should not be null", resultsByType);
        assertTrue("Should return at least one matching device by type", resultsByType.size() >= 1);
    }

    // ========== Shelf Search Service Tests ==========

    @Test
    public void testSearchShelves_FiltersByLabel() throws Exception {
        // Matches label
        when(storageLocationService.getShelvesForAPI(null)).thenReturn(mockShelvesForAPI);

        List<Map<String, Object>> results = searchService.searchShelves("Shelf-A");

        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one matching shelf", results.size() >= 1);
        
        // Verify all results have label containing query
        for (Map<String, Object> shelf : results) {
            String label = (String) shelf.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", 
                    label.toLowerCase().contains("shelf-a"));
        }
    }

    // ========== Rack Search Service Tests ==========

    @Test
    public void testSearchRacks_FiltersByLabel() throws Exception {
        // Matches label
        when(storageLocationService.getRacksForAPI(null)).thenReturn(mockRacksForAPI);

        List<Map<String, Object>> results = searchService.searchRacks("Rack R1");

        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one matching rack", results.size() >= 1);
        
        // Verify all results have label containing query
        for (Map<String, Object> rack : results) {
            String label = (String) rack.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", 
                    label.toLowerCase().contains("rack r1"));
        }
    }
}
