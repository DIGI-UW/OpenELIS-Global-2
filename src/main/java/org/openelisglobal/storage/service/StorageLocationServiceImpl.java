package org.openelisglobal.storage.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private StorageBoxDAO storageBoxDAO;

    @Autowired
    private StorageSearchService storageSearchService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private CodeValidationService codeValidationService;

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
        // Auto-generate code from name if not provided
        if (room.getCode() == null || room.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerationService.generateCodeFromName(room.getName(), "room");
            // Check for conflicts and resolve if needed
            Set<String> existingCodes = new HashSet<>();
            List<StorageRoom> allRooms = storageRoomDAO.getAll();
            for (StorageRoom r : allRooms) {
                if (r.getCode() != null) {
                    existingCodes.add(r.getCode().toUpperCase());
                }
            }
            String finalCode = codeGenerationService.generateCodeWithConflictResolution(room.getName(), "room",
                    existingCodes);
            room.setCode(finalCode);
        } else {
            // Validate provided code
            String normalizedCode = codeValidationService.autoUppercase(room.getCode());
            CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
            if (!formatResult.isValid()) {
                throw new LIMSRuntimeException(formatResult.getErrorMessage());
            }
            CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
            if (!lengthResult.isValid()) {
                throw new LIMSRuntimeException(lengthResult.getErrorMessage());
            }
            CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode, "room",
                    null, null);
            if (!uniquenessResult.isValid()) {
                throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
            }
            room.setCode(normalizedCode);
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
        // Update editable fields
        existingRoom.setName(room.getName());
        existingRoom.setDescription(room.getDescription());
        existingRoom.setActive(room.getActive());

        // Code is editable - validate if provided
        if (room.getCode() != null && !room.getCode().equals(existingRoom.getCode())) {
            String normalizedCode = codeValidationService.autoUppercase(room.getCode());
            CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
            if (!formatResult.isValid()) {
                throw new LIMSRuntimeException(formatResult.getErrorMessage());
            }
            CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
            if (!lengthResult.isValid()) {
                throw new LIMSRuntimeException(lengthResult.getErrorMessage());
            }
            CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode, "room",
                    String.valueOf(id), null);
            if (!uniquenessResult.isValid()) {
                throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
            }
            existingRoom.setCode(normalizedCode);
        }
        // Note: Code does NOT regenerate when name changes - only updates if explicitly
        // provided

        storageRoomDAO.update(existingRoom);
        return existingRoom;
    }

    @Override
    public void deleteRoom(Integer id) {
        StorageRoom room = storageRoomDAO.get(id).orElse(null);
        if (room == null) {
            return;
        }

        // Note: Constraint validation is done in the controller before calling this
        // method
        // This method assumes constraints have been validated
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
    public List<StorageBox> getBoxesByRack(Integer rackId) {
        return storageBoxDAO.findByParentRackId(rackId);
    }

    @Override
    public List<StorageBox> getAllBoxes() {
        return storageBoxDAO.getAll();
    }

    @Override
    public int countOccupiedInDevice(Integer deviceId) {
        return storageBoxDAO.countOccupiedInDevice(deviceId);
    }

    @Override
    public int countOccupied(Integer rackId) {
        return storageBoxDAO.countOccupied(rackId);
    }

    @Override
    public int countOccupiedInShelf(Integer shelfId) {
        return storageBoxDAO.countOccupiedInShelf(shelfId);
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

            // Auto-generate code from name if not provided
            if (device.getCode() == null || device.getCode().trim().isEmpty()) {
                String generatedCode = codeGenerationService.generateCodeFromName(device.getName(), "device");
                // Check for conflicts within parent room
                Set<String> existingCodes = new HashSet<>();
                List<StorageDevice> devicesInRoom = storageDeviceDAO.findByParentRoomId(device.getParentRoom().getId());
                for (StorageDevice d : devicesInRoom) {
                    if (d.getCode() != null) {
                        existingCodes.add(d.getCode().toUpperCase());
                    }
                }
                String finalCode = codeGenerationService.generateCodeWithConflictResolution(device.getName(), "device",
                        existingCodes);
                device.setCode(finalCode);
            } else {
                // Validate provided code
                String normalizedCode = codeValidationService.autoUppercase(device.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode,
                        "device", null, String.valueOf(device.getParentRoom().getId()));
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                device.setCode(normalizedCode);
            }

            // Check for duplicate code in same room
            StorageDevice existing = storageDeviceDAO.findByParentRoomIdAndCode(device.getParentRoom().getId(),
                    device.getCode());
            if (existing != null) {
                throw new LIMSRuntimeException("Device with code " + device.getCode() + " already exists in this room");
            }

            return storageDeviceDAO.insert(device);
        } else if (entity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) entity;

            // Auto-generate code from label/name if not provided
            String shelfName = shelf.getLabel() != null ? shelf.getLabel() : "";
            if (shelf.getCode() == null || shelf.getCode().trim().isEmpty()) {
                String generatedCode = codeGenerationService.generateCodeFromName(shelfName, "shelf");
                // Check for conflicts within parent device
                Set<String> existingCodes = new HashSet<>();
                List<StorageShelf> shelvesInDevice = storageShelfDAO
                        .findByParentDeviceId(shelf.getParentDevice().getId());
                for (StorageShelf s : shelvesInDevice) {
                    if (s.getCode() != null) {
                        existingCodes.add(s.getCode().toUpperCase());
                    }
                }
                String finalCode = codeGenerationService.generateCodeWithConflictResolution(shelfName, "shelf",
                        existingCodes);
                shelf.setCode(finalCode);
            } else {
                // Validate provided code
                String normalizedCode = codeValidationService.autoUppercase(shelf.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode,
                        "shelf", null, String.valueOf(shelf.getParentDevice().getId()));
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                shelf.setCode(normalizedCode);
            }

            return storageShelfDAO.insert(shelf);
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            if (rack.getCode() != null && !rack.getCode().trim().isEmpty()) {
                String normalizedCode = codeValidationService.autoUppercase(rack.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                Integer parentShelfId = rack.getParentShelf() != null ? rack.getParentShelf().getId() : null;
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode, "rack",
                        null, parentShelfId != null ? parentShelfId.toString() : null);
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                rack.setCode(normalizedCode);
            }
            return storageRackDAO.insert(rack);
        } else if (entity instanceof StorageBox) {
            StorageBox box = (StorageBox) entity;
            // Validate grid dimensions (boxes are gridded containers)
            if (box.getRows() == null || box.getColumns() == null || box.getRows() < 0 || box.getColumns() < 0) {
                throw new IllegalArgumentException(
                        "Box must have valid grid dimensions (rows and columns cannot be negative)");
            }
            return storageBoxDAO.insert(box);
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
            // Update editable fields
            existingDevice.setName(device.getName());
            existingDevice.setType(device.getType());
            existingDevice.setTemperatureSetting(device.getTemperatureSetting());
            existingDevice.setCapacityLimit(device.getCapacityLimit());
            existingDevice.setActive(device.getActive());

            // Code is editable - validate if provided
            if (device.getCode() != null && !device.getCode().equals(existingDevice.getCode())) {
                String normalizedCode = codeValidationService.autoUppercase(device.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode,
                        "device", String.valueOf(device.getId()),
                        String.valueOf(existingDevice.getParentRoom().getId()));
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                existingDevice.setCode(normalizedCode);
            }
            // Note: Code does NOT regenerate when name changes - only updates if explicitly
            // provided

            // Check for active samples when deactivating (null-safe check)
            if (existingDevice.getActive() != null && !existingDevice.getActive()) {
                int occupiedCount = storageBoxDAO.countOccupiedInDevice(existingDevice.getId());
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
            // Update editable fields
            existingShelf.setLabel(shelf.getLabel());
            existingShelf.setCapacityLimit(shelf.getCapacityLimit());
            existingShelf.setActive(shelf.getActive());

            // Code is editable - validate if provided
            if (shelf.getCode() != null && !shelf.getCode().equals(existingShelf.getCode())) {
                String normalizedCode = codeValidationService.autoUppercase(shelf.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode,
                        "shelf", String.valueOf(shelf.getId()),
                        String.valueOf(existingShelf.getParentDevice().getId()));
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                existingShelf.setCode(normalizedCode);
            }
            // Note: Code does NOT regenerate when label changes - only updates if
            // explicitly provided

            storageShelfDAO.update(existingShelf);
            return null;
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            // Get existing rack to preserve read-only fields
            StorageRack existingRack = storageRackDAO.get(rack.getId()).orElse(null);
            if (existingRack == null) {
                throw new LIMSRuntimeException("Rack not found: " + rack.getId());
            }
            if (rack.getCode() != null && !rack.getCode().trim().isEmpty()) {
                String normalizedCode = codeValidationService.autoUppercase(rack.getCode());
                CodeValidationResult formatResult = codeValidationService.validateFormat(normalizedCode);
                if (!formatResult.isValid()) {
                    throw new LIMSRuntimeException(formatResult.getErrorMessage());
                }
                CodeValidationResult lengthResult = codeValidationService.validateLength(normalizedCode);
                if (!lengthResult.isValid()) {
                    throw new LIMSRuntimeException(lengthResult.getErrorMessage());
                }
                Integer parentShelfId = rack.getParentShelf() != null ? rack.getParentShelf().getId() : null;
                CodeValidationResult uniquenessResult = codeValidationService.validateUniqueness(normalizedCode, "rack",
                        rack.getId().toString(), parentShelfId != null ? parentShelfId.toString() : null);
                if (!uniquenessResult.isValid()) {
                    throw new LIMSRuntimeException(uniquenessResult.getErrorMessage());
                }
                existingRack.setCode(normalizedCode);
            } else {
                existingRack.setCode(null);
            }
            // Update editable fields
            existingRack.setLabel(rack.getLabel());
            existingRack.setActive(rack.getActive());
            existingRack.setParentShelf(rack.getParentShelf());
            // Note: Code does NOT regenerate when label changes - only updates if
            // explicitly provided

            storageRackDAO.update(existingRack);
            return null;
        } else if (entity instanceof StorageBox) {
            StorageBox box = (StorageBox) entity;
            // Get existing box to preserve read-only fields
            StorageBox existingBox = storageBoxDAO.get(box.getId()).orElse(null);
            if (existingBox == null) {
                throw new LIMSRuntimeException("Box not found: " + box.getId());
            }
            // Update editable fields
            existingBox.setLabel(box.getLabel());
            existingBox.setType(box.getType());
            existingBox.setRows(box.getRows());
            existingBox.setColumns(box.getColumns());
            existingBox.setPositionSchemaHint(box.getPositionSchemaHint());
            existingBox.setCode(box.getCode());
            existingBox.setActive(box.getActive());
            // Validate grid dimensions
            if (existingBox.getRows() == null || existingBox.getColumns() == null || existingBox.getRows() < 0
                    || existingBox.getColumns() < 0) {
                throw new IllegalArgumentException("Box must have valid grid dimensions");
            }
            storageBoxDAO.update(existingBox);
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
     * 2: If capacity_limit is NULL, calculate from child racks and their boxes: -
     * Racks are simple containers - Boxes within racks have grid dimensions (rows ×
     * columns) - Sum all box capacities across all racks
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

        // Tier 2: Calculate from child racks and their boxes
        List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
        if (racks == null || racks.isEmpty()) {
            return null; // No children, cannot determine capacity
        }

        int totalCapacity = 0;
        for (StorageRack rack : racks) {
            // Get all boxes in this rack
            List<StorageBox> boxes = storageBoxDAO.findByParentRackId(rack.getId());
            if (boxes != null) {
                for (StorageBox box : boxes) {
                    // Boxes have grid dimensions (rows × columns)
                    int boxCapacity = (box.getRows() != null ? box.getRows() : 0)
                            * (box.getColumns() != null ? box.getColumns() : 0);
                    totalCapacity += boxCapacity;
                }
            }
        }

        return totalCapacity > 0 ? totalCapacity : null;
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
        } else if (entity instanceof StorageBox) {
            StorageBox box = (StorageBox) entity;
            // Ensure entity is managed by fetching from database
            StorageBox managedBox = storageBoxDAO.get(box.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Box not found: " + box.getId()));
            storageBoxDAO.delete(managedBox);
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
        } else if (entityClass == StorageBox.class) {
            return storageBoxDAO.get(id).orElse(null);
        }
        throw new LIMSRuntimeException("Unsupported entity class for get");
    }

    @Override
    public boolean validateLocationActive(StorageBox box) {
        if (box == null) {
            return false;
        }

        // Validate parent rack exists
        if (box.getParentRack() == null) {
            return false;
        }

        StorageRack rack = box.getParentRack();
        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf != null ? shelf.getParentDevice() : null;
        StorageRoom room = device != null ? device.getParentRoom() : null;

        if (device == null || room == null) {
            return false;
        }

        // Check room and device are active
        if (room.getActive() == null || !room.getActive()) {
            return false;
        }
        if (device.getActive() == null || !device.getActive()) {
            return false;
        }

        // Check optional parents are active if they exist
        if (shelf != null && (shelf.getActive() == null || !shelf.getActive())) {
            return false;
        }

        if (rack.getActive() == null || !rack.getActive()) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String buildHierarchicalPath(StorageBox box) {
        if (box == null) {
            return "Unknown Location";
        }

        StorageRack rack = box.getParentRack();
        if (rack == null || rack.getParentShelf() == null) {
            return "Unknown";
        }

        StorageShelf shelf = rack.getParentShelf();
        StorageDevice device = shelf.getParentDevice();
        StorageRoom room = device != null ? device.getParentRoom() : null;
        if (room == null) {
            return rack.getLabel();
        }

        StringBuilder path = new StringBuilder();
        path.append(room.getName()).append(" > ").append(device.getName());
        path.append(" > ").append(shelf.getLabel());
        path.append(" > ").append(rack.getLabel());

        if (box.getLabel() != null && !box.getLabel().isEmpty()) {
            path.append(" > ").append(box.getLabel());
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
                int occupiedCount = storageBoxDAO.countOccupiedInDevice(device.getId());
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

            // Count occupied boxes using dedicated method (boxes within racks on shelf)
            try {
                int occupiedCount = 0;
                if (shelf.getId() != null) {
                    occupiedCount = storageBoxDAO.countOccupiedInShelf(shelf.getId());
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
            map.put("code", rack.getCode());
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
                    int occupiedCount = storageBoxDAO.countOccupied(rack.getId());
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
    public List<Map<String, Object>> getBoxesForAPI(Integer rackId) {
        List<StorageBox> boxes;
        if (rackId != null) {
            boxes = storageBoxDAO.findByParentRackId(rackId);
        } else {
            boxes = storageBoxDAO.getAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (StorageBox box : boxes) {
            StorageRack parentRack = box.getParentRack();
            StorageShelf parentShelf = parentRack != null ? parentRack.getParentShelf() : null;
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            StorageRoom parentRoom = parentDevice != null ? parentDevice.getParentRoom() : null;

            Map<String, Object> map = new HashMap<>();
            map.put("id", box.getId());
            map.put("label", box.getLabel());
            map.put("type", box.getType());
            map.put("rows", box.getRows());
            map.put("columns", box.getColumns());
            map.put("capacity", box.getCapacity());
            map.put("positionSchemaHint", box.getPositionSchemaHint());
            map.put("code", box.getCode());
            map.put("active", box.getActive());
            map.put("fhirUuid", box.getFhirUuidAsString());
            map.put("locationType", "box");

            if (parentRack != null) {
                map.put("parentRackId", parentRack.getId());
                map.put("rackLabel", parentRack.getLabel());
            }
            if (parentShelf != null) {
                map.put("parentShelfId", parentShelf.getId());
                map.put("shelfLabel", parentShelf.getLabel());
            }
            if (parentDevice != null) {
                map.put("parentDeviceId", parentDevice.getId());
                map.put("deviceName", parentDevice.getName());
            }
            if (parentRoom != null) {
                map.put("parentRoomId", parentRoom.getId());
                map.put("roomName", parentRoom.getName());
            }

            // Build hierarchical path
            StringBuilder path = new StringBuilder();
            if (parentRoom != null && parentRoom.getName() != null) {
                path.append(parentRoom.getName());
            }
            if (parentDevice != null && parentDevice.getName() != null) {
                if (path.length() > 0) {
                    path.append(" > ");
                }
                path.append(parentDevice.getName());
            }
            if (parentShelf != null && parentShelf.getLabel() != null) {
                if (path.length() > 0) {
                    path.append(" > ");
                }
                path.append(parentShelf.getLabel());
            }
            if (parentRack != null && parentRack.getLabel() != null) {
                if (path.length() > 0) {
                    path.append(" > ");
                }
                path.append(parentRack.getLabel());
            }
            if (box.getLabel() != null) {
                if (path.length() > 0) {
                    path.append(" > ");
                }
                path.append(box.getLabel());
            }
            map.put("hierarchicalPath", path.toString());

            // Occupancy
            boolean occupied = sampleStorageAssignmentDAO.isBoxOccupied(box);
            map.put("occupied", occupied);

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
        } else if (locationEntity instanceof StorageBox) {
            return canDeleteBox((StorageBox) locationEntity);
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

        return true;
    }

    /**
     * Check if a device can be deleted (no child shelves, no active samples)
     * OGC-75: Added sample count check
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

        // OGC-75: Check for active samples assigned to this device
        int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("device", device.getId());
        if (sampleCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if a shelf can be deleted (no child racks, no active samples) OGC-75:
     * Added sample count check
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

        // OGC-75: Check for active samples assigned to this shelf
        int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("shelf", shelf.getId());
        if (sampleCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if a rack can be deleted (no active samples) OGC-75: Added sample count
     * check
     */
    private boolean canDeleteRack(StorageRack rack) {
        if (rack == null || rack.getId() == null) {
            return false;
        }

        // OGC-75: Check for active samples assigned to this rack
        int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", rack.getId());
        if (sampleCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if a box can be deleted (no active samples assigned)
     */
    private boolean canDeleteBox(StorageBox box) {
        if (box == null || box.getId() == null) {
            return false;
        }

        int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("box", box.getId());
        return sampleCount == 0;
    }

    /**
     * OGC-75: Updated to include sample counts in error messages
     */
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
            return "Cannot delete room: unknown constraint";
        } else if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            int shelfCount = storageShelfDAO.countByDeviceId(device.getId());
            if (shelfCount > 0) {
                return String.format("Cannot delete Device '%s' because it contains %d shelf(s)", device.getName(),
                        shelfCount);
            }
            // OGC-75: Check for assigned samples
            int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("device", device.getId());
            if (sampleCount > 0) {
                return String.format("Cannot delete Device '%s' because it has %d sample(s) assigned", device.getName(),
                        sampleCount);
            }
            return "Cannot delete device: unknown constraint";
        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            int rackCount = storageRackDAO.countByShelfId(shelf.getId());
            if (rackCount > 0) {
                return String.format("Cannot delete Shelf '%s' because it contains %d rack(s)", shelf.getLabel(),
                        rackCount);
            }
            // OGC-75: Check for assigned samples
            int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("shelf", shelf.getId());
            if (sampleCount > 0) {
                return String.format("Cannot delete Shelf '%s' because it has %d sample(s) assigned", shelf.getLabel(),
                        sampleCount);
            }
            return "Cannot delete shelf: unknown constraint";
        } else if (locationEntity instanceof StorageRack) {
            StorageRack rack = (StorageRack) locationEntity;
            // OGC-75: Check for assigned samples
            int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", rack.getId());
            if (sampleCount > 0) {
                return String.format("Cannot delete Rack '%s' because it has %d sample(s) assigned", rack.getLabel(),
                        sampleCount);
            }
            return "Cannot delete rack: unknown constraint";
        } else if (locationEntity instanceof StorageBox) {
            StorageBox box = (StorageBox) locationEntity;
            int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("box", box.getId());
            if (sampleCount > 0) {
                return String.format("Cannot delete Box '%s' because it has %d sample(s) assigned", box.getLabel(),
                        sampleCount);
            }
            return "Cannot delete box: unknown constraint";
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

                                                // Get boxes in this rack
                                                List<StorageBox> boxes = storageBoxDAO.findByParentRackId(rack.getId());
                                                if (boxes != null) {
                                                    for (StorageBox box : boxes) {
                                                        if (box != null && box.getId() != null) {
                                                            locationIds.add(box.getId());
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

    /**
     * OGC-75: Get summary of what will be deleted in a cascade delete operation
     * Returns counts of child locations and samples that will be affected
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCascadeDeleteSummary(Object locationEntity) {
        Map<String, Object> summary = new HashMap<>();
        Map<String, Integer> childLocations = new HashMap<>();
        int totalSampleCount = 0;
        String childLocationType = null;
        int childLocationCount = 0;

        if (locationEntity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) locationEntity;
            List<StorageDevice> devices = storageDeviceDAO.findByParentRoomId(room.getId());
            childLocationCount = devices.size();
            childLocationType = "device";
            childLocations.put("devices", devices.size());

            // Count samples in room hierarchy
            totalSampleCount = countUniqueSamplesInRoom(room.getId(), devices);

            // Count child shelves and racks
            int shelfCount = 0;
            int rackCount = 0;
            for (StorageDevice device : devices) {
                List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
                shelfCount += shelves.size();
                childLocations.put("shelves", shelfCount);
                for (StorageShelf shelf : shelves) {
                    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                    rackCount += racks.size();
                }
            }
            childLocations.put("racks", rackCount);

        } else if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
            childLocationCount = shelves.size();
            childLocationType = "shelf";
            childLocations.put("shelves", shelves.size());

            // Count samples in device hierarchy
            totalSampleCount = countSamplesInDeviceHierarchy(device.getId());

            // Count child racks
            int rackCount = 0;
            for (StorageShelf shelf : shelves) {
                List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                rackCount += racks.size();
            }
            childLocations.put("racks", rackCount);

        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
            childLocationCount = racks.size();
            childLocationType = "rack";
            childLocations.put("racks", racks.size());

            // Count samples in shelf hierarchy
            totalSampleCount = countSamplesInShelfHierarchy(shelf.getId());

        } else if (locationEntity instanceof StorageRack) {
            StorageRack rack = (StorageRack) locationEntity;
            // Racks have no child locations, only samples
            childLocationCount = 0;
            childLocationType = null;

            // Count samples assigned to this rack
            totalSampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", rack.getId());
        }

        summary.put("childLocations", childLocations);
        summary.put("sampleCount", totalSampleCount);
        summary.put("childLocationType", childLocationType);
        summary.put("childLocationCount", childLocationCount);

        return summary;
    }

    /**
     * Check if a location can be moved to a new parent, and if samples exist
     * downstream Always allows the move but warns if samples exist in the
     * location's hierarchy
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> canMoveLocation(Object locationEntity, Integer newParentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("canMove", true); // Always allow move, but warn about samples

        int sampleCount = 0;
        String warning = null;

        if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            sampleCount = countSamplesInDeviceHierarchy(device.getId());
            if (sampleCount > 0) {
                warning = String.format(
                        "Moving this device will affect %d sample(s) assigned to this device and its child locations. The samples will remain assigned but their hierarchical path will change.",
                        sampleCount);
            }
        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            sampleCount = countSamplesInShelfHierarchy(shelf.getId());
            if (sampleCount > 0) {
                warning = String.format(
                        "Moving this shelf will affect %d sample(s) assigned to this shelf and its child locations. The samples will remain assigned but their hierarchical path will change.",
                        sampleCount);
            }
        } else if (locationEntity instanceof StorageRack) {
            StorageRack rack = (StorageRack) locationEntity;
            sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", rack.getId());
            if (sampleCount > 0) {
                warning = String.format(
                        "Moving this rack will affect %d sample(s) assigned to this rack. The samples will remain assigned but their hierarchical path will change.",
                        sampleCount);
            }
        } else {
            // Rooms cannot be moved (no parent)
            result.put("canMove", false);
            result.put("error", "Rooms cannot be moved");
            return result;
        }

        result.put("hasDownstreamSamples", sampleCount > 0);
        result.put("sampleCount", sampleCount);
        if (warning != null) {
            result.put("warning", warning);
        }

        return result;
    }

    /**
     * OGC-75: Count samples in device hierarchy (device + all shelves + all racks)
     */
    @Transactional(readOnly = true)
    private int countSamplesInDeviceHierarchy(Integer deviceId) {
        List<Integer> locationIds = new ArrayList<>();
        locationIds.add(deviceId);

        List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(deviceId);
        for (StorageShelf shelf : shelves) {
            locationIds.add(shelf.getId());
            List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
            for (StorageRack rack : racks) {
                locationIds.add(rack.getId());
            }
        }

        if (locationIds.isEmpty()) {
            return 0;
        }

        try {
            String hql = "SELECT COUNT(DISTINCT ssa.sampleItem.id) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationId IN :locationIds";
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            query.setParameter("locationIds", locationIds);
            Long count = (Long) query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * OGC-75: Count samples in shelf hierarchy (shelf + all racks)
     */
    @Transactional(readOnly = true)
    private int countSamplesInShelfHierarchy(Integer shelfId) {
        List<Integer> locationIds = new ArrayList<>();
        locationIds.add(shelfId);

        List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelfId);
        for (StorageRack rack : racks) {
            locationIds.add(rack.getId());
        }

        if (locationIds.isEmpty()) {
            return 0;
        }

        try {
            String hql = "SELECT COUNT(DISTINCT ssa.sampleItem.id) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationId IN :locationIds";
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            query.setParameter("locationIds", locationIds);
            Long count = (Long) query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * OGC-75: Find all child locations recursively (for cascade deletion)
     */
    @Transactional(readOnly = true)
    private List<Object> findAllChildLocations(Object locationEntity) {
        List<Object> children = new ArrayList<>();

        if (locationEntity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) locationEntity;
            List<StorageDevice> devices = storageDeviceDAO.findByParentRoomId(room.getId());
            for (StorageDevice device : devices) {
                children.add(device);
                children.addAll(findAllChildLocations(device));
            }
        } else if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
            for (StorageShelf shelf : shelves) {
                children.add(shelf);
                children.addAll(findAllChildLocations(shelf));
            }
        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
            for (StorageRack rack : racks) {
                children.add(rack);
                // Racks have no child locations
            }
        }
        // StorageRack has no child locations

        return children;
    }

    /**
     * OGC-75: Unassign all samples from location and its children
     */
    @Transactional
    private void unassignSamplesFromHierarchy(Object locationEntity) {
        List<Integer> locationIds = new ArrayList<>();
        String locationType = null;

        if (locationEntity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) locationEntity;
            // Room-level assignments not currently supported, but include for completeness
            List<StorageDevice> devices = storageDeviceDAO.findByParentRoomId(room.getId());
            for (StorageDevice device : devices) {
                locationIds.add(device.getId());
                List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
                for (StorageShelf shelf : shelves) {
                    locationIds.add(shelf.getId());
                    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                    for (StorageRack rack : racks) {
                        locationIds.add(rack.getId());
                    }
                }
            }
        } else if (locationEntity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) locationEntity;
            locationIds.add(device.getId());
            locationType = "device";
            List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
            for (StorageShelf shelf : shelves) {
                locationIds.add(shelf.getId());
                List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
                for (StorageRack rack : racks) {
                    locationIds.add(rack.getId());
                }
            }
        } else if (locationEntity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) locationEntity;
            locationIds.add(shelf.getId());
            locationType = "shelf";
            List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
            for (StorageRack rack : racks) {
                locationIds.add(rack.getId());
            }
        } else if (locationEntity instanceof StorageRack) {
            StorageRack rack = (StorageRack) locationEntity;
            locationIds.add(rack.getId());
            locationType = "rack";
        }

        if (locationIds.isEmpty()) {
            return;
        }

        // Find and delete all assignments for these locations
        try {
            String hql = "SELECT ssa FROM SampleStorageAssignment ssa WHERE ssa.locationId IN :locationIds";
            jakarta.persistence.Query query = entityManager.createQuery(hql);
            query.setParameter("locationIds", locationIds);
            @SuppressWarnings("unchecked")
            List<org.openelisglobal.storage.valueholder.SampleStorageAssignment> assignments = query.getResultList();
            for (org.openelisglobal.storage.valueholder.SampleStorageAssignment assignment : assignments) {
                sampleStorageAssignmentDAO.delete(assignment);
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error unassigning samples from location hierarchy", e);
        }
    }

    /**
     * OGC-75: Delete location with cascade deletion of all child locations and
     * unassignment of all samples Deletes children bottom-up (racks → shelves →
     * devices → rooms) to maintain referential integrity
     */
    @Override
    @Transactional
    public void deleteLocationWithCascade(Integer id, Class<?> locationClass) {
        Object locationEntity = get(id, locationClass);
        if (locationEntity == null) {
            throw new LIMSRuntimeException("Location not found: " + id);
        }

        // Step 1: Find all child locations (recursively)
        List<Object> childLocations = findAllChildLocations(locationEntity);

        // Step 2: Unassign all samples from location and its children
        unassignSamplesFromHierarchy(locationEntity);

        // Step 3: Delete child locations bottom-up (racks first, then shelves, then
        // devices)
        // Sort children by type: racks first, then shelves, then devices
        List<StorageRack> racksToDelete = new ArrayList<>();
        List<StorageShelf> shelvesToDelete = new ArrayList<>();
        List<StorageDevice> devicesToDelete = new ArrayList<>();

        for (Object child : childLocations) {
            if (child instanceof StorageRack) {
                racksToDelete.add((StorageRack) child);
            } else if (child instanceof StorageShelf) {
                shelvesToDelete.add((StorageShelf) child);
            } else if (child instanceof StorageDevice) {
                devicesToDelete.add((StorageDevice) child);
            }
        }

        // Delete in order: racks → shelves → devices
        for (StorageRack rack : racksToDelete) {
            storageRackDAO.delete(rack);
        }
        for (StorageShelf shelf : shelvesToDelete) {
            storageShelfDAO.delete(shelf);
        }
        for (StorageDevice device : devicesToDelete) {
            storageDeviceDAO.delete(device);
        }

        // Step 4: Delete the location itself
        delete(locationEntity);
    }

    // Deletion Validation Methods

    @Override
    public DeletionValidationResult canDeleteRoom(Integer roomId) {
        StorageRoom room = storageRoomDAO.get(roomId).orElse(null);
        if (room == null) {
            return DeletionValidationResult.success(); // Room doesn't exist, deletion allowed
        }

        List<StorageDevice> childDevices = getDevicesByRoom(roomId);
        if (!childDevices.isEmpty()) {
            return DeletionValidationResult.referentialIntegrityViolation("Room", room.getName(), "devices",
                    childDevices.size());
        }

        return DeletionValidationResult.success();
    }

    @Override
    public DeletionValidationResult canDeleteDevice(Integer deviceId) {
        StorageDevice device = storageDeviceDAO.get(deviceId).orElse(null);
        if (device == null) {
            return DeletionValidationResult.success(); // Device doesn't exist, deletion allowed
        }

        List<StorageShelf> childShelves = getShelvesByDevice(deviceId);
        if (!childShelves.isEmpty()) {
            return DeletionValidationResult.referentialIntegrityViolation("Device", device.getName(), "shelves",
                    childShelves.size());
        }

        return DeletionValidationResult.success();
    }

    @Override
    public DeletionValidationResult canDeleteShelf(Integer shelfId) {
        StorageShelf shelf = storageShelfDAO.get(shelfId).orElse(null);
        if (shelf == null) {
            return DeletionValidationResult.success(); // Shelf doesn't exist, deletion allowed
        }

        List<StorageRack> childRacks = getRacksByShelf(shelfId);
        if (!childRacks.isEmpty()) {
            return DeletionValidationResult.referentialIntegrityViolation("Shelf", shelf.getLabel(), "racks",
                    childRacks.size());
        }

        return DeletionValidationResult.success();
    }

    @Override
    public DeletionValidationResult canDeleteRack(Integer rackId) {
        StorageRack rack = storageRackDAO.get(rackId).orElse(null);
        if (rack == null) {
            return DeletionValidationResult.success(); // Rack doesn't exist, deletion allowed
        }

        // Check for assigned samples
        int assignedSampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", rackId);
        if (assignedSampleCount > 0) {
            return DeletionValidationResult.activeAssignments("Rack", rack.getLabel(), assignedSampleCount);
        }

        return DeletionValidationResult.success();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameUniqueWithinParent(String name, Integer parentId, String locationType, Integer excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        String trimmedName = name.trim();
        switch (locationType) {
        case "room": {
            StorageRoom existingRoom = storageRoomDAO.findByName(trimmedName);
            return existingRoom == null || existingRoom.getId().equals(excludeId);
        }
        case "device": {
            if (parentId == null) {
                return true;
            }
            StorageDevice existingDevice = storageDeviceDAO.findByNameAndParentRoomId(trimmedName, parentId);
            return existingDevice == null || existingDevice.getId().equals(excludeId);
        }
        case "shelf": {
            if (parentId == null) {
                return true;
            }
            StorageShelf existingShelf = storageShelfDAO.findByLabelAndParentDeviceId(trimmedName, parentId);
            return existingShelf == null || existingShelf.getId().equals(excludeId);
        }
        case "rack": {
            if (parentId == null) {
                return true;
            }
            StorageRack existingRack = storageRackDAO.findByLabelAndParentShelfId(trimmedName, parentId);
            return existingRack == null || existingRack.getId().equals(excludeId);
        }
        default:
            return true;
        }
    }

    @Override
    public boolean isCodeUniqueForRoom(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Null/empty codes are allowed
        }
        String trimmedCode = code.trim();
        StorageRoom existingRoom = storageRoomDAO.findByCode(trimmedCode);
        return existingRoom == null || existingRoom.getId().equals(excludeId);
    }

    @Override
    public boolean isCodeUniqueForDevice(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Null/empty codes are allowed
        }
        String trimmedCode = code.trim();
        StorageDevice existingDevice = storageDeviceDAO.findByCode(trimmedCode);
        return existingDevice == null || existingDevice.getId().equals(excludeId);
    }

    @Override
    public boolean isCodeUniqueForShelf(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Null/empty codes are allowed
        }
        String trimmedCode = code.trim();
        StorageShelf existingShelf = storageShelfDAO.findByCode(trimmedCode);
        return existingShelf == null || existingShelf.getId().equals(excludeId);
    }

    @Override
    public boolean isCodeUniqueForRack(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Null/empty codes are allowed
        }
        String trimmedCode = code.trim();
        StorageRack existingRack = storageRackDAO.findByCode(trimmedCode);
        return existingRack == null || existingRack.getId().equals(excludeId);
    }
}
