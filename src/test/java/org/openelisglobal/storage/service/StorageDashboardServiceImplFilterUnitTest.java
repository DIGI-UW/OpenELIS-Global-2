package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;

@RunWith(MockitoJUnitRunner.class)
public class StorageDashboardServiceImplFilterUnitTest {

    @InjectMocks
    private StorageDashboardServiceImpl storageDashboardService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private StorageLocationService storageLocationService;

    @Mock
    private IStatusService statusService;

    @Test
    public void filterSamples_appliesDepartmentRoomAndDeviceFilters() {
        Map<String, Object> roomOne = sampleRow(1, 10, 100, "Room A > Freezer");
        Map<String, Object> roomTwo = sampleRow(2, 20, 200, "Room B > Fridge");
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(Arrays.asList(roomOne, roomTwo));

        List<Map<String, Object>> byRoom =
                storageDashboardService.filterSamples(null, null, null, 1, null);
        assertEquals(1, byRoom.size());
        assertEquals(1, ((Number) byRoom.get(0).get("roomId")).intValue());

        List<Map<String, Object>> byDevice =
                storageDashboardService.filterSamples(null, null, 20, 2, 200);
        assertEquals(1, byDevice.size());
        assertEquals(200, ((Number) byDevice.get(0).get("deviceId")).intValue());
        assertEquals(20, ((Number) byDevice.get(0).get("departmentTestSectionId")).intValue());
    }

    @Test
    public void filterSamples_keepsLocationAndStatusBehavior() {
        Map<String, Object> match = sampleRow(1, 10, 100, "Room A > Freezer");
        match.put("status", "active");
        Map<String, Object> other = sampleRow(2, 20, 200, "Room B > Fridge");
        other.put("status", "disposed");
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(Arrays.asList(match, other));
        when(statusService.matches("disposed", org.openelisglobal.common.services.StatusService.SampleStatus.Disposed))
                .thenReturn(true);
        when(statusService.matches("active", org.openelisglobal.common.services.StatusService.SampleStatus.Disposed))
                .thenReturn(false);

        List<Map<String, Object>> filtered = storageDashboardService.filterSamples("room a", "active", null, null, null);
        assertEquals(1, filtered.size());
        assertTrue(((String) filtered.get(0).get("location")).toLowerCase().contains("room a"));
    }

    private Map<String, Object> sampleRow(int roomId, int departmentId, int deviceId, String location) {
        Map<String, Object> row = new HashMap<>();
        row.put("roomId", roomId);
        row.put("departmentTestSectionId", departmentId);
        row.put("deviceId", deviceId);
        row.put("location", location);
        row.put("status", "active");
        return row;
    }
}
