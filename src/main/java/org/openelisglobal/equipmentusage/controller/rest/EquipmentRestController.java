package org.openelisglobal.equipmentusage.controller.rest;

import jakarta.validation.Valid;
import java.util.List;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.equipmentusage.service.EquipmentService;
import org.openelisglobal.equipmentusage.valueholder.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/equipment")
public class EquipmentRestController extends BaseRestController {

    @Autowired
    @Setter
    private EquipmentService equipmentService;

    /**
     * Get all active equipment
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Equipment>> getAllActive() {
        try {
            List<Equipment> equipment = equipmentService.getAllActive();
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment for dropdown (active only)
     */
    @GetMapping(value = "/dropdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Equipment>> getEquipmentForDropdown() {
        try {
            List<Equipment> equipment = equipmentService.getEquipmentForDropdown();
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment by ID
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipment> getById(@PathVariable Long id) {
        try {
            Equipment equipment = equipmentService.get(id);
            if (equipment == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment by serial number
     */
    @GetMapping(value = "/serial/{serialNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipment> getBySerialNumber(@PathVariable String serialNumber) {
        try {
            return equipmentService.getBySerialNumber(serialNumber).map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search equipment by name
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Equipment>> searchByName(@RequestParam String q) {
        try {
            List<Equipment> equipment = equipmentService.searchByName(q);
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment by department
     */
    @GetMapping(value = "/department/{department}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Equipment>> getByDepartment(@PathVariable String department) {
        try {
            List<Equipment> equipment = equipmentService.getByDepartment(department);
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new equipment
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipment> createEquipment(@Valid @RequestBody Equipment equipment) {
        try {
            Equipment created = equipmentService.save(equipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update equipment
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Equipment> updateEquipment(@PathVariable Long id, @Valid @RequestBody Equipment equipment) {
        try {
            equipment.setId(id);
            Equipment updated = equipmentService.save(equipment);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deactivate equipment
     */
    @PutMapping(value = "/{id}/deactivate")
    public ResponseEntity<Void> deactivateEquipment(@PathVariable Long id) {
        try {
            equipmentService.deactivateEquipment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate equipment
     */
    @PutMapping(value = "/{id}/activate")
    public ResponseEntity<Void> activateEquipment(@PathVariable Long id) {
        try {
            equipmentService.activateEquipment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
