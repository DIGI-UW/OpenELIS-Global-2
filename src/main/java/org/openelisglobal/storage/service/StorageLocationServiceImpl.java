package org.openelisglobal.storage.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @PersistenceContext
    private EntityManager entityManager;

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
        // Update only editable fields - code is read-only (ignored if provided)
        existingRoom.setName(room.getName());
        // existingRoom.setCode(room.getCode()); // Code is read-only - do not update
        existingRoom.setDescription(room.getDescription());
        existingRoom.setActive(room.getActive());
        storageRoomDAO.update(existingRoom);
        return existingRoom;
    }

    @Override
    public void deleteRoom(Integer id) {
        StorageRoom room = storageRoomDAO.get(id).orElse(null);
        if (room == null) {
            return;
        }

        // Validate constraints before deletion
        if (!canDeleteRoom(room)) {
            String message = getDeleteConstraintMessage(room);
            throw new LIMSRuntimeException(message);
        }

        delete(room);
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
    public int countOccupiedInShelf(Integer shelfId) {
        return storagePositionDAO.countOccupiedInShelf(shelfId);
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
            StorageRoom room = (StorageRoom) entity;
            // Get existing room to preserve read-only fields
            StorageRoom existingRoom = storageRoomDAO.get(room.getId()).orElse(null);
            if (existingRoom == null) {
                throw new LIMSRuntimeException("Room not found: " + room.getId());
            }
            // Update only editable fields - code is read-only
            existingRoom.setName(room.getName());
            existingRoom.setDescription(room.getDescription());
            existingRoom.setActive(room.getActive());
            storageRoomDAO.update(existingRoom);
            return null;
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Get existing device to preserve read-only fields
            StorageDevice existingDevice = storageDeviceDAO.get(device.getId()).orElse(null);
            if (existingDevice == null) {
                throw new LIMSRuntimeException("Device not found: " + device.getId());
            }
            // Update only editable fields - code and parentRoom are read-only
            existingDevice.setName(device.getName());
            existingDevice.setType(device.getType());
            existingDevice.setTemperatureSetting(device.getTemperatureSetting());
            existingDevice.setCapacityLimit(device.getCapacityLimit());
            existingDevice.setActive(device.getActive());
            // Check for active samples when deactivating (null-safe check)
            if (existingDevice.getActive() != null && !existingDevice.getActive()) {
                int occupiedCount = storagePositionDAO.countOccupiedInDevice(existingDevice.getId());
                if (occupiedCount > 0) {
                    throw new LIMSRuntimeException("Warning: Device has " + occupiedCount + " active samples. "
                            + "Please move or dispose samples before deactivating.");
                }
            }
            storageDeviceDAO.update(existingDevice);
            return null;
        } else if (entity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) entity;
            // Get existing shelf to preserve read-only fields
            StorageShelf existingShelf = storageShelfDAO.get(shelf.getId()).orElse(null);
            if (existingShelf == null) {
                throw new LIMSRuntimeException("Shelf not found: " + shelf.getId());
            }
            // Update only editable fields - parentDevice is read-only
            existingShelf.setLabel(shelf.getLabel());
            existingShelf.setCapacityLimit(shelf.getCapacityLimit());
            existingShelf.setActive(shelf.getActive());
            storageShelfDAO.update(existingShelf);
            return null;
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            // Get existing rack to preserve read-only fields
            StorageRack existingRack = storageRackDAO.get(rack.getId()).orElse(null);
            if (existingRack == null) {
                throw new LIMSRuntimeException("Rack not found: " + rack.getId());
            }
            // Update only editable fields - parentShelf is read-only
            existingRack.setLabel(rack.getLabel());
            existingRack.setRows(rack.getRows());
            existingRack.setColumns(rack.getColumns());
            existingRack.setPositionSchemaHint(rack.getPositionSchemaHint());
            existingRack.setActive(rack.getActive());
            storageRackDAO.update(existingRack);
            return null;
        } else if (entity instanceof StoragePosition) {
            storagePositionDAO.update((StoragePosition) entity);
            return null;
        }
        throw new LIMSRuntimeException("Unsupported entity type for update");
    }

    /**
     * Calculate total capacity for a device using two-tier logic (per FR-062a,
     * FR-062b). Returns null if capacity cannot be determined.
     * 
     * Tier 1: If capacity_limit is set, use that value (manual/static limit) Tier
     * 2: If capacity_limit is NULL, calculate from child shelves: - If ALL shelves
     * have defined capacities (either static capacity_limit OR calculated from
     * their own children), sum those capacities - If ANY shelf lacks defined
     * capacity, return null (capacity cannot be determined)
     * 
     * @param device The device to calculate capacity for
     * @return Integer capacity value, or null if capacity cannot be determined
     */
    @Transactional(readOnly = true)
    public Integer calculateDeviceCapacity(StorageDevice device) {
        // Tier 1: Check if static capacity_limit is set
        if (device.getCapacityLimit() != null && device.getCapacityLimit() > 0) {
            return device.getCapacityLimit();
        }

        // Tier 2: Calculate from child shelves
        List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
        if (shelves == null || shelves.isEmpty()) {
            return null; // No children, cannot determine capacity
        }

        int totalCapacity = 0;
        for (StorageShelf shelf : shelves) {
            Integer shelfCapacity = calculateShelfCapacity(shelf);
            if (shelfCapacity == null) {
                // Any child lacks defined capacity - cannot determine parent capacity
                return null;
            }
            totalCapacity += shelfCapacity;
        }

        return totalCapacity;
    }

    /**
     * Calculate total capacity for a shelf using two-tier logic (per FR-062a,
     * FR-062b). Returns null if capacity cannot be determined.
     * 
     * Tier 1: If capacity_limit is set, use that value (manual/static limit) Tier
     * 2: If capacity_limit is NULL, calculate from child racks: - Racks always have
     * defined capacity (rows × columns per FR-017) - Sum all rack capacities
     * 
     * @param shelf The shelf to calculate capacity for
     * @return Integer capacity value, or null if capacity cannot be determined
     */
    @Transactional(readOnly = true)
    public Integer calculateShelfCapacity(StorageShelf shelf) {
        // Tier 1: Check if static capacity_limit is set
        if (shelf.getCapacityLimit() != null && shelf.getCapacityLimit() > 0) {
            return shelf.getCapacityLimit();
        }

        // Tier 2: Calculate from child racks (racks always have defined capacity)
        List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
        if (racks == null || racks.isEmpty()) {
            return null; // No children, cannot determine capacity
        }

        int totalCapacity = 0;
        for (StorageRack rack : racks) {
            // Racks always have defined capacity (rows × columns)
            int rackCapacity = (rack.getRows() != null ? rack.getRows() : 0)
                    * (rack.getColumns() != null ? rack.getColumns() : 0);
            totalCapacity += rackCapacity;
        }

        return totalCapacity;
    }

    @Override
    public void delete(Object entity) {
        // Note: Constraint validation is done in the controller before calling this
        // method
        // This method assumes constraints have been validated
        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            // Ensure entity is managed by fetching from database
            StorageRoom managedRoom = storageRoomDAO.get(room.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Room not found: " + room.getId()));
            storageRoomDAO.delete(managedRoom);
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Ensure entity is managed by fetching from database
            StorageDevice managedDevice = storageDeviceDAO.get(device.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Device not found: " + device.getId()));
            storageDeviceDAO.delete(managedDevice);
        } else if (entity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) entity;
            // Ensure entity is managed by fetching from database
            StorageShelf managedShelf = storageShelfDAO.get(shelf.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Shelf not found: " + shelf.getId()));
            storageShelfDAO.delete(managedShelf);
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            // Ensure entity is managed by fetching from database
            StorageRack managedRack = storageRackDAO.get(rack.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Rack not found: " + rack.getId()));
            storageRackDAO.delete(managedRack);
        } else if (entity instanceof StoragePosition) {
            StoragePosition position = (StoragePosition) entity;
            // Ensure entity is managed by fetching from database
            StoragePosition managedPosition = storagePositionDAO.get(position.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Position not found: " + position.getId()));
            storagePositionDAO.delete(managedPosition);
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

        // Validate hierarchy integrity: if rack exists, shelf must exist; if coordinate
        // exists, rack must exist
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

                // Count unique sample items assigned to locations within this room
                // This counts distinct sample items from sample_storage_assignment, not
                // occupied positions
                // Storage tracking operates at SampleItem level (physical specimens), not
                // Sample level (orders)
                int sampleCount = countUniqueSamplesInRoom(room.getId(), devices);
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

            // Add capacity calculation (per FR-062a, FR-062b, FR-062c)
            if (device.getCapacityLimit() != null) {
                // Tier 1: Manual capacity limit set
                map.put("capacityType", "manual");
            } else {
                // Tier 2: Calculate from children
                Integer calculatedCapacity = calculateDeviceCapacity(device);
                if (calculatedCapacity != null) {
                    map.put("totalCapacity", calculatedCapacity);
                    map.put("capacityType", "calculated");
                } else {
                    // Capacity cannot be determined
                    map.put("capacityType", null);
                }
            }

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

            // Add capacity calculation (per FR-062a, FR-062b, FR-062c)
            if (shelf.getCapacityLimit() != null) {
                // Tier 1: Manual capacity limit set
                map.put("capacityType", "manual");
            } else {
                // Tier 2: Calculate from children
                Integer calculatedCapacity = calculateShelfCapacity(shelf);
                if (calculatedCapacity != null) {
                    map.put("totalCapacity", calculatedCapacity);
                    map.put("capacityType", "calculated");
                } else {
                    // Capacity cannot be determined
                    map.put("capacityType", null);
                }
            }

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

            // Count occupied positions using dedicated method
            // This handles positions directly under shelf AND positions in racks under
            // shelf
            try {
                int occupiedCount = 0;
                if (shelf.getId() != null) {
                    occupiedCount = storagePositionDAO.countOccupiedInShelf(shelf.getId());
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
            // Ensure type is set to hierarchy level (device is already set by
            // getDevicesForAPI)
            result.put("type", "device");
            // Preserve deviceType from getDevicesForAPI (physical type: freezer,
            // refrigerator, etc.)
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

            // Ensure parent IDs and names are explicitly set (they should already be in the
            // map from getShelvesForAPI)
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

            // Ensure parent IDs and names are explicitly set (they should already be in the
            // map from getRacksForAPI)
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

    // ========== Phase 6: Location CRUD Operations - Constraint Validation Methods
    // ==========

    @Override
    @Transactional(readOnly = true)
    public boolean validateDeleteConstraints(Object locationEntity) {
        if (locationEntity == null) {
            return false;
        }

        if (locationEntity instanceof StorageRoom) {
            return canDeleteRoom((StorageRoom) locationEntity);
        } else if (locationEntity instanceof StorageDevice) {
            return canDeleteDevice((StorageDevice) locationEntity);
        } else if (locationEntity instanceof StorageShelf) {
            return canDeleteShelf((StorageShelf) locationEntity);
        } else if (locationEntity instanceof StorageRack) {
            return canDeleteRack((StorageRack) locationEntity);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeleteLocation(Object locationEntity) {
        return validateDeleteConstraints(locationEntity);
    }

    /**
     * Check if a room can be deleted (no child devices, no active samples)
     */
    private boolean canDeleteRoom(StorageRoom room) {
        if (room == null || room.getId() == null) {
            return false;
        }

        // Check for child devices
        int deviceCount = storageDeviceDAO.countByRoomId(room.getId());
        if (deviceCount > 0) {
            return false;
        }

        // TODO: Check for active samples when
        // SampleStorageService.hasActiveSamplesInLocation() is available
        // For now, we only check for child locations
        return true;
    }

    /**
     * Check if a device can be deleted (no child shelves, no active samples)
     */
    private boolean canDeleteDevice(StorageDevice device) {
        if (device == null || device.getId() == null) {
            return false;
        }

        // Check for child shelves
        int shelfCount = storageShelfDAO.countByDeviceId(device.getId());
        if (shelfCount > 0) {
            return false;
        }

        // TODO: Check for active samples when
        // SampleStorageService.hasActiveSamplesInLocation() is available
        return true;
    }

    /**
     * Check if a shelf can be deleted (no child racks, no active samples)
     */
    private boolean canDeleteShelf(StorageShelf shelf) {
        if (shelf == null || shelf.getId() == null) {
            return false;
        }

        // Check for child racks
        int rackCount = storageRackDAO.countByShelfId(shelf.getId());
        if (rackCount > 0) {
            return false;
        }

        // TODO: Check for active samples when
        // SampleStorageService.hasActiveSamplesInLocation() is available
        return true;
    }

    /**
     * Check if a rack can be deleted (no active samples)
     */
    private boolean canDeleteRack(StorageRack rack) {
        if (rack == null || rack.getId() == null) {
            return false;
        }

        // TODO: Check for active samples when
        // SampleStorageService.hasActiveSamplesInLocation() is available
        // For now, racks can be deleted if no constraints (sample check will be added
        // later)
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeleteConstraintMessage(Object locationEntity) {
        if (locationEntity == null) {
            return "Cannot delete location: location is null";
        }

        if (locationEntity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) locationEntity;
            int deviceCount = storageDeviceDAO.countByRoomId(room.getId());
            if (deviceCount > 0) {
                return String.format("Cannot delete Room '%s' because it contains %d device(s)", room.getName(),
                        deviceCount);
            }
            // TODO: Add sample count check when available
            return "Cannot delete room: unknown constraint";
        } else if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            int shelfCount = storageShelfDAO.countByDeviceId(device.getId());
            if (shelfCount > 0) {
                return String.format("Cannot delete Device '%s' because it contains %d shelf(s)", device.getName(),
                        shelfCount);
            }
            // TODO: Add sample count check when available
            return "Cannot delete device: unknown constraint";
        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            int rackCount = storageRackDAO.countByShelfId(shelf.getId());
            if (rackCount > 0) {
                return String.format("Cannot delete Shelf '%s' because it contains %d rack(s)", shelf.getLabel(),
                        rackCount);
            }
            // TODO: Add sample count check when available
            return "Cannot delete shelf: unknown constraint";
        } else if (locationEntity instanceof StorageRack) {
            StorageRack rack = (StorageRack) locationEntity;
            // TODO: Add sample count check when available
            return "Cannot delete rack: unknown constraint";
        }

        return "Cannot delete location: unknown type";
    }

    /**
     * Count unique sample items assigned to locations within a room. This counts
     * distinct sample items from sample_storage_assignment table, not occupied
     * positions, to get accurate sample item counts. Storage tracking operates at
     * SampleItem level (physical specimens), not Sample level (orders).
     * 
     * @param roomId  The room ID
     * @param devices List of devices in the room (can be null)
     * @return Count of unique sample items assigned to locations in this room
     */
    @Transactional(readOnly = true)
    private int countUniqueSamplesInRoom(Integer roomId, List<StorageDevice> devices) {
        try {
            // Build list of location IDs to check
            List<Integer> locationIds = new ArrayList<>();
            locationIds.add(roomId); // Room itself

            if (devices != null) {
                for (StorageDevice device : devices) {
                    if (device != null && device.getId() != null) {
                        locationIds.add(device.getId());

                        // Get shelves in this device
                        List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
                        if (shelves != null) {
                            for (StorageShelf shelf : shelves) {
                                if (shelf != null && shelf.getId() != null) {
                                    locationIds.add(shelf.getId());

                                    // Get racks in this shelf
                                    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                                    if (racks != null) {
                                        for (StorageRack rack : racks) {
                                            if (rack != null && rack.getId() != null) {
                                                locationIds.add(rack.getId());

                                                // Get positions in this rack
                                                List<StoragePosition> positions = storagePositionDAO
                                                        .findByParentRackId(rack.getId());
                                                if (positions != null) {
                                                    for (StoragePosition position : positions) {
                                                        if (position != null && position.getId() != null) {
                                                            locationIds.add(position.getId());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (locationIds.isEmpty()) {
                return 0;
            }

            // Count distinct sample items from assignments where location matches
            // Use HQL to count distinct sample item IDs (not sample IDs)
            String hql = "SELECT COUNT(DISTINCT ssa.sampleItem.id) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationId IN :locationIds";
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            query.setParameter("locationIds", locationIds);
            Long count = (Long) query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            // If query fails, return 0 (data will show but sample item count will be 0)
            return 0;
        }
    }
}
