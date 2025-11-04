package org.openelisglobal.storage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Storage Dashboard filtering operations. Implements
 * tab-specific filter methods with AND logic combination per FR-066.
 */
@Service
public class StorageDashboardServiceImpl implements StorageDashboardService {

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> filterSamples(String location, String status) {
        List<Map<String, Object>> allSamples = sampleStorageService.getAllSamplesWithAssignments();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> sample : allSamples) {
            boolean matchesLocation = location == null || location.isEmpty()
                    || ((String) sample.get("location")).contains(location);
            boolean matchesStatus = status == null || status.isEmpty()
                    || status.equalsIgnoreCase((String) sample.get("status"));

            if (matchesLocation && matchesStatus) {
                filtered.add(sample);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRoom> filterRooms(Boolean activeStatus) {
        List<StorageRoom> allRooms = storageLocationService.getRooms();
        if (activeStatus == null) {
            return allRooms;
        }
        return allRooms.stream()
                .filter(room -> room.getActive().equals(activeStatus))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> filterDevices(StorageDevice.DeviceType deviceType, Integer roomId,
            Boolean activeStatus) {
        List<StorageDevice> allDevices = storageLocationService.getAllDevices();
        List<StorageDevice> filtered = new ArrayList<>();

        for (StorageDevice device : allDevices) {
            boolean matchesType = deviceType == null || device.getTypeEnum() != null
                    && device.getTypeEnum().equals(deviceType);
            boolean matchesRoom = roomId == null
                    || (device.getParentRoom() != null && device.getParentRoom().getId().equals(roomId));
            boolean matchesStatus = activeStatus == null || device.getActive().equals(activeStatus);

            if (matchesType && matchesRoom && matchesStatus) {
                filtered.add(device);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageShelf> filterShelves(Integer deviceId, Integer roomId, Boolean activeStatus) {
        List<StorageShelf> allShelves = storageLocationService.getAllShelves();
        List<StorageShelf> filtered = new ArrayList<>();

        for (StorageShelf shelf : allShelves) {
            boolean matchesDevice = deviceId == null
                    || (shelf.getParentDevice() != null && shelf.getParentDevice().getId().equals(deviceId));
            boolean matchesRoom = roomId == null
                    || (shelf.getParentDevice() != null && shelf.getParentDevice().getParentRoom() != null
                            && shelf.getParentDevice().getParentRoom().getId().equals(roomId));
            boolean matchesStatus = activeStatus == null || shelf.getActive().equals(activeStatus);

            if (matchesDevice && matchesRoom && matchesStatus) {
                filtered.add(shelf);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRack> filterRacks(Integer roomId, Integer shelfId, Integer deviceId, Boolean activeStatus) {
        List<StorageRack> allRacks = storageLocationService.getAllRacks();
        List<StorageRack> filtered = new ArrayList<>();

        for (StorageRack rack : allRacks) {
            boolean matchesShelf = shelfId == null
                    || (rack.getParentShelf() != null && rack.getParentShelf().getId().equals(shelfId));
            boolean matchesDevice = deviceId == null
                    || (rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                            && rack.getParentShelf().getParentDevice().getId().equals(deviceId));
            boolean matchesRoom = roomId == null
                    || (rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                            && rack.getParentShelf().getParentDevice().getParentRoom() != null
                            && rack.getParentShelf().getParentDevice().getParentRoom().getId().equals(roomId));
            boolean matchesStatus = activeStatus == null || rack.getActive().equals(activeStatus);

            if (matchesShelf && matchesDevice && matchesRoom && matchesStatus) {
                filtered.add(rack);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRacksForAPI(Integer roomId, Integer shelfId, Integer deviceId,
            Boolean activeStatus) {
        List<StorageRack> racks = filterRacks(roomId, shelfId, deviceId, activeStatus);
        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageRack rack : racks) {
            Map<String, Object> rackMap = new HashMap<>();
            rackMap.put("id", rack.getId());
            rackMap.put("label", rack.getLabel());
            rackMap.put("shelfId", rack.getParentShelf() != null ? rack.getParentShelf().getId() : null);
            rackMap.put("deviceId", rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                    ? rack.getParentShelf().getParentDevice().getId() : null);
            // FR-065a: Include roomId column
            rackMap.put("roomId",
                    rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                            && rack.getParentShelf().getParentDevice().getParentRoom() != null
                                    ? rack.getParentShelf().getParentDevice().getParentRoom().getId()
                                    : null);
            rackMap.put("rows", rack.getRows());
            rackMap.put("columns", rack.getColumns());
            rackMap.put("active", rack.getActive());
            result.add(rackMap);
        }

        return result;
    }
}

