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
import org.openelisglobal.storage.service.StorageLocationService;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== Room Endpoints ==========

    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody StorageRoomForm form) {
        try {
            StorageRoom room = new StorageRoom();
            room.setName(form.getName());
            room.setCode(form.getCode());
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

    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRooms() {
        try {
            List<StorageRoom> rooms = storageLocationService.getRooms();
            List<Map<String, Object>> response = new ArrayList<>();
            for (StorageRoom room : rooms) {
                response.add(entityToMap(room));
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
            StorageRoom room = storageLocationService.getRoom(id);
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
            StorageRoom roomToUpdate = new StorageRoom();
            roomToUpdate.setName(form.getName());
            roomToUpdate.setCode(form.getCode());
            roomToUpdate.setDescription(form.getDescription());
            roomToUpdate.setActive(form.getActive());

            StorageRoom updatedRoom = storageLocationService.updateRoom(id, roomToUpdate);
            if (updatedRoom == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(entityToMap(updatedRoom));
        } catch (Exception e) {
            logger.error("Error updating room", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String id) {
        try {
            storageLocationService.deleteRoom(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            logger.error("Error deleting room: " + e.getMessage(), e);
            // Conflict if room has children (checked in service layer)
            if (e.getMessage() != null && e.getMessage().contains("active child")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error deleting room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Device Endpoints ==========

    @PostMapping("/devices")
    public ResponseEntity<Map<String, Object>> createDevice(@Valid @RequestBody StorageDeviceForm form) {
        try {
            StorageDevice device = new StorageDevice();
            device.setName(form.getName());
            device.setCode(form.getCode());
            device.setType(form.getType()); // Store as String to match database constraint
            device.setTemperatureSetting(
                    form.getTemperatureSetting() != null ? java.math.BigDecimal.valueOf(form.getTemperatureSetting())
                            : null);
            device.setCapacityLimit(form.getCapacityLimit());
            device.setActive(form.getActive() != null ? form.getActive() : true);
            device.setFhirUuid(UUID.randomUUID());
            device.setSysUserId("1"); // Default system user for REST API

            // Set parent room
            StorageRoom parentRoom = storageLocationService.getRoom(form.getParentRoomId());
            if (parentRoom == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent room not found"));
            }
            device.setParentRoom(parentRoom);

            String id = storageLocationService.insert(device);
            device.setId(id);

            Map<String, Object> response = entityToMap(device);
            response.put("parentRoomId", form.getParentRoomId());
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

    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, Object>>> getDevices(@RequestParam(required = false) String roomId) {
        try {
            List<StorageDevice> devices;
            if (roomId != null) {
                devices = storageLocationService.getDevicesByRoom(roomId);
            } else {
                devices = storageLocationService.getAllDevices();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (StorageDevice device : devices) {
                Map<String, Object> map = entityToMap(device);
                if (device.getParentRoom() != null) {
                    map.put("parentRoomId", device.getParentRoom().getId());
                }
                response.add(map);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting devices", e);
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

            StorageDevice parentDevice = (StorageDevice) storageLocationService.get(form.getParentDeviceId(),
                    StorageDevice.class);
            if (parentDevice == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent device not found"));
            }
            shelf.setParentDevice(parentDevice);

            String id = storageLocationService.insert(shelf);
            shelf.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMap(shelf));
        } catch (Exception e) {
            logger.error("Error creating shelf", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/shelves")
    public ResponseEntity<List<Map<String, Object>>> getShelves(@RequestParam(required = false) String deviceId) {
        try {
            List<StorageShelf> shelves;
            if (deviceId != null) {
                shelves = storageLocationService.getShelvesByDevice(deviceId);
            } else {
                shelves = storageLocationService.getAllShelves();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (StorageShelf shelf : shelves) {
                response.add(entityToMap(shelf));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting shelves", e);
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

            StorageShelf parentShelf = (StorageShelf) storageLocationService.get(form.getParentShelfId(),
                    StorageShelf.class);
            if (parentShelf == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent shelf not found"));
            }
            rack.setParentShelf(parentShelf);

            String id = storageLocationService.insert(rack);
            rack.setId(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMap(rack));
        } catch (Exception e) {
            logger.error("Error creating rack", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/racks")
    public ResponseEntity<List<Map<String, Object>>> getRacks(@RequestParam(required = false) String shelfId) {
        try {
            List<StorageRack> racks;
            if (shelfId != null) {
                racks = storageLocationService.getRacksByShelf(shelfId);
            } else {
                racks = storageLocationService.getAllRacks();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (StorageRack rack : racks) {
                response.add(entityToMap(rack));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting racks", e);
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
            position.setOccupied(form.getOccupied() != null ? form.getOccupied() : false);
            position.setFhirUuid(UUID.randomUUID());
            position.setSysUserId("1"); // Default system user for REST API

            StorageRack parentRack = (StorageRack) storageLocationService.get(form.getParentRackId(),
                    StorageRack.class);
            if (parentRack == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent rack not found"));
            }
            position.setParentRack(parentRack);

            String id = storageLocationService.insert(position);
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
                positions = storageLocationService.getPositionsByRack(rackId);
                // Filter by occupied status if specified
                if (occupied != null) {
                    positions.removeIf(p -> p.getOccupied() != occupied);
                }
            } else {
                positions = storageLocationService.getAllPositions();
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
        } else if (entity instanceof StorageShelf) {
            StorageShelf shelf = (StorageShelf) entity;
            map.put("id", shelf.getId());
            map.put("label", shelf.getLabel());
            map.put("capacityLimit", shelf.getCapacityLimit());
            map.put("active", shelf.getActive());
            map.put("fhirUuid", shelf.getFhirUuidAsString());
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            map.put("id", rack.getId());
            map.put("label", rack.getLabel());
            map.put("rows", rack.getRows());
            map.put("columns", rack.getColumns());
            map.put("positionSchemaHint", rack.getPositionSchemaHint());
            map.put("active", rack.getActive());
            map.put("fhirUuid", rack.getFhirUuidAsString());
        } else if (entity instanceof StoragePosition) {
            StoragePosition position = (StoragePosition) entity;
            map.put("id", position.getId());
            map.put("coordinate", position.getCoordinate());
            map.put("rowIndex", position.getRowIndex());
            map.put("columnIndex", position.getColumnIndex());
            map.put("occupied", position.getOccupied());
            map.put("fhirUuid", position.getFhirUuidAsString());
        }

        return map;
    }
}
