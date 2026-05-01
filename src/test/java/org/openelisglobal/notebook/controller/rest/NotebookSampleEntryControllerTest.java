package org.openelisglobal.notebook.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class NotebookSampleEntryControllerTest {

    @Mock
    private SampleStorageService sampleStorageService;

    @InjectMocks
    private NotebookSampleEntryController controller;

    @SuppressWarnings("unchecked")
    @Test
    public void enrichSampleMapsWithStorageAssignments_pendingSampleWithStorage_becomesInProgress() {
        // Arrange
        Map<String, Object> sampleMap = new HashMap<>();
        sampleMap.put("sampleItemId", "1001");
        sampleMap.put("status", "PENDING");
        sampleMap.put("pageStatus", "PENDING");

        Map<String, Object> existingData = new HashMap<>();
        existingData.put("existingKey", "existingValue");
        sampleMap.put("data", existingData);

        List<Map<String, Object>> sampleMaps = new ArrayList<>();
        sampleMaps.add(sampleMap);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("hierarchicalPath", "Room A > Freezer 1 > Shelf B > Rack C > Box D");
        locationMap.put("positionCoordinate", "A1");
        locationMap.put("roomName", "Room A");
        locationMap.put("deviceName", "Freezer 1");
        locationMap.put("shelfLabel", "Shelf B");
        locationMap.put("rackLabel", "Rack C");
        locationMap.put("boxLabel", "Box D");

        Map<String, Map<String, Object>> locationsBySampleId = new HashMap<>();
        locationsBySampleId.put("1001", locationMap);

        when(sampleStorageService.getSampleItemLocations(List.of("1001"))).thenReturn(locationsBySampleId);

        // Act
        ReflectionTestUtils.invokeMethod(controller, "enrichSampleMapsWithStorageAssignments", sampleMaps, true);

        // Assert
        assertEquals("IN_PROGRESS", sampleMap.get("status"));
        assertEquals("IN_PROGRESS", sampleMap.get("pageStatus"));
        assertEquals("Room A > Freezer 1 > Shelf B > Rack C > Box D", sampleMap.get("storagePath"));
        assertEquals("A1", sampleMap.get("storageWell"));

        Map<String, Object> mergedData = (Map<String, Object>) sampleMap.get("data");
        assertEquals("existingValue", mergedData.get("existingKey"));
        assertEquals("A1", mergedData.get("storageWell"));
        assertEquals("Room A > Freezer 1 > Shelf B > Rack C > Box D", mergedData.get("storagePath"));

        verify(sampleStorageService).getSampleItemLocations(List.of("1001"));
    }

    @Test
    public void enrichSampleMapsWithStorageAssignments_nonBiorepositoryContext_skipsOverlay() {
        // Arrange
        Map<String, Object> sampleMap = new HashMap<>();
        sampleMap.put("sampleItemId", "1001");
        sampleMap.put("status", "PENDING");
        sampleMap.put("pageStatus", "PENDING");

        List<Map<String, Object>> sampleMaps = new ArrayList<>();
        sampleMaps.add(sampleMap);

        // Act
        ReflectionTestUtils.invokeMethod(controller, "enrichSampleMapsWithStorageAssignments", sampleMaps, false);

        // Assert
        assertEquals("PENDING", sampleMap.get("status"));
        assertEquals("PENDING", sampleMap.get("pageStatus"));
        verifyZeroInteractions(sampleStorageService);
    }
}
