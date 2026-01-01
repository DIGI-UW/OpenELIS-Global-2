package org.openelisglobal.equipmentusage.controller.rest;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.equipmentusage.service.EquipmentUsageEntryService;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry.EntryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/rest/equipment-usage")
public class EquipmentUsageRestController extends BaseRestController {

    @Autowired
    @Setter
    private EquipmentUsageEntryService equipmentUsageEntryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getAll() {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getAll();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipmentUsageEntry> getById(@PathVariable Long id) {
        try {
            EquipmentUsageEntry entry = equipmentUsageEntryService.get(id);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/equipment/{equipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getByEquipmentId(@PathVariable Long equipmentId) {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getByEquipmentId(equipmentId);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/equipment/{equipmentId}/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getByEquipmentAndDateRange(@PathVariable Long equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getByEquipmentAndDateRange(equipmentId,
                    startDate, endDate);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/operator/{operatorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getByOperatorId(@PathVariable Long operatorId) {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getByOperatorId(operatorId);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/pending-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getPendingApproval() {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getPendingApproval();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/approved", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getApproved() {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getApproved();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/department/{department}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> getByDepartment(@PathVariable String department) {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.getByDepartment(department);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntry>> search(@RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department, @RequestParam(required = false) EntryStatus status) {
        try {
            List<EquipmentUsageEntry> entries = equipmentUsageEntryService.search(equipmentId, operatorId, startDate,
                    endDate, department, status);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipmentUsageEntry> createEntry(@Valid @RequestBody EquipmentUsageEntry entry) {
        try {
            EquipmentUsageEntry created = equipmentUsageEntryService.createEntry(entry);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/draft", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipmentUsageEntry> saveDraft(@PathVariable Long id,
            @Valid @RequestBody EquipmentUsageEntry entry) {
        try {
            entry.setId(id);
            EquipmentUsageEntry saved = equipmentUsageEntryService.saveDraft(entry);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/submit")
    public ResponseEntity<EquipmentUsageEntry> submitForApproval(@PathVariable Long id) {
        try {
            EquipmentUsageEntry submitted = equipmentUsageEntryService.submitForApproval(id);
            return ResponseEntity.ok(submitted);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/approve")
    public ResponseEntity<EquipmentUsageEntry> approveEntry(@PathVariable Long id, @RequestParam Long approverId) {
        try {
            EquipmentUsageEntry approved = equipmentUsageEntryService.approveEntry(id, approverId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/reject")
    public ResponseEntity<EquipmentUsageEntry> rejectEntry(@PathVariable Long id) {
        try {
            EquipmentUsageEntry rejected = equipmentUsageEntryService.rejectEntry(id);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/can-edit")
    public ResponseEntity<Boolean> canEditEntry(@PathVariable Long id) {
        try {
            boolean canEdit = equipmentUsageEntryService.canEditEntry(id);
            return ResponseEntity.ok(canEdit);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/can-approve")
    public ResponseEntity<Boolean> canApproveEntry(@PathVariable Long id) {
        try {
            boolean canApprove = equipmentUsageEntryService.canApproveEntry(id);
            return ResponseEntity.ok(canApprove);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
