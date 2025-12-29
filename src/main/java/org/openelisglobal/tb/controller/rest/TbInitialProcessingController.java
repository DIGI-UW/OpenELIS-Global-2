package org.openelisglobal.tb.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbMediaPreparationService;
import org.openelisglobal.tb.service.TbSampleProcessingService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * REST controller for TB Initial Processing operations. Handles media
 * preparation, sample processing/decontamination, and inoculation.
 */
@RestController
@RequestMapping(value = "/rest/tb/processing")
public class TbInitialProcessingController extends BaseRestController {

    @Autowired
    private TbMediaPreparationService tbMediaPreparationService;

    @Autowired
    private TbSampleProcessingService tbSampleProcessingService;

    @Autowired
    private TbCultureReadingService tbCultureReadingService;

    // ==================== Media Preparation Endpoints ====================

    /**
     * Get all media batches.
     */
    @GetMapping(value = "/media", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbMediaPreparation>> getAllMediaBatches() {
        List<TbMediaPreparation> batches = tbMediaPreparationService.getAll();
        return ResponseEntity.ok(batches);
    }

    /**
     * Get media batch by ID.
     */
    @GetMapping(value = "/media/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbMediaPreparation> getMediaBatch(@PathVariable Integer id) {
        TbMediaPreparation batch = tbMediaPreparationService.get(id);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(batch);
    }

    /**
     * Get media batch by batch ID.
     */
    @GetMapping(value = "/media/by-batch-id/{batchId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbMediaPreparation> getMediaBatchByBatchId(@PathVariable String batchId) {
        Optional<TbMediaPreparation> batch = tbMediaPreparationService.findByBatchId(batchId);
        return batch.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get available media batches for inoculation.
     */
    @GetMapping(value = "/media/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbMediaPreparation>> getAvailableMedia(
            @RequestParam(required = false) TbEnums.MediaType mediaType) {
        List<TbMediaPreparation> batches;
        if (mediaType != null) {
            batches = tbMediaPreparationService.findAvailableByMediaType(mediaType);
        } else {
            batches = tbMediaPreparationService.findAvailableForInoculation();
        }
        return ResponseEntity.ok(batches);
    }

    /**
     * Get media batches by QC status.
     */
    @GetMapping(value = "/media/by-status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbMediaPreparation>> getMediaByStatus(@PathVariable MediaQcStatus status) {
        List<TbMediaPreparation> batches = tbMediaPreparationService.findByQcStatus(status);
        return ResponseEntity.ok(batches);
    }

    /**
     * Generate a new batch ID.
     */
    @GetMapping(value = "/media/generate-batch-id/{mediaType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> generateBatchId(@PathVariable TbEnums.MediaType mediaType) {
        String batchId = tbMediaPreparationService.generateBatchId(mediaType);
        return ResponseEntity.ok(Map.of("batchId", batchId));
    }

    /**
     * Create a new media batch.
     */
    @PostMapping(value = "/media", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createMediaBatch(@RequestBody TbMediaPreparation batch,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        batch.setSysUserId(sysUserId);
        Integer id = tbMediaPreparationService.insert(batch);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("batchId", batch.getBatchId());
        response.put("message", "Media batch created successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Update media batch QC status.
     */
    @PostMapping(value = "/media/{id}/qc-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateMediaQcStatus(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        MediaQcStatus status = MediaQcStatus.valueOf((String) body.get("status"));
        String notes = (String) body.get("notes");

        TbMediaPreparation updated = tbMediaPreparationService.updateQcStatus(id, status, notes, sysUserId);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "QC status updated", "status", status.name()));
    }

    // ==================== Sample Processing Endpoints ====================

    /**
     * Get processing record by sample item ID.
     */
    @GetMapping(value = "/sample/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbSampleProcessing> getSampleProcessing(@PathVariable String sampleItemId) {
        Optional<TbSampleProcessing> processing = tbSampleProcessingService.findBySampleItemId(sampleItemId);
        return processing.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get samples by processing status.
     */
    @GetMapping(value = "/sample/by-status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbSampleProcessing>> getSamplesByStatus(@PathVariable ProcessingStatus status) {
        List<TbSampleProcessing> samples = tbSampleProcessingService.findByProcessingStatus(status);
        return ResponseEntity.ok(samples);
    }

    /**
     * Get samples ready for inoculation.
     */
    @GetMapping(value = "/sample/ready-for-inoculation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbSampleProcessing>> getSamplesReadyForInoculation() {
        List<TbSampleProcessing> samples = tbSampleProcessingService.findReadyForInoculation();
        return ResponseEntity.ok(samples);
    }

    /**
     * Get samples pending processing.
     */
    @GetMapping(value = "/sample/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSamplesPendingProcessing() {
        List<String> sampleIds = tbSampleProcessingService.findSampleItemIdsPendingProcessing();
        return ResponseEntity.ok(sampleIds);
    }

    /**
     * Process a single sample.
     */
    @PostMapping(value = "/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> processSample(@RequestBody TbSampleProcessing processing,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        processing.setSysUserId(sysUserId);
        processing.setProcessingDate(new Timestamp(System.currentTimeMillis()));
        Integer id = tbSampleProcessingService.insert(processing);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "Sample processed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Batch process multiple samples.
     */
    @PostMapping(value = "/sample/batch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> batchProcessSamples(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        @SuppressWarnings("unchecked")
        List<String> sampleItemIds = (List<String>) body.get("sampleItemIds");
        DecontaminationMethod method = DecontaminationMethod.valueOf((String) body.get("method"));

        List<TbSampleProcessing> processed = tbSampleProcessingService.batchProcess(sampleItemIds, method, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("processedCount", processed.size());
        response.put("message", "Samples processed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Mark sample as ready for inoculation.
     */
    @PutMapping(value = "/sample/{id}/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> markReadyForInoculation(@PathVariable Integer id,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbSampleProcessing updated = tbSampleProcessingService.markReadyForInoculation(id);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Sample marked ready for inoculation"));
    }

    // ==================== Inoculation Endpoints ====================

    /**
     * Inoculate a sample to media.
     */
    @PostMapping(value = "/inoculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> inoculateSample(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        // Handle type conversion safely - JSON can parse numbers as Integer, Long, or
        // String
        String sampleItemId = String.valueOf(body.get("sampleItemId"));
        Integer mediaBatchId = parseInteger(body.get("mediaBatchId"));
        Integer processingId = parseInteger(body.get("processingId"));

        TbMediaPreparation mediaBatch = tbMediaPreparationService.get(mediaBatchId);
        if (mediaBatch == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Media batch not found"));
        }

        if (!mediaBatch.isAvailableForInoculation()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Media batch not available for inoculation"));
        }

        TbSampleProcessing processing = null;
        if (processingId != null) {
            processing = tbSampleProcessingService.get(processingId);
        }

        TbCultureReading reading = tbCultureReadingService.inoculate(sampleItemId, mediaBatch, processing, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", reading.getId());
        response.put("message", "Sample inoculated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get inoculated samples.
     */
    @GetMapping(value = "/inoculated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getInoculatedSamples() {
        List<TbCultureReading> samples = tbCultureReadingService.findInoculatedSamples();
        return ResponseEntity.ok(samples);
    }

    // ==================== Statistics Endpoints ====================

    /**
     * Get processing statistics for dashboard tiles.
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getProcessingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Media statistics
        stats.put("mediaPending", tbMediaPreparationService.countByQcStatus(MediaQcStatus.PENDING));
        stats.put("mediaPassed", tbMediaPreparationService.countByQcStatus(MediaQcStatus.PASSED));
        stats.put("mediaFailed", tbMediaPreparationService.countByQcStatus(MediaQcStatus.FAILED));

        // Processing statistics
        stats.put("samplesPending", tbSampleProcessingService.countByProcessingStatus(ProcessingStatus.PENDING));
        stats.put("samplesProcessed", tbSampleProcessingService.countByProcessingStatus(ProcessingStatus.PROCESSED));
        stats.put("samplesReadyForInoculation",
                tbSampleProcessingService.countByProcessingStatus(ProcessingStatus.READY_FOR_INOCULATION));

        // Inoculated count
        stats.put("inoculatedCount", tbCultureReadingService.findInoculatedSamples().size());

        return ResponseEntity.ok(stats);
    }

    // ==================== Helper Methods ====================

    /**
     * Safely parse an Object to Integer. Handles Integer, Long, String, and Number
     * types.
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
