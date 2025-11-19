package org.openelisglobal.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.form.*;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.service.StorageSearchService;
import org.openelisglobal.storage.valueholder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Storage Location management Handles CRUD operations for
 * all storage hierarchy levels: Room, Device, Shelf, Rack, Position
 */
@RestController
@RequestMapping("/rest/storage")
public class StorageLocationRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(StorageLocationRestController.class);

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private StorageDashboardService storageDashboardService;

    @Autowired
    private StorageSearchService storageSearchService;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== Room Endpoints ==========

    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody StorageRoomForm form) {
        try {
            StorageRoom room = new StorageRoom();
            room.setName(form.getName());
            // Generate code if not provided
            if (form.getCode() == null || form.getCode().trim().isEmpty()) {
                room.setCode(generateUniqueRoomCode(form.getName()));
            } else {
                room.setCode(form.getCode());
            }
            room.setDescription(form.getDescription());
            room.setActive(form.getActive() != null ? form.getActive() : true);
            room.setFhirUuid(UUID.randomUUID());
            room.setSysUserId("1"); // Default system user for REST API (should come from security context in
                                    // production)

            StorageRoom createdRoom = storageLocationService.createRoom(room);

            Map<String, Object> response = entityToMap(createdRoom);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error creating room: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error creating room", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get rooms with optional status filter (FR-065: Rooms tab - filter by status)
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRooms(@RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> response;
            if (status != null && !status.isEmpty()) {
                // Filter by status - service returns Maps with all data resolved
                Boolean activeStatus = "active".equalsIgnoreCase(status) ? true
                        : "inactive".equalsIgnoreCase(status) ? false : null;
                response = storageDashboardService.filterRoomsForAPI(activeStatus);
            } else {
                // No filter - return all rooms
                response = storageLocationService.getRoomsForAPI();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting rooms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> getRoomById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(room));
        } catch (Exception e) {
            logger.error("Error getting room by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> updateRoom(@PathVariable String id,
            @Valid @RequestBody StorageRoomForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom roomToUpdate = new StorageRoom();
            roomToUpdate.setName(form.getName());
            // Code is read-only - ignored if provided in form
            // roomToUpdate.setCode(form.getCode()); // Do not set code - it's read-only
            roomToUpdate.setDescription(form.getDescription());
            roomToUpdate.setActive(form.getActive());

            StorageRoom updatedRoom = storageLocationService.updateRoom(idInt, roomToUpdate);
            if (updatedRoom == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(updatedRoom));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error updating room: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating room", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> deleteRoom(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Validate constraints before deletion
            if (!storageLocationService.canDeleteLocation(room)) {
                String message = storageLocationService.getDeleteConstraintMessage(room);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot delete room");
                error.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            storageLocationService.deleteRoom(idInt);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error deleting room: " + e.getMessage(), e);
            // Conflict if room has constraints (checked in service layer)
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot delete room");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Error deleting room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Device Endpoints ==========

    @PostMapping("/devices")
    public ResponseEntity<Map<String, Object>> createDevice(@Valid @RequestBody StorageDeviceForm form) {
        try {
            // Set parent room first (needed for code generation)
            Integer parentRoomId = form.getParentRoomId() != null ? Integer.parseInt(form.getParentRoomId()) : null;
            StorageRoom parentRoom = storageLocationService.getRoom(parentRoomId);
            if (parentRoom == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent room not found"));
            }

            StorageDevice device = new StorageDevice();
            device.setName(form.getName());
            // Generate code if not provided
            if (form.getCode() == null || form.getCode().trim().isEmpty()) {
                device.setCode(generateUniqueDeviceCode(form.getName(), parentRoomId));
            } else {
                device.setCode(form.getCode());
            }
            device.setType(form.getType()); // Store as String to match database constraint
            device.setTemperatureSetting(
                    form.getTemperatureSetting() != null ? java.math.BigDecimal.valueOf(form.getTemperatureSetting())
                            : null);
            device.setCapacityLimit(form.getCapacityLimit());
            device.setActive(form.getActive() != null ? form.getActive() : true);
            device.setFhirUuid(UUID.randomUUID());
            device.setSysUserId("1"); // Default system user for REST API
            device.setParentRoom(parentRoom);

            Integer id = storageLocationService.insert(device);
            device.setId(id);

            Map<String, Object> response = entityToMap(device);
            response.put("parentRoomId", parentRoomId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (jakarta.persistence.PersistenceException e) {
            logger.error("Error creating device: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Database constraint violation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error creating device: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error creating device", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get devices with optional filters (FR-065: Devices tab - filter by type,
     * room, and status)
     */
    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, Object>>> getDevices(@RequestParam(required = false) String roomId,
            @RequestParam(required = false) String type, @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> response;
            if (type != null || roomId != null || status != null) {
                // Apply filters - service returns Maps with all data resolved
                StorageDevice.DeviceType deviceType = type != null ? StorageDevice.DeviceType.valueOf(type) : null;
                Integer roomIdInt = roomId != null ? Integer.parseInt(roomId) : null;
                Boolean activeStatus = status != null && !status.isEmpty() ? ("active".equalsIgnoreCase(status) ? true
                        : "inactive".equalsIgnoreCase(status) ? false : null) : null;
                response = storageDashboardService.filterDevicesForAPI(deviceType, roomIdInt, activeStatus);
            } else {
                // No filters - return all devices
                Integer roomIdInt = roomId != null ? Integer.parseInt(roomId) : null;
                response = storageLocationService.getDevicesForAPI(roomIdInt);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting devices", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/devices/{id}")
    public ResponseEntity<Map<String, Object>> getDeviceById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(device));
        } catch (Exception e) {
            logger.error("Error getting device by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/devices/{id}")
    public ResponseEntity<Map<String, Object>> updateDevice(@PathVariable String id,
            @Valid @RequestBody StorageDeviceForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice deviceToUpdate = new StorageDevice();
            deviceToUpdate.setName(form.getName());
            // Code and parentRoom are read-only - ignored if provided
            deviceToUpdate.setType(form.getType());
            deviceToUpdate.setTemperatureSetting(
                    form.getTemperatureSetting() != null ? java.math.BigDecimal.valueOf(form.getTemperatureSetting())
                            : null);
            deviceToUpdate.setCapacityLimit(form.getCapacityLimit());
            deviceToUpdate.setActive(form.getActive());

            // Get existing device to preserve ID
            StorageDevice existingDevice = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (existingDevice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            deviceToUpdate.setId(existingDevice.getId());

            storageLocationService.update(deviceToUpdate);
            StorageDevice updatedDevice = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            return ResponseEntity.ok(entityToMap(updatedDevice));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error updating device: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating device", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Validate constraints before deletion
            if (!storageLocationService.canDeleteLocation(device)) {
                String message = storageLocationService.getDeleteConstraintMessage(device);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot delete device");
                error.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            storageLocationService.delete(device);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error deleting device: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot delete device");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Error deleting device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Shelf Endpoints ==========

    @PostMapping("/shelves")
    public ResponseEntity<Map<String, Object>> createShelf(@Valid @RequestBody StorageShelfForm form) {
        try {
            StorageShelf shelf = new StorageShelf();
            shelf.setLabel(form.getLabel());
            shelf.setCapacityLimit(form.getCapacityLimit());
            shelf.setActive(form.getActive() != null ? form.getActive() : true);
            shelf.setFhirUuid(UUID.randomUUID());
            shelf.setSysUserId("1"); // Default system user for REST API

            Integer parentDeviceId = form.getParentDeviceId() != null ? Integer.parseInt(form.getParentDeviceId())
                    : null;
            StorageDevice parentDevice = (StorageDevice) storageLocationService.get(parentDeviceId,
                    StorageDevice.class);
            if (parentDevice == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent device not found"));
            }
            shelf.setParentDevice(parentDevice);

            Integer id = storageLocationService.insert(shelf);
            shelf.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMap(shelf));
        } catch (Exception e) {
            logger.error("Error creating shelf", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get shelves with optional filters (FR-065: Shelves tab - filter by device,
     * room, and status)
     */
    @GetMapping("/shelves")
    public ResponseEntity<List<Map<String, Object>>> getShelves(@RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String roomId, @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> response;
            if (deviceId != null || roomId != null || status != null) {
                // Apply filters
                Integer deviceIdInt = deviceId != null ? Integer.parseInt(deviceId) : null;
                Integer roomIdInt = roomId != null ? Integer.parseInt(roomId) : null;
                Boolean activeStatus = status != null && !status.isEmpty() ? ("active".equalsIgnoreCase(status) ? true
                        : "inactive".equalsIgnoreCase(status) ? false : null) : null;
                // Service returns Maps with all data resolved
                response = storageDashboardService.filterShelvesForAPI(deviceIdInt, roomIdInt, activeStatus);
            } else {
                // No filters - return all shelves
                Integer deviceIdInt = deviceId != null ? Integer.parseInt(deviceId) : null;
                response = storageLocationService.getShelvesForAPI(deviceIdInt);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting shelves", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/shelves/{id}")
    public ResponseEntity<Map<String, Object>> getShelfById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(shelf));
        } catch (Exception e) {
            logger.error("Error getting shelf by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/shelves/{id}")
    public ResponseEntity<Map<String, Object>> updateShelf(@PathVariable String id,
            @Valid @RequestBody StorageShelfForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelfToUpdate = new StorageShelf();
            shelfToUpdate.setLabel(form.getLabel());
            // parentDevice is read-only - ignored if provided
            shelfToUpdate.setCapacityLimit(form.getCapacityLimit());
            shelfToUpdate.setActive(form.getActive());

            // Get existing shelf to preserve ID
            StorageShelf existingShelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (existingShelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            shelfToUpdate.setId(existingShelf.getId());

            storageLocationService.update(shelfToUpdate);
            StorageShelf updatedShelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            return ResponseEntity.ok(entityToMap(updatedShelf));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error updating shelf: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating shelf", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/shelves/{id}")
    public ResponseEntity<Map<String, Object>> deleteShelf(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Validate constraints before deletion
            if (!storageLocationService.canDeleteLocation(shelf)) {
                String message = storageLocationService.getDeleteConstraintMessage(shelf);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot delete shelf");
                error.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            storageLocationService.delete(shelf);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error deleting shelf: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot delete shelf");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Error deleting shelf", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Rack Endpoints ==========

    @PostMapping("/racks")
    public ResponseEntity<Map<String, Object>> createRack(@Valid @RequestBody StorageRackForm form) {
        try {
            StorageRack rack = new StorageRack();
            rack.setLabel(form.getLabel());
            rack.setRows(form.getRows());
            rack.setColumns(form.getColumns());
            rack.setPositionSchemaHint(form.getPositionSchemaHint());
            rack.setActive(form.getActive() != null ? form.getActive() : true);
            rack.setFhirUuid(UUID.randomUUID());
            rack.setSysUserId("1"); // Default system user for REST API

            Integer parentShelfId = form.getParentShelfId() != null ? Integer.parseInt(form.getParentShelfId()) : null;
            StorageShelf parentShelf = (StorageShelf) storageLocationService.get(parentShelfId, StorageShelf.class);
            if (parentShelf == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent shelf not found"));
            }
            rack.setParentShelf(parentShelf);

            Integer id = storageLocationService.insert(rack);
            rack.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMap(rack));
        } catch (Exception e) {
            logger.error("Error creating rack", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get racks with optional filters (FR-065: Racks tab - filter by room, shelf,
     * device, and status) Returns racks with roomId column (FR-065a)
     */
    @GetMapping("/racks")
    public ResponseEntity<List<Map<String, Object>>> getRacks(@RequestParam(required = false) String shelfId,
            @RequestParam(required = false) String deviceId, @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> response;
            if (shelfId != null || deviceId != null || roomId != null || status != null) {
                // Apply filters using dashboard service (includes roomId column per FR-065a)
                Integer shelfIdInt = shelfId != null ? Integer.parseInt(shelfId) : null;
                Integer deviceIdInt = deviceId != null ? Integer.parseInt(deviceId) : null;
                Integer roomIdInt = roomId != null ? Integer.parseInt(roomId) : null;
                Boolean activeStatus = status != null && !status.isEmpty() ? ("active".equalsIgnoreCase(status) ? true
                        : "inactive".equalsIgnoreCase(status) ? false : null) : null;
                response = storageDashboardService.getRacksForAPI(roomIdInt, shelfIdInt, deviceIdInt, activeStatus);
            } else {
                // No filters - return all racks with roomId column (FR-065a)
                // Service layer already includes roomId in getRacksForAPI response
                Integer shelfIdInt = shelfId != null ? Integer.parseInt(shelfId) : null;
                response = storageLocationService.getRacksForAPI(shelfIdInt);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting racks", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/racks/{id}")
    public ResponseEntity<Map<String, Object>> getRackById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(rack));
        } catch (Exception e) {
            logger.error("Error getting rack by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/racks/{id}")
    public ResponseEntity<Map<String, Object>> updateRack(@PathVariable String id,
            @Valid @RequestBody StorageRackForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rackToUpdate = new StorageRack();
            rackToUpdate.setLabel(form.getLabel());
            rackToUpdate.setRows(form.getRows());
            rackToUpdate.setColumns(form.getColumns());
            rackToUpdate.setPositionSchemaHint(form.getPositionSchemaHint());
            // parentShelf is read-only - ignored if provided
            rackToUpdate.setActive(form.getActive());

            // Get existing rack to preserve ID
            StorageRack existingRack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (existingRack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            rackToUpdate.setId(existingRack.getId());

            storageLocationService.update(rackToUpdate);
            StorageRack updatedRack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            return ResponseEntity.ok(entityToMap(updatedRack));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error updating rack: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating rack", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/racks/{id}")
    public ResponseEntity<Map<String, Object>> deleteRack(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Validate constraints before deletion
            if (!storageLocationService.canDeleteLocation(rack)) {
                String message = storageLocationService.getDeleteConstraintMessage(rack);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot delete rack");
                error.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            storageLocationService.delete(rack);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error deleting rack: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot delete rack");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Error deleting rack", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Position Endpoints ==========

    @PostMapping("/positions")
    public ResponseEntity<Map<String, Object>> createPosition(@Valid @RequestBody StoragePositionForm form) {
        try {
            StoragePosition position = new StoragePosition();
            position.setCoordinate(form.getCoordinate());
            position.setRowIndex(form.getRowIndex());
            position.setColumnIndex(form.getColumnIndex());
            position.setFhirUuid(UUID.randomUUID());
            position.setSysUserId("1"); // Default system user for REST API

            Integer parentRackId = form.getParentRackId() != null ? Integer.parseInt(form.getParentRackId()) : null;
            StorageRack parentRack = (StorageRack) storageLocationService.get(parentRackId, StorageRack.class);
            if (parentRack == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent rack not found"));
            }
            position.setParentRack(parentRack);

            // StoragePosition requires parentDevice (not-null constraint)
            // Traverse hierarchy: Rack → Shelf → Device
            StorageShelf parentShelf = parentRack.getParentShelf();
            if (parentShelf != null) {
                position.setParentShelf(parentShelf);
                StorageDevice parentDevice = parentShelf.getParentDevice();
                if (parentDevice != null) {
                    position.setParentDevice(parentDevice);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Parent device not found in hierarchy"));
                }
            } else {
                // If no shelf, we need to get device from rack's shelf relationship
                // But rack always has a shelf (nullable = false in StorageRack entity)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Parent shelf not found in rack hierarchy"));
            }

            Integer id = storageLocationService.insert(position);
            position.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMap(position));
        } catch (Exception e) {
            logger.error("Error creating position", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<List<Map<String, Object>>> getPositions(@RequestParam(required = false) String rackId,
            @RequestParam(required = false) Boolean occupied) {
        try {
            List<StoragePosition> positions;
            if (rackId != null) {
                Integer rackIdInt = Integer.parseInt(rackId);
                positions = storageLocationService.getPositionsByRack(rackIdInt);
                // Filter by occupied status if specified
                // Calculate occupied dynamically from SampleStorageAssignment (source of truth)
                if (occupied != null) {
                    positions.removeIf(p -> sampleStorageAssignmentDAO.isPositionOccupied(p) != occupied);
                }
            } else {
                positions = storageLocationService.getAllPositions();
                // Filter by occupied status if specified
                if (occupied != null) {
                    positions.removeIf(p -> sampleStorageAssignmentDAO.isPositionOccupied(p) != occupied);
                }
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (StoragePosition position : positions) {
                response.add(entityToMap(position));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting positions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Helper Methods ==========

    /**
     * Generate a unique room code from the room name. If the base code already
     * exists, appends a numeric suffix.
     * 
     * @param name Room name
     * @return Unique code (max 50 characters)
     */
    private String generateUniqueRoomCode(String name) {
        // Generate base code from name: uppercase, alphanumeric only, max 45 chars
        String baseCode = name.substring(0, Math.min(45, name.length())).toUpperCase().replaceAll("[^A-Z0-9]", "");

        // If base code is empty (name had no alphanumeric chars), use default
        if (baseCode.isEmpty()) {
            baseCode = "ROOM";
        }

        String code = baseCode;
        int suffix = 1;

        // Check for uniqueness and append suffix if needed
        while (storageRoomDAO.findByCode(code) != null) {
            String suffixStr = String.valueOf(suffix);
            int maxBaseLength = 50 - suffixStr.length() - 1; // -1 for hyphen
            if (maxBaseLength < 1) {
                maxBaseLength = 1;
            }
            code = baseCode.substring(0, Math.min(maxBaseLength, baseCode.length())) + "-" + suffixStr;
            suffix++;

            // Safety check to prevent infinite loop
            if (suffix > 9999) {
                // Fallback to timestamp-based code
                code = baseCode.substring(0, Math.min(30, baseCode.length())) + "-" + System.currentTimeMillis();
                break;
            }
        }

        return code;
    }

    /**
     * Generate a unique device code from the device name within a room. If the base
     * code already exists in the room, appends a numeric suffix.
     * 
     * @param name   Device name
     * @param roomId Parent room ID
     * @return Unique code (max 50 characters)
     */
    private String generateUniqueDeviceCode(String name, Integer roomId) {
        // Generate base code from name: uppercase, alphanumeric only, max 45 chars
        String baseCode = name.substring(0, Math.min(45, name.length())).toUpperCase().replaceAll("[^A-Z0-9]", "");

        // If base code is empty (name had no alphanumeric chars), use default
        if (baseCode.isEmpty()) {
            baseCode = "DEVICE";
        }

        String code = baseCode;
        int suffix = 1;

        // Check for uniqueness within the room and append suffix if needed
        while (storageDeviceDAO.findByParentRoomIdAndCode(roomId, code) != null) {
            String suffixStr = String.valueOf(suffix);
            int maxBaseLength = 50 - suffixStr.length() - 1; // -1 for hyphen
            if (maxBaseLength < 1) {
                maxBaseLength = 1;
            }
            code = baseCode.substring(0, Math.min(maxBaseLength, baseCode.length())) + "-" + suffixStr;
            suffix++;

            // Safety check to prevent infinite loop
            if (suffix > 9999) {
                // Fallback to timestamp-based code
                code = baseCode.substring(0, Math.min(30, baseCode.length())) + "-" + System.currentTimeMillis();
                break;
            }
        }

        return code;
    }

    private Map<String, Object> entityToMap(Object entity) {
        Map<String, Object> map = new HashMap<>();

        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            map.put("id", room.getId());
            map.put("name", room.getName());
            map.put("code", room.getCode());
            map.put("description", room.getDescription());
            map.put("active", room.getActive());
            map.put("fhirUuid", room.getFhirUuidAsString());
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            map.put("id", device.getId());
            map.put("name", device.getName());
            map.put("code", device.getCode());
            map.put("type", device.getTypeAsString());
            map.put("temperatureSetting", device.getTemperatureSetting());
            map.put("capacityLimit", device.getCapacityLimit());
            map.put("active", device.getActive());
            map.put("fhirUuid", device.getFhirUuidAsString());
            // Add parent room for filtering (FR-065: filter by room) and display
            StorageRoom parentRoom = device.getParentRoom();
            if (parentRoom != null) {
                // Trigger lazy load within transaction
                parentRoom.getName();
                map.put("roomId", parentRoom.getId());
                map.put("roomName", parentRoom.getName());
                map.put("parentRoomName", parentRoom.getName());
            }
        } else if (entity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) entity;
            map.put("id", shelf.getId());
            map.put("label", shelf.getLabel());
            map.put("capacityLimit", shelf.getCapacityLimit());
            map.put("active", shelf.getActive());
            map.put("fhirUuid", shelf.getFhirUuidAsString());
            // Add parent relationships for filtering (FR-065: filter by device and room)
            // and display
            StorageDevice parentDevice = shelf.getParentDevice();
            StorageRoom parentRoom = null;
            if (parentDevice != null) {
                // Trigger lazy load within transaction
                parentDevice.getName();
                map.put("deviceId", parentDevice.getId());
                map.put("deviceName", parentDevice.getName());
                map.put("parentDeviceName", parentDevice.getName());

                parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null) {
                    // Trigger lazy load within transaction
                    parentRoom.getName();
                    map.put("roomId", parentRoom.getId());
                    map.put("roomName", parentRoom.getName());
                }
            }
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            map.put("id", rack.getId());
            map.put("label", rack.getLabel());
            map.put("rows", rack.getRows());
            map.put("columns", rack.getColumns());
            map.put("positionSchemaHint", rack.getPositionSchemaHint());
            map.put("active", rack.getActive());
            map.put("fhirUuid", rack.getFhirUuidAsString());

            // Add parent relationships for filtering (FR-065: filter by room, shelf,
            // device) and display - use parent-prefixed names for consistency
            StorageShelf parentShelf = rack.getParentShelf();
            StorageDevice parentDevice = null;
            StorageRoom parentRoom = null;
            if (parentShelf != null) {
                // Trigger lazy load within transaction
                parentShelf.getLabel();
                map.put("parentShelfId", parentShelf.getId());
                map.put("shelfLabel", parentShelf.getLabel());
                map.put("parentShelfLabel", parentShelf.getLabel());

                parentDevice = parentShelf.getParentDevice();
                if (parentDevice != null) {
                    // Trigger lazy load within transaction
                    parentDevice.getName();
                    map.put("parentDeviceId", parentDevice.getId());
                    map.put("deviceName", parentDevice.getName());
                    map.put("parentDeviceName", parentDevice.getName());

                    parentRoom = parentDevice.getParentRoom();
                    if (parentRoom != null) {
                        // Trigger lazy load within transaction
                        parentRoom.getName();
                        map.put("parentRoomId", parentRoom.getId());
                        map.put("roomName", parentRoom.getName());
                        map.put("parentRoomName", parentRoom.getName());
                    }
                }
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

            // Set type for consistency
            map.put("type", "rack");
        } else if (entity instanceof StoragePosition) {
            StoragePosition position = (StoragePosition) entity;
            map.put("id", position.getId());
            map.put("coordinate", position.getCoordinate());
            map.put("rowIndex", position.getRowIndex());
            map.put("columnIndex", position.getColumnIndex());
            // Calculate occupied dynamically from SampleStorageAssignment (source of truth)
            // instead of using StoragePosition.occupied flag
            map.put("occupied", sampleStorageAssignmentDAO.isPositionOccupied(position));
            map.put("fhirUuid", position.getFhirUuidAsString());

            // Add parent relationships for hierarchy display
            StorageRack parentRack = position.getParentRack();
            StorageShelf parentShelf = position.getParentShelf();
            StorageDevice parentDevice = position.getParentDevice();
            StorageRoom parentRoom = null;

            if (parentDevice != null) {
                parentDevice.getName(); // Trigger lazy load
                map.put("parentDeviceId", parentDevice.getId());
                map.put("deviceName", parentDevice.getName());
                map.put("parentDeviceName", parentDevice.getName());

                parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null) {
                    parentRoom.getName(); // Trigger lazy load
                    map.put("parentRoomId", parentRoom.getId());
                    map.put("roomName", parentRoom.getName());
                    map.put("parentRoomName", parentRoom.getName());
                }
            }

            if (parentShelf != null) {
                parentShelf.getLabel(); // Trigger lazy load
                map.put("parentShelfId", parentShelf.getId());
                map.put("shelfLabel", parentShelf.getLabel());
                map.put("parentShelfLabel", parentShelf.getLabel());
            }

            if (parentRack != null) {
                parentRack.getLabel(); // Trigger lazy load
                map.put("parentRackId", parentRack.getId());
                map.put("rackLabel", parentRack.getLabel());
                map.put("parentRackLabel", parentRack.getLabel());
            }

            // Build hierarchicalPath: Room > Device > Shelf > Rack > Position
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
            if (parentRack != null && parentRack.getLabel() != null) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(" > ");
                }
                pathBuilder.append(parentRack.getLabel());
            }
            if (position.getCoordinate() != null) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(" > ");
                }
                pathBuilder.append("Position ").append(position.getCoordinate());
            }
            if (pathBuilder.length() > 0) {
                map.put("hierarchicalPath", pathBuilder.toString());
            }

            // Set type for consistency
            map.put("type", "position");
        }

        return map;
    }

    // ========== Search Endpoints (FR-064, FR-064a - Phase 3.1) ==========

    /**
     * Search samples by sample ID, accession number type/prefix, and assigned
     * location (full hierarchical path). Matches ANY of these fields (OR logic).
     * GET /rest/storage/samples/search?q={searchTerm}
     */
    @GetMapping("/samples/search")
    public ResponseEntity<List<Map<String, Object>>> searchSamples(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> results = storageSearchService.searchSamples(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching samples with query: " + q, e);
            e.printStackTrace(); // Temporary debugging
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * Search rooms by name and code. Matches name OR code (OR logic). GET
     * /rest/storage/rooms/search?q={searchTerm}
     */
    @GetMapping("/rooms/search")
    public ResponseEntity<List<Map<String, Object>>> searchRooms(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> response = storageSearchService.searchRooms(q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching rooms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search devices by name, code, and type. Matches name OR code OR type (OR
     * logic). GET /rest/storage/devices/search?q={searchTerm}
     */
    @GetMapping("/devices/search")
    public ResponseEntity<List<Map<String, Object>>> searchDevices(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> response = storageSearchService.searchDevices(q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search shelves by label (name). GET
     * /rest/storage/shelves/search?q={searchTerm}
     */
    @GetMapping("/shelves/search")
    public ResponseEntity<List<Map<String, Object>>> searchShelves(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> response = storageSearchService.searchShelves(q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching shelves", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search racks by label (name). GET /rest/storage/racks/search?q={searchTerm}
     */
    @GetMapping("/racks/search")
    public ResponseEntity<List<Map<String, Object>>> searchRacks(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> response = storageSearchService.searchRacks(q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching racks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Dashboard Endpoints ==========

    /**
     * Get location counts by type for active locations only (FR-057, FR-057a). GET
     * /rest/storage/dashboard/location-counts Returns counts for Room, Device,
     * Shelf, and Rack levels (Position excluded). Only counts active
     * (non-decommissioned) locations.
     * 
     * @return JSON map with keys: "rooms", "devices", "shelves", "racks" and
     *         integer count values
     */
    @GetMapping("/dashboard/location-counts")
    public ResponseEntity<Map<String, Integer>> getLocationCounts() {
        try {
            Map<String, Integer> counts = storageDashboardService.getLocationCountsByType();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            logger.error("Error getting location counts", e);
            // Return empty counts on error
            Map<String, Integer> emptyCounts = new HashMap<>();
            emptyCounts.put("rooms", 0);
            emptyCounts.put("devices", 0);
            emptyCounts.put("shelves", 0);
            emptyCounts.put("racks", 0);
            return ResponseEntity.ok(emptyCounts);
        }
    }

    /**
     * Search locations across all hierarchy levels (Room, Device, Shelf, Rack) GET
     * /rest/storage/locations/search?q={term} Returns locations matching search
     * term with full hierarchical paths
     * 
     * @param q Search term (case-insensitive partial match)
     * @return List of matching locations with hierarchicalPath field
     */
    @GetMapping("/locations/search")
    public ResponseEntity<List<Map<String, Object>>> searchLocations(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> results = storageLocationService.searchLocations(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching locations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
