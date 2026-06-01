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
public class StorageDashboardServiceFilterSamplesTest {

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
        Map<String, Object> roomOneDeviceOne = sampleRow(1, 1, 10, "Room A > Freezer");
        Map<String, Object> roomTwoDeviceThree = sampleRow(2, 3, 10, "Room B > Fridge");
        when(sampleStorageService.getAllSamplesWithAssignments())
                .thenReturn(Arrays.asList(roomOneDeviceOne, roomTwoDeviceThree));

        List<Map<String, Object>> byRoom =
                storageDashboardService.filterSamples(null, null, null, 1, null);
        assertEquals(1, byRoom.size());
        assertEquals(1, ((Number) byRoom.get(0).get("roomId")).intValue());

        List<Map<String, Object>> byDevice =
                storageDashboardService.filterSamples(null, null, null, 1, 1);
        assertEquals(1, byDevice.size());
        assertEquals(1, ((Number) byDevice.get(0).get("deviceId")).intValue());

        List<Map<String, Object>> byDepartment =
                storageDashboardService.filterSamples(null, null, 10, null, null);
        assertEquals(2, byDepartment.size());
        assertTrue(
                byDepartment.stream()
                        .allMatch(row -> "10".equals(String.valueOf(row.get("departmentTestSectionId")))));
    }

    private Map<String, Object> sampleRow(int roomId, int deviceId, int departmentId, String location) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "sample-" + roomId + "-" + deviceId);
        row.put("roomId", roomId);
        row.put("deviceId", deviceId);
        row.put("departmentTestSectionId", departmentId);
        row.put("location", location);
        row.put("status", "active");
        return row;
    }
}
