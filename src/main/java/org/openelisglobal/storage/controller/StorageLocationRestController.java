package org.openelisglobal.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.form.*;
import org.openelisglobal.storage.form.response.StorageBoxResponse;
import org.openelisglobal.storage.form.response.StorageDeviceResponse;
import org.openelisglobal.storage.form.response.StorageRackResponse;
import org.openelisglobal.storage.form.response.StorageRoomResponse;
import org.openelisglobal.storage.form.response.StorageShelfResponse;
import org.openelisglobal.storage.service.DeletionValidationResult;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.service.StorageSearchService;
import org.openelisglobal.storage.valueholder.*;
import org.openelisglobal.userrole.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Storage Location management Handles CRUD operations for
 * all storage hierarchy levels: Room, Device, Shelf, Rack, Box
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

    @Autowired
    private UserModuleService userModuleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired(required = false)
    private FreezerService freezerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Helper method to check admin status with graceful error handling
     *
     * @param request HTTP request containing session information
     * @return true if user is admin, false otherwise (defaults to false if session
     *         unavailable)
     */
    private boolean checkAdminStatus(HttpServletRequest request) {
        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId == null) {
                return false;
            }
            return userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN);
        } catch (Exception e) {
            logger.debug("Could not determine admin status, treating as non-admin: " + e.getMessage());
            return false;
        }
    }

    // ========== Room Endpoints ==========

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@Valid @RequestBody StorageRoomForm form) {
        try {
            if (!storageLocationService.isNameUniqueWithinParent(form.getName(), null, "room", null)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Room name must be unique");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

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

            StorageRoomResponse response = toRoomResponse(createdRoom);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error creating room: {}", e.getMessage());
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
    public ResponseEntity<StorageRoomResponse> getRoomById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toRoomResponse(room));
        } catch (Exception e) {
            logger.error("Error getting room by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable String id, @Valid @RequestBody StorageRoomForm form) {
        try {
            // Explicit validation guard: name is required (test expects 400 before
            // persisting)
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Room name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            Integer idInt = Integer.parseInt(id);
            if (!storageLocationService.isNameUniqueWithinParent(form.getName(), null, "room", idInt)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Room name must be unique");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Validate code uniqueness if code is being changed
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                StorageRoom existingRoom = storageLocationService.getRoom(idInt);
                if (existingRoom != null && !form.getCode().equals(existingRoom.getCode())) {
                    // Code is being changed - validate uniqueness
                    if (!storageLocationService.isCodeUniqueForRoom(form.getCode(), idInt)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Room code must be unique");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                    }
                }
            }

            StorageRoom roomToUpdate = new StorageRoom();
            roomToUpdate.setName(form.getName());
            roomToUpdate.setCode(form.getCode()); // Code is now editable per spec FR-037l1
            roomToUpdate.setDescription(form.getDescription());
            roomToUpdate.setActive(form.getActive());

            StorageRoom updatedRoom = storageLocationService.updateRoom(idInt, roomToUpdate);
            if (updatedRoom == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toRoomResponse(updatedRoom));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error updating room: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating room", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * OGC-75: Check if a room can be deleted (pre-flight check for frontend)
     */
    @GetMapping("/rooms/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteRoom(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);

            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", isAdmin);

            if (storageLocationService.canDeleteLocation(room)) {
                response.put("canDelete", true);
                return ResponseEntity.ok(response);
            } else {
                String message = storageLocationService.getDeleteConstraintMessage(room);
                response.put("canDelete", false);
                response.put("error", "Cannot delete room");
                response.put("message", message);
                // Admin can still delete with cascade, so canDelete is false but isAdmin allows
                // override
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            logger.error("Error checking room delete constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * OGC-75: Get cascade delete summary for a room (admin only)
     */
    @GetMapping("/rooms/{id}/cascade-delete-summary")
    public ResponseEntity<Map<String, Object>> getRoomCascadeDeleteSummary(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> summary = storageLocationService.getCascadeDeleteSummary(room);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting room cascade delete summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRoom room = storageLocationService.getRoom(idInt);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DeletionValidationResult validation = storageLocationService.canDeleteRoom(idInt);
            if (!validation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(validation);
            }

            // Admin can delete with cascade (new OGC-75 behavior)
            storageLocationService.deleteLocationWithCascade(idInt, StorageRoom.class);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Constraint violation deleting room: {}", e.getMessage());
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
    public ResponseEntity<?> createDevice(@Valid @RequestBody StorageDeviceForm form,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Set parent room first (needed for code generation)
            Integer parentRoomId = form.getParentRoomId() != null ? Integer.parseInt(form.getParentRoomId()) : null;
            StorageRoom parentRoom = storageLocationService.getRoom(parentRoomId);
            if (parentRoom == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent room not found"));
            }
            if (!storageLocationService.isNameUniqueWithinParent(form.getName(), parentRoomId, "device", null)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Device name must be unique within the room");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
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
            // Set connectivity fields for network-connected equipment
            device.setIpAddress(form.getIpAddress());
            device.setPort(form.getPort());
            device.setCommunicationProtocol(form.getCommunicationProtocol());
            device.setFhirUuid(UUID.randomUUID());
            device.setSysUserId("1"); // Default system user for REST API
            device.setParentRoom(parentRoom);

            Integer id = storageLocationService.insert(device);
            device.setId(id);

            if (shouldEnableMonitoring(device)) {
                try {
                    String sysUserId = getSysUserId(request);
                    if (sysUserId == null) {
                        sysUserId = "1"; // Default system user for tests/REST API without session
                    }
                    createFreezerMonitoringStub(device, sysUserId);
                } catch (Exception e) {
                    logger.warn("Failed to auto-create freezer monitoring stub for device {}: {}", device.getName(),
                            e.getMessage());
                }
            }

            StorageDeviceResponse response = toDeviceResponse(device);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (jakarta.persistence.PersistenceException e) {
            logger.error("Error creating device: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Database constraint violation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error creating device: {}", e.getMessage());
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
    public ResponseEntity<StorageDeviceResponse> getDeviceById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toDeviceResponse(device));
        } catch (Exception e) {
            logger.error("Error getting device by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/devices/{id}")
    public ResponseEntity<?> updateDevice(@PathVariable String id, @Valid @RequestBody StorageDeviceForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice deviceToUpdate = new StorageDevice();
            deviceToUpdate.setName(form.getName());
            deviceToUpdate.setType(form.getType());
            deviceToUpdate.setTemperatureSetting(
                    form.getTemperatureSetting() != null ? java.math.BigDecimal.valueOf(form.getTemperatureSetting())
                            : null);
            deviceToUpdate.setCapacityLimit(form.getCapacityLimit());
            deviceToUpdate.setActive(form.getActive());
            deviceToUpdate.setCode(form.getCode());

            // Get existing device to preserve ID
            StorageDevice existingDevice = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (existingDevice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Handle parent room change if provided
            Integer parentRoomId = existingDevice.getParentRoom() != null ? existingDevice.getParentRoom().getId()
                    : null;
            if (form.getParentRoomId() != null && !form.getParentRoomId().trim().isEmpty()) {
                Integer newParentRoomId = Integer.parseInt(form.getParentRoomId());
                // Only update if parent actually changed
                if (!newParentRoomId.equals(parentRoomId)) {
                    StorageRoom newParentRoom = storageLocationService.getRoom(newParentRoomId);
                    if (newParentRoom == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "New parent room not found");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                    }
                    deviceToUpdate.setParentRoom(newParentRoom);
                    parentRoomId = newParentRoomId; // Use new parent for validation
                }
            } else {
                // Preserve existing parent if not provided
                deviceToUpdate.setParentRoom(existingDevice.getParentRoom());
            }

            // Validate name uniqueness in parent scope (new parent if changed)
            if (!storageLocationService.isNameUniqueWithinParent(form.getName(), parentRoomId, "device", idInt)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Device name must be unique within the room");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Validate code uniqueness if code is being changed (per spec FR-037l1)
            // Check uniqueness in new parent scope if parent changed
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                if (!form.getCode().equals(existingDevice.getCode())) {
                    // Code is being changed - validate uniqueness in current parent scope
                    if (!storageLocationService.isCodeUniqueForDevice(form.getCode(), idInt)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Device code must be unique within the room");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                    }
                }
            }
            deviceToUpdate.setId(existingDevice.getId());

            storageLocationService.update(deviceToUpdate);
            StorageDevice updatedDevice = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);

            // Sync device name to Freezer monitoring if linked
            if (shouldEnableMonitoring(updatedDevice)) {
                syncDeviceNameToFreezer(updatedDevice);
            }

            return ResponseEntity.ok(toDeviceResponse(updatedDevice));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error updating device: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating device", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * OGC-75: Check if a device can be deleted (pre-flight check for frontend)
     */
    @GetMapping("/devices/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteDevice(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", isAdmin);

            if (storageLocationService.canDeleteLocation(device)) {
                response.put("canDelete", true);
                return ResponseEntity.ok(response);
            } else {
                String message = storageLocationService.getDeleteConstraintMessage(device);
                response.put("canDelete", false);
                response.put("error", "Cannot delete device");
                response.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            logger.error("Error checking device delete constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * OGC-75: Get cascade delete summary for a device (admin only)
     */
    @GetMapping("/devices/{id}/cascade-delete-summary")
    public ResponseEntity<Map<String, Object>> getDeviceCascadeDeleteSummary(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> summary = storageLocationService.getCascadeDeleteSummary(device);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting device cascade delete summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a device can be moved to a new parent room, and if samples exist
     * downstream
     */
    @GetMapping("/devices/{id}/can-move")
    public ResponseEntity<Map<String, Object>> canMoveDevice(@PathVariable String id,
            @RequestParam(required = false) String newParentRoomId) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Integer newParentId = newParentRoomId != null ? Integer.parseInt(newParentRoomId) : null;
            Map<String, Object> result = storageLocationService.canMoveLocation(device, newParentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error checking device move constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageDevice device = (StorageDevice) storageLocationService.get(idInt, StorageDevice.class);
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DeletionValidationResult validation = storageLocationService.canDeleteDevice(idInt);
            if (!validation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(validation);
            }

            // Admin can delete with cascade (new OGC-75 behavior)
            storageLocationService.deleteLocationWithCascade(idInt, StorageDevice.class);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Constraint violation deleting device: {}", e.getMessage());
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
    public ResponseEntity<?> createShelf(@Valid @RequestBody StorageShelfForm form) {
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
            if (!storageLocationService.isNameUniqueWithinParent(form.getLabel(), parentDeviceId, "shelf", null)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Shelf label must be unique within the device");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            shelf.setParentDevice(parentDevice);

            Integer id = storageLocationService.insert(shelf);
            shelf.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(toShelfResponse(shelf));
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
    public ResponseEntity<StorageShelfResponse> getShelfById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toShelfResponse(shelf));
        } catch (Exception e) {
            logger.error("Error getting shelf by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a shelf can be moved to a new parent device, and if samples exist
     * downstream
     */
    @GetMapping("/shelves/{id}/can-move")
    public ResponseEntity<Map<String, Object>> canMoveShelf(@PathVariable String id,
            @RequestParam(required = false) String newParentDeviceId) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Integer newParentId = newParentDeviceId != null ? Integer.parseInt(newParentDeviceId) : null;
            Map<String, Object> result = storageLocationService.canMoveLocation(shelf, newParentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error checking shelf move constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/shelves/{id}")
    public ResponseEntity<?> updateShelf(@PathVariable String id, @Valid @RequestBody StorageShelfForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelfToUpdate = new StorageShelf();
            shelfToUpdate.setLabel(form.getLabel());
            shelfToUpdate.setCapacityLimit(form.getCapacityLimit());
            shelfToUpdate.setActive(form.getActive());
            shelfToUpdate.setCode(form.getCode());

            // Get existing shelf to preserve ID
            StorageShelf existingShelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (existingShelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Handle parent device change if provided
            Integer parentDeviceId = existingShelf.getParentDevice() != null ? existingShelf.getParentDevice().getId()
                    : null;
            if (form.getParentDeviceId() != null && !form.getParentDeviceId().trim().isEmpty()) {
                Integer newParentDeviceId = Integer.parseInt(form.getParentDeviceId());
                // Only update if parent actually changed
                if (!newParentDeviceId.equals(parentDeviceId)) {
                    StorageDevice newParentDevice = (StorageDevice) storageLocationService.get(newParentDeviceId,
                            StorageDevice.class);
                    if (newParentDevice == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "New parent device not found");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                    }
                    shelfToUpdate.setParentDevice(newParentDevice);
                    parentDeviceId = newParentDeviceId; // Use new parent for validation
                }
            } else {
                // Preserve existing parent if not provided
                shelfToUpdate.setParentDevice(existingShelf.getParentDevice());
            }

            // Validate name uniqueness in parent scope (new parent if changed)
            if (!storageLocationService.isNameUniqueWithinParent(form.getLabel(), parentDeviceId, "shelf", idInt)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Shelf label must be unique within the device");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Validate code uniqueness if code is being changed (per spec FR-037l1)
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                if (!form.getCode().equals(existingShelf.getCode())) {
                    // Code is being changed - validate uniqueness
                    if (!storageLocationService.isCodeUniqueForShelf(form.getCode(), idInt)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Shelf code must be unique");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                    }
                }
            }

            shelfToUpdate.setId(existingShelf.getId());

            storageLocationService.update(shelfToUpdate);
            StorageShelf updatedShelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            return ResponseEntity.ok(toShelfResponse(updatedShelf));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error updating shelf: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating shelf", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * OGC-75: Check if a shelf can be deleted (pre-flight check for frontend)
     */
    @GetMapping("/shelves/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteShelf(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", isAdmin);

            if (storageLocationService.canDeleteLocation(shelf)) {
                response.put("canDelete", true);
                return ResponseEntity.ok(response);
            } else {
                String message = storageLocationService.getDeleteConstraintMessage(shelf);
                response.put("canDelete", false);
                response.put("error", "Cannot delete shelf");
                response.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            logger.error("Error checking shelf delete constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * OGC-75: Get cascade delete summary for a shelf (admin only)
     */
    @GetMapping("/shelves/{id}/cascade-delete-summary")
    public ResponseEntity<Map<String, Object>> getShelfCascadeDeleteSummary(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> summary = storageLocationService.getCascadeDeleteSummary(shelf);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting shelf cascade delete summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/shelves/{id}")
    public ResponseEntity<?> deleteShelf(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageShelf shelf = (StorageShelf) storageLocationService.get(idInt, StorageShelf.class);
            if (shelf == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DeletionValidationResult validation = storageLocationService.canDeleteShelf(idInt);
            if (!validation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(validation);
            }

            // Admin can delete with cascade (new OGC-75 behavior)
            storageLocationService.deleteLocationWithCascade(idInt, StorageShelf.class);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Constraint violation deleting shelf: {}", e.getMessage());
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
    public ResponseEntity<?> createRack(@Valid @RequestBody StorageRackForm form) {
        try {
            StorageRack rack = new StorageRack();
            rack.setLabel(form.getLabel());
            rack.setCode(form.getCode());
            rack.setActive(form.getActive() != null ? form.getActive() : true);
            rack.setFhirUuid(UUID.randomUUID());
            rack.setSysUserId("1"); // Default system user for REST API

            Integer parentShelfId = form.getParentShelfId() != null ? Integer.parseInt(form.getParentShelfId()) : null;
            StorageShelf parentShelf = (StorageShelf) storageLocationService.get(parentShelfId, StorageShelf.class);
            if (parentShelf == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent shelf not found"));
            }
            if (!storageLocationService.isNameUniqueWithinParent(form.getLabel(), parentShelfId, "rack", null)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Rack label must be unique within the shelf");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            rack.setParentShelf(parentShelf);

            Integer id = storageLocationService.insert(rack);
            rack.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(toRackResponse(rack));
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
    public ResponseEntity<StorageRackResponse> getRackById(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(toRackResponse(rack));
        } catch (Exception e) {
            logger.error("Error getting rack by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a rack can be moved to a new parent shelf, and if samples exist
     * downstream
     */
    @GetMapping("/racks/{id}/can-move")
    public ResponseEntity<Map<String, Object>> canMoveRack(@PathVariable String id,
            @RequestParam(required = false) String newParentShelfId) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Integer newParentId = newParentShelfId != null ? Integer.parseInt(newParentShelfId) : null;
            Map<String, Object> result = storageLocationService.canMoveLocation(rack, newParentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error checking rack move constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/racks/{id}")
    public ResponseEntity<?> updateRack(@PathVariable String id, @Valid @RequestBody StorageRackForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rackToUpdate = new StorageRack();
            rackToUpdate.setLabel(form.getLabel());
            rackToUpdate.setCode(form.getCode());
            rackToUpdate.setActive(form.getActive());

            // Get existing rack to preserve ID
            StorageRack existingRack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (existingRack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Handle parent shelf change if provided
            Integer parentShelfId = existingRack.getParentShelf() != null ? existingRack.getParentShelf().getId()
                    : null;
            if (form.getParentShelfId() != null && !form.getParentShelfId().trim().isEmpty()) {
                Integer newParentShelfId = Integer.parseInt(form.getParentShelfId());
                // Only update if parent actually changed
                if (!newParentShelfId.equals(parentShelfId)) {
                    StorageShelf newParentShelf = (StorageShelf) storageLocationService.get(newParentShelfId,
                            StorageShelf.class);
                    if (newParentShelf == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "New parent shelf not found");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                    }
                    rackToUpdate.setParentShelf(newParentShelf);
                    parentShelfId = newParentShelfId; // Use new parent for validation
                } else {
                    // Parent unchanged - still need to set it on rackToUpdate for service layer
                    // validation
                    rackToUpdate.setParentShelf(existingRack.getParentShelf());
                }
            } else {
                // Preserve existing parent if not provided
                rackToUpdate.setParentShelf(existingRack.getParentShelf());
            }

            // Validate name uniqueness in parent scope (new parent if changed)
            if (!storageLocationService.isNameUniqueWithinParent(form.getLabel(), parentShelfId, "rack", idInt)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Rack label must be unique within the shelf");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Validate code uniqueness if code is being changed (per spec FR-037l1)
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                if (!form.getCode().equals(existingRack.getCode())) {
                    // Code is being changed - validate uniqueness
                    if (!storageLocationService.isCodeUniqueForRack(form.getCode(), idInt)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Rack code must be unique");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                    }
                }
            }

            rackToUpdate.setId(existingRack.getId());

            storageLocationService.update(rackToUpdate);
            StorageRack updatedRack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            return ResponseEntity.ok(toRackResponse(updatedRack));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error updating rack: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating rack", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * OGC-75: Check if a rack can be deleted (pre-flight check for frontend)
     */
    @GetMapping("/racks/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteRack(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);

            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", isAdmin);

            if (storageLocationService.canDeleteLocation(rack)) {
                response.put("canDelete", true);
                return ResponseEntity.ok(response);
            } else {
                String message = storageLocationService.getDeleteConstraintMessage(rack);
                response.put("canDelete", false);
                response.put("error", "Cannot delete rack");
                response.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            logger.error("Error checking rack delete constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * OGC-75: Get cascade delete summary for a rack (admin only)
     */
    @GetMapping("/racks/{id}/cascade-delete-summary")
    public ResponseEntity<Map<String, Object>> getRackCascadeDeleteSummary(@PathVariable String id) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> summary = storageLocationService.getCascadeDeleteSummary(rack);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting rack cascade delete summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/racks/{id}")
    public ResponseEntity<?> deleteRack(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageRack rack = (StorageRack) storageLocationService.get(idInt, StorageRack.class);
            if (rack == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DeletionValidationResult validation = storageLocationService.canDeleteRack(idInt);
            if (!validation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(validation);
            }

            // Admin can delete with cascade (new OGC-75 behavior)
            storageLocationService.deleteLocationWithCascade(idInt, StorageRack.class);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Constraint violation deleting rack: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot delete rack");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            logger.error("Error deleting rack", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Box Endpoints ==========

    @PostMapping("/boxes")
    public ResponseEntity<?> createBox(@Valid @RequestBody StorageBoxForm form) {
        try {
            StorageBox box = new StorageBox();
            box.setLabel(form.getLabel());
            box.setType(form.getType());
            box.setRows(form.getRows());
            box.setColumns(form.getColumns());
            box.setPositionSchemaHint(form.getPositionSchemaHint());
            box.setCode(form.getCode());
            box.setActive(form.getActive() != null ? form.getActive() : true);
            box.setFhirUuid(UUID.randomUUID());
            box.setSysUserId("1"); // Default system user for REST API

            Integer parentRackId = form.getParentRackId() != null ? Integer.parseInt(form.getParentRackId()) : null;
            StorageRack parentRack = (StorageRack) storageLocationService.get(parentRackId, StorageRack.class);
            if (parentRack == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent rack not found"));
            }
            box.setParentRack(parentRack);

            // Validate code uniqueness within parent rack
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                List<StorageBox> siblingBoxes = storageLocationService.getBoxesByRack(parentRackId);
                boolean codeConflict = siblingBoxes.stream()
                        .anyMatch(b -> b.getCode() != null && b.getCode().equals(form.getCode()));
                if (codeConflict) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Box code must be unique within the rack"));
                }
            }

            Integer id = storageLocationService.insert(box);
            box.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(toBoxResponse(box));
        } catch (Exception e) {
            logger.error("Error creating box", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/boxes")
    public ResponseEntity<List<StorageBoxResponse>> getBoxes(@RequestParam(required = false) String rackId,
            @RequestParam(required = false) Boolean occupied) {
        try {
            List<StorageBox> boxes;
            if (rackId != null) {
                Integer rackIdInt = Integer.parseInt(rackId);
                boxes = storageLocationService.getBoxesByRack(rackIdInt);
                // Filter by occupied status if specified
                if (occupied != null) {
                    boxes.removeIf(b -> sampleStorageAssignmentDAO.isBoxOccupied(b) != occupied);
                }
            } else {
                boxes = storageLocationService.getAllBoxes();
                if (occupied != null) {
                    boxes.removeIf(b -> sampleStorageAssignmentDAO.isBoxOccupied(b) != occupied);
                }
            }

            List<StorageBoxResponse> response = new ArrayList<>();
            for (StorageBox box : boxes) {
                response.add(toBoxResponse(box));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting boxes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/boxes/{id}")
    public ResponseEntity<?> updateBox(@PathVariable String id, @Valid @RequestBody StorageBoxForm form) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageBox boxToUpdate = new StorageBox();
            boxToUpdate.setLabel(form.getLabel());
            boxToUpdate.setType(form.getType());
            boxToUpdate.setRows(form.getRows());
            boxToUpdate.setColumns(form.getColumns());
            boxToUpdate.setPositionSchemaHint(form.getPositionSchemaHint());
            boxToUpdate.setCode(form.getCode());
            boxToUpdate.setActive(form.getActive() != null ? form.getActive() : true);

            // Get existing box to preserve ID and parent rack
            StorageBox existingBox = (StorageBox) storageLocationService.get(idInt, StorageBox.class);
            if (existingBox == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Integer parentRackId = existingBox.getParentRack() != null ? existingBox.getParentRack().getId() : null;

            // Validate label uniqueness within parent rack
            if (!storageLocationService.isNameUniqueWithinParent(form.getLabel(), parentRackId, "box", idInt)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Box label must be unique within the rack");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Validate code uniqueness within parent rack
            if (form.getCode() != null && !form.getCode().trim().isEmpty()) {
                if (!form.getCode().equals(existingBox.getCode())) {
                    List<StorageBox> siblingBoxes = storageLocationService.getBoxesByRack(parentRackId);
                    boolean codeConflict = siblingBoxes.stream().anyMatch(b -> !b.getId().equals(existingBox.getId())
                            && b.getCode() != null && b.getCode().equals(form.getCode()));
                    if (codeConflict) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Box code must be unique within the rack"));
                    }
                }
            }

            boxToUpdate.setId(existingBox.getId());
            boxToUpdate.setParentRack(existingBox.getParentRack()); // Parent rack is read-only

            storageLocationService.update(boxToUpdate);
            StorageBox updatedBox = (StorageBox) storageLocationService.get(idInt, StorageBox.class);
            return ResponseEntity.ok(toBoxResponse(updatedBox));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.warn("Validation error updating box: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating box", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * OGC-75: Check if a box can be deleted (pre-flight check for frontend)
     */
    @GetMapping("/boxes/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteBox(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageBox box = (StorageBox) storageLocationService.get(idInt, StorageBox.class);
            if (box == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);

            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", isAdmin);

            if (storageLocationService.canDeleteLocation(box)) {
                response.put("canDelete", true);
                return ResponseEntity.ok(response);
            } else {
                String message = storageLocationService.getDeleteConstraintMessage(box);
                response.put("canDelete", false);
                response.put("error", "Cannot delete box");
                response.put("message", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        } catch (Exception e) {
            logger.error("Error checking box delete constraints", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/boxes/{id}")
    public ResponseEntity<?> deleteBox(@PathVariable String id, HttpServletRequest request) {
        try {
            Integer idInt = Integer.parseInt(id);
            StorageBox box = (StorageBox) storageLocationService.get(idInt, StorageBox.class);
            if (box == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            boolean isAdmin = checkAdminStatus(request);
            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if box can be deleted (no assigned samples)
            if (!storageLocationService.canDeleteLocation(box)) {
                int sampleCount = sampleStorageAssignmentDAO.countByLocationTypeAndId("box", box.getId());
                String message = storageLocationService.getDeleteConstraintMessage(box);
                DeletionValidationResult validation = new DeletionValidationResult(false, "ACTIVE_ASSIGNMENTS", message,
                        sampleCount);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(validation);
            }

            storageLocationService.delete(box);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting box", e);
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

    private boolean shouldEnableMonitoring(StorageDevice device) {
        if (freezerService == null) {
            return false;
        }
        StorageDevice.DeviceType type = device.getTypeEnum();
        return type == StorageDevice.DeviceType.FREEZER || type == StorageDevice.DeviceType.REFRIGERATOR;
    }

    private void createFreezerMonitoringStub(StorageDevice device, String sysUserId) {
        Freezer freezer = new Freezer();
        freezer.setName(device.getName());
        freezer.setStorageDevice(device);
        freezer.setProtocol(Freezer.Protocol.TCP);
        freezer.setHost("");
        freezer.setPort(502);
        freezer.setSlaveId(1);
        freezer.setTemperatureRegister(0);
        freezer.setTemperatureScale(java.math.BigDecimal.ONE);
        freezer.setTemperatureOffset(java.math.BigDecimal.ZERO);
        freezer.setHumidityScale(java.math.BigDecimal.ONE);
        freezer.setHumidityOffset(java.math.BigDecimal.ZERO);
        freezer.setPollingIntervalSeconds(60);
        freezer.setActive(false);

        if (device.getTemperatureSetting() != null) {
            freezer.setTargetTemperature(device.getTemperatureSetting());
        }

        freezerService.createFreezer(freezer, device.getParentRoom().getId().longValue(), sysUserId);
        logger.info("Auto-created Freezer monitoring stub for StorageDevice: {} (ID: {})", device.getName(),
                device.getId());
    }

    private void syncDeviceNameToFreezer(StorageDevice device) {
        if (freezerService == null) {
            return;
        }

        try {
            List<Freezer> allFreezers = freezerService.getAllFreezers("");
            java.util.Optional<Freezer> linkedFreezer = allFreezers.stream()
                    .filter(f -> f.getStorageDevice() != null && f.getStorageDevice().getId().equals(device.getId()))
                    .findFirst();

            if (linkedFreezer.isPresent()) {
                Freezer freezer = linkedFreezer.get();
                if (!freezer.getName().equals(device.getName())) {
                    freezer.setName(device.getName()); // Sync name
                    freezerService.updateFreezer(freezer.getId(), freezer, device.getParentRoom().getId().longValue(),
                            device.getSysUserId());
                    logger.info("Synced device name to Freezer: {} (ID: {})", device.getName(), device.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to sync device name to freezer for device {}: {}", device.getId(), e.getMessage());
        }
    }

    private StorageRoomResponse toRoomResponse(StorageRoom room) {
        StorageRoomResponse response = new StorageRoomResponse();
        response.setId(room.getId());
        response.setName(room.getName());
        response.setCode(room.getCode());
        response.setDescription(room.getDescription());
        response.setActive(room.getActive());
        response.setFhirUuid(room.getFhirUuidAsString());
        return response;
    }

    private StorageDeviceResponse toDeviceResponse(StorageDevice device) {
        StorageDeviceResponse response = new StorageDeviceResponse();
        response.setId(device.getId());
        response.setName(device.getName());
        response.setCode(device.getCode());
        response.setType(device.getTypeAsString());
        response.setTemperatureSetting(device.getTemperatureSetting());
        response.setCapacityLimit(device.getCapacityLimit());
        response.setActive(device.getActive());
        response.setFhirUuid(device.getFhirUuidAsString());
        response.setIpAddress(device.getIpAddress());
        response.setPort(device.getPort());
        response.setCommunicationProtocol(device.getCommunicationProtocol());

        StorageRoom parentRoom = device.getParentRoom();
        if (parentRoom != null) {
            parentRoom.getName();
            response.setRoomId(parentRoom.getId());
            response.setRoomName(parentRoom.getName());
            response.setParentRoomId(parentRoom.getId());
            response.setParentRoomName(parentRoom.getName());
        }
        return response;
    }

    private StorageShelfResponse toShelfResponse(StorageShelf shelf) {
        StorageShelfResponse response = new StorageShelfResponse();
        response.setId(shelf.getId());
        response.setLabel(shelf.getLabel());
        response.setCapacityLimit(shelf.getCapacityLimit());
        response.setActive(shelf.getActive());
        response.setCode(shelf.getCode());
        response.setFhirUuid(shelf.getFhirUuidAsString());

        StorageDevice parentDevice = shelf.getParentDevice();
        if (parentDevice != null) {
            parentDevice.getName();
            response.setDeviceId(parentDevice.getId());
            response.setDeviceName(parentDevice.getName());
            response.setParentDeviceName(parentDevice.getName());

            StorageRoom parentRoom = parentDevice.getParentRoom();
            if (parentRoom != null) {
                parentRoom.getName();
                response.setRoomId(parentRoom.getId());
                response.setRoomName(parentRoom.getName());
            }
        }
        return response;
    }

    private StorageRackResponse toRackResponse(StorageRack rack) {
        StorageRackResponse response = new StorageRackResponse();
        response.setId(rack.getId());
        response.setLabel(rack.getLabel());
        response.setCode(rack.getCode());
        response.setActive(rack.getActive());
        response.setFhirUuid(rack.getFhirUuidAsString());

        StorageShelf parentShelf = rack.getParentShelf();
        StorageDevice parentDevice = null;
        StorageRoom parentRoom = null;
        if (parentShelf != null) {
            parentShelf.getLabel();
            response.setParentShelfId(parentShelf.getId());
            response.setShelfLabel(parentShelf.getLabel());
            response.setParentShelfLabel(parentShelf.getLabel());

            parentDevice = parentShelf.getParentDevice();
            if (parentDevice != null) {
                parentDevice.getName();
                response.setParentDeviceId(parentDevice.getId());
                response.setDeviceName(parentDevice.getName());
                response.setParentDeviceName(parentDevice.getName());

                parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null) {
                    parentRoom.getName();
                    response.setParentRoomId(parentRoom.getId());
                    response.setRoomName(parentRoom.getName());
                    response.setParentRoomName(parentRoom.getName());
                }
            }
        }

        String hierarchicalPath = buildPath(parentRoom, parentDevice, parentShelf, rack.getLabel());
        response.setHierarchicalPath(hierarchicalPath);
        response.setType("rack");

        return response;
    }

    private StorageBoxResponse toBoxResponse(StorageBox box) {
        StorageBoxResponse response = new StorageBoxResponse();
        response.setId(box.getId());
        response.setLabel(box.getLabel());
        response.setType(box.getType());
        response.setRows(box.getRows());
        response.setColumns(box.getColumns());
        response.setCapacity(box.getCapacity());
        response.setPositionSchemaHint(box.getPositionSchemaHint());
        response.setCode(box.getCode());
        response.setActive(box.getActive());

        Map<String, Map<String, String>> occupiedCoordinatesMap = sampleStorageAssignmentDAO
                .getOccupiedCoordinatesWithSampleInfo(box.getId());
        response.setOccupied(!occupiedCoordinatesMap.isEmpty());
        response.setOccupiedCoordinates(occupiedCoordinatesMap);
        response.setFhirUuid(box.getFhirUuidAsString());

        StorageRack parentRack = box.getParentRack();
        StorageShelf parentShelf = null;
        StorageDevice parentDevice = null;
        StorageRoom parentRoom = null;

        if (parentRack != null) {
            parentRack.getLabel();
            response.setParentRackId(parentRack.getId());
            response.setRackLabel(parentRack.getLabel());
            response.setParentRackLabel(parentRack.getLabel());
            parentShelf = parentRack.getParentShelf();
        }

        if (parentShelf != null) {
            parentShelf.getLabel();
            response.setParentShelfId(parentShelf.getId());
            response.setShelfLabel(parentShelf.getLabel());
            response.setParentShelfLabel(parentShelf.getLabel());
            parentDevice = parentShelf.getParentDevice();
        }

        if (parentDevice != null) {
            parentDevice.getName();
            response.setParentDeviceId(parentDevice.getId());
            response.setDeviceName(parentDevice.getName());
            response.setParentDeviceName(parentDevice.getName());
            parentRoom = parentDevice.getParentRoom();
        }

        if (parentRoom != null) {
            parentRoom.getName();
            response.setParentRoomId(parentRoom.getId());
            response.setRoomName(parentRoom.getName());
            response.setParentRoomName(parentRoom.getName());
        }

        String hierarchicalPath = buildPath(parentRoom, parentDevice, parentShelf,
                parentRack != null ? parentRack.getLabel() : null, box.getLabel());
        response.setHierarchicalPath(hierarchicalPath);

        return response;
    }

    private String buildPath(StorageRoom room, StorageDevice device, StorageShelf shelf, String... tailLabels) {
        StringBuilder pathBuilder = new StringBuilder();
        if (room != null && room.getName() != null) {
            pathBuilder.append(room.getName());
        }
        if (device != null && device.getName() != null) {
            appendPathSegment(pathBuilder, device.getName());
        }
        if (shelf != null && shelf.getLabel() != null) {
            appendPathSegment(pathBuilder, shelf.getLabel());
        }
        if (tailLabels != null) {
            for (String label : tailLabels) {
                if (label != null) {
                    appendPathSegment(pathBuilder, label);
                }
            }
        }
        return pathBuilder.length() > 0 ? pathBuilder.toString() : null;
    }

    private void appendPathSegment(StringBuilder builder, String segment) {
        if (builder.length() > 0) {
            builder.append(" > ");
        }
        builder.append(segment);
    }

    // ========== Search Endpoints (FR-064, FR-064a - Phase 3.1) ==========

    /**
     * Search sample items by sample item ID, external ID, parent sample accession
     * number, and assigned location (full hierarchical path). Matches ANY of these
     * fields (OR logic). GET /rest/storage/sample-items/search?q={searchTerm}
     *
     * Note: This is the canonical endpoint. /samples/search is kept for backwards
     * compatibility.
     */
    @GetMapping("/sample-items/search")
    public ResponseEntity<List<Map<String, Object>>> searchSampleItems(@RequestParam(required = false) String q) {
        try {
            List<Map<String, Object>> results = storageSearchService.searchSamples(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching sample items with query: " + q, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * @deprecated Use /sample-items/search instead. This endpoint returns
     *             SampleItems, not Samples. Kept for backwards compatibility.
     */
    @Deprecated
    @GetMapping("/samples/search")
    public ResponseEntity<List<Map<String, Object>>> searchSamples(@RequestParam(required = false) String q) {
        return searchSampleItems(q);
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
