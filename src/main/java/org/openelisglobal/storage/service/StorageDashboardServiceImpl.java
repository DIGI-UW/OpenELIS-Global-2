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

        // Normalize location filter (case-insensitive, trim whitespace)
        String normalizedLocation = (location != null && !location.isEmpty()) ? location.trim().toLowerCase() : null;

        for (Map<String, Object> sample : allSamples) {
            // Location filtering: case-insensitive substring match
            boolean matchesLocation = true;
            if (normalizedLocation != null) {
                String sampleLocation = (String) sample.get("location");
                if (sampleLocation == null) {
                    matchesLocation = false;
                } else {
                    // Case-insensitive contains check
                    matchesLocation = sampleLocation.toLowerCase().contains(normalizedLocation);
                }
            }

            // Status filtering: case-insensitive exact match
            boolean matchesStatus = true;
            if (status != null && !status.isEmpty()) {
                String sampleStatus = (String) sample.get("status");
                if (sampleStatus == null) {
                    matchesStatus = false;
                } else {
                    matchesStatus = status.trim().equalsIgnoreCase(sampleStatus);
                }
            }

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
        return allRooms.stream().filter(room -> room.getActive() != null && room.getActive().equals(activeStatus))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> filterRoomsForAPI(Boolean activeStatus) {
        List<Map<String, Object>> allRooms = storageLocationService.getRoomsForAPI();
        if (activeStatus == null) {
            return allRooms;
        }
        return allRooms.stream().filter(room -> {
            Boolean active = (Boolean) room.get("active");
            return active != null && active.equals(activeStatus);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> filterDevices(StorageDevice.DeviceType deviceType, Integer roomId,
            Boolean activeStatus) {
        List<StorageDevice> allDevices = storageLocationService.getAllDevices();
        List<StorageDevice> filtered = new ArrayList<>();

        for (StorageDevice device : allDevices) {
            boolean matchesType = deviceType == null
                    || device.getTypeEnum() != null && device.getTypeEnum().equals(deviceType);
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
    public List<Map<String, Object>> filterDevicesForAPI(StorageDevice.DeviceType deviceType, Integer roomId,
            Boolean activeStatus) {
        List<Map<String, Object>> allDevices = storageLocationService.getDevicesForAPI(null);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> device : allDevices) {
            boolean matchesType = true;
            if (deviceType != null) {
                String typeStr = (String) device.get("type");
                if (typeStr == null) {
                    matchesType = false;
                } else {
                    try {
                        StorageDevice.DeviceType deviceTypeFromMap = StorageDevice.DeviceType
                                .valueOf(typeStr.toUpperCase());
                        matchesType = deviceTypeFromMap.equals(deviceType);
                    } catch (IllegalArgumentException e) {
                        matchesType = false;
                    }
                }
            }

            boolean matchesRoom = true;
            if (roomId != null) {
                // getDevicesForAPI returns maps with "parentRoomId", not "roomId"
                Integer deviceRoomId = (Integer) device.get("parentRoomId");
                matchesRoom = deviceRoomId != null && deviceRoomId.equals(roomId);
            }

            boolean matchesStatus = true;
            if (activeStatus != null) {
                Boolean deviceActive = (Boolean) device.get("active");
                matchesStatus = deviceActive != null && deviceActive.equals(activeStatus);
            }

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
    public List<Map<String, Object>> filterShelvesForAPI(Integer deviceId, Integer roomId, Boolean activeStatus) {
        List<Map<String, Object>> allShelves = storageLocationService.getShelvesForAPI(null);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> shelf : allShelves) {
            boolean matchesDevice = true;
            if (deviceId != null) {
                // Use parentDeviceId (set by getShelvesForAPI) instead of deviceId
                Integer shelfDeviceId = (Integer) shelf.get("parentDeviceId");
                matchesDevice = shelfDeviceId != null && shelfDeviceId.equals(deviceId);
            }

            boolean matchesRoom = true;
            if (roomId != null) {
                // Use parentRoomId (set by getShelvesForAPI) instead of roomId
                Integer shelfRoomId = (Integer) shelf.get("parentRoomId");
                matchesRoom = shelfRoomId != null && shelfRoomId.equals(roomId);
            }

            boolean matchesStatus = true;
            if (activeStatus != null) {
                Boolean shelfActive = (Boolean) shelf.get("active");
                matchesStatus = shelfActive != null && shelfActive.equals(activeStatus);
            }

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
            // Initialize relationships within transaction to trigger lazy loading
            StorageShelf parentShelf = rack.getParentShelf();
            StorageDevice parentDevice = null;
            StorageRoom parentRoom = null;
            if (parentShelf != null) {
                parentShelf.getLabel(); // Trigger lazy load
                parentDevice = parentShelf.getParentDevice();
                if (parentDevice != null) {
                    parentDevice.getName(); // Trigger lazy load
                    parentRoom = parentDevice.getParentRoom();
                    if (parentRoom != null) {
                        parentRoom.getName(); // Trigger lazy load
                    }
                }
            }

            Map<String, Object> rackMap = new HashMap<>();
            rackMap.put("id", rack.getId());
            rackMap.put("label", rack.getLabel());
            rackMap.put("rows", rack.getRows());
            rackMap.put("columns", rack.getColumns());
            rackMap.put("active", rack.getActive());
            rackMap.put("fhirUuid", rack.getFhirUuidAsString());

            // Add relationship data with IDs and names for filtering and display - use
            // parent-prefixed names for consistency
            if (parentShelf != null) {
                rackMap.put("parentShelfId", parentShelf.getId());
                rackMap.put("shelfLabel", parentShelf.getLabel());
                rackMap.put("parentShelfLabel", parentShelf.getLabel());
            }
            if (parentDevice != null) {
                rackMap.put("parentDeviceId", parentDevice.getId());
                rackMap.put("deviceName", parentDevice.getName());
                rackMap.put("parentDeviceName", parentDevice.getName());
            }
            // FR-065a: Include parentRoomId and room name
            if (parentRoom != null) {
                rackMap.put("parentRoomId", parentRoom.getId());
                rackMap.put("roomName", parentRoom.getName());
                rackMap.put("parentRoomName", parentRoom.getName());
            }

            // Build hierarchicalPath: Room > Device > Shelf > Rack
            StringBuilder pathBuilder = new StringBuilder();
            if (parentRoom != null && parentRoom.getName() != null) {
                pathBuilder.append(parentRoom.getName());
            }
            if (parentDevice != null && parentDevice.getName() != null) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(" > ");
                }
                pathBuilder.append(parentDevice.getName());
            }
            if (parentShelf != null && parentShelf.getLabel() != null) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(" > ");
                }
                pathBuilder.append(parentShelf.getLabel());
            }
            if (rack.getLabel() != null) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(" > ");
                }
                pathBuilder.append(rack.getLabel());
            }
            if (pathBuilder.length() > 0) {
                rackMap.put("hierarchicalPath", pathBuilder.toString());
            }

            // Set type for consistency
            rackMap.put("type", "rack");

            result.add(rackMap);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getLocationCountsByType() {
        Map<String, Integer> counts = new HashMap<>();

        // Count active rooms only (FR-057 - active locations only)
        // Use Boolean.TRUE.equals() for null-safe comparison
        List<StorageRoom> activeRooms = filterRooms(true);
        counts.put("rooms", activeRooms.size());

        // Count active devices only - use Boolean.TRUE.equals() for null-safe
        // comparison
        List<StorageDevice> activeDevices = storageLocationService.getAllDevices().stream()
                .filter(device -> Boolean.TRUE.equals(device.getActive())).collect(Collectors.toList());
        counts.put("devices", activeDevices.size());

        // Count active shelves only - use Boolean.TRUE.equals() for null-safe
        // comparison
        List<StorageShelf> activeShelves = storageLocationService.getAllShelves().stream()
                .filter(shelf -> Boolean.TRUE.equals(shelf.getActive())).collect(Collectors.toList());
        counts.put("shelves", activeShelves.size());

        // Count active racks only - use Boolean.TRUE.equals() for null-safe comparison
        List<StorageRack> activeRacks = storageLocationService.getAllRacks().stream()
                .filter(rack -> Boolean.TRUE.equals(rack.getActive())).collect(Collectors.toList());
        counts.put("racks", activeRacks.size());

        return counts;
    }
}
