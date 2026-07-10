package org.openelisglobal.reports.vectorsurveillance.manualentry.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntryFieldMapService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin field-map management for the Manual Entry Helper (US5 / FR-009).
 * Permission module {@code VectorManualEntryFieldMap}.
 */
@RestController
@RequestMapping("/rest/admin/vector/manual-entry-fields")
public class ManualEntryFieldMapRestController extends BaseRestController {

    @Autowired
    private ManualEntryFieldMapService fieldMapService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ManualEntryFieldMap>> getFields() {
        try {
            return ResponseEntity.ok(fieldMapService.getAllOrdered());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualEntryFieldMap> createField(@RequestBody ManualEntryFieldMap fieldMap,
            HttpServletRequest request) {
        try {
            Integer id = fieldMapService.create(fieldMap, getSysUserId(request));
            fieldMap.setId(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(fieldMap);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualEntryFieldMap> updateField(@PathVariable Integer id,
            @RequestBody ManualEntryFieldMap fieldMap, HttpServletRequest request) {
        try {
            ManualEntryFieldMap updated = fieldMapService.patchUpdate(id, fieldMap, getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
