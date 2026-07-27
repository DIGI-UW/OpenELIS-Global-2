package org.openelisglobal.unitofmeasure.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rest")
public class UnitOfMeasureRestController {

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    /**
     * Inline "Add new unit" payload (FR-29): name is required, the rest optional.
     */
    public static class CreateUomRequest {
        public String name;
        public String description;
        public String code;
        public String ucumCode;
    }

    @GetMapping(value = "/uom", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getUnitOfMeasuresByType(
            @RequestParam(required = false) String type) {
        try {
            List<UnitOfMeasure> uoms;

            if (type != null && !type.trim().isEmpty()) {
                uoms = unitOfMeasureService.getUnitOfMeasuresByType(type);
            } else {
                uoms = unitOfMeasureService.getAll();
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (UnitOfMeasure uom : uoms) {
                Map<String, String> uomData = new HashMap<>();
                uomData.put("id", uom.getId());
                uomData.put("value", uom.getUnitOfMeasureName());
                result.add(uomData);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getUnitOfMeasuresByType",
                    "Error fetching UOMs: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a unit of measure inline (FR-29 / OGC-963). Returns the new unit as
     * {id, value} so the caller can append it to its picker and auto-select it.
     * Admin-gated to match the other catalog-editing endpoints.
     */
    @PostMapping(value = "/uom", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createUnitOfMeasure(@RequestBody CreateUomRequest body,
            HttpServletRequest request) {
        if (body == null || body.name == null || body.name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setUnitOfMeasureName(body.name.trim());
        uom.setDescription(
                body.description == null || body.description.isBlank() ? body.name.trim() : body.description.trim());
        uom.setCode(body.code == null || body.code.isBlank() ? null : body.code.trim());
        uom.setUcumCode(body.ucumCode == null || body.ucumCode.isBlank() ? null : body.ucumCode.trim());
        uom.setSysUserId(ControllerUtills.getSysUserId(request));
        unitOfMeasureService.insert(uom);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.UNIT_OF_MEASURE);

        Map<String, String> result = new HashMap<>();
        result.put("id", uom.getId());
        result.put("value", uom.getUnitOfMeasureName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
