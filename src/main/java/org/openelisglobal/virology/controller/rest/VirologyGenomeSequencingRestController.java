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
public class VirologyGenomeSequencingRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    public static class GenomeSequencingRequest {
        public Long notebookPageId;
        public List<Long> sampleIds;
        public String sequencingDate;
        public String fastaFileReference;
        public String genbankAccession;
        public String notes;
    }

    @PostMapping(value = "/genome-sequencing", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveGenomeSequencing(
            HttpServletRequest httpRequest,
            @RequestBody GenomeSequencingRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (request.notebookPageId == null || request.sampleIds == null || request.sampleIds.isEmpty()) {
                response.put("success", false);
                response.put("error", "Missing required fields: notebookPageId or sampleIds");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.fastaFileReference == null || request.fastaFileReference.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "FASTA file reference is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build sequencing data map
            Map<String, Object> sequencingData = new HashMap<>();
            sequencingData.put("sequencingDate", request.sequencingDate);
            sequencingData.put("fastaFileReference", request.fastaFileReference);
            sequencingData.put("genbankAccession", request.genbankAccession);
            sequencingData.put("notes", request.notes);

            // Convert Long to Integer for service method
            Integer pageId = request.notebookPageId.intValue();
            List<Integer> sampleIdInts = request.sampleIds.stream()
                .map(Long::intValue)
                .toList();

            // Apply genome sequencing data to samples
            int updatedCount = notebookPageSampleService.bulkApplyData(
                pageId,
                sampleIdInts,
                sequencingData,
                getSysUserId(httpRequest)
            );

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Genome sequencing data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save genome sequencing data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
