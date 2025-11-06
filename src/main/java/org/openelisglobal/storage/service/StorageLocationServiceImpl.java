package org.openelisglobal.storage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StorageLocationServiceImpl implements StorageLocationService {

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Autowired
    private StoragePositionDAO storagePositionDAO;

    @Autowired
    private StorageSearchService storageSearchService;

    @Override
    public List<StorageRoom> getRooms() {
        return storageRoomDAO.getAll();
    }

    @Override
    public StorageRoom getRoom(Integer id) {
        return storageRoomDAO.get(id).orElse(null);
    }

    @Override
    public StorageRoom createRoom(StorageRoom room) {
        // Check for duplicate code
        StorageRoom existing = storageRoomDAO.findByCode(room.getCode());
        if (existing != null) {
            throw new LIMSRuntimeException("Room with code " + room.getCode() + " already exists");
        }
        Integer id = storageRoomDAO.insert(room);
        room.setId(id);
        return room;
    }

    @Override
    public StorageRoom updateRoom(Integer id, StorageRoom room) {
        StorageRoom existingRoom = storageRoomDAO.get(id).orElse(null);
        if (existingRoom == null) {
            return null;
        }
        existingRoom.setName(room.getName());
        existingRoom.setCode(room.getCode());
        existingRoom.setDescription(room.getDescription());
        existingRoom.setActive(room.getActive());
        storageRoomDAO.update(existingRoom);
        return existingRoom;
    }

    @Override
    public void deleteRoom(Integer id) {
        StorageRoom room = storageRoomDAO.get(id).orElse(null);
        if (room != null) {
            delete(room);
        }
    }

    @Override
    public List<StorageDevice> getDevicesByRoom(Integer roomId) {
        return storageDeviceDAO.findByParentRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> getAllDevices() {
        List<StorageDevice> devices = storageDeviceDAO.getAll();
        // Initialize lazy relationships within transaction for REST API serialization
        // This ensures relationships are accessible when entities are serialized to
        // JSON
        for (StorageDevice device : devices) {
            if (device.getParentRoom() != null) {
                device.getParentRoom().getName(); // Trigger lazy load
            }
        }
        return devices;
    }

    @Override
    public List<StorageShelf> getShelvesByDevice(Integer deviceId) {
        return storageShelfDAO.findByParentDeviceId(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageShelf> getAllShelves() {
        List<StorageShelf> shelves = storageShelfDAO.getAll();
        // Initialize lazy relationships within transaction for REST API serialization
        for (StorageShelf shelf : shelves) {
            if (shelf.getParentDevice() != null) {
                shelf.getParentDevice().getName(); // Trigger lazy load
                if (shelf.getParentDevice().getParentRoom() != null) {
                    shelf.getParentDevice().getParentRoom().getName(); // Trigger lazy load
                }
            }
        }
        return shelves;
    }

    @Override
    public List<StorageRack> getRacksByShelf(Integer shelfId) {
        return storageRackDAO.findByParentShelfId(shelfId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRack> getAllRacks() {
        List<StorageRack> racks = storageRackDAO.getAll();
        // Initialize lazy relationships within transaction for REST API serialization
        for (StorageRack rack : racks) {
            if (rack.getParentShelf() != null) {
                rack.getParentShelf().getLabel(); // Trigger lazy load
                StorageDevice device = rack.getParentShelf().getParentDevice();
                if (device != null) {
                    device.getName(); // Trigger lazy load
                    // Also initialize parentRoom for full hierarchy
                    if (device.getParentRoom() != null) {
                        device.getParentRoom().getName(); // Trigger lazy load
                    }
                }
            }
        }
        return racks;
    }

    @Override
    public List<StoragePosition> getPositionsByRack(Integer rackId) {
        return storagePositionDAO.findByParentRackId(rackId);
    }

    @Override
    public List<StoragePosition> getAllPositions() {
        return storagePositionDAO.getAll();
    }

    @Override
    public int countOccupiedInDevice(Integer deviceId) {
        return storagePositionDAO.countOccupiedInDevice(deviceId);
    }

    @Override
    public int countOccupied(Integer rackId) {
        return storagePositionDAO.countOccupied(rackId);
    }

    @Override
    public Integer insert(Object entity) {
        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            // Check for duplicate code
            StorageRoom existing = storageRoomDAO.findByCode(room.getCode());
            if (existing != null) {
                throw new LIMSRuntimeException("Room with code " + room.getCode() + " already exists");
            }
            return storageRoomDAO.insert(room);
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Check for duplicate code in same room
            StorageDevice existing = storageDeviceDAO.findByParentRoomIdAndCode(device.getParentRoom().getId(),
                    device.getCode());
            if (existing != null) {
                throw new LIMSRuntimeException("Device with code " + device.getCode() + " already exists in this room");
            }
            return storageDeviceDAO.insert(device);
        } else if (entity instanceof StorageShelf) {
            return storageShelfDAO.insert((StorageShelf) entity);
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            // Validate grid dimensions
            if (rack.getRows() < 0 || rack.getColumns() < 0) {
                throw new IllegalArgumentException("Grid dimensions cannot be negative");
            }
            return storageRackDAO.insert(rack);
        } else if (entity instanceof StoragePosition) {
            return storagePositionDAO.insert((StoragePosition) entity);
        }
        throw new LIMSRuntimeException("Unsupported entity type for insert");
    }

    @Override
    public Integer update(Object entity) {
        if (entity instanceof StorageRoom) {
            storageRoomDAO.update((StorageRoom) entity);
            return null;
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Check for active samples when deactivating
            if (!device.getActive()) {
                int occupiedCount = storagePositionDAO.countOccupiedInDevice(device.getId());
                if (occupiedCount > 0) {
                    throw new LIMSRuntimeException("Warning: Device has " + occupiedCount + " active samples. "
                            + "Please move or dispose samples before deactivating.");
                }
            }
            storageDeviceDAO.update(device);
            return null;
        } else if (entity instanceof StorageShelf) {
            storageShelfDAO.update((StorageShelf) entity);
            return null;
        } else if (entity instanceof StorageRack) {
            storageRackDAO.update((StorageRack) entity);
            return null;
        } else if (entity instanceof StoragePosition) {
            storagePositionDAO.update((StoragePosition) entity);
            return null;
        }
        throw new LIMSRuntimeException("Unsupported entity type for update");
    }

    @Override
    public void delete(Object entity) {
        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            // Check for active child devices
            var devices = storageDeviceDAO.findByParentRoomId(room.getId());
            boolean hasActiveDevices = devices.stream().anyMatch(d -> d.getActive() != null && d.getActive());
            if (hasActiveDevices) {
                throw new LIMSRuntimeException("Cannot delete room with active child devices");
            }
            storageRoomDAO.delete(room);
        } else if (entity instanceof StorageDevice) {
            storageDeviceDAO.delete((StorageDevice) entity);
        } else if (entity instanceof StorageShelf) {
            storageShelfDAO.delete((StorageShelf) entity);
        } else if (entity instanceof StorageRack) {
            storageRackDAO.delete((StorageRack) entity);
        } else if (entity instanceof StoragePosition) {
            storagePositionDAO.delete((StoragePosition) entity);
        } else {
            throw new LIMSRuntimeException("Unsupported entity type for delete");
        }
    }

    @Override
    public Object get(Integer id, Class<?> entityClass) {
        if (entityClass == StorageRoom.class) {
            return storageRoomDAO.get(id).orElse(null);
        } else if (entityClass == StorageDevice.class) {
            return storageDeviceDAO.get(id).orElse(null);
        } else if (entityClass == StorageShelf.class) {
            return storageShelfDAO.get(id).orElse(null);
        } else if (entityClass == StorageRack.class) {
            return storageRackDAO.get(id).orElse(null);
        } else if (entityClass == StoragePosition.class) {
            return storagePositionDAO.get(id).orElse(null);
        }
        throw new LIMSRuntimeException("Unsupported entity class for get");
    }

    @Override
    public boolean validateLocationActive(StoragePosition position) {
        if (position == null) {
            return false;
        }

        // Validate parent_device_id exists (minimum 2 levels requirement)
        if (position.getParentDevice() == null) {
            return false;
        }

        StorageDevice device = position.getParentDevice();
        if (device.getParentRoom() == null) {
            return false;
        }

        StorageRoom room = device.getParentRoom();

        // Validate hierarchy integrity: if rack exists, shelf must exist; if coordinate exists, rack must exist
        if (!position.validateHierarchyIntegrity()) {
            return false;
        }

        // Check room and device are active (minimum 2 levels)
        if (room.getActive() == null || !room.getActive()) {
            return false;
        }
        if (device.getActive() == null || !device.getActive()) {
            return false;
        }

        // Check optional parents are active if they exist
        if (position.getParentShelf() != null) {
            StorageShelf shelf = position.getParentShelf();
            if (shelf.getActive() == null || !shelf.getActive()) {
                return false;
            }
        }

        if (position.getParentRack() != null) {
            StorageRack rack = position.getParentRack();
            if (rack.getActive() == null || !rack.getActive()) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String buildHierarchicalPath(StoragePosition position) {
        if (position == null) {
            return "Unknown Location";
        }

        // Position always has parent_device (required), which has parent_room
        if (position.getParentDevice() == null) {
            return "Unknown";
        }

        StorageDevice device = position.getParentDevice();
        StorageRoom room = device.getParentRoom();
        if (room == null) {
            return device.getName();
        }

        StringBuilder path = new StringBuilder();
        path.append(room.getName()).append(" > ").append(device.getName());

        // Add shelf if present (3+ level position)
        if (position.getParentShelf() != null) {
            StorageShelf shelf = position.getParentShelf();
            path.append(" > ").append(shelf.getLabel());

            // Add rack if present (4+ level position)
            if (position.getParentRack() != null) {
                StorageRack rack = position.getParentRack();
                path.append(" > ").append(rack.getLabel());

                // Add coordinate if present (5-level position)
                if (position.getCoordinate() != null && !position.getCoordinate().isEmpty()) {
                    path.append(" > Position ").append(position.getCoordinate());
                }
            }
        }

        return path.toString();
    }

    // ========== REST API methods - prepare all data within transaction ==========

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomsForAPI() {
        List<StorageRoom> rooms = storageRoomDAO.getAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageRoom room : rooms) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getId());
            map.put("name", room.getName());
            map.put("code", room.getCode());
            map.put("description", room.getDescription());
            map.put("active", room.getActive());
            map.put("fhirUuid", room.getFhirUuidAsString());

            // Calculate counts within transaction
            try {
                List<StorageDevice> devices = storageDeviceDAO.findByParentRoomId(room.getId());
                map.put("deviceCount", devices != null ? devices.size() : 0);

                int sampleCount = 0;
                if (devices != null) {
                    for (StorageDevice device : devices) {
                        if (device != null && device.getId() != null) {
                            List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
                            if (shelves != null) {
                                for (StorageShelf shelf : shelves) {
                                    if (shelf != null && shelf.getId() != null) {
                                        List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                                        if (racks != null) {
                                            for (StorageRack rack : racks) {
                                                if (rack != null && rack.getId() != null) {
                                                    sampleCount += storagePositionDAO.countOccupied(rack.getId());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                map.put("sampleCount", sampleCount);
            } catch (Exception e) {
                map.put("deviceCount", 0);
                map.put("sampleCount", 0);
            }

            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDevicesForAPI(Integer roomId) {
        List<StorageDevice> devices;
        if (roomId != null) {
            devices = storageDeviceDAO.findByParentRoomId(roomId);
        } else {
            devices = storageDeviceDAO.getAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageDevice device : devices) {
            // Initialize relationship within transaction
            StorageRoom parentRoom = device.getParentRoom();
            if (parentRoom != null) {
                parentRoom.getName(); // Trigger lazy load
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", device.getId());
            map.put("name", device.getName());
            map.put("code", device.getCode());
            map.put("type", "device"); // Hierarchy level: device is a location level
            map.put("deviceType", device.getTypeAsString()); // Physical type: freezer, refrigerator, cabinet, etc.
            map.put("temperatureSetting", device.getTemperatureSetting());
            map.put("capacityLimit", device.getCapacityLimit());
            map.put("active", device.getActive());
            map.put("fhirUuid", device.getFhirUuidAsString());

            // Add relationship data - all accessed within transaction
            if (parentRoom != null) {
                map.put("parentRoomId", parentRoom.getId());
                map.put("roomName", parentRoom.getName());
                map.put("parentRoomName", parentRoom.getName());
            }

            // Add occupied count
            try {
                int occupiedCount = storagePositionDAO.countOccupiedInDevice(device.getId());
                map.put("occupiedCount", occupiedCount);
            } catch (Exception e) {
                map.put("occupiedCount", 0);
            }

            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getShelvesForAPI(Integer deviceId) {
        List<StorageShelf> shelves;
        if (deviceId != null) {
            shelves = storageShelfDAO.findByParentDeviceId(deviceId);
        } else {
            shelves = storageShelfDAO.getAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageShelf shelf : shelves) {
            // Initialize relationships within transaction
            StorageDevice parentDevice = shelf.getParentDevice();
            StorageRoom parentRoom = null;
            if (parentDevice != null) {
                parentDevice.getName(); // Trigger lazy load
                parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null) {
                    parentRoom.getName(); // Trigger lazy load
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", shelf.getId());
            map.put("label", shelf.getLabel());
            map.put("capacityLimit", shelf.getCapacityLimit());
            map.put("active", shelf.getActive());
            map.put("fhirUuid", shelf.getFhirUuidAsString());

            // Add relationship data - all accessed within transaction
            if (parentDevice != null) {
                map.put("parentDeviceId", parentDevice.getId());
                map.put("deviceName", parentDevice.getName());
                map.put("parentDeviceName", parentDevice.getName());
            }
            if (parentRoom != null) {
                map.put("parentRoomId", parentRoom.getId());
                map.put("roomName", parentRoom.getName());
                map.put("parentRoomName", parentRoom.getName());
            }

            // Set type for consistency with searchLocations
            map.put("type", "shelf");

            // Count occupied positions
            try {
                int occupiedCount = 0;
                if (shelf.getId() != null) {
                    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                    if (racks != null) {
                        for (StorageRack rack : racks) {
                            if (rack != null && rack.getId() != null) {
                                occupiedCount += storagePositionDAO.countOccupied(rack.getId());
                            }
                        }
                    }
                }
                map.put("occupiedCount", occupiedCount);
            } catch (Exception e) {
                map.put("occupiedCount", 0);
            }

            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRacksForAPI(Integer shelfId) {
        List<StorageRack> racks;
        if (shelfId != null) {
            racks = storageRackDAO.findByParentShelfId(shelfId);
        } else {
            racks = storageRackDAO.getAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageRack rack : racks) {
            // Initialize relationships within transaction
            StorageShelf parentShelf = rack.getParentShelf();
            StorageDevice parentDevice = null;
            if (parentShelf != null) {
                parentShelf.getLabel(); // Trigger lazy load
                parentDevice = parentShelf.getParentDevice();
                if (parentDevice != null) {
                    parentDevice.getName(); // Trigger lazy load
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", rack.getId());
            map.put("label", rack.getLabel());
            map.put("rows", rack.getRows());
            map.put("columns", rack.getColumns());
            map.put("positionSchemaHint", rack.getPositionSchemaHint());
            map.put("active", rack.getActive());
            map.put("fhirUuid", rack.getFhirUuidAsString());

            // Add relationship data - all accessed within transaction
            StorageRoom parentRoom = null;
            if (parentDevice != null) {
                parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null) {
                    parentRoom.getName(); // Trigger lazy load
                }
            }

            if (parentShelf != null) {
                map.put("parentShelfId", parentShelf.getId());
                map.put("shelfLabel", parentShelf.getLabel());
                map.put("parentShelfLabel", parentShelf.getLabel());
            }
            if (parentDevice != null) {
                map.put("parentDeviceId", parentDevice.getId());
                map.put("deviceName", parentDevice.getName());
                map.put("parentDeviceName", parentDevice.getName());
            }
            // FR-065a: Include parentRoomId and room name
            if (parentRoom != null) {
                map.put("parentRoomId", parentRoom.getId());
                map.put("roomName", parentRoom.getName());
                map.put("parentRoomName", parentRoom.getName());
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
                map.put("hierarchicalPath", pathBuilder.toString());
            }
            
            // Set type for consistency with searchLocations
            map.put("type", "rack");

            // Add occupied count
            try {
                if (rack.getId() != null) {
                    int occupiedCount = storagePositionDAO.countOccupied(rack.getId());
                    map.put("occupiedCount", occupiedCount);
                } else {
                    map.put("occupiedCount", 0);
                }
            } catch (Exception e) {
                map.put("occupiedCount", 0);
            }

            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchLocations(String searchTerm) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Search across all hierarchy levels
        List<Map<String, Object>> rooms = storageSearchService.searchRooms(searchTerm);
        List<Map<String, Object>> devices = storageSearchService.searchDevices(searchTerm);
        List<Map<String, Object>> shelves = storageSearchService.searchShelves(searchTerm);
        List<Map<String, Object>> racks = storageSearchService.searchRacks(searchTerm);

        // Add hierarchical paths and type information
        for (Map<String, Object> room : rooms) {
            Map<String, Object> result = new HashMap<>(room);
            result.put("hierarchicalPath", room.get("name"));
            result.put("type", "room");
            // Rooms have no parents
            results.add(result);
        }

        for (Map<String, Object> device : devices) {
            Map<String, Object> result = new HashMap<>(device);
            String roomName = (String) device.get("roomName");
            String deviceName = (String) device.get("name");
            String path = roomName != null ? roomName + " > " + deviceName : deviceName;
            result.put("hierarchicalPath", path);
            // Ensure type is set to hierarchy level (device is already set by getDevicesForAPI)
            result.put("type", "device");
            // Preserve deviceType from getDevicesForAPI (physical type: freezer, refrigerator, etc.)
            // deviceType is already in the map from getDevicesForAPI, no need to override
            
            // Ensure parentRoomId and parentRoomName are explicitly set
            Object parentRoomId = device.get("parentRoomId");
            if (parentRoomId != null) {
                result.put("parentRoomId", parentRoomId);
            }
            if (roomName != null) {
                result.put("parentRoomName", roomName);
            }
            
            results.add(result);
        }

        for (Map<String, Object> shelf : shelves) {
            Map<String, Object> result = new HashMap<>(shelf);
            String roomName = (String) shelf.get("roomName");
            String deviceName = (String) shelf.get("deviceName");
            String shelfLabel = (String) shelf.get("label");
            StringBuilder pathBuilder = new StringBuilder();
            if (roomName != null) {
                pathBuilder.append(roomName).append(" > ");
            }
            if (deviceName != null) {
                pathBuilder.append(deviceName).append(" > ");
            }
            pathBuilder.append(shelfLabel);
            result.put("hierarchicalPath", pathBuilder.toString());
            result.put("type", "shelf");
            
            // Ensure parent IDs and names are explicitly set (they should already be in the map from getShelvesForAPI)
            Object parentDeviceId = shelf.get("parentDeviceId");
            Object parentRoomId = shelf.get("parentRoomId");
            if (parentDeviceId != null) {
                result.put("parentDeviceId", parentDeviceId);
            }
            if (parentRoomId != null) {
                result.put("parentRoomId", parentRoomId);
            }
            // Parent names should already be in the map, but ensure they're explicitly set
            if (deviceName != null) {
                result.put("parentDeviceName", deviceName);
            }
            if (roomName != null) {
                result.put("parentRoomName", roomName);
            }
            
            results.add(result);
        }

        for (Map<String, Object> rack : racks) {
            Map<String, Object> result = new HashMap<>(rack);
            String roomName = (String) rack.get("roomName");
            String deviceName = (String) rack.get("deviceName");
            String shelfLabel = (String) rack.get("shelfLabel");
            String rackLabel = (String) rack.get("label");
            StringBuilder pathBuilder = new StringBuilder();
            if (roomName != null) {
                pathBuilder.append(roomName).append(" > ");
            }
            if (deviceName != null) {
                pathBuilder.append(deviceName).append(" > ");
            }
            if (shelfLabel != null) {
                pathBuilder.append(shelfLabel).append(" > ");
            }
            pathBuilder.append(rackLabel);
            result.put("hierarchicalPath", pathBuilder.toString());
            result.put("type", "rack");
            
            // Ensure parent IDs and names are explicitly set (they should already be in the map from getRacksForAPI)
            Object parentShelfId = rack.get("parentShelfId");
            Object parentDeviceId = rack.get("parentDeviceId");
            Object parentRoomId = rack.get("parentRoomId");
            if (parentShelfId != null) {
                result.put("parentShelfId", parentShelfId);
            }
            if (parentDeviceId != null) {
                result.put("parentDeviceId", parentDeviceId);
            }
            if (parentRoomId != null) {
                result.put("parentRoomId", parentRoomId);
            }
            // Parent names should already be in the map, but ensure they're explicitly set
            if (shelfLabel != null) {
                result.put("parentShelfLabel", shelfLabel);
            }
            if (deviceName != null) {
                result.put("parentDeviceName", deviceName);
            }
            if (roomName != null) {
                result.put("parentRoomName", roomName);
            }
            
            results.add(result);
        }

        return results;
    }
}
