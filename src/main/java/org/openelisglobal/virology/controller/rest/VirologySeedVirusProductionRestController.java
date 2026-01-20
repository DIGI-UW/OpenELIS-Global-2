package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/virology")
public class VirologySeedVirusProductionRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    public static class SeedVirusProductionRequest {
        public Long notebookPageId;
        public List<Long> sampleIds;
        public String productionDate;
        public String seedVirusBatchId;
        public String selectedStrain;
        public String selectionCriteria;
        public String notes;
    }

    @PostMapping(value = "/seed-virus-production", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveSeedVirusProduction(
            HttpServletRequest httpRequest,
            @RequestBody SeedVirusProductionRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (request.notebookPageId == null || request.sampleIds == null || request.sampleIds.isEmpty()) {
                response.put("success", false);
                response.put("error", "Missing required fields: notebookPageId or sampleIds");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.seedVirusBatchId == null || request.seedVirusBatchId.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Seed virus batch ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.selectionCriteria == null || request.selectionCriteria.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Selection criteria is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build production data map
            Map<String, Object> productionData = new HashMap<>();
            productionData.put("productionDate", request.productionDate);
            productionData.put("seedVirusBatchId", request.seedVirusBatchId);
            productionData.put("selectedStrain", request.selectedStrain);
            productionData.put("selectionCriteria", request.selectionCriteria);
            productionData.put("notes", request.notes);

            // Convert Long to Integer for service method
            Integer pageId = request.notebookPageId.intValue();
            List<Integer> sampleIdInts = request.sampleIds.stream()
                .map(Long::intValue)
                .toList();

            // Apply seed virus production data to samples
            int updatedCount = notebookPageSampleService.bulkApplyData(
                pageId,
                sampleIdInts,
                productionData,
                getSysUserId(httpRequest)
            );

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Seed virus production data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save seed virus production data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
