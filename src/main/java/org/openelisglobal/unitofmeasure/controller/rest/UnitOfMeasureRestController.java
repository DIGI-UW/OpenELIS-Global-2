package org.openelisglobal.unitofmeasure.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class UnitOfMeasureRestController {

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

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
}
