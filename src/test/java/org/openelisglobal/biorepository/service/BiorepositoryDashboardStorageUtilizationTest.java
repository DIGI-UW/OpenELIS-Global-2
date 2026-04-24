package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryDashboardStorageUtilizationTest {

    @Mock
    private BioSampleService bioSampleService;
    @Mock
    private BiorepositoryQCInspectionService qcInspectionService;
    @Mock
    private SampleRetrievalService retrievalService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryTemperatureLogService temperatureLogService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;
    @Mock
    private SampleStorageService storageService;
    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private BiorepositoryDashboardServiceImpl dashboardService;

    @Test
    public void storageCapacityMetrics_ExposeComputedDeviceSummary() {
        StorageDevice freezerA = buildDevice(1, "Freezer-A", "FRA", true, 100);
        StorageDevice freezerB = buildDevice(2, "Freezer-B", "FRB", true, null);
        StorageDevice inactive = buildDevice(3, "Freezer-C", "FRC", false, 200);
        StorageShelf shelfB = buildShelf(21, freezerB, true);
        StorageRack rackB = buildRack(31, shelfB, true);
        StorageBox boxB = buildBox(41, rackB, true, 4, 10); // derived capacity = 40

        when(bioSampleService.getAll()).thenReturn(List.of(
                buildBioSample(101, BioSample.WorkflowStatus.STORED),
                buildBioSample(102, BioSample.WorkflowStatus.STORED),
                buildBioSample(103, BioSample.WorkflowStatus.PENDING_STORAGE)));
        when(storageLocationService.getAllDevices()).thenReturn(List.of(freezerA, freezerB, inactive));
        when(storageLocationService.getAllShelves()).thenReturn(List.of(shelfB));
        when(storageLocationService.getAllRacks()).thenReturn(List.of(rackB));
        when(storageLocationService.getAllBoxes()).thenReturn(List.of(boxB));
        when(storageLocationService.countOccupiedInDevice(1)).thenReturn(20);
        when(storageLocationService.countOccupiedInDevice(2)).thenReturn(10);

        Map<String, Object> metrics = dashboardService.getStorageCapacityMetrics();

        assertEquals(2L, ((Number) metrics.get("totalSamplesStored")).longValue());
        assertEquals(1L, ((Number) metrics.get("pendingStorage")).longValue());
        assertEquals(2, ((Number) metrics.get("totalDevices")).intValue());
        assertEquals(2, ((Number) metrics.get("capacityDefinedDevices")).intValue());
        assertEquals(0, ((Number) metrics.get("capacityUndefinedDevices")).intValue());
        assertEquals(140L, ((Number) metrics.get("totalConfiguredCapacity")).longValue());
        assertEquals(30L, ((Number) metrics.get("totalCurrentUsage")).longValue());
        assertEquals(21.4285, ((Number) metrics.get("averageUtilization")).doubleValue(), 0.001);
    }

    @Test
    public void storageUtilizationByDevice_UsesConfiguredOrDerivedCapacitySources() {
        StorageDevice freezerA = buildDevice(1, "Freezer-A", "FRA", true, 100);
        StorageDevice freezerB = buildDevice(2, "Freezer-B", "FRB", true, null);
        StorageShelf shelfB = buildShelf(21, freezerB, true);
        StorageRack rackB = buildRack(31, shelfB, true);
        StorageBox boxB = buildBox(41, rackB, true, 4, 10);

        when(storageLocationService.getAllDevices()).thenReturn(List.of(freezerA, freezerB));
        when(storageLocationService.getAllShelves()).thenReturn(List.of(shelfB));
        when(storageLocationService.getAllRacks()).thenReturn(List.of(rackB));
        when(storageLocationService.getAllBoxes()).thenReturn(List.of(boxB));
        when(storageLocationService.countOccupiedInDevice(1)).thenReturn(20);
        when(storageLocationService.countOccupiedInDevice(2)).thenReturn(10);

        Map<String, Object> utilization = dashboardService.getStorageUtilizationByDevice();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> devices = (List<Map<String, Object>>) utilization.get("devices");

        assertNotNull(devices);
        assertEquals(2, devices.size());
        Map<String, Object> first = devices.get(0);
        Map<String, Object> second = devices.get(1);

        assertEquals("Freezer-A", first.get("deviceName"));
        assertEquals("DEVICE_CAPACITY_LIMIT", first.get("capacitySource"));
        assertEquals(20.0, ((Number) first.get("utilizationPercent")).doubleValue(), 0.001);

        assertEquals("Freezer-B", second.get("deviceName"));
        assertEquals("BOX_GRID_SUM", second.get("capacitySource"));
        assertEquals(40L, ((Number) second.get("totalCapacity")).longValue());
        assertEquals(25.0, ((Number) second.get("utilizationPercent")).doubleValue(), 0.001);
    }

    private BioSample buildBioSample(int id, BioSample.WorkflowStatus status) {
        BioSample sample = new BioSample();
        sample.setId(id);
        sample.setWorkflowStatus(status);
        return sample;
    }

    private StorageDevice buildDevice(int id, String name, String code, boolean active, Integer capacityLimit) {
        StorageDevice device = new StorageDevice();
        device.setId(id);
        device.setName(name);
        device.setCode(code);
        device.setType(StorageDevice.DeviceType.FREEZER.getValue());
        device.setActive(active);
        device.setCapacityLimit(capacityLimit);
        return device;
    }

    private StorageShelf buildShelf(int id, StorageDevice parentDevice, boolean active) {
        StorageShelf shelf = new StorageShelf();
        shelf.setId(id);
        shelf.setParentDevice(parentDevice);
        shelf.setActive(active);
        shelf.setLabel("Shelf-" + id);
        return shelf;
    }

    private StorageRack buildRack(int id, StorageShelf parentShelf, boolean active) {
        StorageRack rack = new StorageRack();
        rack.setId(id);
        rack.setParentShelf(parentShelf);
        rack.setActive(active);
        rack.setLabel("Rack-" + id);
        return rack;
    }

    private StorageBox buildBox(int id, StorageRack parentRack, boolean active, int rows, int columns) {
        StorageBox box = new StorageBox();
        box.setId(id);
        box.setParentRack(parentRack);
        box.setActive(active);
        box.setRows(rows);
        box.setColumns(columns);
        box.setLabel("Box-" + id);
        box.setCode("B" + id);
        return box;
    }
}
